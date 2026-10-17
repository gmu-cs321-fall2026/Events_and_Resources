package edu.gmu.cs321.events_resources.common;

import org.springframework.http.HttpStatus;

/**
 * Throw this (or one of its subclasses) from any service when a request can't be completed.
 * ErrorHandler catches it and turns it into an HTTP response with the right status code.
 *
 * Example: throw new ApiException(HttpStatus.CONFLICT, "EVENT_FULL", "This event is full", "");
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String detail;

    public ApiException(HttpStatus status, String code, String message, String detail) {
        super(message); // RuntimeException stores the message; we read it back with getMessage()
        this.status = status;
        this.code = code;
        this.detail = detail;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getDetail() {
        return detail;
    }

    // ---- Shortcuts for the errors we use most. Each one fills in the status and code for you. ----

    /** 404: the thing asked for doesn't exist. */
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message, "");
    }

    /** 409: the request clashes with the current state, e.g. EVENT_FULL or ALREADY_REGISTERED. */
    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message, "");
    }

    /** 400: the request itself is wrong, e.g. a missing field. detail names the bad field(s). */
    public static ApiException badRequest(String message, String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, detail);
    }

    /** 401: we don't know who you are (no token, or a bad one). */
    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message, "");
    }

    /** 403: we know who you are, but you're not allowed to do this. */
    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message, "");
    }
}
