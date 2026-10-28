package com.nexturn.lms.aws;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import static org.assertj.core.api.Assertions.*;

/** Verifies real SDK HTTP writes, stable archive keys, validation and retry-triggering failures locally. */
class ArchiveLambdaHandlerTest {
    private HttpServer server;
    private S3Client client;
    private ArchiveLambdaHandler handler;
    private final List<Map<String, String>> requests = new ArrayList<>();
    private int responseStatus = 200;
    private static final String EVENT_ID = "7b3e5a41-0aca-4c59-91a5-7c8cfa991002";
    private static final String BODY = "{\"eventId\":\"" + EVENT_ID + "\",\"eventType\":\"DEPLOYMENT_SMOKE_TEST\"}";

    @BeforeEach void startLocalS3Fixture() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(Map.of("method", exchange.getRequestMethod(), "path", exchange.getRequestURI().getPath(),
                    "body", new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                    "encryption", exchange.getRequestHeaders().getFirst("x-amz-server-side-encryption"),
                    "contentType", exchange.getRequestHeaders().getFirst("Content-Type")));
            exchange.sendResponseHeaders(responseStatus, -1);
            exchange.close();
        });
        server.start();
        client = S3Client.builder().endpointOverride(URI.create("http://127.0.0.1:" + server.getAddress().getPort()))
                .region(Region.AP_SOUTH_1).forcePathStyle(true)
                .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(false).build())
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("local-test", "local-test")))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .overrideConfiguration(config -> config.retryPolicy(RetryPolicy.none())).build();
        handler = new ArchiveLambdaHandler(client, "test-audit-bucket");
    }

    @AfterEach void stopLocalS3Fixture() {
        if (client != null) client.close();
        if (server != null) server.stop(0);
    }

    private Map<String, Object> batch(String body) {
        return Map.of("Records", List.of(Map.of("body", body)));
    }

    @Test void writesOriginalJsonWithEncryptionAndStableEventKey() {
        assertThat(handler.handleRequest(batch(BODY), null)).containsEntry("archived", 1);
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0)).containsEntry("method", "PUT")
                .containsEntry("path", "/test-audit-bucket/audit/events/" + EVENT_ID + ".json")
                .containsEntry("body", BODY).containsEntry("encryption", "AES256")
                .containsEntry("contentType", "application/json");
    }

    @Test void repeatedDeliveryUsesTheSameObjectKey() {
        handler.handleRequest(batch(BODY), null);
        handler.handleRequest(batch(BODY), null);
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).get("path")).isEqualTo(requests.get(1).get("path"));
    }

    @Test void invalidEventIdDoesNotWriteAnObject() {
        assertThatThrownBy(() -> handler.handleRequest(batch("{\"eventId\":\"../invalid\"}"), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(requests).isEmpty();
    }

    @Test void s3FailurePropagatesSoSqsCanRetry() {
        responseStatus = 500;
        assertThatThrownBy(() -> handler.handleRequest(batch(BODY), null)).isInstanceOf(RuntimeException.class);
        assertThat(requests).hasSize(1);
    }

    @Test void emptyBatchCompletesWithoutWriting() {
        assertThat(handler.handleRequest(Map.of("Records", List.of()), null)).containsEntry("archived", 0);
        assertThat(requests).isEmpty();
    }
}
