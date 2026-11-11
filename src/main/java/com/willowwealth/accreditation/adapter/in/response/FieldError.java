package com.willowwealth.accreditation.adapter.in.response;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record FieldError(
        @Schema(description = "JSON path of the field or name of the path parameter; an unknown field is reported "
                + "under its own name", examples = {
                "user_id",
                "accreditation_type",
                "document",
                "document.name",
                "document.mime_type",
                "document.content",
                "outcome",
                "accreditationId",
                "userId",
                "body"})
        String field,

        @Schema(examples = {
                "must not be null",
                "must not be empty",
                "must not be blank",
                "must be exactly 32 letters or digits",
                "must be one of BY_INCOME, BY_NET_WORTH",
                "must be one of CONFIRMED, EXPIRED, FAILED",
                "size must be between 0 and 255",
                "only application/pdf is accepted",
                "must be Base64 encoded and at most 5242880 bytes once decoded",
                "must be a valid UUID",
                "is not a known field",
                "has the wrong type"})
        String message) {
}
