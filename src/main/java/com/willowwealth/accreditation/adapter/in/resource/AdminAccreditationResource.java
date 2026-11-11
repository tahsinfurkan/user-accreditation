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
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.UUID;

// Both endpoints answer 202 rather than 200 or 201: the change is recorded asynchronously, once its event is consumed.
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
    public RestResponse<AccreditationIdResponse> submit(
            @Valid @NotNull(message = "must not be empty") SubmitAccreditationRequest body) {
        var id = submitAccreditation.submit(requestMapper.toCommand(body));
        return RestResponse.status(RestResponse.Status.ACCEPTED, new AccreditationIdResponse(id.value()));
    }

    // The id is validated as a string: declared as a UUID, a malformed one would be answered with 404, not 400.
    @PUT
    @Path("{accreditationId}")
    @RunOnVirtualThread
    public RestResponse<AccreditationIdResponse> finalizeAccreditation(
            @RestPath @Pattern(regexp = AccreditationId.PATTERN, message = "must be a valid UUID") String accreditationId,
            @Valid @NotNull(message = "must not be empty") FinalizeAccreditationRequest body) {
        var id = UUID.fromString(accreditationId);
        finalizeAccreditation.finalizeAccreditation(requestMapper.toCommand(id, body));
        return RestResponse.status(RestResponse.Status.ACCEPTED, new AccreditationIdResponse(id));
    }

    @GET
    @Path("{accreditationId}/history")
    @RunOnVirtualThread
    public AccreditationHistoryResponse history(
            @RestPath @Pattern(regexp = AccreditationId.PATTERN, message = "must be a valid UUID") String accreditationId) {
        var query = new GetAccreditationHistoryQuery(new AccreditationId(UUID.fromString(accreditationId)));
        return responseMapper.toHistoryResponse(getAccreditationHistory.get(query));
    }
}
