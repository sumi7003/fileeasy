# FE-V105 Remove Remote Admin Page PM Acceptance

## Goal

Verify that FileEasy v1 no longer exposes the remote `/admin` management page or remote file-management APIs, while preserving the remote upload page.

## 1. Task Identity

- Task:
  - `FE-V105-remove-remote-admin-page`
- Source change:
  - `FE-CR001 Remove Remote Admin Page From FileEasy v1`
- Decision:
  - v1 keeps upload-only remote web surface

## 2. APK Shell Result

- APK homepage no longer shows remote admin entry:
  - record `pass` or exact issue
- APK homepage still shows upload URL/QR:
  - record `pass` or exact issue
- APK-local password/service controls still work or remain visible as designed:
  - record `pass` or exact issue
- Player and monitor entries remain absent:
  - record `pass` or exact issue

## 3. Remote Route Result

- `GET /`:
  - expected: upload page loads
  - result:
- `GET /admin`:
  - expected: no file-management UI
  - accepted behaviors: `404`, `410`, or redirect to `/`
  - result:
- Unknown route behavior:
  - confirm it does not expose admin management

## 4. Remote API Result

- Upload APIs still work:
  - `/api/v1/ping`
  - `/api/v1/home/summary`
  - `/api/v1/auth/login`
  - `/api/v1/upload/init`
  - `/api/v1/upload/chunk`
  - `/api/v1/upload/status/{uploadId}`
  - `/api/v1/upload/complete`
- Management APIs no longer succeed in FileEasy mode:
  - `/api/v1/files`
  - `/api/v1/files/{id}`
  - `/api/v1/files/{id}/preview`
  - `/api/v1/files/{id}/download`
  - `PATCH /api/v1/files/{id}`
  - `DELETE /api/v1/files/{id}`
  - `/api/v1/files/batch-delete`
  - `/uploads/{path...}`

## 5. Build And Device Verification

- Web build:
  - `npm --prefix apps/web-admin run build`
- Android compile:
  - `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home GRADLE_USER_HOME=.gradle-home ./gradlew compileFileeasyDebugKotlin`
- Real device install if available:
  - `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home GRADLE_USER_HOME=.gradle-home ./gradlew installFileeasyDebug`
- Same-WiFi manual route checks if available:
  - `/`
  - `/admin`
  - `/api/v1/files`
  - `/uploads/nonexistent`

## 6. Scope Compliance

- Changed files stay within task pack `allowedPaths` or `controlPaths`:
  - record `pass` or exact issue
- No forbidden PRD/design/code paths changed:
  - record `pass` or exact issue
- No unrelated feature cleanup bundled:
  - record `pass` or exact issue

## 7. Final Decision

- `accepted` or `rework`
- If rework, list exact remaining issues

## 8. Follow-up

- If upload page is unstable:
  - keep or reopen Wi-Fi reachability blocker
- If local APK file management is desired later:
  - open a separate v1.1 or new-scope task
- If docs still mention remote admin:
  - send back to `FE-CR001-doc-alignment-remove-admin`
