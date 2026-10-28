package com.nexturn.lms.aws;
import com.amazonaws.services.lambda.runtime.*;
import java.time.Duration;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
/** EventBridge invokes this relay every minute; a failed publish leaves the event available for retry. */
public class OutboxLambdaHandler implements RequestHandler<Map<String,Object>,Map<String,Object>> {
    private final javax.sql.DataSource source=DatabaseConnections.create("DB_USERNAME","DB_PASSWORD");
    private final JdbcTemplate jdbc=new JdbcTemplate(source);
    private final TransactionTemplate transaction=new TransactionTemplate(new DataSourceTransactionManager(source));
    private final SnsClient sns=SnsClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder()
            .connectionTimeout(Duration.ofSeconds(3)).socketTimeout(Duration.ofSeconds(5))).build();
    /** Delivery is at least once; the immutable event ID is the S3 archive's idempotency key. */
    @Override public Map<String,Object> handleRequest(Map<String,Object> event,Context context) {
        Integer sent=transaction.execute(status -> {
            var pending=jdbc.queryForList("SELECT event_id,payload FROM outbox_event WHERE published_at IS NULL ORDER BY created_at LIMIT 10 FOR UPDATE SKIP LOCKED");
            pending.forEach(row -> {
                sns.publish(PublishRequest.builder().topicArn(DatabaseConnections.required("NOTIFICATION_TOPIC_ARN"))
                        .message((String)row.get("payload")).build());
                jdbc.update("UPDATE outbox_event SET published_at=CURRENT_TIMESTAMP(6) WHERE event_id=?",row.get("event_id"));
            });
            return pending.size();
        });
        return Map.of("published",sent);
    }
}
