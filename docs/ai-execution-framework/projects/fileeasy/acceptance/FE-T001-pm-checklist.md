# FE-T001 PM Acceptance Checklist

## Goal

Verify that FileEasy shell isolation is complete enough to allow the project to move forward without mixing player-facing product behavior into the `fileeasy` flavor.

## Checks

1. Flavor identity
- `fileeasy` has an independent `applicationId`
- `fileeasy` has independent product naming and wording

2. Homepage scope
- FileEasy homepage shows only:
  - service status
  - upload QR code
  - upload address
  - admin entry
  - password entry
- FileEasy homepage does not show:
  - player role section
  - player switch
  - player fullscreen entry
  - player status wording

3. Product semantics
- FileEasy no longer asks the user to understand player concepts
- FileEasy homepage does not depend on playlist or player connection state

4. Xplay protection
- `xplay` flavor behavior remains available
- isolation changes did not unintentionally break player-facing flow

5. Verification
- compile evidence is provided
- any unverified areas are explicitly listed

## Reject Conditions

- Any remaining visible player entry in FileEasy shell
- Any FileEasy homepage logic that depends on player state
- Any unexplained regression risk for `xplay`
