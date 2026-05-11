# FileEasy Execution Prompt

You are responsible for one bounded project task.

## Task Identity

- Task ID: `FE-T005`
- Task name: `Admin page file management core`
- Stage: `implementation`

## Goal

- Implement the FileEasy admin page core for file listing, search, preview, rename, download, single delete, and batch delete within the approved product scope.

## Non-goals

- Do not add batch download.
- Do not add log query pages or dashboards.
- Do not extend preview to unsupported office document types.
- Do not modify PRD or design documents.

## Required Reading

1. PRD: `docs/FileEasy-PRD-v1.0.md`
2. Design: `docs/FileEasy-Design-v1.0.md`
3. Workflow: `docs/FileEasy-AI-Workflow-v1.0.md`
4. Visual spec: `docs/FileEasy-Visual-Interaction-Spec-v1.0.md`
5. Wireframe spec: `docs/FileEasy-Wireframe-Spec-v1.0.md`
6. Task pack: `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-T005-admin-page-core.json`

Read these first. Do not implement from memory.

## Allowed Paths

- `apps/web-admin/src`
- `apps/web-admin/index.html`

## Control Paths

- `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-T005-admin-page-core.json`
- `docs/ai-execution-framework/projects/fileeasy/prompts/FE-T005-admin-page-core.md`

## Forbidden Paths

- `apps/_deprecated_server`
- `docs/FileEasy-PRD-v1.0.md`
- `docs/FileEasy-Design-v1.0.md`

If you believe a forbidden path must change, stop implementation and report the reason back to PM. Do not cross scope on your own.

## What You Must Complete

### 1. Admin entry flow

Implement the `/admin` experience so that authenticated admins can enter the file-management view in a way that matches the approved product scope.

### 2. File-management core

Support the approved operations:

- file listing
- search
- preview
- rename
- download
- single delete
- batch delete

### 3. Rename constraints

The admin page must respect the approved rename rule:

- only the base name may be edited
- extension changes must not be allowed
- the UI and API integration must not imply that extension editing is legal

### 4. Preview boundary

The admin page must follow the approved preview boundary:

- preview only for supported types
- do not extend preview to unsupported office-document types
- unsupported types may show metadata and download only

### 5. Product-shell compliance

The admin experience must remain inside FileEasy scope:

- no batch download
- no logs dashboard
- no monitoring panel
- no legacy Xplay admin-shell styling bias

## Deliverables

1. Admin page implementation
2. Preview and file action integration
3. Verification notes

## Acceptance Checks

1. When an authenticated admin opens `/admin`, they can list, search, preview, rename, download, delete, and batch delete files within the approved scope.
2. When a user attempts to rename a file, the UI and API integration only allow the base name to change and do not allow extension changes.
3. Visually confirm that admin interactions follow the approved FileEasy visual and wireframe guidance without adding dashboard-style or logging surfaces.

## Recommended Verification

At minimum, perform:

1. Build verification
   `npm --prefix apps/web-admin run build`

2. If feasible, add focused verification for:
- rename flow
- preview boundary behavior
- batch delete behavior

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
