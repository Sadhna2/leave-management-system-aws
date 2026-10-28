package com.nexturn.lms.aws;
import com.amazonaws.services.lambda.runtime.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
        if ("true".equals(System.getenv("DEMO_DATA_ENABLED"))) initializeDemo(jdbc);
        return Map.of("SchemaVersion","2","ManagerUsername","manager");
    }

    /** Adds explicitly requested synthetic demo data once, without resetting decisions or balances. */
    private void initializeDemo(JdbcTemplate jdbc) {
        int employeeId = 900100;
        int leaveTypeId = 900001;
        jdbc.update("INSERT IGNORE INTO employee(emp_id,emp_name,designation,manager_id,gender,address) VALUES (?, 'Demo Employee', 'Demo Employee', 100, 'Other', 'Synthetic demonstration record')", employeeId);
        Integer assigned = jdbc.queryForObject("SELECT COUNT(*) FROM employee WHERE emp_id=? AND emp_name='Demo Employee' AND manager_id=100", Integer.class, employeeId);
        if (!Integer.valueOf(1).equals(assigned)) throw new IllegalStateException("Demo employee ID is already in use");
        Integer existingLogin = jdbc.queryForObject("SELECT COUNT(*) FROM login WHERE username='employee.demo'", Integer.class);
        if (Integer.valueOf(0).equals(existingLogin)) jdbc.update("INSERT INTO login(username,password,role,emp_id) VALUES ('employee.demo',?,'EMPLOYEE',?)",
                new BCryptPasswordEncoder(12).encode(DatabaseConnections.required("DEMO_EMPLOYEE_PASSWORD")), employeeId);
        Integer ownedLogin = jdbc.queryForObject("SELECT COUNT(*) FROM login WHERE username='employee.demo' AND emp_id=? AND role='EMPLOYEE'", Integer.class, employeeId);
        if (!Integer.valueOf(1).equals(ownedLogin)) throw new IllegalStateException("Demo username is already in use");
        jdbc.update("INSERT IGNORE INTO leave_type(leave_type_id,leave_type_name,description,annual_quota,allow_negative_balance) VALUES (?, 'Demo Annual Leave', 'Synthetic leave type for Member 3 verification', 20, FALSE)", leaveTypeId);
        LocalDate start = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        if (start.plusDays(1).getYear() != start.getYear()) start = start.plusDays(1);
        jdbc.update("INSERT IGNORE INTO leave_balance(emp_id,leave_type_id,total_allocated_days,used_days,remaining_days,`year`) VALUES (?,?,20,0,20,?)", employeeId, leaveTypeId, start.getYear());
        jdbc.update("INSERT INTO leave_application(emp_id,leave_type_id,start_date,end_date,no_of_days_requested,reason,status) SELECT ?,?,?,?,2,'Demo request for Member 3 approval verification','PENDING' WHERE NOT EXISTS (SELECT 1 FROM leave_application WHERE emp_id=? AND leave_type_id=? AND reason='Demo request for Member 3 approval verification')",
                employeeId, leaveTypeId, start, start.plusDays(1), employeeId, leaveTypeId);
    }
}
