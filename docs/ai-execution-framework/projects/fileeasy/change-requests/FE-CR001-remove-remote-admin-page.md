# FE-CR001 Remove Remote Admin Page From FileEasy v1

## Decision

FileEasy v1 removes the remote management page `/admin`.

The v1 product boundary changes from:

- APK service shell
- remote upload page `/`
- remote admin page `/admin`

to:

- APK service shell
- remote upload page `/`
- APK-local service/password controls only

## Reason

The current project goal is to ship a stable LAN upload server APK first.

The remote management page adds disproportionate release risk:

- admin route and WebView landing have already produced validation blockers
- authenticated file list and management API have already produced validation blockers
- preview, rename, delete, batch delete, and download are not required for the core upload value
- removing remote admin narrows the attack surface and simplifies v1 acceptance

## New v1 Scope

Remote LAN users can:

- open the upload page by QR code or direct URL
- authenticate with the shared password
- upload allowed files
- resume interrupted uploads within the approved session window
- see upload progress and final success/failure state

APK-local users can:

- see service status
- see/copy upload URL
- show upload QR code
- change the shared password through the APK-local control only
- keep the service running as a foreground service

## Removed From v1

Remote LAN users cannot:

- open a remote management page at `/admin`
- browse all uploaded files remotely
- preview uploaded files remotely
- rename uploaded files remotely
- delete uploaded files remotely
- batch delete uploaded files remotely
- download uploaded files remotely through a management UI

## API Boundary After Change

The FileEasy remote web surface should keep only upload-page APIs, including:

- `/api/v1/ping`
- `/api/v1/home/summary`
- `/api/v1/auth/login`
- `/api/v1/auth/logout`
- `/api/v1/upload/init`
- `/api/v1/upload/chunk`
- `/api/v1/upload/status/{uploadId}`
- `/api/v1/upload/complete`
- upload cancellation if already required by the upload page

The FileEasy remote web surface should not expose remote management APIs, including:

- `/api/v1/files`
- `/api/v1/files/{id}`
- `/api/v1/files/{id}/preview`
- `/api/v1/files/{id}/download`
- `PATCH /api/v1/files/{id}`
- `DELETE /api/v1/files/{id}`
- `/api/v1/files/batch-delete`
- `/uploads/{path...}` as a remote download surface

## Routing Boundary After Change

- `/` remains the upload page.
- `/admin` must not render a remote management page.
- `/admin` may return `404`, `410`, or redirect to `/`, but it must not expose file-management capability.
- The APK homepage must not show a remote admin entry.
- Any existing APK-local password change entry may remain if it does not depend on the remote admin page.

## Documentation Impact

PRD, design, workflow, acceptance reports, and release checklist must be updated so FileEasy v1 is described as an upload-only remote web surface.

## Implementation Impact

Implementation should remove or disable:

- APK admin-entry button/WebView path in FileEasy mode
- React `/admin/*` route and admin page entry
- remote file-management API exposure in FileEasy mode
- stale acceptance checks that require remote preview, rename, delete, download, or batch delete

## Acceptance Summary

This change is accepted only when:

- upload page still works
- remote `/admin` no longer exposes management UI
- APK homepage no longer advertises remote admin
- FileEasy mode no longer exposes remote file-management APIs
- player and monitor UI remain absent
- same-WiFi upload core remains the primary release target
