package com.devikapps.caverne.modules.common;

import com.devikapps.caverne.modules.common.logging.RequestTraceFilter;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .orElse("Request validation failed");
    log.warn("Validation failed: {}", message);
    return build(HttpStatus.UNPROCESSABLE_CONTENT, message);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
    log.warn("Illegal argument: {}", exception.getMessage());
    return build(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
    HttpStatusCode status = exception.getStatusCode();
    String message =
        exception.getReason() == null
            ? HttpStatus.valueOf(status.value()).getReasonPhrase()
            : exception.getReason();
    if (status.is5xxServerError()) {
      log.error("ResponseStatusException {}: {}", status.value(), message, exception);
    } else {
      log.warn("ResponseStatusException {}: {}", status.value(), message);
    }
    return build(status, message);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiError> handleAuthentication(AuthenticationException exception) {
    log.warn("Authentication failure: {}", exception.getMessage());
    return build(HttpStatus.UNAUTHORIZED, "Authentication required");
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception) {
    log.warn("Access denied: {}", exception.getMessage());
    return build(HttpStatus.FORBIDDEN, "Access is forbidden");
  }

  /**
   * Catch-all: any exception not handled above is a technical failure. Log the full stack trace
   * with the traceId (via MDC) and return a generic 500 to the client — never expose the stack
   * trace.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
    log.error("Unhandled exception: {}", exception.getMessage(), exception);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
  }

  private ResponseEntity<ApiError> build(HttpStatusCode status, String message) {
    String traceId = MDC.get(RequestTraceFilter.MDC_TRACE_ID);
    return ResponseEntity.status(status)
        .body(new ApiError(status.value(), message, traceId, Instant.now()));
  }
}
