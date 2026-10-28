package com.nexturn.lms.entity;
import jakarta.persistence.*;
import java.time.Instant;
/** Stores only a SHA-256 token digest; raw bearer tokens are returned once at login. */
@Entity
@Table(name = "auth_session")
public class AuthSession {
    @Id @Column(length = 64) private String tokenHash;
    @Column(nullable = false, length = 50) private String username;
    @Column(nullable = false) private Instant expiresAt;
    public AuthSession() { }
    public AuthSession(String tokenHash, String username, Instant expiresAt) {
        this.tokenHash = tokenHash; this.username = username; this.expiresAt = expiresAt;
    }
    /** Returns token hash. */
    public String getTokenHash() { return tokenHash; }
    /** Returns username. */
    public String getUsername() { return username; }
    /** Returns expires at. */
    public Instant getExpiresAt() { return expiresAt; }
}
