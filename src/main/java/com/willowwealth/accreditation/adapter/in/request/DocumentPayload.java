package com.willowwealth.accreditation.adapter.in.request;

import com.willowwealth.accreditation.adapter.in.validation.Base64Content;
import com.willowwealth.accreditation.domain.model.Document;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DocumentPayload(
        @NotBlank
        @Size(max = 255)
        String name,

        @NotNull
        @Pattern(regexp = "application/pdf", message = "only application/pdf is accepted")
        String mimeType,

        @NotBlank
        @Base64Content(maxBytes = Document.MAX_SIZE_BYTES)
        String content) {
}
