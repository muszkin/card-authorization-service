package pl.fairydeck.authorization.adapter.in.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.fairydeck.authorization.application.CardNotFoundException;

/**
 * Maps application and domain failures onto RFC 9457 problem details. Framework-level failures (validation,
 * missing headers, unreadable bodies) are handled by Spring's own problem details support.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(CardNotFoundException.class)
    ProblemDetail cardNotFound(CardNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidInput(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
