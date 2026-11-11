package com.willowwealth.accreditation.adapter.in.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willowwealth.accreditation.adapter.in.error.ApiError;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String message, String detail, List<FieldError> errors, String traceId) {

    public static ErrorResponse of(ApiError error, String traceId) {
        return new ErrorResponse(error.getMessage(), error.getDetail(), null, traceId);
    }

    public static ErrorResponse of(ApiError error, List<FieldError> errors, String traceId) {
        return new ErrorResponse(error.getMessage(), error.getDetail(), errors, traceId);
    }
}
