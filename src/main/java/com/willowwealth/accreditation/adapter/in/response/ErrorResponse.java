package com.willowwealth.accreditation.adapter.in.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willowwealth.accreditation.adapter.in.error.ApiError;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

// The examples of message and detail are every text in ApiError; a new entry there belongs here as well.
@Schema(description = "The body of every error response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(description = "What went wrong, in a few words", examples = {
                "Invalid request",
                "Resource not found",
                "User not found",
                "Accreditation not found",
                "Method not allowed",
                "Not acceptable",
                "Accreditation already pending",
                "Status change not allowed",
                "Request too large",
                "Unsupported media type",
                "Too many requests",
                "Something went wrong",
                "Service temporarily unavailable"})
        String message,

        @Schema(description = "What the caller can do about it", examples = {
                "Some fields are missing or invalid. Please check them and try again.",
                "The request body is not valid JSON. Please send a valid JSON object.",
                "The requested URL does not exist.",
                "We couldn't find any accreditation records for this user.",
                "We couldn't find an accreditation with this ID.",
                "This endpoint does not support the requested HTTP method.",
                "This endpoint only returns application/json.",
                "This user already has a pending accreditation request. Please wait until it has been reviewed.",
                "A PENDING accreditation can only be changed to CONFIRMED or FAILED.",
                "A CONFIRMED accreditation can only be changed to EXPIRED.",
                "A FAILED accreditation can no longer be updated.",
                "An EXPIRED accreditation can no longer be updated.",
                "The request body is larger than the allowed limit.",
                "Please send the request body as application/json.",
                "You are sending requests too quickly. Please wait a moment and try again.",
                "An unexpected error occurred. Please try again later.",
                "We couldn't process your request right now. Please try again in a moment."})
        String detail,

        @Schema(description = "The fields that are missing or invalid; only for 400 responses")
        List<FieldError> errors,

        @Schema(description = "The same value as the X-Request-Id header",
                examples = {"6f0a1c32-5b6e-4a8f-9a77-1d2c3b4a5e6f", "b9e2d4f6-8a1c-4e3b-9d5f-7c0a2e4b6d8f"})
        String traceId) {

    public static ErrorResponse of(ApiError error, String traceId) {
        return new ErrorResponse(error.getMessage(), error.getDetail(), null, traceId);
    }

    public static ErrorResponse of(ApiError error, List<FieldError> errors, String traceId) {
        return new ErrorResponse(error.getMessage(), error.getDetail(), errors, traceId);
    }
}
