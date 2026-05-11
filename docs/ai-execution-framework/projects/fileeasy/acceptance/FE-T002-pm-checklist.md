# FE-T002 PM Acceptance Checklist

## Goal

Verify that FileEasy can restore and present its local service reliably, and that homepage LAN address and QR behavior follow the approved design rules.

## Checks

1. Lifecycle
- service can be started from the app shell
- service state is visible on the homepage
- failure to start is not silently reported as healthy

2. Boot recovery
- reboot recovery path exists
- `BootReceiver` or equivalent lifecycle trigger is in place
- design-required startup behavior is implemented

3. Foreground-service behavior
- persistent notification exists
- notification wording matches FileEasy product wording
- lifecycle remains inside FileEasy pure-service shell

4. LAN address strategy
- address selection prefers Wi-Fi IPv4 first
- hotspot IPv4 is considered
- fallback to other usable non-loopback IPv4 is handled
- unusable state is explicitly represented

5. QR behavior
- QR points to upload page address
- QR and address refresh on the required triggers
- no misleading QR is shown when there is no usable LAN address

6. Verification
- compile evidence is provided
- device/network limitations are explicitly listed if they block validation

## Reject Conditions

- homepage still shows stale or misleading LAN address
- no clear no-address fallback state
- reboot restoration path is missing
- foreground-service expectations are not met
