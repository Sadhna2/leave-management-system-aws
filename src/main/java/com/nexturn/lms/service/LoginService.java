package com.nexturn.lms.service;
import com.nexturn.lms.dto.*;
import com.nexturn.lms.entity.*;
import com.nexturn.lms.exception.ApiException;
import com.nexturn.lms.repository.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/** Checks BCrypt credentials and issues expiring, database-backed opaque bearer tokens. */
@Service
public class LoginService {
    private final LoginRepository accounts;
    private final AuthSessionRepository sessions;
    private final long tokenSeconds;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    private final String dummyHash = encoder.encode(UUID.randomUUID().toString());
    private final SecureRandom random = new SecureRandom();
    public LoginService(LoginRepository accounts, AuthSessionRepository sessions,
                        @Value("${app.auth.token-seconds:3600}") long tokenSeconds) {
        this.accounts = accounts; this.sessions = sessions; this.tokenSeconds = tokenSeconds;
        if (tokenSeconds < 60 || tokenSeconds > 86400) throw new IllegalArgumentException("Invalid token lifetime");
    }
    @Transactional
    public LoginResponse login(LoginRequest request) throws NoSuchAlgorithmException {
        if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password exceeds BCrypt's 72-byte limit");
        Login account = accounts.findById(request.username()).orElse(null);
        boolean matches = encoder.matches(request.password(), account == null ? dummyHash : account.getPassword());
        if (account == null || !matches) throw unauthorized();
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiry = Instant.now().plusSeconds(tokenSeconds);
        sessions.deleteByExpiresAtBefore(Instant.now());
        sessions.save(new AuthSession(digest(token), account.getUsername(), expiry));
        return new LoginResponse(token, "Bearer", expiry, account.getUsername(), account.getEmpId(), account.getRole());
    }
    @Transactional(readOnly = true)
    public Login authenticate(String header) throws NoSuchAlgorithmException {
        String token = token(header);
        AuthSession session = sessions.findById(digest(token)).orElseThrow(this::unauthorized);
        if (!session.getExpiresAt().isAfter(Instant.now())) throw unauthorized();
        return accounts.findById(session.getUsername()).orElseThrow(this::unauthorized);
    }
    @Transactional
    public void logout(String header) throws NoSuchAlgorithmException { sessions.deleteById(digest(token(header))); }
    private String token(String header) {
        if (header == null || !header.startsWith("Bearer ") || !header.substring(7).matches("[A-Za-z0-9_-]{43}"))
            throw unauthorized();
        return header.substring(7);
    }
    private String digest(String token) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
    }
    private ApiException unauthorized() { return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials or expired token"); }
}
