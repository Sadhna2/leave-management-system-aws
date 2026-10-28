package com.nexturn.lms.dto;
import jakarta.validation.constraints.*;
/** Required decision explanation; manager identity comes exclusively from the bearer token. */
public record ApprovalRequest(@NotBlank @Size(max=200) String comment) { }
