# FE-V106 Batch Upload Homepage State PM Acceptance

## Goal

Verify that FileEasy APK homepage stays in receiving/uploading state until the whole selected upload batch finishes.

## 1. Task Identity

- Task:
  - `FE-V106-batch-upload-home-state`
- Problem:
  - batch upload can switch APK homepage to transfer-complete after the first file completes
- Expected fix:
  - homepage completion is based on the whole effective batch, not the first completed file

## 2. Scope Compliance

- Changed files stay within `allowedPaths` or `controlPaths`:
  - record `pass` or exact issue
- No remote admin/file-management scope reintroduced:
  - record `pass` or exact issue
- No unrelated network guidance/history/icon work bundled:
  - record `pass` or exact issue

## 3. Build Verification

- Web build:
  - `npm --prefix apps/web-admin run build`
  - result:
- Android compile:
  - from `apps/android-player`
  - `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home GRADLE_USER_HOME=../../.gradle-home ./gradlew compileFileeasyDebugKotlin`
  - result:

## 4. Code Review Checks

- Upload page creates or exposes batch-level state before only the first file completes:
  - result:
- Existing resumable tasks avoid duplicate session creation:
  - result:
- APK homepage receiving/completed state uses effective queue state:
  - result:
- Completed state requires all effective batch files to be complete:
  - result:

## 5. Scenario Acceptance

### Single File

- During upload:
  - expected: homepage shows receiving/uploading
  - result:
- After upload completes:
  - expected: homepage shows transfer-complete
  - result:

### Multi-File Batch

- Select 3 valid files and start upload.
- After file 1 completes and file 2/3 are not complete:
  - expected: homepage still shows receiving/uploading
  - result:
- After file 2 completes and file 3 is not complete:
  - expected: homepage still shows receiving/uploading
  - result:
- After all files complete:
  - expected: homepage shows transfer-complete
  - result:

### Pause Or Network Interruption

- During a multi-file batch, pause or interrupt before all files complete:
  - expected: homepage must not show transfer-complete
  - result:

### Invalid Files In Batch

- Batch includes unsupported or over-4GB files:
  - expected: invalid files fail on upload page; valid pending files keep homepage from false completion
  - result:

## 6. Final Decision

- `accepted` or `rework`
- If rework, list exact remaining failure reasons

## 7. Follow-Up

- If real-device verification was not performed:
  - mark as code/build accepted only and request PM device validation
- If network guidance or upload history issues remain:
  - keep them as separate tasks
