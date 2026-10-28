package com.nexturn.lms.exception;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.*;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
/** Converts controller, interceptor and transaction failures to consistent JSON responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now().toString(),
                "status", status.value(), "message", message, "path", request.getRequestURI()));
    }
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> api(ApiException ex, HttpServletRequest request) {
        return response(ex.getStatus(), ex.getMessage(), request);
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
        HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        MissingRequestValueException.class, org.springframework.data.mapping.PropertyReferenceException.class})
    public ResponseEntity<Map<String, Object>> invalid(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Invalid request data; check required fields and types", request);
    }
    @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
    public ResponseEntity<Map<String, Object>> conflict(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "Data conflict; refresh before retrying", request);
    }
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> missing(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "Endpoint not found", request);
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> method(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported", request);
    }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> media(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Use application/json", request);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception ex, HttpServletRequest request) {
        LOG.error("Unhandled backend failure", ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }
}
