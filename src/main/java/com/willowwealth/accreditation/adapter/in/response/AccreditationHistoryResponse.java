package com.willowwealth.accreditation.adapter.in.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

// One accreditation that failed review, and one that was confirmed and later expired by the 30-day job.
@Schema(examples = {
        """
        {"accreditation_id": "87bb6030-458e-11ed-b023-039b275a916a",
         "user_id": "g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V",
         "history": [
           {"event_id": "0b6f2c1e-3d4a-4f5b-8c7d-9e0f1a2b3c4d", "previous_status": null, "new_status": "PENDING",
            "origin": "ADMIN", "occurred_at": "2026-09-01T11:30:00+02:00"},
           {"event_id": "7d3e9a1b-4c5f-4e2a-9b8c-1f0e2d3c4b5a", "previous_status": "PENDING", "new_status": "FAILED",
            "origin": "ADMIN", "occurred_at": "2026-09-03T13:20:00+02:00"}]}""",
        """
        {"accreditation_id": "3f9d2b7e-8a41-4c6e-b5d0-71e2a9c4f803",
         "user_id": "g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V",
         "history": [
           {"event_id": "2a4c6e8f-1b3d-4f5a-9c7e-0d2f4b6a8c1e", "previous_status": null, "new_status": "PENDING",
            "origin": "ADMIN", "occurred_at": "2026-10-01T11:30:00+02:00"},
           {"event_id": "5f1c2a8e-6d1b-4c47-9a0e-2b8f3c9d7e10", "previous_status": "PENDING", "new_status": "CONFIRMED",
            "origin": "ADMIN", "occurred_at": "2026-10-02T16:05:00+02:00"},
           {"event_id": "a3e9d7c5-1b2f-4e6a-8d0c-7f5b3a1e9c2d", "previous_status": "CONFIRMED", "new_status": "EXPIRED",
            "origin": "SYSTEM_EXPIRY", "occurred_at": "2026-11-01T16:00:00+01:00"}]}"""
})
public record AccreditationHistoryResponse(UUID accreditationId, String userId, List<StatusChangeResponse> history) {
}
