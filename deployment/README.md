# AWS deployment: Member 3

The complete deployment is defined in `cloudformation.json`. The earlier root `template.yml` remains an example for deployments using an existing database; use the complete template for this requested new RDS setup.

Target account: **773384831067**. Region: **ap-south-1**. Stack: **leave-management-system-aws-v2**. The template references a ZIP whose filename contains its SHA-256 prefix, so a rebuilt backend has a new deployment artifact rather than silently reusing old code.

| Component | Configuration |
|---|---|
| Main backend | Java 17 Lambda `leave-management-system-lambda-aws`; handler `com.nexturn.lms.StreamLambdaHandler::handleRequest` |
| API | Regional REST API `leave-management-system-api-aws`; ANY proxy; stage `stage1` |
| Database | New private RDS MySQL `leave-management-system-rds-aws`; Single-AZ db.t4g.micro, 20 GiB encrypted gp3, one-day automated backups (account free-plan limit) |
| Frontend | Compiled `frontend/dist` files in a separate encrypted/versioned S3 bucket; public HTTPS GetObject only, opened at `/index.html` |
| Notifications | In-app records and a transactional outbox; a scheduled Lambda publishes committed events to the encrypted SNS topic |
| Queue | SNS delivers to SQS; failed archive batches retry and then move to the dead-letter queue |
| Archive | SQS-triggered Lambda writes stable event IDs under `audit/events/` in the private project S3 bucket |
| Network | Dedicated isolated VPC and two subnets; database ingress from project Lambda only; SNS interface endpoint and S3 gateway endpoint |
| Credentials | Three generated Secrets Manager credentials retained from the first stack attempt and reused by ARN parameters |

CloudFormation initializes the schema through `SchemaBootstrapHandler`, creates the restricted `lms_app` user, and provisions an initial manager employee/account with ID 100 and username `manager`. Its generated password is in the stack's `InitialManagerSecretArn` output. Retrieve that secret privately in AWS; do not put it into frontend files, source control or chat. No public self-registration endpoint exists. Employee records, assignments, balances and pending leaves come from the other project modules or administrator-provided integration data.

Database initialization was exercised against an isolated local MySQL instance, including an idempotent update and restricted grants. Manager assignment uses a shared locking read, which is compatible with SELECT-only employee permissions; a dedicated regression test checks approval with the restricted user. The leave and balance rows still use write locks to serialize decisions and balance deductions.

The HTTP decision transaction writes the status, balance change, approval, notification, audit and outbox event together. AWS publishing occurs in a separate scheduled invocation. Delivery is at least once; the event UUID is the archive key, so retries address the same logical object. S3 versioning preserves prior versions. No email or SMS subscription is configured; SNS currently delivers to the project's SQS queue.

The API and bootstrap Lambdas have VPC/logging execution permissions. The relay additionally has Publish access to this SNS topic and its encryption operations. The archive worker can receive/delete messages from this queue and put objects only into this bucket's `audit/` prefix. No AWS access keys are embedded in application configuration. The bootstrap uses administrative database credentials only to initialize the new database; the running application has table-level grants.

RDS connections verify the server hostname and CA using `src/main/resources/rds-truststore.p12`, built from the official Mumbai regional bundle at `https://truststore.pki.rds.amazonaws.com/ap-south-1/ap-south-1-bundle.pem`. Its public trust-store password is not an authentication credential. Refresh the bundle when AWS rotates certificates. Runtime database secret rotation requires a function configuration refresh; no automatic rotation is configured by this starter.

After stack creation, set `frontend/dist/config.json` to `{"apiBaseUrl":"<ApiBaseUrl stack output>"}`. Upload the contents of `frontend/dist/` to `FrontendBucketName`, with index.html at the bucket root. That JSON contains only the public API URL ending in `/stage1`. The frontend loads config.json without browser caching, and backend CORS includes the frontend bucket HTTPS origin. The frontend uses hash routing, so static hosting needs no route rewrite.

Before reporting deployment complete, verify stack completion, `/stage1/ping`, protected API HTTP 401, frontend login/rendering, and SNS-to-SQS-to-S3 delivery. The first manager has no team or pending leaves until shared employee/leave data is supplied. Local demo fixtures are for disposable databases only.

Running RDS, Secrets Manager, the SNS private endpoint, storage, requests and delivery use AWS credits or incur charges. The frontend bucket and credential secrets are retained if the stack is removed; RDS is snapshotted. The former `next26-rds-db` was outside this stack; the user later requested and confirmed its permanent deletion without retained backups. See STATUS.md for the verified deletion state.

## Account-specific deployment recovery

The first stack rolled back at the VPC quota. After explicit confirmation, unused VPC `vpc-045d07c90f37c0db7` and its three empty subnets/internet gateway were deleted. The existing RDS VPC was preserved. The retry created dedicated VPC `vpc-054ba38c266bf8be6` and retained successfully created resources when CloudFront and RDS settings were rejected.

AWS requires account verification before creating a CloudFront distribution. The user approved serving only compiled frontend files through S3 HTTPS instead. The template permits anonymous `s3:GetObject` over TLS in the separate frontend bucket; it grants no listing or write access. Backend ZIPs, audit archives and RDS remain private. The account also rejected seven-day backup retention, so the new RDS instance requests one day. Unused CloudFront response headers and origin access control remain harmless stack resources, without a distribution.

Current recovery template: `cloudformation-java17-stage1-s3-https.json` in the artifact bucket. `cloudformation.json` in this directory matches it. The stack reuses retained credentials through ARN parameters, without printing secret values. See `STATUS.md` for the latest verified cloud state; a submitted update is not proof of a working application.

## Final Java 17 and demo update

The archive worker now uses Java 17 (`ArchiveLambdaHandler`) with its existing scoped SQS/S3 role. The Python implementation was removed. A generated demo credential is in Secrets Manager as `leave-management-system-demo-employee-aws`; username `employee.demo`, employee ID 900100, assigned to manager 100. Schema version `2-demo-employee` seeds one labelled two-day pending leave and a 20-day balance idempotently. See deployment/STATUS.md and docs/Leave-Management-System-AWS-Guide.pdf for the final verified setup.
