# FileEasy v1 Next-Phase Plan

## 1. Objective

Move FileEasy from task-level acceptance to product-level release readiness.

The next stage is not another feature-splitting stage. It is an integration and release-closure stage.

## 2. Current Position

Accepted task packs:

1. `FE-T001`
2. `FE-T002`
3. `FE-T003`
4. `FE-T004`
5. `FE-T005`

This means FileEasy now has:

1. an isolated Android product shell
2. service lifecycle and LAN address behavior
3. resumable upload backend core
4. upload page flow
5. a historical admin page core task result that is no longer part of v1 release scope after `FE-CR001`

`FE-CR001` changes the v1 release target to upload-only remote web access. Remote `/admin` management, file list, preview, rename, delete, batch delete, and download are removed from v1 acceptance.

## 3. Next-Stage Success Criteria

FileEasy v1 may move toward release sign-off only after the following are completed:

1. end-to-end integration validation
2. real-device and network-scene validation
3. release-blocker closure
4. final PM release assessment

## 4. Next-Stage Work Packages

### `FE-V101` Environment and build verification

- Goal:
  - restore a usable Java/Android build environment for reliable final verification
- Output:
  - one verified Android compile path
  - one verified web build path
  - one short environment status note
- Owner type:
  - execution AI plus user for local environment actions if needed

### `FE-V102` End-to-end integration validation

- Goal:
  - validate the complete product path:
  - APK homepage -> QR/upload page -> password login -> upload flow -> resumable status
- Output:
  - one integration test record
  - one pass/fail matrix
- Owner type:
  - execution AI for scripted or code-level validation
  - user for hands-on device/browser steps where required

### `FE-V103` Real-device network-scene validation

- Goal:
  - verify Wi-Fi, hotspot, reconnect, and reboot scenarios on actual devices
- Output:
  - one device-scene verification record
  - one issue list grouped by severity
- Owner type:
  - user-led execution with PM checklist support

### `FE-V104` Release-gap closure

- Goal:
  - close only the blockers found in `FE-V102` and `FE-V103`
- Output:
  - bounded fix prompts
  - updated acceptance result
- Owner type:
  - execution AI under PM-managed prompts

### `FE-V105` Remove remote admin implementation alignment

- Goal:
  - remove or disable remote `/admin` and remote file-management exposure in FileEasy mode according to `FE-CR001`
- Output:
  - APK homepage no longer advertises remote admin
  - React `/admin` route no longer exposes FileEasy management UI
  - FileEasy mode remote file-management APIs are unavailable
  - upload page behavior remains intact
- Owner type:
  - execution AI under PM-managed prompt

## 5. PM Dispatch Order

Recommended order:

1. `FE-V101` environment and build verification
2. `FE-V105` remove remote admin implementation alignment
3. `FE-V102` upload-only end-to-end integration validation
4. `FE-V103` real-device scene validation
5. `FE-V104` release-gap closure if blockers are found

## 6. PM Rules For This Stage

1. do not open new feature scope unless PM first updates PRD and design
2. do not treat task-level acceptance as release readiness
3. any new coding work must come from a bounded validation or blocker-fix prompt
4. final release judgment must be based on integrated behavior, not isolated task success
5. do not use remote admin management behavior as a v1 release blocker after `FE-CR001`

## 7. Immediate Next Action

Immediate next action:

1. archive the integration checklist as the source of truth for end-to-end validation
2. dispatch `FE-V105` if code still exposes remote admin behavior
3. prepare the next prompt for upload-only `FE-V102`

Recommended next dispatch:

- `FE-V101` if build environment is still unstable
- `FE-V105` before final upload-only integration validation if implementation still exposes remote admin
- otherwise upload-only `FE-V102`
