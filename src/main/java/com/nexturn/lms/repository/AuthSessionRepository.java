package com.nexturn.lms.repository;
import com.nexturn.lms.entity.AuthSession;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
/** Persists revocable tokens across independent Lambda instances. */
public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {
    long deleteByExpiresAtBefore(Instant now);
}
