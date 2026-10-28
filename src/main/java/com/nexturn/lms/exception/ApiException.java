package com.nexturn.lms.exception;
import org.springframework.http.HttpStatus;
/** Carries a safe client message and HTTP status to the centralized handler. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    public ApiException(HttpStatus status, String message) { super(message); this.status = status; }
    /** Returns status. */
    public HttpStatus getStatus() { return status; }
}
