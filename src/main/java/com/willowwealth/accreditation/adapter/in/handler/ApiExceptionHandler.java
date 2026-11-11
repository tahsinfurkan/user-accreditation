package com.willowwealth.accreditation.adapter.in.handler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.willowwealth.accreditation.adapter.in.error.ApiError;
import com.willowwealth.accreditation.adapter.in.filter.TraceIdFilter;
import com.willowwealth.accreditation.adapter.in.response.ErrorResponse;
import com.willowwealth.accreditation.adapter.in.response.FieldError;
import com.willowwealth.accreditation.application.exception.EventPublicationException;
import com.willowwealth.accreditation.domain.exception.AccreditationException;
import com.willowwealth.accreditation.domain.exception.UserNotFoundException;
import io.vertx.core.http.HttpServerRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ApiExceptionHandler {

    private static final PropertyNamingStrategies.NamingBase SNAKE_CASE = new PropertyNamingStrategies.SnakeCaseStrategy();
    private static final int RETRY_AFTER_SECONDS = 5;

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> invalidFields(ConstraintViolationException exception, HttpServerRequest request) {
        var errors = exception.getConstraintViolations().stream()
                .map(violation -> new FieldError(fieldOf(violation), violation.getMessage()))
                .sorted(Comparator.comparing(FieldError::field))
                .toList();
        var reason = errors.stream()
                .map(error -> error.field() + " " + error.message())
                .collect(Collectors.joining(", "));
        return clientError(request, ApiError.VALIDATION_FAILED, errors, reason);
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> unreadableField(MismatchedInputException exception, HttpServerRequest request) {
        var field = pathOf(exception);
        // Without a path the JSON as a whole is wrong, e.g. there is more data after the closing brace.
        if (field.isEmpty()) {
            return clientError(request, ApiError.MALFORMED_JSON, null, exception.getOriginalMessage());
        }
        var errors = List.of(new FieldError(field, problemWith(exception)));
        return clientError(request, ApiError.VALIDATION_FAILED, errors, exception.getOriginalMessage());
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> ruleBroken(AccreditationException exception, HttpServerRequest request) {
        var error = switch (exception) {
            case UserNotFoundException notFound -> ApiError.USER_NOT_FOUND;
        };
        return clientError(request, error, null, exception.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> httpError(WebApplicationException exception, HttpServerRequest request) {
        var error = switch (exception.getResponse().getStatus()) {
            // Quarkus wraps JSON syntax errors, such as a missing brace or a duplicate key, in a plain 400.
            case 400 -> exception.getCause() instanceof JsonParseException
                    ? ApiError.MALFORMED_JSON
                    : ApiError.VALIDATION_FAILED;
            case 404 -> ApiError.RESOURCE_NOT_FOUND;
            case 405 -> ApiError.METHOD_NOT_ALLOWED;
            case 406 -> ApiError.NOT_ACCEPTABLE;
            case 413 -> ApiError.REQUEST_TOO_LARGE;
            case 415 -> ApiError.UNSUPPORTED_MEDIA_TYPE;
            default -> null;
        };
        if (error == null) {
            return unexpected(exception, request);
        }
        var reason = exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage();
        return clientError(request, error, null, reason);
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> unavailable(EventPublicationException exception, HttpServerRequest request) {
        var error = ApiError.SERVICE_UNAVAILABLE;
        log.error("{} {} -> {} {}: {}", request.method(), request.path(), error.getStatus(), error,
                exception.getMessage(), exception);
        return response(error, null)
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .build();
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> unexpected(Throwable exception, HttpServerRequest request) {
        var error = ApiError.UNEXPECTED_ERROR;
        log.error("{} {} -> {} {}: {}", request.method(), request.path(), error.getStatus(), error,
                exception.getMessage(), exception);
        return response(error, null).build();
    }

    private static RestResponse<ErrorResponse> clientError(HttpServerRequest request, ApiError error,
                                                           List<FieldError> errors, String reason) {
        log.warn("{} {} -> {} {}: {}", request.method(), request.path(), error.getStatus(), error, reason);
        return response(error, errors).build();
    }

    private static RestResponse.ResponseBuilder<ErrorResponse> response(ApiError error, List<FieldError> errors) {
        var body = ErrorResponse.of(error, errors, MDC.get(TraceIdFilter.TRACE_ID));
        // Set explicitly so the error body is JSON even when the client asked for another type.
        return RestResponse.ResponseBuilder.<ErrorResponse>create(error.getStatus())
                .entity(body)
                .type(MediaType.APPLICATION_JSON_TYPE);
    }

    // Violations name the Java path (submit.body.document.mimeType); clients only know the JSON names.
    // A violation on a parameter itself, like a missing body or a malformed path parameter, keeps its name.
    private static String fieldOf(ConstraintViolation<?> violation) {
        var properties = new ArrayList<String>();
        var parameter = "";
        for (var node : violation.getPropertyPath()) {
            if (node.getKind() == ElementKind.PARAMETER) {
                parameter = node.getName();
            } else if (node.getKind() == ElementKind.PROPERTY) {
                properties.add(SNAKE_CASE.translate(node.getName()));
            }
        }
        return properties.isEmpty() ? parameter : String.join(".", properties);
    }

    private static String pathOf(JsonMappingException exception) {
        return exception.getPath().stream()
                .map(reference -> reference.getFieldName() != null
                        ? reference.getFieldName()
                        : String.valueOf(reference.getIndex()))
                .collect(Collectors.joining("."));
    }

    private static String problemWith(MismatchedInputException exception) {
        return switch (exception) {
            case UnrecognizedPropertyException unknown -> "is not a known field";
            case InvalidFormatException invalid when invalid.getTargetType() != null && invalid.getTargetType().isEnum() ->
                    "must be one of " + Arrays.stream(invalid.getTargetType().getEnumConstants())
                            .map(String::valueOf)
                            .collect(Collectors.joining(", "));
            default -> "has the wrong type";
        };
    }
}
