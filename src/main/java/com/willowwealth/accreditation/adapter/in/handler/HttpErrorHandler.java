package com.willowwealth.accreditation.adapter.in.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.willowwealth.accreditation.adapter.in.error.ApiError;
import com.willowwealth.accreditation.adapter.in.filter.TraceIdFilter;
import com.willowwealth.accreditation.adapter.in.response.ErrorResponse;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.vertx.http.runtime.filters.Filters;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

// Requests that never reach a REST resource, such as unknown paths outside /api/v1 or bodies over the size
// limit, are answered by Vert.x. These handlers give them the same error body as everything else.
@Slf4j
@ApplicationScoped
public class HttpErrorHandler {

    private final ObjectMapper objectMapper;
    private final long maxBodySize;

    HttpErrorHandler(ObjectMapper objectMapper,
                     @ConfigProperty(name = "quarkus.http.limits.max-body-size") MemorySize maxBodySize) {
        this.objectMapper = objectMapper;
        this.maxBodySize = maxBodySize.asLongValue();
    }

    void routes(@Observes Router router) {
        router.route().last().handler(context -> respond(context, ApiError.RESOURCE_NOT_FOUND, "no route matches"));
        router.route().failureHandler(this::failed);
    }

    // Quarkus enforces the same limit a little later, but answers with an empty body.
    void filters(@Observes Filters filters) {
        filters.register(this::rejectTooLarge, 50);
    }

    private void rejectTooLarge(RoutingContext context) {
        var contentLength = context.request().getHeader(HttpHeaders.CONTENT_LENGTH);
        if (contentLength != null && Long.parseLong(contentLength) > maxBodySize) {
            context.response().putHeader(HttpHeaders.CONNECTION, HttpHeaders.CLOSE);
            respond(context, ApiError.REQUEST_TOO_LARGE, "Content-Length " + contentLength + " is over " + maxBodySize);
            return;
        }
        context.next();
    }

    private void failed(RoutingContext context) {
        var error = switch (context.statusCode()) {
            case 404 -> ApiError.RESOURCE_NOT_FOUND;
            case 405 -> ApiError.METHOD_NOT_ALLOWED;
            case 413 -> ApiError.REQUEST_TOO_LARGE;
            default -> ApiError.UNEXPECTED_ERROR;
        };
        if (error.getStatus() >= 500) {
            log.error("{} {} -> {} {}", context.request().method(), context.request().path(), error.getStatus(), error,
                    context.failure());
            write(context, error);
        } else {
            respond(context, error, String.valueOf(context.failure()));
        }
    }

    private void respond(RoutingContext context, ApiError error, String reason) {
        log.warn("{} {} -> {} {}: {}", context.request().method(), context.request().path(), error.getStatus(), error,
                reason);
        write(context, error);
    }

    private void write(RoutingContext context, ApiError error) {
        if (context.response().ended()) {
            return;
        }
        context.response()
                .setStatusCode(error.getStatus())
                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .end(toJson(ErrorResponse.of(error, context.get(TraceIdFilter.TRACE_ID))));
    }

    private String toJson(ErrorResponse body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize the error body", e);
        }
    }
}
