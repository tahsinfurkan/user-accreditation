package com.willowwealth.accreditation.adapter.in.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Every error the API returns is built from this list, so no internal text can reach a client.
@Getter
@RequiredArgsConstructor
public enum ApiError {

    VALIDATION_FAILED(400, "Invalid request", "Some fields are missing or invalid. Please check them and try again."),
    MALFORMED_JSON(400, "Invalid request", "The request body is not valid JSON. Please send a valid JSON object."),
    RESOURCE_NOT_FOUND(404, "Resource not found", "The requested URL does not exist."),
    USER_NOT_FOUND(404, "User not found", "We couldn't find any accreditation records for this user."),
    ACCREDITATION_NOT_FOUND(404, "Accreditation not found", "We couldn't find an accreditation with this ID."),
    METHOD_NOT_ALLOWED(405, "Method not allowed", "This endpoint does not support the requested HTTP method."),
    NOT_ACCEPTABLE(406, "Not acceptable", "This endpoint only returns application/json."),
    ACCREDITATION_ALREADY_PENDING(409, "Accreditation already pending",
            "This user already has a pending accreditation request. Please wait until it has been reviewed."),
    PENDING_CHANGE_NOT_ALLOWED(409, "Status change not allowed",
            "A PENDING accreditation can only be changed to CONFIRMED or FAILED."),
    CONFIRMED_CHANGE_NOT_ALLOWED(409, "Status change not allowed",
            "A CONFIRMED accreditation can only be changed to EXPIRED."),
    FAILED_ACCREDITATION_IS_FINAL(409, "Status change not allowed", "A FAILED accreditation can no longer be updated."),
    EXPIRED_ACCREDITATION_IS_FINAL(409, "Status change not allowed",
            "An EXPIRED accreditation can no longer be updated."),
    REQUEST_TOO_LARGE(413, "Request too large", "The request body is larger than the allowed limit."),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported media type", "Please send the request body as application/json."),
    RATE_LIMITED(429, "Too many requests", "You are sending requests too quickly. Please wait a moment and try again."),
    UNEXPECTED_ERROR(500, "Something went wrong", "An unexpected error occurred. Please try again later."),
    SERVICE_UNAVAILABLE(503, "Service temporarily unavailable",
            "We couldn't process your request right now. Please try again in a moment."),
    // The client gets the same text; the separate names tell the logs that PostgreSQL could not be reached, or that
    // a circuit breaker turned the request away.
    DATABASE_UNAVAILABLE(503, "Service temporarily unavailable",
            "We couldn't process your request right now. Please try again in a moment."),
    CIRCUIT_OPEN(503, "Service temporarily unavailable",
            "We couldn't process your request right now. Please try again in a moment.");

    private final int status;
    private final String message;
    private final String detail;
}
