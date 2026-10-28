package com.nexturn.lms.dto;
import java.time.Instant;
import com.nexturn.lms.enums.Role;
/** One-time token response; callers send it in the Authorization bearer header. */
public record LoginResponse(String accessToken, String tokenType, Instant expiresAt,
                            String username, Integer empId, Role role) { }
