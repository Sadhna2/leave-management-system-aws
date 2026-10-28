# Verification — 28 October 2026

The backend was written directly into `E:\eclipse-workspace\leave-management-system-aws`. The previous project was used only as a read-only source of the required shared model and Member 3 files. Its frontend and unrelated modules were not copied. The original AWS starter was backed up in this chat workspace before updating it.

## Completed checks

- `mvn -Plambda clean verify`: exit code 0, using Java 21 and Maven 3.9.16.
- `ApprovalServiceTest`: 25 passed, 0 failures/errors. Covers decisions, assignment/role checks, self approval, comments, balances, duplicates, cancellation, cross-year restrictions, rollback, competing transactions, authentication and notification/audit access.
- `StreamLambdaHandlerTest`: 1 passed, 0 failures/errors. The actual stream handler returned HTTP 200 for `/ping` and HTTP 401 for an unauthenticated protected API request.
- `MySqlSmokeTest`: 1 passed, 0 failures/errors against an isolated MySQL Community Server 8.0.46 instance using a new temporary data directory and loopback port 13316. The supplied schema and local fixture loaded successfully. Hibernate `validate`, BCrypt login, token authentication, balance deduction, decision persistence, notification creation and actor-attributed audit persistence passed. The test transaction rolled back afterward.
- Total: **27 tests passed; no failures, errors or skips** in this verification run. The MySQL test is opt-in and is skipped during ordinary builds without its environment variables.
- The generated Lambda ZIP was extracted and executed using its own production classes/dependencies, without embedded Tomcat or H2. It connected to the isolated MySQL schema, passed Hibernate validation and returned `{"status":"UP"}` through the real Lambda stream handler.
- The archive contains compiled main classes/resources and runtime libraries; no tests, test database configuration, Mockito, H2 or embedded Tomcat. Compressed size: 46,246,034 bytes; uncompressed size: 51,882,291 bytes.
- All 45 Java source/test files have documentation comments. Source scan found no explicit `try`/`catch` blocks.
- Maven and assembly XML parsed successfully. SAM YAML syntax parsed successfully with SnakeYAML, including its CloudFormation tags.

Artifacts in the project:

- `target/leave-management-system-aws-0.0.1-SNAPSHOT.jar` — executable local server.
- `target/leave-management-system-aws-0.0.1-SNAPSHOT-lambda-package.zip` — Lambda deployment archive.
- `target/surefire-reports/` — machine-readable and text test results.

## Repeating the optional MySQL check

Use a **disposable, fresh MySQL database**, load `database/schema.sql` and `database/local-fixture.sql`, and set these variables only in the terminal used for the check:

```powershell
$env:MYSQL_SMOKE_URL='jdbc:mysql://127.0.0.1:<test-port>/<disposable-database>'
$env:MYSQL_SMOKE_USERNAME='<test database user>'
$env:MYSQL_SMOKE_PASSWORD='<test database password>'
mvn -Dtest=MySqlSmokeTest test
```

The test expects the unchanged optional local fixture and creates its own temporary test login inside a rolled-back transaction. Never point it at production or an existing shared database. The isolated instance used for this verification was shut down afterward; no existing MySQL service or database was modified.

## Remaining environment-dependent checks

No AWS resources were created and no deployment was performed. AWS SAM CLI was not installed, so semantic `sam validate`, CloudFormation deployment, IAM/Secrets Manager access, VPC routing/security groups, RDS TLS/certificates, secret refresh, region/account quotas and deployed API behavior remain to be checked during the deployment stage. The local MySQL and packaged-handler checks do not verify those AWS settings.
