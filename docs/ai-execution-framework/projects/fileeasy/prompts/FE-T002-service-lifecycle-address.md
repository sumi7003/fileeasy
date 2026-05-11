# FileEasy Execution Prompt

You are responsible for one bounded project task.

## Task Identity

- Task ID: `FE-T002`
- Task name: `Service lifecycle and LAN address strategy`
- Stage: `implementation`

## Goal

- Implement the FileEasy service lifecycle, startup recovery, and LAN address plus QR refresh strategy defined in the design document.

## Non-goals

- Do not implement chunk upload logic.
- Do not redesign the upload or admin web pages.
- Do not change file metadata storage.
- Do not modify PRD or design documents.

## Required Reading

1. PRD: `docs/FileEasy-PRD-v1.0.md`
2. Design: `docs/FileEasy-Design-v1.0.md`
3. Workflow: `docs/FileEasy-AI-Workflow-v1.0.md`
4. Task pack: `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-T002-service-lifecycle-address.json`

Read these first. Do not implement from memory.

## Allowed Paths

- `apps/android-player/src/main/java/com/xplay/player/MainActivity.kt`
- `apps/android-player/src/main/java/com/xplay/player/BootReceiver.kt`
- `apps/android-player/src/main/java/com/xplay/player/server/LocalServerService.kt`
- `apps/android-player/src/main/java/com/xplay/player/utils`
- `apps/android-player/src/main/AndroidManifest.xml`

## Control Paths

- `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-T002-service-lifecycle-address.json`
- `docs/ai-execution-framework/projects/fileeasy/prompts/FE-T002-service-lifecycle-address.md`

## Forbidden Paths

- `apps/web-admin`
- `apps/_deprecated_server`
- `docs/FileEasy-PRD-v1.0.md`
- `docs/FileEasy-Design-v1.0.md`

If you believe a forbidden path must change, stop implementation and report the reason back to PM. Do not cross scope on your own.

## What You Must Complete

### 1. Service lifecycle

Implement the lifecycle behavior required by the design document:

- service can be started from the app shell
- service can be restored after device reboot
- service runs as a foreground service
- service state is surfaced back to the homepage

### 2. Boot recovery

Use the approved lifecycle chain so that:

- `BootReceiver` restores service startup after reboot
- failure to restore does not silently look healthy
- homepage can reflect the difference between running and not running

### 3. Foreground-service behavior

Ensure the foreground service behavior matches design expectations:

- persistent notification exists
- notification wording matches FileEasy product wording
- service startup path remains inside the pure-service product shell

### 4. LAN address strategy

Implement the address-selection rules from the design document:

- prefer active Wi-Fi IPv4
- then hotspot IPv4
- then other non-loopback IPv4
- if none is usable, mark the service as not currently reachable via LAN

### 5. QR refresh behavior

Ensure the homepage data refreshes when required:

- app start
- local service start success
- Wi-Fi or hotspot change
- IP address change
- app returns to foreground

### 6. No misleading output

If there is no usable LAN address:

- do not present a misleading QR code
- clearly show that no usable LAN address is currently available

## Deliverables

1. Lifecycle implementation
2. Address and QR refresh implementation
3. Verification notes

## Acceptance Checks

1. When the device has a usable Wi-Fi or hotspot IPv4 address, the homepage shows a LAN upload address and a QR code that targets the upload page.
2. When the device has no usable LAN address, the homepage clearly shows that no upload address is available and does not present a misleading QR code.
3. When the device reboots, FileEasy restores the local service according to the lifecycle rules in the design document.

## Recommended Verification

At minimum, perform:

1. Compile verification
   `GRADLE_USER_HOME=.gradle-home ./gradlew compileFileeasyDebugKotlin`

2. If feasible, add focused verification for:
- boot-recovery path
- address refresh behavior
- no-address fallback state

If some validation cannot be run, say so explicitly.

## Publish Policy

- Remote: `origin`
- Working branch: `fileeasy`
- Target branch: `main`
- Auto-push after acceptance: `false`
- Accepted status required before publish: `true`
- Pull request required: `true`

## Required Return Format

1. Completed work
2. Changed files
3. Verification performed
4. Known risks
5. Unfinished items
6. Decisions that must return to PM/design
7. Publish readiness and anything blocking remote publish
