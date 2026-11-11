package com.willowwealth.accreditation.adapter.in.filter;

import io.quarkus.vertx.http.runtime.filters.Filters;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Pattern;

@ApplicationScoped
public class TraceIdFilter {

    public static final String TRACE_ID = "trace_id";

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final Pattern UUID_PATTERN =
            Pattern.compile("\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}");

    // Runs as a Vert.x filter rather than a REST filter, so requests that never reach a resource get an id too.
    void register(@Observes Filters filters) {
        filters.register(this::tag, 100);
    }

    private void tag(RoutingContext context) {
        var traceId = traceIdFor(context.request().getHeader(REQUEST_ID_HEADER));
        context.put(TRACE_ID, traceId);
        MDC.put(TRACE_ID, traceId);
        context.response().putHeader(REQUEST_ID_HEADER, traceId);
        context.next();
    }

    // A caller's id is kept so both sides log the same value, but only if it is a UUID:
    // anything else would end up verbatim in our logs and responses.
    private static String traceIdFor(String requestId) {
        if (requestId != null && UUID_PATTERN.matcher(requestId).matches()) {
            return requestId;
        }
        return UUID.randomUUID().toString();
    }
}
