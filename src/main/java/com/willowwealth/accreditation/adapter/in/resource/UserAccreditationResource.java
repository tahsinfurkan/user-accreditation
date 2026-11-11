package com.willowwealth.accreditation.adapter.in.resource;

import com.willowwealth.accreditation.adapter.in.mapper.AccreditationResponseMapper;
import com.willowwealth.accreditation.adapter.in.response.UserAccreditationsResponse;
import com.willowwealth.accreditation.application.port.in.query.GetUserAccreditationsQuery;
import com.willowwealth.accreditation.application.port.in.usecase.GetUserAccreditationsUseCase;
import com.willowwealth.accreditation.domain.model.UserId;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.reactive.RestPath;

@Path("/user/{userId}/accreditation")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class UserAccreditationResource {

    private final GetUserAccreditationsUseCase getUserAccreditations;
    private final AccreditationResponseMapper responseMapper;

    @GET
    @RunOnVirtualThread
    public UserAccreditationsResponse get(
            @RestPath @Pattern(regexp = UserId.PATTERN, message = "must be exactly 32 letters or digits") String userId) {
        var user = new UserId(userId);
        var accreditations = getUserAccreditations.get(new GetUserAccreditationsQuery(user));
        return responseMapper.toResponse(user, accreditations);
    }
}
