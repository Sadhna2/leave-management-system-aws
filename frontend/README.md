# Member 3 frontend

This frontend selectively reuses the previous project's React/Bootstrap LeaveHub theme, responsive layout, shared components, login form, approval cards and notification list. Only login, approvals, notifications and the newly required audit screen are included. There are no registration, leave application, employee management, HR, leave-type, allocation, calendar or holiday screens.

The old frontend trusted a stored role and queried unrelated employee/HR APIs. This adaptation uses the scoped backend's bearer token, `/api/auth/me`, pending manager queue, paginated owned notifications and authorized audit endpoints. A required comment is sent with each decision; manager identity is supplied by the backend session. Passwords are never stored. The bearer token is retained in sessionStorage until logout or expiry. Backend authorization remains authoritative.

Run `npm ci`, then `npm run dev` or `npm run build` with Node 22.12 or later. Development defaults to localhost:8080 when `public/config.json` has an empty apiBaseUrl. After AWS provisioning, set the public apiBaseUrl to the API Gateway stack output ending in `/stage1`, then upload the contents of `dist/` (not this source folder) to the frontend S3 bucket. No AWS credentials or database secrets belong in frontend files.

Hash routing supports refreshing nested screens on static hosting. The deployment serves compiled files from a separate encrypted/versioned S3 bucket over HTTPS at `/index.html`. The user approved public TLS-only read access after AWS blocked CloudFront pending account verification. config.json contains only the public API URL and is fetched without browser caching. The final S3 build has flat filenames for straightforward browser upload.

All source files contain documentation comments. All request errors are handled by the central API client and promise handlers; there are no explicit try/catch blocks.
