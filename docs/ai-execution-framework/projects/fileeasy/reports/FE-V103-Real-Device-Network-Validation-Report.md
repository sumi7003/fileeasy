# FE-V103 Real-Device and Network-Scene Validation Report

Date: `2026-05-08`
Task: `FE-V103`
Stage: `validation`

## 1. Validation scope

- This run covered:
  - connected Android real-device verification via `adb devices -l`
  - web-admin production build verification
  - on-device homepage screenshot capture
  - on-device homepage UI-tree capture
  - on-device admin-entry jump verification
  - same-subnet desktop-to-device LAN access checks for:
    - `/api/v1/ping`
    - `/`
    - `/admin`
    - `/api/v1/auth/login`
    - `/api/v1/files`
    - `/api/v1/upload/init`
    - `/api/v1/upload/chunk`
    - `/api/v1/upload/complete`
  - code-path inspection for still-unexecuted scenes:
    - LAN address resolution
    - QR refresh trigger conditions
    - boot-completed service restart path
- Commands executed:
  - `adb devices -l`
    - result: connected device detected
    - device: `MAX_5`
    - Android version: `13`
  - `npm --prefix apps/web-admin run build`
    - result: `passed`
- Real-scene limits in this run:
  - hotspot scene was not executed
  - network-switch scene was not executed
  - upload interruption / reconnect scene was not executed
  - reboot recovery scene was not executed
  - full protected upload-and-management flow was not completed in this run because same-Wi-Fi connectivity became unstable again during live requests
- Supporting observations gathered for later manual validation:
  - `LanAddressResolver` distinguishes `WIFI`, `HOTSPOT`, `OTHER`, and `UNAVAILABLE`
  - `FileEasyShellScreen` refreshes LAN state on:
    - service-state changes
    - network available/lost/capabilities/link-property callbacks
    - lifecycle `ON_RESUME`
  - upload QR is only generated when service state is `RUNNING` and a resolved upload URL exists
  - `BootReceiver` listens for `BOOT_COMPLETED` and restarts `LocalServerService` when `host_mode` is enabled

## 2. Passed scenarios

- Real device homepage shell renders the expected FileEasy shell
  - screenshot and UI dump both show:
    - `文件服务状态`
    - service state `运行中`
    - upload QR visible
    - upload URL visible as `http://192.168.110.141:3000/`
    - `进入文件管理后台`
    - `修改密码`

- Same-subnet LAN access is possible at least initially
  - after moving the workstation to `192.168.110.x`, the following requests succeeded:
    - `GET /api/v1/ping` -> `200`
    - `GET /` -> `200`
    - `GET /admin` -> `200`
    - `GET /api/v1/files` -> `401 Unauthorized`
  - this confirms:
    - the FileEasy server can be reached from another device on the same subnet
    - upload-page route is externally reachable at least once
    - admin route is externally reachable at least once
    - protected file-management API is auth-gated

- Protected login and upload initialization can succeed over real same-Wi-Fi access
  - observed successful requests:
    - `POST /api/v1/auth/login` with the real FileEasy password -> `200`, body returned `{"status":"ok","token":"admin-token"}`
    - `POST /api/v1/upload/init` with `xplay_auth=admin-token` -> `200`, body returned a real `uploadId`
  - this confirms:
    - password-based login is functional on real device
    - the protected upload entrypoint is reachable and accepts authenticated requests at least once

- On-device admin entry is functional as a navigation action
  - tapping `进入文件管理后台` transitions from the homepage shell into the admin WebView shell
  - evidence:
    - screenshot titled `文件管理后台`
    - UI tree showing an embedded `android.webkit.WebView`

## 3. Failed scenarios

- Wi-Fi same-LAN access
  - status: `failed / flaky`
  - initial same-subnet requests succeeded, but follow-up requests to:
    - `/api/v1/ping`
    - `/`
    - `/admin`
    failed repeatedly with connection errors
  - at the same time:
    - device homepage still showed `运行中`
    - device homepage still showed the same upload URL and QR
    - `adb shell ss -ltn` still showed `*:3000` in `LISTEN`
  - current judgment:
    - same-Wi-Fi reachability is unstable and not release-safe

- Real-device admin landing content is not the expected FileEasy admin experience
  - after tapping `进入文件管理后台`, the captured screen shows the legacy `Xplay ADMIN CENTER` / `设备管理` style content instead of the expected FileEasy admin page
  - current judgment:
    - admin-entry scenario is failing on real device

- Same-Wi-Fi upload and management actions
  - status: `failed / blocked by real-network instability`
  - observed behavior:
    - `POST /api/v1/auth/login` succeeded
    - `POST /api/v1/upload/init` succeeded and returned a real `uploadId`
    - authenticated `GET /api/v1/files` unexpectedly returned `404 Not Found` with empty body instead of the expected managed-file list
    - a later upload run failed before `chunk` / `complete` could finish because the same-Wi-Fi connection dropped again
    - a five-attempt retry loop then showed `GET /api/v1/ping` failing all five times with `curl: (7) Failed to connect`
  - at the same time:
    - `adb shell pidof com.xplay.fileeasy` still returned a live process id
    - `adb shell ss -ltn` still showed `*:3000` in `LISTEN`
  - current judgment:
    - authenticated upload and management cannot be considered passable on real same-Wi-Fi in the current build

- Hotspot access
  - status: `unverified`
  - not executed in this run

- Address and QR refresh after network switching
  - status: `unverified`
  - not executed in this run

- Interrupted upload recovery
  - status: `unverified`
  - not executed in this run

- Reboot recovery
  - status: `unverified`
  - not executed in this run

## 4. Release blockers

- Same-Wi-Fi reachability is unstable
  - same-subnet external access succeeded once and then failed repeatedly while the device still reported service `运行中`

- Real-device admin entry lands on the wrong admin experience
  - current observed result is legacy Xplay admin content instead of the expected FileEasy admin page

- Authenticated management path is not release-safe
  - authenticated `GET /api/v1/files` returned `404 Not Found`
  - authenticated upload could start at `upload/init` but could not be carried through `chunk` / `complete` because the real same-Wi-Fi connection dropped again

- Hotspot / network-switch / reconnect / reboot scenes are still not executed
  - FE-V103 cannot close until these required real-device scenes are actually run

## 5. Non-blocking follow-ups

- Web build remains healthy and does not currently add a separate FE-V103 blocker.

- Code-path inspection suggests the project has implementation hooks for:
  - LAN refresh
  - QR refresh
  - boot recovery
  - resumable upload
  - these are useful readiness signals, but they do not replace live validation

## 6. PM handoff

- Recommendation: `Option B`
- Do not move to release-closure review yet.
- Reason:
  - this run now has real-device evidence, and it already exposed:
    - unstable same-Wi-Fi access
    - wrong admin landing behavior on real device
    - authenticated file-management path returning unexpected `404`
    - authenticated upload flow blocked by real same-Wi-Fi instability after `upload/init`
  - hotspot / network-switch / reconnect / reboot scenes remain unexecuted
- What is needed next:
  - open blocker-fix work for:
    - same-Wi-Fi access stability
    - real-device admin-entry landing behavior
    - authenticated `/api/v1/files` returning `404`
  - continue manual real-device validation for:
    - hotspot upload-page and `/admin` access
    - network switch with address + QR refresh confirmation
    - interrupted upload recovery after reconnect and page re-entry
    - reboot recovery of service status, address, and QR
  - record each scene as:
    - passed
    - failed
    - stable repro or flaky repro
- FE-V103 should remain open until the blocker scenes are fixed or confirmed.
