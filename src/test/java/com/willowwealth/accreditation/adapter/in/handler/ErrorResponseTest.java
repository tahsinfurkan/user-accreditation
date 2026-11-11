package com.willowwealth.accreditation.adapter.in.handler;

import com.willowwealth.accreditation.application.exception.EventPublicationException;
import com.willowwealth.accreditation.application.port.out.AccreditationEventPublisher;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.http.ContentType;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static com.willowwealth.accreditation.adapter.in.handler.ErrorBodyAssertions.assertErrorBody;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@QuarkusTest
class ErrorResponseTest {

    private static final String ENDPOINT = "/api/v1/user/accreditation";
    private static final String UNKNOWN_PATH = "/api/v1/nothing-here";
    private static final String VALID_REQUEST = """
            {"user_id": "g8NlYJnk7zK9BlB1J2Ebjs0AkhCTpE1V", "accreditation_type": "BY_INCOME",
             "document": {"name": "2018.pdf", "mime_type": "application/pdf",
                          "content": "ICAiQC8qIjogWyJzcmMvKiJdCiAgICB9CiAgfQp9Cg=="}}""";

    @InjectMock
    AccreditationEventPublisher eventPublisher;

    @Test
    void asksToRetryLaterWhenKafkaDoesNotAcknowledge() {
        doThrow(new EventPublicationException("Event was not acknowledged by Kafka within PT5S", new TimeoutException()))
                .when(eventPublisher).publish(any());

        var response = given().contentType(ContentType.JSON).body(VALID_REQUEST)
                .when().post(ENDPOINT)
                .then();

        assertErrorBody(response, 503, "Service temporarily unavailable");
        response.header("Retry-After", "5");
    }

    @Test
    void hidesTheCauseOfAnUnexpectedErrorButLogsIt() {
        doThrow(new IllegalStateException("connection to db.internal:5432 refused"))
                .when(eventPublisher).publish(any());
        var errors = new CopyOnWriteArrayList<ExtLogRecord>();
        var logger = Logger.getLogger(ApiExceptionHandler.class.getName());
        var capture = collectErrors(errors);
        logger.addHandler(capture);
        try {
            var response = given().contentType(ContentType.JSON).body(VALID_REQUEST)
                    .when().post(ENDPOINT)
                    .then();

            var body = assertErrorBody(response, 500, "Something went wrong");
            assertThat(response.extract().asString()).doesNotContain("db.internal");
            assertThat(errors).singleElement().satisfies(record -> {
                assertThat(record.getMdc("trace_id")).isEqualTo(body.getString("trace_id"));
                assertThat(record.getThrown()).isInstanceOf(IllegalStateException.class);
            });
        } finally {
            logger.removeHandler(capture);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {UNKNOWN_PATH, "/nothing-here"})
    void answersUnknownPathsWithNotFound(String path) {
        assertErrorBody(given().when().get(path).then(), 404, "Resource not found");
    }

    @Test
    void rejectsMethodsTheEndpointDoesNotSupport() {
        assertErrorBody(given().when().get(ENDPOINT).then(), 405, "Method not allowed");
    }

    @Test
    void rejectsClientsThatDoNotAcceptJson() {
        var response = given().contentType(ContentType.JSON).accept(ContentType.TEXT).body(VALID_REQUEST)
                .when().post(ENDPOINT)
                .then();

        assertErrorBody(response, 406, "Not acceptable");
    }

    @Test
    void rejectsBodiesOverTheSizeLimit() {
        var body = """
                {"user_id": "%s"}""".formatted("a".repeat(9 * 1024 * 1024));
        // With Expect: 100-continue the client waits for an answer before sending the body, so it can read the 413
        // instead of running into the closed connection.
        var expectContinue = RestAssured.config()
                .httpClient(HttpClientConfig.httpClientConfig().setParam("http.protocol.expect-continue", true));

        var response = given().config(expectContinue).contentType(ContentType.JSON).body(body)
                .when().post(ENDPOINT)
                .then();

        assertErrorBody(response, 413, "Request too large");
    }

    @Test
    void keepsTheCallersRequestIdWhenItIsAUuid() {
        var requestId = UUID.randomUUID().toString();

        var response = given().header("X-Request-Id", requestId)
                .when().get(UNKNOWN_PATH)
                .then();

        assertThat(assertErrorBody(response, 404, "Resource not found").getString("trace_id")).isEqualTo(requestId);
    }

    @Test
    void replacesARequestIdThatIsNotAUuid() {
        var response = given().header("X-Request-Id", "<script>")
                .when().get(UNKNOWN_PATH)
                .then();

        var traceId = assertErrorBody(response, 404, "Resource not found").getString("trace_id");
        assertThat(UUID.fromString(traceId)).isNotNull();
    }

    private static Handler collectErrors(List<ExtLogRecord> records) {
        return new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.ERROR.intValue()) {
                    records.add((ExtLogRecord) record);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
    }
}
