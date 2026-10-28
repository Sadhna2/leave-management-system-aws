# Source and configuration guide

Each Java file has a class/interface/enum/record documentation comment. Shared beans also document their accessors.

| File | Responsibility |
|---|---|
| `src/main/java/com/nexturn/lms/dto/PageResponse.java` | Stable paginated JSON response with content and page metadata. |
| `src/test/java/com/nexturn/lms/MySqlSmokeTest.java` | Opt-in schema, authentication and approval verification against disposable MySQL. |
| `src/main/java/com/nexturn/lms/Application.java` | Starts the scoped Member 3 backend locally or through the Lambda adapter. |
| `src/main/java/com/nexturn/lms/config/WebConfig.java` | Authenticates API calls before controller execution and limits browser origins explicitly. |
| `src/main/java/com/nexturn/lms/controller/AuditLogController.java` | Provides read-only audit access to the employee or their currently assigned manager. |
| `src/main/java/com/nexturn/lms/controller/LoginController.java` | Provides login, current identity and logout without public role registration. |
| `src/main/java/com/nexturn/lms/controller/ManagerController.java` | Exposes the authenticated manager's queue and decision endpoints. |
| `src/main/java/com/nexturn/lms/controller/NotificationController.java` | Lets authenticated employees list and mark only their own in-app notifications. |
| `src/main/java/com/nexturn/lms/controller/PingController.java` | Public liveness probe without database or credential details. |
| `src/main/java/com/nexturn/lms/dto/ApprovalRequest.java` | Required decision explanation; manager identity comes exclusively from the bearer token. |
| `src/main/java/com/nexturn/lms/dto/LoginRequest.java` | Credentials accepted only by the login endpoint. |
| `src/main/java/com/nexturn/lms/dto/LoginResponse.java` | One-time token response; callers send it in the Authorization bearer header. |
| `src/main/java/com/nexturn/lms/entity/Approval.java` | Final manager decision with required comment and decision timestamp. |
| `src/main/java/com/nexturn/lms/entity/AuditLog.java` | Append-only decision evidence identifying the affected employee and the acting manager. |
| `src/main/java/com/nexturn/lms/entity/AuthSession.java` | Stores only a SHA-256 token digest; raw bearer tokens are returned once at login. |
| `src/main/java/com/nexturn/lms/entity/Employee.java` | Shared employee record used to verify manager assignment; employee management belongs to the employee module. |
| `src/main/java/com/nexturn/lms/entity/LeaveApplication.java` | Shared pending leave record; dates and working-day counts are supplied by the leave application module. |
| `src/main/java/com/nexturn/lms/entity/LeaveBalance.java` | Shared yearly balance; approval updates used and remaining days under a database row lock. |
| `src/main/java/com/nexturn/lms/entity/LeaveType.java` | Shared leave policy controlling whether approval may create a negative balance. |
| `src/main/java/com/nexturn/lms/entity/Login.java` | Provisioned account explicitly linked to an employee; passwords contain BCrypt hashes only. |
| `src/main/java/com/nexturn/lms/entity/Notification.java` | Persistent in-app message addressed to one employee. |
| `src/main/java/com/nexturn/lms/enums/LeaveStatus.java` | Lifecycle values used to allow decisions only while a leave is pending. |
| `src/main/java/com/nexturn/lms/enums/NotificationType.java` | Shared event categories retained for compatibility with the original notification model. |
| `src/main/java/com/nexturn/lms/enums/Role.java` | Shared account roles; only MANAGER can approve or reject a request. |
| `src/main/java/com/nexturn/lms/exception/ApiException.java` | Carries a safe client message and HTTP status to the centralized handler. |
| `src/main/java/com/nexturn/lms/exception/GlobalExceptionHandler.java` | Converts controller, interceptor and transaction failures to consistent JSON responses. |
| `src/main/java/com/nexturn/lms/repository/ApprovalRepository.java` | Persistence access for approval repository. |
| `src/main/java/com/nexturn/lms/repository/AuditLogRepository.java` | Persistence access for audit log repository. |
| `src/main/java/com/nexturn/lms/repository/AuthSessionRepository.java` | Persists revocable tokens across independent Lambda instances. |
| `src/main/java/com/nexturn/lms/repository/EmployeeRepository.java` | Reads employee assignment; a lock stabilizes it while a decision is being recorded. |
| `src/main/java/com/nexturn/lms/repository/LeaveBalanceRepository.java` | Locks the employee/type/year balance so concurrent approvals cannot overspend it. |
| `src/main/java/com/nexturn/lms/repository/LeaveRepository.java` | Serializes decisions on one leave and pages the assigned manager's pending queue. |
| `src/main/java/com/nexturn/lms/repository/LeaveTypeRepository.java` | Persistence access for leave type repository. |
| `src/main/java/com/nexturn/lms/repository/LoginRepository.java` | Resolves provisioned accounts by unique username. |
| `src/main/java/com/nexturn/lms/repository/NotificationRepository.java` | Persistence access for notification repository. |
| `src/main/java/com/nexturn/lms/service/ApprovalService.java` | Member 3 contract for authorized pending-leave decisions. |
| `src/main/java/com/nexturn/lms/service/ApprovalServiceImpl.java` | Commits the decision, balance, notification and audit atomically; any failure rolls all back. |
| `src/main/java/com/nexturn/lms/service/AuditLogService.java` | Records audit evidence inside the calling approval transaction; exposes no update or delete operation. |
| `src/main/java/com/nexturn/lms/service/LoginService.java` | Checks BCrypt credentials and issues expiring, database-backed opaque bearer tokens. |
| `src/main/java/com/nexturn/lms/service/NotificationService.java` | Contract for creating and reading persisted in-app notifications. |
| `src/main/java/com/nexturn/lms/service/NotificationServiceImpl.java` | Creates employee notifications inside the calling approval transaction. |
| `src/main/java/com/nexturn/lms/StreamLambdaHandler.java` | Adapts API Gateway REST events; initialization failures propagate to the Lambda runtime. |
| `src/main/java/com/nexturn/lms/tools/PasswordHashTool.java` | Offline account provisioning utility; reads passwords without command-line or console echo. |
| `src/test/java/com/nexturn/lms/service/ApprovalServiceTest.java` | Exercises real transactions, authorization, HTTP validation, rollback and competing decisions. |
| `src/test/java/com/nexturn/lms/StreamLambdaHandlerTest.java` | Verifies the actual API Gateway stream adapter initializes and returns JSON. |

| Other file | Purpose |
|---|---|
| `pom.xml` | Single dependency/build definition; executable JAR and optional Lambda ZIP. |
| `build.gradle` | Compatibility task delegating builds to Maven. |
| `src/assembly/bin.xml` | Lambda archive structure and embedded server exclusions. |
| `src/main/resources/application.properties` | Environment-based database, authentication and CORS configuration. |
| `src/test/resources/application-test.properties` | Isolated H2 test database. |
| `database/schema.sql` | Fresh MySQL schema and integrity constraints. |
| `database/local-fixture.sql` | Optional local demonstration records without passwords. |
| `template.yml` | SAM deployment preparation for Lambda, API Gateway, existing VPC/database and secret. |
| `.gitignore` | Build output, editor metadata and local secret exclusions. |
| `README.md` | Scope, business rules, setup, API usage, tests and AWS preparation. |
| `docs/VERIFICATION.md` | Actual build/test outcomes and remaining environmental checks. |
