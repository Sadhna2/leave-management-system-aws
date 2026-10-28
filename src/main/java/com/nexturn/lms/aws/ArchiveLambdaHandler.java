package com.nexturn.lms.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.json.JsonParserFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

/** Archives raw SNS payloads delivered through SQS into private S3 storage using Java 17. */
public class ArchiveLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final S3Client s3;
    private final String bucket;

    /** Reuses the AWS SDK client across invocations and obtains credentials from the Lambda role. */
    public ArchiveLambdaHandler() {
        this(S3Client.builder().httpClientBuilder(UrlConnectionHttpClient.builder()).build(),
                DatabaseConnections.required("ARCHIVE_BUCKET"));
    }

    /** Allows a real SDK client against a local HTTP fixture to verify requests without AWS access. */
    ArchiveLambdaHandler(S3Client s3, String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    /**
     * Returns only after every object is written. Failures propagate to Lambda so SQS retries the batch.
     * Repeated event IDs address the same logical key; bucket versioning can retain earlier writes.
     */
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        if (!(event.get("Records") instanceof List<?> records))
            throw new IllegalArgumentException("SQS Records must be a list");
        records.forEach(record -> {
            if (!(record instanceof Map<?, ?> message) || !(message.get("body") instanceof String body))
                throw new IllegalArgumentException("SQS record body must be JSON text");
            var payload = JsonParserFactory.getJsonParser().parseMap(body);
            if (!(payload.get("eventId") instanceof String eventId)
                    || !UUID.fromString(eventId).toString().equals(eventId))
                throw new IllegalArgumentException("A canonical event UUID is required");
            s3.putObject(PutObjectRequest.builder().bucket(bucket)
                    .key("audit/events/" + eventId + ".json")
                    .contentType("application/json").serverSideEncryption(ServerSideEncryption.AES256).build(),
                    RequestBody.fromString(body));
        });
        return Map.of("archived", records.size());
    }
}
