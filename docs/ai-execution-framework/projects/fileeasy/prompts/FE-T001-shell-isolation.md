# FileEasy Execution Prompt

You are responsible for one bounded project task.

## Task Identity

- Task ID: `FE-T001`
- Task name: `FileEasy pure service shell isolation`
- Stage: `implementation`

## Goal

- Turn the `fileeasy` flavor into a pure service-shell APK that does not expose player-facing UI, product wording, or interaction paths while keeping `xplay` behavior intact.

## Non-goals

- Do not implement chunk upload backend logic.
- Do not redesign the upload or admin web pages.
- Do not modify PRD or design documents.
- Do not change `_deprecated_server`.
- Do not change `xplay` feature behavior except where flavor gating requires isolation.

## Required Reading

1. PRD: `docs/FileEasy-PRD-v1.0.md`
2. Design: `docs/FileEasy-Design-v1.0.md`
3. Workflow: `docs/FileEasy-AI-Workflow-v1.0.md`
4. Task pack: `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-T001-shell-isolation.json`

Read these first. Do not implement from memory.

## Allowed Paths

- `apps/android-player/build.gradle.kts`
- `apps/android-player/src/fileeasy`
- `apps/android-player/src/main/java/com/xplay/player/MainActivity.kt`
- `apps/android-player/src/main/java/com/xplay/player/ProductFlavorConfig.kt`
- `apps/android-player/src/main/res/values`
- `apps/android-player/src/main/AndroidManifest.xml`

## Control Paths

- `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-T001-shell-isolation.json`
- `docs/ai-execution-framework/projects/fileeasy/prompts/FE-T001-shell-isolation.md`

## Forbidden Paths

- `apps/web-admin`
- `apps/_deprecated_server`
- `docs/FileEasy-PRD-v1.0.md`
- `docs/FileEasy-Design-v1.0.md`

If you believe a forbidden path must change, stop implementation and report the reason back to PM. Do not cross scope on your own.

## What You Must Complete

### 1. Flavor isolation

Make sure `fileeasy` is a truly separate product flavor with:

- independent `applicationId`
- independent app naming
- independent product wording
- a pure service-shell product surface

### 2. Pure service homepage

When `fileeasy` launches, the homepage must keep only:

- service status
- upload QR code
- upload address
- admin entry
- password entry

It must not expose:

- player role section
- player toggle
- player fullscreen entry
- player status wording
- player-related setup hints
- Xplay player product semantics

### 3. Preserve Xplay behavior

When `xplay` launches, player-facing behavior must remain available. Do not let FileEasy shell cleanup break the original Xplay product behavior.

### 4. Product wording cleanup

Under `fileeasy`, clean up player-oriented wording, including where applicable:

- product title
- subtitle
- control-center style wording
- service notification wording
- APK naming wording

### 5. Design-boundary compliance

Follow the design document's pure-service disable matrix. Hiding one button is not enough.

Isolation is complete only when:

- the FileEasy homepage has no player entry
- the FileEasy homepage does not depend on playlist, device registration, or player connection state
- the FileEasy shell does not expect users to understand player concepts

## Deliverables

1. Android flavor isolation changes
2. Verification notes
3. Delivery-ready summary

## Acceptance Checks

1. When the `fileeasy` flavor launches, the homepage shows only file-service actions and no player-facing UI.
2. When the `xplay` flavor launches, existing player-facing behavior remains available.
3. `GRADLE_USER_HOME=.gradle-home ./gradlew compileFileeasyDebugKotlin`

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
