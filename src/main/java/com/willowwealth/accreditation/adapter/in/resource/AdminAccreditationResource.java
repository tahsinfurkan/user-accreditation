package com.willowwealth.accreditation.adapter.in.resource;

import com.willowwealth.accreditation.adapter.in.mapper.AccreditationRequestMapper;
import com.willowwealth.accreditation.adapter.in.mapper.AccreditationResponseMapper;
import com.willowwealth.accreditation.adapter.in.request.FinalizeAccreditationRequest;
import com.willowwealth.accreditation.adapter.in.request.SubmitAccreditationRequest;
import com.willowwealth.accreditation.adapter.in.response.AccreditationHistoryResponse;
import com.willowwealth.accreditation.adapter.in.response.AccreditationIdResponse;
import com.willowwealth.accreditation.application.port.in.query.GetAccreditationHistoryQuery;
import com.willowwealth.accreditation.application.port.in.usecase.FinalizeAccreditationUseCase;
import com.willowwealth.accreditation.application.port.in.usecase.GetAccreditationHistoryUseCase;
import com.willowwealth.accreditation.application.port.in.usecase.SubmitAccreditationUseCase;
import com.willowwealth.accreditation.domain.model.AccreditationId;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.faulttolerance.api.ApplyGuard;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.UUID;

import static com.willowwealth.accreditation.config.OpenApiDocumentation.ACCREDITATION_ALREADY_PENDING;
import static com.willowwealth.accreditation.config.OpenApiDocumentation.ACCREDITATION_NOT_FOUND;
import static com.willowwealth.accreditation.config.OpenApiDocumentation.INVALID_REQUEST;
import static com.willowwealth.accreditation.config.OpenApiDocumentation.REQUEST_ID;
import static com.willowwealth.accreditation.config.OpenApiDocumentation.SERVICE_UNAVAILABLE;
import static com.willowwealth.accreditation.config.OpenApiDocumentation.STATUS_CHANGE_NOT_ALLOWED;
import static com.willowwealth.accreditation.config.OpenApiDocumentation.TOO_MANY_REQUESTS;
import static com.willowwealth.accreditation.config.RateLimitProducer.FINALIZE_RATE_LIMIT;
import static com.willowwealth.accreditation.config.RateLimitProducer.HISTORY_RATE_LIMIT;
import static com.willowwealth.accreditation.config.RateLimitProducer.SUBMIT_RATE_LIMIT;

// Both endpoints answer 202 rather than 200 or 201: the change is recorded asynchronously, once its event is consumed.
@Tag(name = "Admin", description = "Used by administrators to submit accreditation requests and decide on them")
@Path("/user/accreditation")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class AdminAccreditationResource {

    private final SubmitAccreditationUseCase submitAccreditation;
    private final FinalizeAccreditationUseCase finalizeAccreditation;
    private final GetAccreditationHistoryUseCase getAccreditationHistory;
    private final AccreditationRequestMapper requestMapper;
    private final AccreditationResponseMapper responseMapper;

    @POST
    @RunOnVirtualThread
    @ApplyGuard(SUBMIT_RATE_LIMIT)
    @Operation(summary = "Submit an accreditation request",
            description = "Stores the document and publishes the request. The accreditation shows up as PENDING once "
                    + "its event is consumed. A user can have only one PENDING accreditation at a time.")
    @APIResponse(responseCode = "202", description = "Accepted",
            headers = @Header(name = "X-Request-Id", ref = REQUEST_ID),
            content = @Content(schema = @Schema(implementation = AccreditationIdResponse.class)))
    @APIResponse(responseCode = "400", ref = INVALID_REQUEST)
    @APIResponse(responseCode = "409", ref = ACCREDITATION_ALREADY_PENDING)
    @APIResponse(responseCode = "429", ref = TOO_MANY_REQUESTS)
    @APIResponse(responseCode = "503", ref = SERVICE_UNAVAILABLE)
    public RestResponse<AccreditationIdResponse> submit(
            @Valid @NotNull(message = "must not be empty") SubmitAccreditationRequest body) {
        var id = submitAccreditation.submit(requestMapper.toCommand(body));
        return RestResponse.status(RestResponse.Status.ACCEPTED, new AccreditationIdResponse(id.value()));
    }

    // The id is validated as a string: declared as a UUID, a malformed one would be answered with 404, not 400.
    @PUT
    @Path("{accreditationId}")
    @RunOnVirtualThread
    @ApplyGuard(FINALIZE_RATE_LIMIT)
    @Operation(summary = "Finalize an accreditation",
            description = "Publishes the outcome; the accreditation shows it once its event is consumed. Allowed "
                    + "changes: PENDING to CONFIRMED or FAILED, and CONFIRMED to EXPIRED. FAILED and EXPIRED are final.")
    @APIResponse(responseCode = "202", description = "Accepted",
            headers = @Header(name = "X-Request-Id", ref = REQUEST_ID),
            content = @Content(schema = @Schema(implementation = AccreditationIdResponse.class)))
    @APIResponse(responseCode = "400", ref = INVALID_REQUEST)
    @APIResponse(responseCode = "404", ref = ACCREDITATION_NOT_FOUND)
    @APIResponse(responseCode = "409", ref = STATUS_CHANGE_NOT_ALLOWED)
    @APIResponse(responseCode = "429", ref = TOO_MANY_REQUESTS)
    @APIResponse(responseCode = "503", ref = SERVICE_UNAVAILABLE)
    public RestResponse<AccreditationIdResponse> finalizeAccreditation(
            @Parameter(description = "Id of the accreditation", example = "87bb6030-458e-11ed-b023-039b275a916a")
            @RestPath @Pattern(regexp = AccreditationId.PATTERN, message = "must be a valid UUID") String accreditationId,
            @Valid @NotNull(message = "must not be empty") FinalizeAccreditationRequest body) {
        var id = UUID.fromString(accreditationId);
        finalizeAccreditation.finalizeAccreditation(requestMapper.toCommand(id, body));
        return RestResponse.status(RestResponse.Status.ACCEPTED, new AccreditationIdResponse(id));
    }

    @GET
    @Path("{accreditationId}/history")
    @RunOnVirtualThread
    @ApplyGuard(HISTORY_RATE_LIMIT)
    @Operation(summary = "Get the status history of an accreditation",
            description = "Every applied status change, oldest first, starting with the submission.")
    @APIResponse(responseCode = "200", description = "The status history",
            headers = @Header(name = "X-Request-Id", ref = REQUEST_ID),
            content = @Content(schema = @Schema(implementation = AccreditationHistoryResponse.class)))
    @APIResponse(responseCode = "400", ref = INVALID_REQUEST)
    @APIResponse(responseCode = "404", ref = ACCREDITATION_NOT_FOUND)
    @APIResponse(responseCode = "429", ref = TOO_MANY_REQUESTS)
    @APIResponse(responseCode = "503", ref = SERVICE_UNAVAILABLE)
    public AccreditationHistoryResponse history(
            @Parameter(description = "Id of the accreditation", example = "87bb6030-458e-11ed-b023-039b275a916a")
            @RestPath @Pattern(regexp = AccreditationId.PATTERN, message = "must be a valid UUID") String accreditationId) {
        var query = new GetAccreditationHistoryQuery(new AccreditationId(UUID.fromString(accreditationId)));
        return responseMapper.toHistoryResponse(getAccreditationHistory.get(query));
    }
}
