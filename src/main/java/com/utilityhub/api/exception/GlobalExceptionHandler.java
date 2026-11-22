package com.utilityhub.api.exception;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Translates exceptions into RFC 9457 "problem detail" responses:
 *
 * <pre>
 * { "type": "about:blank", "title": "Not Found", "status": 404,
 *   "detail": "Book not found: 42", "instance": "/api/library/books/42" }
 * </pre>
 *
 * Extending {@link ResponseEntityExceptionHandler} gives correct status codes
 * for all of Spring MVC's own exceptions for free: malformed JSON (400), wrong
 * path-variable type (400), unsupported HTTP method (405), unknown URL (404),
 * unsupported content type (415), and so on.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(PasskeyNotSetUpException.class)
    public ProblemDetail handlePasskeyNotSetUp(PasskeyNotSetUpException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Wrong password, or a passkey that didn't check out. */
    @ExceptionHandler(SignInFailedException.class)
    public ProblemDetail handleSignInFailed(SignInFailedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /** Too many wrong sign-ins lately: 429, with how many seconds to wait in {@code Retry-After}. */
    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<ProblemDetail> handleTooManyLoginAttempts(TooManyLoginAttemptsException ex) {
        long seconds = Math.max(1, (ex.getRetryAfter().toMillis() + 999) / 1000);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(seconds))
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage()));
    }

    /** A database constraint (unique, not-null, foreign key) rejected the write. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        // The root cause contains SQL and table names, so log it but don't return it.
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The request conflicts with existing data (e.g. a duplicate or a missing reference).");
    }

    /** Safety net: anything unexpected is a bug, so log the full stack trace and hide internals from the client. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    /** Bean Validation (@Valid on a @RequestBody) failures. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return handleExceptionInternal(ex, validationProblem(errors), headers, status, request);
    }

    /** Constraint violations on @PathVariable / @RequestParam (e.g. a malformed globalId). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String parameter = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error -> errors.putIfAbsent(parameter, error.getDefaultMessage()));
        }
        return handleExceptionInternal(ex, validationProblem(errors), headers, status, request);
    }

    /** Programmatic validation failures (Validator.validate(...) in a service). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return validationProblem(errors);
    }

    /**
     * The request body could not be converted to the DTO. If the cause is a
     * value of the wrong type (e.g. "SAVINGS" for an enum, "abc" for a number),
     * report the field in the same shape as validation errors. Otherwise (truly
     * malformed JSON) fall back to Spring's generic 400.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex.getCause() instanceof MismatchedInputException mismatch
                && !mismatch.getPath().isEmpty()) {
            return handleExceptionInternal(ex, mismatchProblem(mismatch), headers, status, request);
        }
        return super.handleHttpMessageNotReadable(ex, headers, status, request);
    }

    /** Same as above, for JSON converted inside a service (e.g. merging a PATCH). */
    @ExceptionHandler(MismatchedInputException.class)
    public ProblemDetail handleMismatchedInput(MismatchedInputException ex) {
        return mismatchProblem(ex);
    }

    private static ProblemDetail mismatchProblem(MismatchedInputException mismatch) {
        String field = mismatch.getPath().stream()
                .map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining("."));

        Class<?> target = mismatch.getTargetType();
        String message = target != null && target.isEnum()
                ? "must be one of " + Arrays.toString(target.getEnumConstants())
                : "has an invalid format";
        return validationProblem(Map.of(field.isEmpty() ? "body" : field, message));
    }

    private static ProblemDetail validationProblem(Map<String, String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("errors", errors);
        return problem;
    }
}
