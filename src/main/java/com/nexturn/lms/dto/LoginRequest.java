package com.nexturn.lms.dto;
import jakarta.validation.constraints.*;
/** Credentials accepted only by the login endpoint. */
public record LoginRequest(@NotBlank @Size(max=50) String username,
                           @NotBlank @Size(max=72) String password) { }
