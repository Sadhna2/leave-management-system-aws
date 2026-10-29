# Verification — Java 17 deployment build, 28 October 2026

The requested backend and scoped frontend are in `E:\eclipse-workspace\leave-management-system-aws`. The previous project remains unchanged.

The Maven compiler targets Java 17 (`--release 17`); all 49 application classes in the ZIP have class-file major version 61. Tests executed on **Temurin Java 17.0.20.1**, using Maven 3.9.16. Lambda is configured with AWS's current Java 17 runtime identifier, **java17.al2023**. Maven `clean verify -Plambda` completed successfully.

| Test suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| MySqlSmokeTest | 1 | 0 | 0 | 1 |
| RestrictedMySqlSmokeTest | 1 | 0 | 0 | 0 |
| ApprovalServiceTest | 25 | 0 | 0 | 0 |
| StreamLambdaHandlerTest | 1 | 0 | 0 | 0 |

**27 enabled tests passed; one optional fresh-database test was skipped.** The regression suite used the actual restricted `lms_app` database user and demonstrated that the shared employee-assignment lock works with SELECT-only permissions. It checked the balance deduction and transactional outbox write against isolated MySQL 8.0.46, with rollback afterward. Approval tests cover assignment/role checks, rejection, duplicate/cancelled decisions, balance policy, failed notification rollback and concurrent balance updates. The actual Lambda stream adapter returns 200 for `/ping` and 401 for unauthenticated protected routes.

The exact AWS database initializer was exercised on an isolated MySQL instance: fresh schema creation, idempotent update, initial manager provisioning and table-level application grants succeeded. The application's database identity has no ALL PRIVILEGES grant. The employee lock was changed to a shared lock after the full frontend check exposed MySQL's additional privilege requirement for `FOR UPDATE`.

The frontend production build (`npm run build`) succeeded with Vite 8.3.5, React 18.3.1 and the previous project's Bootstrap styling. Browser checks verified login, the assigned manager's pending queue, mandatory comments, successful approval and removal from the pending queue. Direct database checks confirmed used days 3, remaining days 17, APPROVED status and acting manager 100. The audit/notification screens use only authorized scoped endpoints.

The Lambda ZIP is `backend-828346eb2db07607.zip`: 56,063,585 compressed bytes and 62,956,677 uncompressed bytes. It contains the RDS regional trust store, deployment schema, main code and runtime libraries; it excludes test classes, H2 and embedded Tomcat. All 52 Java source/test files have documentation comments, and a source scan found no explicit try/catch blocks.

The complete CloudFormation template passed `cfn-lint` for ap-south-1 with no errors or warnings, and AWS accepted its template-summary validation. The API stage is **stage1**. Actual resource creation and deployed API/frontend checks are tracked separately in `deployment/STATUS.md`.

The opt-in `RestrictedMySqlSmokeTest` is restricted to localhost URLs and requires `MYSQL_RESTRICTED_URL`, `MYSQL_RESTRICTED_PASSWORD` and `MYSQL_RESTRICTED_MANAGER_PASSWORD`. It expects the disposable local fixture initialized with `lms_app` and manager 100. Do not point local test fixtures at production data.

## Final archive/demo update

31 enabled tests passed on Java 17; two optional MySQL tests were skipped in this final build. ArchiveLambdaHandlerTest adds five local SDK HTTP tests. The updated initializer passed a separate double-run against isolated MySQL, verifying demo BCrypt authentication, manager assignment, one pending request and a 20-day balance. Deployed manager and employee logins passed; request 1 remains pending. A post-migration SNS event reached S3 through the Java 17 archive function. The new ZIP contains 50 application classes, all major version 61.
