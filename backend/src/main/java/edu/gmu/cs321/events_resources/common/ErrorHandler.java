package edu.gmu.cs321.events_resources.common;

import org.slf4j.Logger;  // writes messages to the server's console/log
import org.slf4j.LoggerFactory;    // creates a Logger for this class
import org.springframework.http.HttpStatus;    // named status codes: NOT_FOUND = 404, CONFLICT = 409, ...
import org.springframework.http.ResponseEntity;      // a full HTTP response: status code + body
import org.springframework.web.HttpRequestMethodNotSupportedException;      // Spring throws this for a wrong method (405)
import org.springframework.web.bind.annotation.ExceptionHandler;      // marks a method that catches an exception
import org.springframework.web.bind.annotation.RestControllerAdvice;     // marks this class as the error handler for ALL controllers
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;    // Spring throws this for a wrong type in the URL (400)
import org.springframework.web.servlet.resource.NoResourceFoundException;   // Spring throws this for a URL that doesn't exist (404)
import org.springframework.http.converter.HttpMessageNotReadableException;   // Spring throws this when the request body isn't valid JSON (400)
/**
 * Catches exceptions thrown anywhere in a controller or service and turns them into
 * the standard error JSON. Because of this class, no endpoint has to build error
 * responses itself: it just throws, and this class answers.
 */
@RestControllerAdvice
public class ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    /** Our own errors: 400, 401, 403, 404, 409 ... thrown on purpose by our code. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException e) {
        return respond(e.getStatus(), e.getMessage(), e.getCode(), e.getDetail());
    }

    /** A URL value has the wrong type, e.g. /events/abc/register when the id must be a number. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return respond(HttpStatus.BAD_REQUEST, "Invalid value in the request", "VALIDATION_ERROR",
                e.getName() + ": must be a valid " + typeName(e));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleBadJson(HttpMessageNotReadableException e) {
        return respond(HttpStatus.BAD_REQUEST, "The request body is not valid JSON", "INVALID_JSON",
                "Check for a missing body, a typo in the JSON, or a value of the wrong type");
    }

    /** No endpoint exists at this URL. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoEndpoint(NoResourceFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "Not found", "NOT_FOUND", "/" + e.getResourcePath());
    }

    /** The URL exists, but not for this method, e.g. DELETE on a GET-only endpoint. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleWrongMethod(HttpRequestMethodNotSupportedException e) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", "METHOD_NOT_ALLOWED",
                e.getMethod() + " is not supported here");
    }
    //SAFETY NET 
    /** Anything we didn't expect (a bug, the database is down...). Never show the user the Java details. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e) {
        log.error("Unexpected error", e); // the full stack trace goes to the server log for us to debug
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", "INTERNAL_ERROR", "");
    }
    /** Builds the actual response: the status code + an ApiError body (which Spring turns into JSON). */
    private ResponseEntity<ApiError> respond(HttpStatus status, String error, String code, String detail) {
        return ResponseEntity.status(status).body(new ApiError(error, code, detail));
    }
    /** Gets a readable type name, e.g. "long" or "Integer", for the type-mismatch message. */
    private String typeName(MethodArgumentTypeMismatchException e) {
        Class<?> type = e.getRequiredType();
        // Ternary operator: condition ? valueIfTrue : valueIfFalse
        // getRequiredType() can be null in rare cases, so fall back to the word "value".
        return type == null ? "value" : type.getSimpleName();
    }
}
