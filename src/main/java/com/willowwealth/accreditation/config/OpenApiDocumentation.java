package com.willowwealth.accreditation.config;

import com.willowwealth.accreditation.adapter.in.response.ErrorResponse;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

// The parts of the API description that several endpoints share. Endpoints refer to these responses by name,
// so each of them only lists the ones it can return. It extends Application only because SmallRye OpenAPI reads
// @OpenAPIDefinition from the JAX-RS application class; without @ApplicationPath it changes nothing else.
@OpenAPIDefinition(
        info = @Info(
                title = "User Accreditation API",
                version = "1.0",
                description = """
                        Administrators submit accreditation requests and decide on them; the client-facing web \
                        application reads the current status of a user's accreditations.

                        Submitting and finalizing answer 202: the change is published to Kafka first and shows up \
                        once the consumer has written it to the database. Every response carries an `X-Request-Id` \
                        header, which error bodies repeat as `trace_id`."""),
        components = @Components(
                headers = {
                        @Header(name = OpenApiDocumentation.REQUEST_ID,
                                description = "Identifies the request in the service's logs. A UUID sent by the "
                                        + "caller in the same header is kept.",
                                schema = @Schema(type = SchemaType.STRING, format = "uuid")),
                        @Header(name = OpenApiDocumentation.RETRY_AFTER,
                                description = "Seconds to wait before trying again",
                                schema = @Schema(type = SchemaType.INTEGER))
                },
                responses = {
                        @APIResponse(name = OpenApiDocumentation.INVALID_REQUEST, responseCode = "400",
                                description = "The body is not valid JSON, or a field or path parameter is invalid",
                                headers = @Header(name = "X-Request-Id", ref = OpenApiDocumentation.REQUEST_ID),
                                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                        schema = @Schema(implementation = ErrorResponse.class),
                                        examples = {
                                                @ExampleObject(name = "Invalid field", value = """
                                                        {"message": "Invalid request",
                                                         "detail": "Some fields are missing or invalid. Please check them and try again.",
                                                         "errors": [{"field": "user_id", "message": "must be exactly 32 letters or digits"}],
                                                         "trace_id": "6f0a1c32-5b6e-4a8f-9a77-1d2c3b4a5e6f"}"""),
                                                @ExampleObject(name = "Malformed JSON", value = """
                                                        {"message": "Invalid request",
                                                         "detail": "The request body is not valid JSON. Please send a valid JSON object.",
                                                         "trace_id": "6f0a1c32-5b6e-4a8f-9a77-1d2c3b4a5e6f"}""")
                                        })),
                        @APIResponse(name = OpenApiDocumentation.USER_NOT_FOUND, responseCode = "404",
                                description = "The user has no accreditations",
                                headers = @Header(name = "X-Request-Id", ref = OpenApiDocumentation.REQUEST_ID),
                                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                        schema = @Schema(implementation = ErrorResponse.class),
                                        examples = @ExampleObject(name = "User not found", value = """
                                                {"message": "User not found",
                                                 "detail": "We couldn't find any accreditation records for this user.",
                                                 "trace_id": "6f0a1c32-5b6e-4a8f-9a77-1d2c3b4a5e6f"}"""))),
                        @APIResponse(name = OpenApiDocumentation.ACCREDITATION_NOT_FOUND, responseCode = "404",
                                description = "There is no accreditation with this id",
                                headers = @Header(name = "X-Request-Id", ref = OpenApiDocumentation.REQUEST_ID),
                                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                        schema = @Schema(implementation = ErrorResponse.class),
                                        examples = @ExampleObject(name = "Accreditation not found", value = """
                                                {"message": "Accreditation not found",
                                                 "detail": "We couldn't find an accreditation with this ID.",
                                                 "trace_id": "6f0a1c32-5b6e-4a8f-9a77-1d2c3b4a5e6f"}"""))),
                        @APIResponse(name = OpenApiDocumentation.ACCREDITATION_ALREADY_PENDING, responseCode = "409",
                                description = "The user already has a PENDING accreditation",
                                headers = @Header(name = "X-Request-Id", ref = OpenApiDocumentation.REQUEST_ID),
                                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                        schema = @Schema(implementation = ErrorResponse.class),
                                        examples = @ExampleObject(name = "Already pending", value = """
                                                {"message": "Accreditation already pending",
                                                 "detail": "This user already has a pending accreditation request. Please wait until it has been reviewed.",
                                                 "trace_id": "6f0a1c32-5b6e-4a8f-9a77-1d2c3b4a5e6f"}"""))),
                        @APIResponse(name = OpenApiDocumentation.STATUS_CHANGE_NOT_ALLOWED, responseCode = "409",
                                description = "The current status does not allow this outcome",
                                headers = @Header(name = "X-Request-Id", ref = OpenApiDocumentation.REQUEST_ID),
                                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                        schema = @Schema(implementation = ErrorResponse.class),
                                        examples = {
                                                @ExampleObject(name = "Failed is final", value = """
                                                        {"message": "Status change not allowed",
                                                         "detail": "A FAILED accreditation can no longer be updated.",
                                                         "trace_id": "6f0a1c32-5b6e-4a8f-9a77-1d2c3b4a5e6f"}"""),
                                                @ExampleObject(name = "Pending cannot expire", value = """
                                                        {"message": "Status change not allowed",
                                                         "detail": "A PENDING accreditation can only be changed to CONFIRMED or FAILED.",
                                                         "trace_id": "6f0a1c32-5b6e-4a8f-9a77-1d2c3b4a5e6f"}""")
                                        })),
                        @APIResponse(name = OpenApiDocumentation.TOO_MANY_REQUESTS, responseCode = "429",
                                description = "The endpoint's rate limit is used up for now",
                                headers = {
                                        @Header(name = "X-Request-Id", ref = OpenApiDocumentation.REQUEST_ID),
                                        @Header(name = "Retry-After", ref = OpenApiDocumentation.RETRY_AFTER)
                                },
                                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                        schema = @Schema(implementation = ErrorResponse.class),
                                        examples = @ExampleObject(name = "Too many requests", value = """
                                                {"message": "Too many requests",
                                                 "detail": "You are sending requests too quickly. Please wait a moment and try again.",
                                                 "trace_id": "6f0a1c32-5b6e-4a8f-9a77-1d2c3b4a5e6f"}"""))),
                        @APIResponse(name = OpenApiDocumentation.SERVICE_UNAVAILABLE, responseCode = "503",
                                description = "Kafka or the database can't be reached at the moment",
                                headers = {
                                        @Header(name = "X-Request-Id", ref = OpenApiDocumentation.REQUEST_ID),
                                        @Header(name = "Retry-After", ref = OpenApiDocumentation.RETRY_AFTER)
                                },
                                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                        schema = @Schema(implementation = ErrorResponse.class),
                                        examples = @ExampleObject(name = "Service unavailable", value = """
                                                {"message": "Service temporarily unavailable",
                                                 "detail": "We couldn't process your request right now. Please try again in a moment.",
                                                 "trace_id": "6f0a1c32-5b6e-4a8f-9a77-1d2c3b4a5e6f"}""")))
                }))
public class OpenApiDocumentation extends Application {

    public static final String REQUEST_ID = "RequestId";
    public static final String RETRY_AFTER = "RetryAfter";

    public static final String INVALID_REQUEST = "InvalidRequest";
    public static final String USER_NOT_FOUND = "UserNotFound";
    public static final String ACCREDITATION_NOT_FOUND = "AccreditationNotFound";
    public static final String ACCREDITATION_ALREADY_PENDING = "AccreditationAlreadyPending";
    public static final String STATUS_CHANGE_NOT_ALLOWED = "StatusChangeNotAllowed";
    public static final String TOO_MANY_REQUESTS = "TooManyRequests";
    public static final String SERVICE_UNAVAILABLE = "ServiceUnavailable";
}
