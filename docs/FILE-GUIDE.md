# Source and configuration guide

Only Member 3 and required infrastructure/authentication support are included.

| Java file | Responsibility |
|---|---|
| `src/main/java/com/nexturn/lms/Application.java` | Starts the scoped Member 3 backend locally or through the Lambda adapter. |
| `src/main/java/com/nexturn/lms/aws/DatabaseConnections.java` | Creates a minimal JDBC connection factory for infrastructure handlers without starting the web app. |
| `src/main/java/com/nexturn/lms/aws/OutboxLambdaHandler.java` | EventBridge invokes this relay every minute; a failed publish leaves the event available for retry. |
| `src/main/java/com/nexturn/lms/aws/SchemaBootstrapHandler.java` | CloudFormation custom resource initializes only this stack's fresh RDS schema and restricted application user. |
| `src/main/java/com/nexturn/lms/config/WebConfig.java` | Authenticates API calls before controller execution and limits browser origins explicitly. |
| `src/main/java/com/nexturn/lms/controller/AuditLogController.java` | Provides read-only audit access to the employee or their currently assigned manager. |
| `src/main/java/com/nexturn/lms/controller/LoginController.java` | Provides login, current identity and logout without public role registration. |
| `src/main/java/com/nexturn/lms/controller/ManagerController.java` | Exposes the authenticated manager's queue and decision endpoints. |
| `src/main/java/com/nexturn/lms/controller/NotificationController.java` | Lets authenticated employees list and mark only their own in-app notifications. |
| `src/main/java/com/nexturn/lms/controller/PingController.java` | Public liveness probe without database or credential details. |
| `src/main/java/com/nexturn/lms/dto/ApprovalRequest.java` | Required decision explanation; manager identity comes exclusively from the bearer token. |
| `src/main/java/com/nexturn/lms/dto/LoginRequest.java` | Credentials accepted only by the login endpoint. |
| `src/main/java/com/nexturn/lms/dto/LoginResponse.java` | One-time token response; callers send it in the Authorization bearer header. |
| `src/main/java/com/nexturn/lms/dto/PageResponse.java` | Stable paginated JSON contract independent of Spring's internal PageImpl serialization. |
| `src/main/java/com/nexturn/lms/entity/Approval.java` | Final manager decision with required comment and decision timestamp. |
| `src/main/java/com/nexturn/lms/entity/AuditLog.java` | Append-only decision evidence identifying the affected employee and the acting manager. |
| `src/main/java/com/nexturn/lms/entity/AuthSession.java` | Stores only a SHA-256 token digest; raw bearer tokens are returned once at login. |
| `src/main/java/com/nexturn/lms/entity/Employee.java` | Shared employee record used to verify manager assignment; employee management belongs to the employee module. |
| `src/main/java/com/nexturn/lms/entity/LeaveApplication.java` | Shared pending leave record; dates and working-day counts are supplied by the leave application module. |
| `src/main/java/com/nexturn/lms/entity/LeaveBalance.java` | Shared yearly balance; approval updates used and remaining days under a database row lock. |
| `src/main/java/com/nexturn/lms/entity/LeaveType.java` | Shared leave policy controlling whether approval may create a negative balance. |
| `src/main/java/com/nexturn/lms/entity/Login.java` | Provisioned account explicitly linked to an employee; passwords contain BCrypt hashes only. |
| `src/main/java/com/nexturn/lms/entity/Notification.java` | Persistent in-app message addressed to one employee. |
| `src/main/java/com/nexturn/lms/entity/OutboxEvent.java` | Durable notification event committed alongside the approval; delivery is retried separately. |
| `src/main/java/com/nexturn/lms/enums/LeaveStatus.java` | Lifecycle values used to allow decisions only while a leave is pending. |
| `src/main/java/com/nexturn/lms/enums/NotificationType.java` | Shared event categories retained for compatibility with the original notification model. |
| `src/main/java/com/nexturn/lms/enums/Role.java` | Shared account roles; only MANAGER can approve or reject a request. |
| `src/main/java/com/nexturn/lms/exception/ApiException.java` | Carries a safe client message and HTTP status to the centralized handler. |
| `src/main/java/com/nexturn/lms/exception/GlobalExceptionHandler.java` | Converts controller, interceptor and transaction failures to consistent JSON responses. |
| `src/main/java/com/nexturn/lms/repository/ApprovalRepository.java` | Persistence access for approval repository. |
| `src/main/java/com/nexturn/lms/repository/AuditLogRepository.java` | Persistence access for audit log repository. |
| `src/main/java/com/nexturn/lms/repository/AuthSessionRepository.java` | Persists revocable tokens across independent Lambda instances. |
| `src/main/java/com/nexturn/lms/repository/EmployeeRepository.java` | Stabilizes employee assignment with a shared lock and SELECT-only MySQL permissions. |
| `src/main/java/com/nexturn/lms/repository/LeaveBalanceRepository.java` | Locks the employee/type/year balance so concurrent approvals cannot overspend it. |
| `src/main/java/com/nexturn/lms/repository/LeaveRepository.java` | Serializes decisions on one leave and pages the assigned manager's pending queue. |
| `src/main/java/com/nexturn/lms/repository/LeaveTypeRepository.java` | Persistence access for leave type repository. |
| `src/main/java/com/nexturn/lms/repository/LoginRepository.java` | Resolves provisioned accounts by unique username. |
| `src/main/java/com/nexturn/lms/repository/NotificationRepository.java` | Persistence access for notification repository. |
| `src/main/java/com/nexturn/lms/repository/OutboxEventRepository.java` | Inserts events into the same relational transaction as the manager decision. |
| `src/main/java/com/nexturn/lms/service/ApprovalService.java` | Member 3 contract for authorized pending-leave decisions. |
| `src/main/java/com/nexturn/lms/service/ApprovalServiceImpl.java` | Commits the decision, balance, notification and audit atomically; any failure rolls all back. |
| `src/main/java/com/nexturn/lms/service/AuditLogService.java` | Records audit evidence inside the calling approval transaction; exposes no update or delete operation. |
| `src/main/java/com/nexturn/lms/service/LoginService.java` | Checks BCrypt credentials and issues expiring, database-backed opaque bearer tokens. |
| `src/main/java/com/nexturn/lms/service/NotificationService.java` | Contract for creating and reading persisted in-app notifications. |
| `src/main/java/com/nexturn/lms/service/NotificationServiceImpl.java` | Creates employee notifications inside the calling approval transaction. |
| `src/main/java/com/nexturn/lms/service/OutboxService.java` | Enqueues cloud events locally so SNS outages cannot invalidate a committed manager decision. |
| `src/main/java/com/nexturn/lms/StreamLambdaHandler.java` | Adapts API Gateway REST events; initialization failures propagate to the Lambda runtime. |
| `src/main/java/com/nexturn/lms/tools/PasswordHashTool.java` | Offline account provisioning utility; reads passwords without command-line or console echo. |
| `src/test/java/com/nexturn/lms/MySqlSmokeTest.java` | Opt-in check against a disposable MySQL schema loaded from schema.sql and local-fixture.sql. |
| `src/test/java/com/nexturn/lms/RestrictedMySqlSmokeTest.java` | Regression check for MySQL shared assignment locks using the deployment's restricted application grants. |
| `src/test/java/com/nexturn/lms/service/ApprovalServiceTest.java` | Exercises real transactions, authorization, HTTP validation, rollback and competing decisions. |
| `src/test/java/com/nexturn/lms/StreamLambdaHandlerTest.java` | Verifies the actual API Gateway stream adapter initializes and returns JSON. |

| Frontend file | Responsibility |
|---|---|
| `frontend/src/api.js` | Central API client for Member 3 only; runtime configuration keeps the API stage out of the compiled bundle. |
| `frontend/src/App.jsx` | Includes only login, approvals, notifications and audit; unrelated employee and HR routes are excluded. |
| `frontend/src/auth.jsx` | Restores identity from the authenticated backend; passwords and user roles are never trusted from local storage. |
| `frontend/src/components/Layout.jsx` | Responsive LeaveHub shell reused from the previous project with Member 3 navigation only. |
| `frontend/src/components/ui.jsx` | Shared page, alert, status and date presentation reused from the previous project. |
| `frontend/src/custom.css` | LeaveHub – "Emerald Soft" theme (CSS3 on top of Bootstrap 5) |
| `frontend/src/main.jsx` | Mounts the selected original React UI using hash routing compatible with static S3 hosting. |
| `frontend/src/pages/Approvals.jsx` | Reuses the previous approval cards with the scoped pending queue, required comments and paginated responses. |
| `frontend/src/pages/Audit.jsx` | Adds the required read-only audit screen; managers can inspect assigned employees and employees see their own history. |
| `frontend/src/pages/Login.jsx` | Adapts the original split-screen login to authenticated bearer tokens and explicit employee identity. |
| `frontend/src/pages/Notifications.jsx` | Reuses the original notification list with owned-record pagination and mark-as-read support. |

Other documented configuration: pom.xml, template.yml (existing-database alternative), src/assembly/bin.xml, database/schema.sql, optional database/local-fixture.sql, application.properties, application-test.properties, frontend/package.json, frontend/vite.config.js, frontend/public/config.json, deployment/cloudformation.json, deployment/archive_worker.py and deployment/README.md.
