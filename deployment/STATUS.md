# Verified deployment

AWS account **773384831067**, Mumbai **ap-south-1**. CloudFormation stack **leave-management-system-aws-v2** is **UPDATE_COMPLETE**.

- [Open frontend](https://leave-management-system-frontend-773384831067-ap-south-1.s3.ap-south-1.amazonaws.com/index.html)
- [API health](https://96h1em86gl.execute-api.ap-south-1.amazonaws.com/stage1/ping) — verified HTTP 200, `{"status":"UP"}`.
- API base: `https://96h1em86gl.execute-api.ap-south-1.amazonaws.com/stage1`; REST API stage **stage1**.
- Main Lambda: **leave-management-system-lambda-aws**, verified in the AWS Console as **Java 17 on Amazon Linux 2023** (`java17.al2023`), handler `com.nexturn.lms.StreamLambdaHandler::handleRequest`.
- Private encrypted RDS **leave-management-system-rds-aws** is available. Schema initialization completed successfully; automated backups retained for one day under the account free-plan limits.
- SNS, SQS, dead-letter queue, scheduled Java outbox relay and Python archive Lambda are created. A non-sensitive SNS smoke event reached `audit/events/7b3e5a41-0aca-4c59-91a5-7c8cfa991002.json` in private S3 through SQS and the archive worker.
- Only the four compiled frontend files were uploaded to the separate frontend bucket. Source and built `config.json` now point to the live API.

## Login

Username: **manager**. Retrieve its generated password privately from [the initial manager secret](https://ap-south-1.console.aws.amazon.com/secretsmanager/secret?name=InitialManagerSecret-HgyoWZFuAaYc&region=ap-south-1) in the AWS Console. No password or token is recorded in this project, report or frontend build. The initial manager has employee ID 100. Shared employee assignments, pending leaves and balances must be provided by the other modules before real approval work is available. Public registration is intentionally absent.

## Verification and limits

API health returned 200; protected manager API returned 401 without authentication; CORS preflight returned the exact HTTPS S3 frontend origin. The hosted frontend rendered correctly and a synthetic nonexistent login reached the backend and returned its expected invalid-credentials message. A valid production login/approval was not exercised with the generated secret. Local positive approval, employee notification/read and audit flows were browser-verified; 27 enabled tests passed on actual Java 17. See `docs/VERIFICATION.md`.

CloudFront distribution creation was blocked by AWS account verification. The user approved public, TLS-only GetObject access to compiled frontend files through S3's HTTPS `/index.html` URL. Backend artifacts, archived events and RDS remain private. No email/SMS subscriber is configured. The Java API may have an initial cold-start delay after inactivity.

The user confirmed deletion of unused VPC `vpc-045d07c90f37c0db7` and its three empty subnets/internet gateway; AWS confirmed deletion. The user later confirmed permanent deletion of old RDS `next26-rds-db`, including `nexturnapplndb`, without a final snapshot or retained automated backups. AWS confirmed **Successfully deleted DB instance next26-rds-db**. The RDS list now contains only the new available project instance. The new project RDS is retained.

Temporary local backend, frontend preview and isolated MySQL verification processes were stopped. The original source project remains unchanged.
