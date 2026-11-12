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
import com.willowwealth.accreditation.config.CircuitBreakerConfig;
import com.willowwealth.accreditation.domain.exception.AccreditationException;
import com.willowwealth.accreditation.domain.exception.AccreditationNotFoundException;
import com.willowwealth.accreditation.domain.exception.InvalidStatusTransitionException;
import com.willowwealth.accreditation.domain.exception.PendingAccreditationExistsException;
import com.willowwealth.accreditation.domain.exception.UserNotFoundException;
import io.smallrye.faulttolerance.api.RateLimitException;
import io.vertx.core.http.HttpServerRequest;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.MDC;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ApiExceptionHandler {

    private static final PropertyNamingStrategies.NamingBase SNAKE_CASE = new PropertyNamingStrategies.SnakeCaseStrategy();
    private static final int RETRY_AFTER_SECONDS = 5;

    // A request turned away by an open circuit is told to come back once the circuit lets calls through again.
    private final long circuitRetryAfterSeconds;

    ApiExceptionHandler(CircuitBreakerConfig circuitBreakers) {
        this.circuitRetryAfterSeconds = circuitBreakers.delay().toSeconds();
    }

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
            case AccreditationNotFoundException notFound -> ApiError.ACCREDITATION_NOT_FOUND;
            case PendingAccreditationExistsException pending -> ApiError.ACCREDITATION_ALREADY_PENDING;
            // The client is told what its accreditation's current status allows.
            case InvalidStatusTransitionException invalid -> switch (invalid.getCurrentStatus()) {
                case PENDING -> ApiError.PENDING_CHANGE_NOT_ALLOWED;
                case CONFIRMED -> ApiError.CONFIRMED_CHANGE_NOT_ALLOWED;
                case FAILED -> ApiError.FAILED_ACCREDITATION_IS_FINAL;
                case EXPIRED -> ApiError.EXPIRED_ACCREDITATION_IS_FINAL;
            };
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
        // The publisher reports an open Kafka circuit breaker this way as well.
        if (exception.getCause() instanceof CircuitBreakerOpenException) {
            return turnedAway(request, exception.getMessage());
        }
        var error = ApiError.SERVICE_UNAVAILABLE;
        log.error("{} {} -> {} {}: {}", request.method(), request.path(), error.getStatus(), error,
                exception.getMessage(), exception);
        return response(error, null)
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .build();
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> circuitOpen(CircuitBreakerOpenException exception, HttpServerRequest request) {
        return turnedAway(request, exception.getMessage());
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> rateLimited(RateLimitException exception, HttpServerRequest request) {
        var error = ApiError.RATE_LIMITED;
        // Rounded up, so a client that waits this long finds the limit open again.
        var retryAfterSeconds = Math.max(1, (exception.getRetryAfterMillis() + 999) / 1000);
        log.warn("{} {} -> {} {}: retry after {} s", request.method(), request.path(), error.getStatus(), error,
                retryAfterSeconds);
        return response(error, null)
                .header(HttpHeaders.RETRY_AFTER, retryAfterSeconds)
                .build();
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> databaseFailed(PersistenceException exception, HttpServerRequest request) {
        if (!unreachable(exception)) {
            return unexpected(exception, request);
        }
        var error = ApiError.DATABASE_UNAVAILABLE;
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

    // The failures that opened the circuit were logged as errors already, so the requests it turns away only get
    // a warning each.
    private RestResponse<ErrorResponse> turnedAway(HttpServerRequest request, String reason) {
        var error = ApiError.CIRCUIT_OPEN;
        log.warn("{} {} -> {} {}: {}", request.method(), request.path(), error.getStatus(), error, reason);
        return response(error, null)
                .header(HttpHeaders.RETRY_AFTER, circuitRetryAfterSeconds)
                .build();
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

    // SQLState class 08 is a connection problem and 57P an operator stopping the server. Not getting a connection from
    // the pool in time comes without a SQLState, only with Hibernate's wording, and means the same to the client.
    // Either way the request can work once PostgreSQL is back; any other database error is a bug.
    private static boolean unreachable(Throwable exception) {
        for (var cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getSQLState() != null
                    && (sql.getSQLState().startsWith("08") || sql.getSQLState().startsWith("57P"))) {
                return true;
            }
        }
        return exception.getMessage() != null && exception.getMessage().startsWith("Unable to acquire JDBC Connection");
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
