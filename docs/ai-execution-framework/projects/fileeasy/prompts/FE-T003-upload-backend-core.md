# FileEasy Execution Prompt

You are responsible for one bounded project task.

## Task Identity

- Task ID: `FE-T003`
- Task name: `Chunk upload backend and resumable session core`
- Stage: `implementation`

## Goal

- Implement the backend core for allowlisted file upload, resumable chunk sessions, `4GB` init validation, same-name auto-renaming on complete, and persistent upload state tracking.

## Non-goals

- Do not redesign homepage UI.
- Do not build the full admin page.
- Do not add unsupported file types beyond the approved whitelist.
- Do not modify PRD or design documents.

## Required Reading

1. PRD: `docs/FileEasy-PRD-v1.0.md`
2. Design: `docs/FileEasy-Design-v1.0.md`
3. Workflow: `docs/FileEasy-AI-Workflow-v1.0.md`
4. Task pack: `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-T003-upload-backend-core.json`

Read these first. Do not implement from memory.

## Allowed Paths

- `apps/android-player/src/main/java/com/xplay/player/server/LocalServerService.kt`
- `apps/android-player/src/main/java/com/xplay/player/server/storage`
- `apps/android-player/src/main/java/com/xplay/player/utils`
- `apps/android-player/src/main/AndroidManifest.xml`

## Control Paths

- `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-T003-upload-backend-core.json`
- `docs/ai-execution-framework/projects/fileeasy/prompts/FE-T003-upload-backend-core.md`

## Forbidden Paths

- `apps/web-admin`
- `apps/_deprecated_server`
- `docs/FileEasy-PRD-v1.0.md`
- `docs/FileEasy-Design-v1.0.md`

If you believe a forbidden path must change, stop implementation and report the reason back to PM. Do not cross scope on your own.

## What You Must Complete

### 1. Upload initialization

Implement or correct `upload/init` behavior so that:

- the server validates `fileSize <= 4GB`
- file-size handling uses `Long`
- file type is checked against the approved whitelist before upload starts
- files over `4GB` return the fixed error: `单文件不能超过 4GB`
- unsupported file types return the fixed whitelist error

### 2. Upload session model

The upload session model must be strong enough for resumable upload recovery.

At minimum it must support:

- `uploadId`
- `fileName`
- `fileSize`
- `chunkSize`
- `totalChunks`
- `uploadedChunks`
- `uploadedChunkIndexes`
- `status`
- `expiresAt`
- `createdAt`

If the current storage model is insufficient, extend it within the allowed scope.

### 3. Chunk upload and recovery

Implement or correct:

- persistence of every uploaded chunk index
- safe handling of out-of-order chunks
- safe handling of duplicate chunk upload
- `GET /api/v1/upload/status/{uploadId}` so it can return:
  - `totalChunks`
  - `uploadedChunkIndexes`
  - `missingChunkIndexes`
  - `status`

### 4. Upload completion and final file creation

In `upload/complete` or the equivalent completion chain:

- verify all chunks are present before merge
- verify merged size matches declared `fileSize`
- create final file metadata only after successful merge
- determine the final `displayName` only at formal file finalization time

### 5. Same-name auto rename

PRD requires that same-name upload must not overwrite and must not fail directly.

Implement at finalization time:

- if the target directory already has the same display name
- generate a safe final name such as `文件(1).ext`
- return the final persisted file name to the client

## Deliverables

1. Upload session backend
2. Storage and metadata changes
3. Verification notes

## Acceptance Checks

1. When a file larger than `4GB` is initialized for upload, the backend rejects it with the fixed size-limit error.
2. When an upload is interrupted and resumed, the backend reports `uploadedChunkIndexes` and `missingChunkIndexes` accurately enough for the client to continue.
3. When a newly completed file conflicts with an existing display name, the backend preserves the old file and finalizes the new file with an auto-renamed safe name.

## Recommended Verification

At minimum, perform:

1. Compile verification
   `GRADLE_USER_HOME=.gradle-home ./gradlew compileFileeasyDebugKotlin`

2. If feasible, add focused backend verification for:
- upload session status recovery
- same-name auto rename
- oversize upload rejection

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
