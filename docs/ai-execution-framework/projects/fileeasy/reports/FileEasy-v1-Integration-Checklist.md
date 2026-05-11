# FileEasy v1 Integration Checklist

## 1. APK Shell

1. FileEasy branding appears instead of player branding
2. homepage contains only:
   - service status
   - upload QR code
   - upload address
   - password-change entry
3. no player entry, player status, or player wording remains
4. no remote admin entry appears in FileEasy mode

## 2. Service and Address

1. service starts and reports real state
2. Wi-Fi scene shows a valid upload address
3. hotspot scene shows a valid upload address
4. QR code matches the current upload address
5. no misleading QR appears when no usable LAN address exists

## 3. Upload Page Flow

1. QR users land on `/`
2. page is visible before authentication
3. upload actions are blocked before login
4. valid password unlocks upload actions
5. invalid password shows explicit feedback

## 4. Upload Behavior

1. single-file upload works
2. multi-file upload works
3. unsupported file types are rejected
4. files larger than `4GB` are rejected
5. per-file progress is visible
6. interrupted uploads can resume
7. refreshed browser sessions can resume after file reselect

## 5. Backend Upload Core

1. `upload/init` enforces allowlist and `4GB` limit
2. `upload/status/{uploadId}` returns:
   - `totalChunks`
   - `uploadedChunkIndexes`
   - `missingChunkIndexes`
   - `status`
3. incomplete uploads cannot complete
4. merged file size must match declared file size
5. same-name completion auto-renames safely
6. no legacy upload bypass remains for FileEasy

## 6. Removed Remote Admin Scope

1. `/admin` does not expose a FileEasy remote management page
2. remote file list is not exposed in FileEasy mode
3. remote preview is not exposed in FileEasy mode
4. remote rename is not exposed in FileEasy mode
5. remote download is not exposed in FileEasy mode
6. remote single delete is not exposed in FileEasy mode
7. remote batch delete is not exposed in FileEasy mode
8. any previous admin-page validation blocker is closed or downgraded by `FE-CR001`

## 7. Product Boundary

1. no batch download appears
2. no log page appears
3. no monitoring dashboard appears
4. no extra non-PRD admin surfaces appear
5. `/` remains the only remote user-facing page for v1
6. FileEasy mode does not expose `/api/v1/files`, preview, download, rename, delete, or batch-delete APIs

## 8. Real Device Scenarios

1. same-Wi-Fi access works
2. hotspot access works
3. network switch refreshes address and QR correctly
4. reboot recovery works
5. reconnect after interruption works

## 9. Final Output Format

Record the result in five sections:

1. passed items
2. failed items
3. release blockers
4. non-blocking follow-ups
5. release recommendation
