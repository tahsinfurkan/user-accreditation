package com.willowwealth.accreditation.adapter.in.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.SequencedMap;
import java.util.UUID;

// The first two accreditations are the ones from the specification; the other two show the remaining statuses.
@Schema(examples = """
        {"user_id": "g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V",
         "accreditation_statuses": {
           "87bb6030-458e-11ed-b023-039b275a916a": {"accreditation_type": "BY_INCOME", "status": "FAILED"},
           "c031a5da-d59a-4d35-b1f0-0f8324dcc156": {"accreditation_type": "BY_NET_WORTH", "status": "CONFIRMED"},
           "3f9d2b7e-8a41-4c6e-b5d0-71e2a9c4f803": {"accreditation_type": "BY_INCOME", "status": "EXPIRED"},
           "d47a1c90-2e5b-4f38-9b6d-0c8e3f5a2b17": {"accreditation_type": "BY_NET_WORTH", "status": "PENDING"}}}""")
public record UserAccreditationsResponse(
        @Schema(examples = {"g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V", "Q3vT9cLw2KxM8nRb5HdZ1pYf7GsJ4uAe"})
        String userId,

        @Schema(description = "Keyed by accreditation id, oldest first")
        SequencedMap<UUID, AccreditationStatusResponse> accreditationStatuses) {
}
