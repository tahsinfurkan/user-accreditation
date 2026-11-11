package com.willowwealth.accreditation.adapter.in.resource;

import com.willowwealth.accreditation.adapter.in.mapper.AccreditationRequestMapper;
import com.willowwealth.accreditation.adapter.in.request.SubmitAccreditationRequest;
import com.willowwealth.accreditation.adapter.in.response.AccreditationIdResponse;
import com.willowwealth.accreditation.application.port.in.usecase.SubmitAccreditationUseCase;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/user/accreditation")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class AdminAccreditationResource {

    private final SubmitAccreditationUseCase submitAccreditation;
    private final AccreditationRequestMapper requestMapper;

    // 202 rather than 201: the accreditation is recorded asynchronously, once its event is consumed.
    @POST
    @RunOnVirtualThread
    public RestResponse<AccreditationIdResponse> submit(
            @Valid @NotNull(message = "must not be empty") SubmitAccreditationRequest request) {
        var id = submitAccreditation.submit(requestMapper.toCommand(request));
        return RestResponse.status(RestResponse.Status.ACCEPTED, new AccreditationIdResponse(id.value()));
    }
}
