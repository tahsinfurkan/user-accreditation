package com.willowwealth.accreditation.adapter.in.resource;

import com.willowwealth.accreditation.adapter.in.mapper.AccreditationResponseMapper;
import com.willowwealth.accreditation.adapter.in.response.UserAccreditationsResponse;
import com.willowwealth.accreditation.application.port.in.query.GetUserAccreditationsQuery;
import com.willowwealth.accreditation.application.port.in.usecase.GetUserAccreditationsUseCase;
import com.willowwealth.accreditation.domain.model.UserId;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.faulttolerance.api.ApplyGuard;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.GET;
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

import static com.willowwealth.accreditation.config.OpenApiDocumentation.INVALID_REQUEST;
import static com.willowwealth.accreditation.config.OpenApiDocumentation.REQUEST_ID;
import static com.willowwealth.accreditation.config.OpenApiDocumentation.SERVICE_UNAVAILABLE;
import static com.willowwealth.accreditation.config.OpenApiDocumentation.TOO_MANY_REQUESTS;
import static com.willowwealth.accreditation.config.OpenApiDocumentation.USER_NOT_FOUND;
import static com.willowwealth.accreditation.config.RateLimitProducer.USER_ACCREDITATIONS_RATE_LIMIT;

@Tag(name = "Client", description = "Serves the client-facing web application")
@Path("/user/{userId}/accreditation")
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class UserAccreditationResource {

    private final GetUserAccreditationsUseCase getUserAccreditations;
    private final AccreditationResponseMapper responseMapper;

    @GET
    @RunOnVirtualThread
    @ApplyGuard(USER_ACCREDITATIONS_RATE_LIMIT)
    @Operation(summary = "Get the accreditations of a user",
            description = "Every accreditation of the user with its type and current status, oldest first.")
    @APIResponse(responseCode = "200", description = "The user's accreditations",
            headers = @Header(name = "X-Request-Id", ref = REQUEST_ID),
            content = @Content(schema = @Schema(implementation = UserAccreditationsResponse.class)))
    @APIResponse(responseCode = "400", ref = INVALID_REQUEST)
    @APIResponse(responseCode = "404", ref = USER_NOT_FOUND)
    @APIResponse(responseCode = "429", ref = TOO_MANY_REQUESTS)
    @APIResponse(responseCode = "503", ref = SERVICE_UNAVAILABLE)
    public UserAccreditationsResponse get(
            @Parameter(description = "Id of the user", example = "g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V")
            @RestPath @Pattern(regexp = UserId.PATTERN, message = "must be exactly 32 letters or digits") String userId) {
        var user = new UserId(userId);
        var accreditations = getUserAccreditations.get(new GetUserAccreditationsQuery(user));
        return responseMapper.toResponse(user, accreditations);
    }
}
