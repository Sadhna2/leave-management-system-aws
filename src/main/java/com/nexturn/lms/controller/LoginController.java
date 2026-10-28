package com.nexturn.lms.controller;
import com.nexturn.lms.dto.*;
import com.nexturn.lms.entity.Login;
import com.nexturn.lms.service.LoginService;
import jakarta.validation.Valid;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
/** Provides login, current identity and logout without public role registration. */
@RestController
@RequestMapping("/api/auth")
public class LoginController {
    private final LoginService service;
    public LoginController(LoginService service) { this.service = service; }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) throws NoSuchAlgorithmException {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.login(request));
    }
    @GetMapping("/me")
    public Map<String, Object> me(@RequestAttribute("authenticatedUser") Login user) {
        return Map.of("username", user.getUsername(), "empId", user.getEmpId(), "role", user.getRole());
    }
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String header) throws NoSuchAlgorithmException { service.logout(header); }
}
