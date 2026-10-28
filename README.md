# Leave Management System AWS — Member 3 Backend

This project implements **Approval, Notifications and Audit** from the supplied Member 3 requirements. Sources are written directly in `E:\eclipse-workspace\leave-management-system-aws`; the previous project is unchanged. Java 21, Spring Boot 3.4.5 and the existing AWS serverless Java container 2.1.6 starter are retained. Maven is the build definition. Import **Existing Maven Projects** in Eclipse and select this folder; use Java 21 and Maven > Update Project.

## Scope and included files

| Requirement | Implementation |
|---|---|
| Approve/reject pending leave | `ManagerController`, `ApprovalService`, `ApprovalServiceImpl` |
| Check assigned manager | Token identity → `Login.empId` → locked `Employee.managerId`; MANAGER role required; self approval forbidden |
| Record comment and decision | Validated `ApprovalRequest`, `Approval`, `ApprovalRepository`; one decision per leave |
| Deduct balance on approval | Locked `LeaveBalanceRepository`; allocated minus used; respects explicit negative-balance policy |
| Create notification | `NotificationService`, `NotificationServiceImpl`, `NotificationRepository`, `NotificationType`, `Notification` |
| Log audit entry | `AuditLogService`, `AuditLogRepository`, `AuditLog`; records affected employee and acting manager |
| Authentication support | `LoginController`, `LoginService`, `Login`, `AuthSession`, associated DTOs and repositories, `WebConfig` |
| Global errors | `ApiException`, `GlobalExceptionHandler` |
| Verification | `ApprovalServiceTest`, `StreamLambdaHandlerTest` |

Only the shared entities/repositories needed to decide a leave are included: Employee, LeaveApplication, LeaveBalance and LeaveType. Existing notification/audit code, enums and shared model shapes were selectively reused and documented. No frontend, HR controller, employee CRUD, leave submission controller, holiday scheduler or unrelated tests are copied. See `docs/FILE-GUIDE.md` for the role of every source/configuration file.

Login is necessary to establish a real manager identity. Public registration is intentionally absent: employees, assignments, roles and initial balances are provisioned by the owning employee/HR module or a database administrator. This prevents public callers from registering as managers. This backend consumes existing PENDING leave records; it does not implement another team member's leave submission workflow.

## Business behavior

1. Authenticate with username/password and obtain an expiring bearer token. Only its SHA-256 digest is stored; BCrypt hashes protect passwords. Tokens are shared through the database across Lambda instances, expire after one hour by default, and are revoked on logout. Account/role changes are checked on every request. Expired sessions are purged during successful login.
2. The manager reads their pending queue. An approval/rejection request supplies **only a comment**, never a manager ID.
3. The decision transaction locks the leave, the employee assignment and, for approval, the relevant balance. It verifies MANAGER role, assignment, no self approval, PENDING status and a nonblank comment of at most 200 characters.
4. Approval requires valid dates and positive stored working days. The upstream leave module supplies holiday/weekend calculations. Cross-year requests must be split upstream; they are rejected for approval rather than charged entirely against one year. A missing or inconsistent balance is rejected; no balance is silently created. Rejection needs no balance and does not change it.
5. The decision, balance, notification and audit entry commit together. A persistence failure rolls everything back. Duplicate decisions return 409. Database locks also prevent different leave requests from overspending one shared balance.

Notifications are **in-app database records**, not email/SMS/SNS messages. Employees can read and mark their own notifications. Audit reads are limited to the employee or their currently assigned manager. There is no audit update/delete endpoint. Database administrators should restrict UPDATE/DELETE on `audit_log` for the runtime database user.

There are **no explicit try/catch blocks** in project Java sources. Services throw exceptions; `GlobalExceptionHandler` maps request-time failures to JSON. Lambda constructor initialization failures and I/O failures propagate to the Lambda runtime because HTTP advice cannot handle a failure before Spring starts.

## Local setup

Requirements: Java 21, Maven 3.9+, MySQL 8. AWS credentials are not needed for local development or tests.

1. Create a **new** database such as `lms_approval`; do not point this project at the previous database without an explicit migration. Run `database/schema.sql` as schema owner after selecting that database. Hibernate uses `validate`, so it never creates or changes production tables. The script is for a fresh database and is not an idempotent migration.
2. Create a restricted application database user with SELECT on the shared employee/leave-type tables; SELECT/UPDATE on leave_application and leave_balance; SELECT/INSERT on approval and audit_log; SELECT/INSERT/UPDATE on notification; SELECT on login; and SELECT/INSERT/UPDATE/DELETE on auth_session. Account provisioning uses a separate administrative identity. Do not use root as the application user.
3. For an optional local demonstration on the empty schema, run `database/local-fixture.sql`. It creates manager 100, employee 200, their assignment, an annual balance and a pending leave. It creates **no login credentials**.
4. Generate a BCrypt hash in an interactive terminal; the password is entered without echo:

```powershell
cd E:\eclipse-workspace\leave-management-system-aws
mvn -Plambda package -DskipTests
java -cp "target/classes;target/lib/*" com.nexturn.lms.tools.PasswordHashTool
```

Provision each account using its own generated hash, for example with prepared statements or your database client (replace the hash placeholders before executing):

```sql
INSERT INTO login(username,password,role,emp_id)
VALUES ('manager','<manager BCrypt hash>','MANAGER',100);
INSERT INTO login(username,password,role,emp_id)
VALUES ('employee','<employee BCrypt hash>','EMPLOYEE',200);
```

Use passwords of 12–72 ASCII characters with the included utility. Existing BCrypt `$2a$`, `$2b$`, `$2y$` hashes are accepted; plaintext passwords are not. The new `login.emp_id` mapping and `auth_session` table differ from the old login schema. Old usernames are not assumed to be employee IDs.

5. Set local environment variables and run the server:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/lms_approval'
$env:DB_USERNAME='lms_app'
$env:DB_PASSWORD='<your local database password>'
mvn spring-boot:run
```

`GET http://localhost:8080/ping` returns `{"status":"UP"}`. Configuration comments explain each environment dependency in `src/main/resources/application.properties`. `CORS_ORIGINS` accepts exact comma-separated frontend origins. `AUTH_TOKEN_SECONDS` must be 60–86400; default 3600. Use HTTPS for remote authentication. No cookies are used; callers explicitly attach bearer tokens.

## API

All `/api/**` routes except login require `Authorization: Bearer <accessToken>`. Send JSON bodies with `Content-Type: application/json`. Unknown JSON fields are rejected, including the old managerId field. List routes accept `page`, `size` and `sort`; maximum page size is 100.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/ping` | Public liveness probe |
| POST | `/api/auth/login` | Body: `{"username":"manager","password":"your password"}`; returns token, expiry, empId and role |
| GET | `/api/auth/me` | Current account identity |
| POST | `/api/auth/logout` | Revoke current token; 204 |
| GET | `/api/manager/leaves/pending` | Assigned pending leaves; MANAGER only |
| POST | `/api/manager/leave/{leaveId}/approve` | Body: `{"comment":"Coverage confirmed"}` |
| POST | `/api/manager/leave/{leaveId}/reject` | Body: `{"comment":"Coverage unavailable"}` |
| GET | `/api/notifications` | Current employee's notifications |
| PATCH | `/api/notifications/{id}/read` | Mark an owned notification read; no body |
| GET | `/api/audit-logs/employee/{empId}` | Own audit or assigned team's audit |

PowerShell local example after provisioning an account:

```powershell
$credentials = Get-Credential -UserName manager
$loginBody = @{username=$credentials.UserName;password=$credentials.GetNetworkCredential().Password} | ConvertTo-Json
$session = Invoke-RestMethod http://localhost:8080/api/auth/login -Method Post -ContentType application/json -Body $loginBody
$headers = @{Authorization="Bearer $($session.accessToken)"}
Invoke-RestMethod http://localhost:8080/api/manager/leaves/pending -Headers $headers
Invoke-RestMethod http://localhost:8080/api/manager/leave/1/approve -Method Post -Headers $headers -ContentType application/json -Body '{"comment":"Coverage confirmed"}'
```

Errors have `timestamp`, `status`, `message` and `path`. Statuses: 400 invalid JSON/validation; 401 missing, invalid or expired credentials/token; 403 role/assignment denied; 404 resource missing; 409 final decision, balance or database conflict; 415 wrong content type; 500 unexpected failure with safe message. Stack traces are not returned to clients. Technical unexpected failures are logged on the server.

## Build and tests

```powershell
mvn clean test
mvn -Plambda clean verify
```

The H2 MySQL-mode test profile is test-only. Tests cover success/rejection, assignment, roles, self approval, duplicate/cancelled requests, insufficient/negative balance policy, missing balance, invalid days/cross-year requests, rollback after notification failure, concurrent duplicate decisions, concurrent shared-balance approvals, login/logout/expiry, HTTP validation, notification ownership, audit permissions and the real Lambda stream adapter. The normal build runs 26 tests and skips the opt-in MySQL test. `MySqlSmokeTest` additionally checks the supplied SQL schema, login and approval against a disposable MySQL database; see `docs/VERIFICATION.md` for its setup and the successful 27-test run. A deployed RDS smoke test is still required before production use.

The normal build produces an executable JAR. The lambda profile additionally produces `target/leave-management-system-aws-0.0.1-SNAPSHOT-lambda-package.zip`, with compiled classes/resources at the root and runtime dependencies in `lib/`. `src/assembly/bin.xml` excludes the embedded Tomcat server from the Lambda ZIP. The Gradle file delegates to Maven to avoid a second inconsistent dependency definition.

## AWS preparation (deployment is a separate next step)

`template.yml` prepares an API Gateway REST API and Java 21 Lambda function; it does not create RDS, networks or accounts. The handler is `com.nexturn.lms.StreamLambdaHandler::handleRequest`. This follows the existing starter's [AWS Spring Boot adapter](https://github.com/aws/serverless-java-container/wiki/Quick-start---Spring-Boot3) and [Lambda Java ZIP layout](https://docs.aws.amazon.com/lambda/latest/dg/java-package.html).

Before deploying: choose an AWS region, provide an existing private MySQL/RDS database (or RDS Proxy), private subnet IDs and Lambda security groups, create the fresh schema/provisioned data, and store the restricted DB username/password in an existing Secrets Manager secret. Configure DB ingress TCP 3306 from the Lambda security group, not the public internet. Use a TLS JDBC URL with `sslMode=VERIFY_IDENTITY` and a trusted CA setup. Budget database connections for reserved concurrency × the Hikari pool limit (5 × 3 in this starter), and adjust after measuring. API stage throttling is included; production abuse protection, monitoring and credential policies remain deployment configuration.

Authenticate locally with an AWS CLI SSO profile or temporary role credentials. **Do not paste AWS passwords, root credentials or access keys into chat or source files.** Once AWS setup is agreed, a deployment can use:

```powershell
mvn -Plambda clean verify
sam validate --template-file template.yml
sam deploy --template-file template.yml --guided --profile <your-sso-profile>
```

Do not run `sam build` against this template's ZIP CodeUri; Maven already built the archive. CloudFormation resolves the supplied secret at deployment; the deploy identity needs access to that secret/KMS key. The function runtime needs only logging/VPC permissions for this implementation. Secret rotation requires a function configuration update/redeployment to refresh environment values. Database connections use TLS and credentials injected at deployment; AWS credentials are never application settings. The API base URL is a stack output. Verify `/ping`, login, both decisions, notifications and audit against MySQL after deployment.

This backend has not been deployed to AWS. See `docs/VERIFICATION.md` for the checks actually run.
