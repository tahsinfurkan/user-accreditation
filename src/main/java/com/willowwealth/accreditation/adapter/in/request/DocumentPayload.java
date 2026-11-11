package com.willowwealth.accreditation.adapter.in.request;

import com.willowwealth.accreditation.adapter.in.validation.Base64Content;
import com.willowwealth.accreditation.domain.model.Document;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record DocumentPayload(
        @NotBlank
        @Size(max = 255)
        @Schema(examples = {"2018.pdf", "2019-tax-return.pdf", "bank-statement-2021.pdf"})
        String name,

        @NotNull
        @Pattern(regexp = "application/pdf", message = "only application/pdf is accepted")
        @Schema(description = "Only PDF documents are accepted", examples = "application/pdf")
        String mimeType,

        @NotBlank
        @Base64Content(maxBytes = Document.MAX_SIZE_BYTES)
        @Schema(description = "The file, Base64 encoded; at most 5 MB once decoded",
                examples = {"ICAiQC8qIjogWyJzcmMvKiJdCiAgICB9CiAgfQp9Cg==", "JVBERi0xLjcK"})
        String content) {
}
