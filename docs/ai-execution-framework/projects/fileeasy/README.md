# FileEasy AI Project Package

This directory is the execution-ready AI package for FileEasy v1. It is used by the PM agent to prepare work, dispatch bounded prompts to implementation agents, and verify delivery before release.

## Product Summary

FileEasy turns an Android phone, tablet, or box into a LAN file receiving server. The APK shows an upload URL and QR code. Users on the same Wi-Fi or hotspot open `/`, log in with the shared password, and upload files through the browser.

FileEasy v1 scope:

- Android-only LAN server APK.
- Wi-Fi and hotspot LAN access.
- Remote upload page `/`.
- Password login with a 7-day session.
- Single-file and multi-file upload.
- 8MB chunk upload with resumable sessions.
- 4GB single-file limit.
- No player features.
- No remote `/admin` page in v1.
- No remote file list, preview, rename, delete, batch delete, or download in v1.

## Directory Contents

- `project-bundle.json`
  - Registered source of truth for PRD, design, workflow, and supporting specs.
- `task-packs/`
  - One bounded implementation or validation task per file.
- `prompts/`
  - Execution prompts generated from approved task packs.
- `acceptance/`
  - PM checklists used to accept or reject implementation work.
- `reports/`
  - PM acceptance reports, integration reports, and current-stage summaries.
- `templates/`
  - Reusable templates for release-blocker tasks and PM acceptance.
- `change-requests/`
  - Requirement or scope changes that must update PRD/design before code work.

## Source Of Truth

FileEasy work must be aligned with these documents:

1. `docs/FileEasy-PRD-v1.0.md`
2. `docs/FileEasy-Design-v1.0.md`
3. `docs/FileEasy-AI-Workflow-v1.0.md`
4. `docs/ai-execution-framework/projects/fileeasy/project-bundle.json`

Do not dispatch implementation work directly from chat notes. If the requirement changes, update PRD/design first, then create or update the task pack and prompt.

## Dispatch Rule

Always dispatch implementation in this order:

1. Confirm the requirement exists in PRD/design.
2. Create or update the task pack in `task-packs/`.
3. Validate the task pack.
4. Write the execution prompt in `prompts/`.
5. Write or update the PM checklist in `acceptance/`.
6. Send the prompt to the implementation AI.
7. Review changed files against the task scope.
8. Accept only when the checklist is satisfied.

Task validation:

```bash
npm run ai:validate:bundle -- docs/ai-execution-framework/projects/fileeasy/project-bundle.json
npm run ai:validate:task -- docs/ai-execution-framework/projects/fileeasy/task-packs/<task>.json
```

Scope validation:

```bash
npm run ai:check-scope -- docs/ai-execution-framework/projects/fileeasy/task-packs/<task>.json <changed-files...>
```

Publish gate:

```bash
npm run ai:validate:publish -- docs/ai-execution-framework/projects/fileeasy/task-packs/<task>.json
npm run ai:publish -- docs/ai-execution-framework/projects/fileeasy/task-packs/<task>.json
```

## Implementation Verification

Common FileEasy verification commands:

```bash
npm --prefix apps/web-admin run build
```

```bash
cd apps/android-player
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
GRADLE_USER_HOME=../../.gradle-home \
./gradlew compileFileeasyDebugKotlin
```

For APK generation:

```bash
cd apps/android-player
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
GRADLE_USER_HOME=../../.gradle-home \
./gradlew assembleFileeasyDebug
```

For device install:

```bash
cd apps/android-player
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
GRADLE_USER_HOME=../../.gradle-home \
./gradlew installFileeasyDebug
```

## Current Stage

FileEasy has completed the first product-definition and implementation-acceptance pass. Remote `/admin` was removed from v1 scope by `FE-CR001`, and the code-side removal was accepted in `FE-V105`.

Current working stage:

1. Close remaining release blockers.
2. Keep tasks narrowly scoped.
3. Validate real-device Wi-Fi and hotspot behavior.
4. Validate batch upload state behavior.
5. Prepare release-ready APK once blockers are closed.

## Accepted Work

- `FE-T001`: pure service shell isolation.
- `FE-T002`: service lifecycle and LAN address strategy.
- `FE-T003`: upload backend and resumable session core.
- `FE-T004`: upload page web flow.
- `FE-CR001`: PRD/design alignment to remove remote admin.
- `FE-V101`: environment and build verification.
- `FE-V102`: end-to-end integration validation.
- `FE-V103`: real-device network validation.
- `FE-V105`: remove remote admin page from implementation.

## Closed By Scope Change

- `FE-T005`: remote admin/file management work is no longer part of FileEasy v1 after `FE-CR001`.

## Current Rework

- `FE-V106`: batch upload home-state fix.

Acceptance result:

- Core batch-state design is directionally correct.
- Build checks passed.
- PM acceptance is blocked because the implementation bundled unrelated network guidance and upload-record shortcut work.

Required action:

1. Keep only the batch upload state fix.
2. Remove network guidance changes from the FE-V106 diff.
3. Remove upload-record shortcut changes from the FE-V106 diff.
4. Re-run web and Android verification.
5. Re-submit for PM acceptance.

See:

- `reports/FE-V106-Batch-Upload-Home-State-Acceptance-Report.md`
- `acceptance/FE-V106-batch-upload-home-state-pm-acceptance.md`

## PM Rules

- No task may expand FileEasy v1 scope without a PRD and design update.
- No implementation should reintroduce remote `/admin`.
- No player behavior should be required for FileEasy to function.
- No generated cache directories should be committed.
- Reports should be written for every formal PM acceptance or rejection.
