# FileEasy Execution Prompt

You are responsible for one bounded project task.

## Task Identity

- Task ID: `FE-T004`
- Task name: `Upload page web flow`
- Stage: `implementation`

## Goal

- Implement the FileEasy upload page flow for login, multi-file upload, progress visibility, resume status feedback, and error messaging using the approved visual and wireframe specs.

## Non-goals

- Do not implement backend storage logic beyond required integration calls.
- Do not build the full admin page.
- Do not add monitoring or advanced dashboard content.
- Do not modify PRD or design documents.

## Required Reading

1. PRD: `docs/FileEasy-PRD-v1.0.md`
2. Design: `docs/FileEasy-Design-v1.0.md`
3. Workflow: `docs/FileEasy-AI-Workflow-v1.0.md`
4. Visual spec: `docs/FileEasy-Visual-Interaction-Spec-v1.0.md`
5. Wireframe spec: `docs/FileEasy-Wireframe-Spec-v1.0.md`
6. Task pack: `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-T004-web-upload-page.json`

Read these first. Do not implement from memory.

## Allowed Paths

- `apps/web-admin/src`
- `apps/web-admin/index.html`

## Control Paths

- `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-T004-web-upload-page.json`
- `docs/ai-execution-framework/projects/fileeasy/prompts/FE-T004-web-upload-page.md`

## Forbidden Paths

- `apps/android-player/src/main/java/com/xplay/player/server/storage`
- `apps/_deprecated_server`
- `docs/FileEasy-PRD-v1.0.md`
- `docs/FileEasy-Design-v1.0.md`

If you believe a forbidden path must change, stop implementation and report the reason back to PM. Do not cross scope on your own.

## What You Must Complete

### 1. Upload page entry flow

Implement the upload page so that:

- QR code users land on the upload page
- the page is visible before authentication
- authentication is required before upload actions can proceed

### 2. Login and session behavior

Align the page with the approved auth rules:

- password entry is clear and minimal
- authenticated state is reflected correctly
- error feedback is explicit when login fails

### 3. Upload interaction

Support the approved upload flow:

- single-file and multi-file selection
- per-file progress
- resumable status visibility
- actionable error messages

### 4. State feedback

The page must make long-running actions legible:

- uploading
- paused/interrupted
- resumed
- failed
- completed

### 5. Product-shell compliance

The page must look and feel like FileEasy, not like the legacy Xplay admin UI:

- no dashboard-like framing
- no player-oriented wording
- no monitoring or system-panel feel

## Deliverables

1. Upload page implementation
2. Integration with upload APIs
3. Verification notes

## Acceptance Checks

1. When a user scans the QR code and opens the upload page, they see the page before authentication and then complete password validation before uploading.
2. When one or more files are uploading, the page shows per-file progress, resumable status, and actionable error feedback.
3. Visually confirm that the upload page matches the approved FileEasy visual and wireframe guidance and does not look like the legacy Xplay admin UI.

## Recommended Verification

At minimum, perform:

1. Build verification
   `npm --prefix apps/web-admin run build`

2. If feasible, add focused verification for:
- login-before-upload flow
- multi-file progress visibility
- interrupted/resumed upload UI behavior

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
