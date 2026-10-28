package com.nexturn.lms.aws;
import com.amazonaws.services.lambda.runtime.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
/** CloudFormation custom resource initializes only this stack's fresh RDS schema and restricted application user. */
public class SchemaBootstrapHandler implements RequestHandler<Map<String,Object>,Void> {
    private final ObjectMapper json=new ObjectMapper();
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    /** Future completion centralizes error reporting without explicit try/catch blocks or credential logging. */
    @Override public Void handleRequest(Map<String,Object> event,Context context) {
        URI callback=URI.create((String)event.get("ResponseURL"));
        if (!"https".equals(callback.getScheme()) || !callback.getHost().endsWith(".amazonaws.com"))
            throw new IllegalArgumentException("Unexpected CloudFormation response destination");
        CompletableFuture.completedFuture(event).thenApply(this::initialize).handle((data,failure) -> {
            Map<String,Object> response=new LinkedHashMap<>();
            response.put("Status",failure==null?"SUCCESS":"FAILED");
            response.put("Reason",failure==null?"Schema is ready":"Schema initialization failed; inspect the bootstrap function logs without logging credentials");
            response.put("PhysicalResourceId",event.getOrDefault("PhysicalResourceId","lms-approval-schema-v1"));
            response.put("StackId",event.get("StackId")); response.put("RequestId",event.get("RequestId"));
            response.put("LogicalResourceId",event.get("LogicalResourceId")); response.put("NoEcho",true);
            response.put("Data",failure==null?data:Map.of());
            if (failure!=null) context.getLogger().log("Schema initialization failure type: "+failure.getClass().getName());
            return json.valueToTree(response).toString();
        }).thenCompose(body -> http.sendAsync(HttpRequest.newBuilder(callback).timeout(Duration.ofSeconds(30))
                .header("Content-Type","").PUT(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.discarding()))
          .thenAccept(response -> {
              if (response.statusCode()<200 || response.statusCode()>=300) throw new IllegalStateException("CloudFormation callback was rejected");
          }).join();
        return null;
    }
    private Map<String,Object> initialize(Map<String,Object> event) {
        if ("Delete".equals(event.get("RequestType"))) return Map.of("Retained",true);
        var source=DatabaseConnections.create("MASTER_USERNAME","MASTER_PASSWORD");
        new ResourceDatabasePopulator(new ClassPathResource("database/aws-schema.sql")).execute(source);
        JdbcTemplate jdbc=new JdbcTemplate(source);
        jdbc.update("CREATE USER IF NOT EXISTS 'lms_app'@'%' IDENTIFIED BY ?",DatabaseConnections.required("APP_PASSWORD"));
        jdbc.update("ALTER USER 'lms_app'@'%' IDENTIFIED BY ?",DatabaseConnections.required("APP_PASSWORD"));
        List.of("GRANT SELECT ON lms_approval.employee TO 'lms_app'@'%'",
            "GRANT SELECT ON lms_approval.leave_type TO 'lms_app'@'%'",
            "GRANT SELECT,UPDATE ON lms_approval.leave_application TO 'lms_app'@'%'",
            "GRANT SELECT,UPDATE ON lms_approval.leave_balance TO 'lms_app'@'%'",
            "GRANT SELECT,INSERT ON lms_approval.approval TO 'lms_app'@'%'",
            "GRANT SELECT,INSERT ON lms_approval.audit_log TO 'lms_app'@'%'",
            "GRANT SELECT,INSERT,UPDATE ON lms_approval.notification TO 'lms_app'@'%'",
            "GRANT SELECT ON lms_approval.login TO 'lms_app'@'%'",
            "GRANT SELECT,INSERT,UPDATE,DELETE ON lms_approval.auth_session TO 'lms_app'@'%'",
            "GRANT SELECT,INSERT,UPDATE ON lms_approval.outbox_event TO 'lms_app'@'%'").forEach(jdbc::execute);
        jdbc.update("INSERT IGNORE INTO employee(emp_id,emp_name,designation,gender,address) VALUES (100,'Initial Manager','Manager','Other','Not provided')");
        Integer exists=jdbc.queryForObject("SELECT COUNT(*) FROM login WHERE username='manager'",Integer.class);
        if (exists==0) jdbc.update("INSERT INTO login(username,password,role,emp_id) VALUES ('manager',?,'MANAGER',100)",
                new BCryptPasswordEncoder(12).encode(DatabaseConnections.required("INITIAL_MANAGER_PASSWORD")));
        return Map.of("SchemaVersion","1","ManagerUsername","manager");
    }
}
