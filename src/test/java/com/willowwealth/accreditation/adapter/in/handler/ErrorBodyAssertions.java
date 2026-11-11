package com.willowwealth.accreditation.adapter.in.handler;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;

import static org.assertj.core.api.Assertions.assertThat;

public final class ErrorBodyAssertions {

    private ErrorBodyAssertions() {
    }

    // Checks what every error response must have in common and returns the body for further checks.
    public static JsonPath assertErrorBody(ValidatableResponse response, int status, String message) {
        var extracted = response.statusCode(status).contentType(ContentType.JSON).extract();
        assertThat(extracted.asString()).doesNotContain("Exception", "java.", "org.", "com.", "\tat ");

        var body = extracted.jsonPath();
        assertThat(body.getString("message")).isEqualTo(message);
        assertThat(body.getString("detail")).isNotBlank();
        assertThat(body.getString("trace_id")).isNotBlank().isEqualTo(extracted.header("X-Request-Id"));
        return body;
    }
}
