# Verified deployment - final Java 17 and demo update

AWS account **773384831067**, region **ap-south-1**. Stack **leave-management-system-aws-v2** completed its latest update on 28 October 2026, 19:27 IST.

- Frontend: https://leave-management-system-frontend-773384831067-ap-south-1.s3.ap-south-1.amazonaws.com/index.html
- Backend: https://96h1em86gl.execute-api.ap-south-1.amazonaws.com/stage1
- Health: GET /ping returned 200 / UP; unauthenticated protected route returned 401; CORS accepted the exact HTTPS frontend origin.
- All four project functions now use **Java 17 on Amazon Linux 2023** (`java17.al2023`): main API, outbox, archive and bootstrap.
- Private encrypted RDS **leave-management-system-rds-aws**, schema **lms_approval**, remains available with one-day automated backups.
- The archive was converted from inline Python to `com.nexturn.lms.aws.ArchiveLambdaHandler::handleRequest`; its existing role and SQS mapping were preserved. Memory is 512 MB; timeout is 30 seconds.
- A non-sensitive SNS event `61e30776-d835-4d80-8c1f-e91bd7da1700` traversed SQS and the Java archive function and appeared at `audit/events/61e30776-d835-4d80-8c1f-e91bd7da1700.json` in the private artifact/archive bucket.

## Accounts and demo data

Manager username: **manager**, employee ID **100**. Password secret: **InitialManagerSecret-HgyoWZFuAaYc**.

Demo employee username: **employee.demo**, employee ID **900100**, manager ID **100**. Password secret: **leave-management-system-demo-employee-aws**.

Retrieve passwords privately using AWS Secrets Manager's Retrieve secret value action. No actual password or bearer token is stored in the PDF, Postman collection or source.

The user explicitly approved a synthetic demo employee, leave type **900001**, a **20-day** balance and one pending **two-day** leave. Deployed request **1** spans **29-30 October 2026** and remains **PENDING** for user testing. Both actual manager and employee logins succeeded in the deployed browser; the manager saw the pending request and the employee saw owned notifications navigation. Both sessions were logged out after verification. No production decision was made during this check.

The seed is idempotent: it does not duplicate the labelled request or reset a used balance/decided request. No public registration or employee-administration API was added.

## Build verification

Final Java 17 build: **31 enabled tests passed; two opt-in MySQL tests skipped**. Five archive tests use a real SDK client against a local HTTP fixture. The earlier restricted MySQL regression passed separately. The updated exact initializer was run twice on actual isolated MySQL with Java 17, checking BCrypt demo credentials, assignment, one pending leave, initial balance and restricted application grants. All **50 application class files** in the new ZIP target Java 17 (major version 61).

## Hosting and cleanup

CloudFront distribution creation was blocked by account verification. The approved fallback serves compiled frontend files through S3 HTTPS, with public TLS-only GetObject access and no public listing/writes. Backend artifacts, audit archives and RDS remain private. No email/SMS subscriber is configured.

Confirmed removed: unused VPC `vpc-045d07c90f37c0db7`, its three empty subnets and internet gateway; old RDS `next26-rds-db` without final snapshot/retained automated backups; old Lambda `next26_aws_function`. The old API Gateway, old Lambda logs/roles, and failed first stack record were not deleted. Two unused CloudFront configuration objects remain in the active stack, without a distribution.

The original source project was not modified. Temporary local database/backend/frontend test processes were stopped. The PDF guide and Postman collection are in docs/.

Final archive/bootstrap artifact: `archive-java17-3d4a404dbbf3b1ec.zip`. Final template: `cloudformation-all-java17-3d4a404dbbf3b1ec.json`. Main API/outbox keep `backend-828346eb2db07607.zip`.
