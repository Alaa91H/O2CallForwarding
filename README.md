<div align="center">

# O2 Call Forwarding

### A clean Android interface for managing GSM call-forwarding settings locally

<img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
<img src="https://img.shields.io/badge/UI-Material%203-4285F4?style=for-the-badge&logo=materialdesign&logoColor=white" alt="Material 3" />
<img src="https://img.shields.io/badge/Network-GSM%20MMI%20%2F%20USSD-2563EB?style=for-the-badge" alt="GSM MMI / USSD" />
<img src="https://img.shields.io/badge/Service-Local--first-16A34A?style=for-the-badge" alt="Local-first" />

</div>

---

## Overview

**O2 Call Forwarding** is an Android utility for configuring call forwarding through standard GSM/MMI codes. It is designed primarily for **O2 Germany**, with a simple Material 3 interface that keeps the workflow on the device and hands commands to the mobile network.

The app provides dedicated controls for common forwarding modes instead of requiring users to remember or manually type network codes.

## Features

- Forward **all calls**.
- Forward when **busy**.
- Forward when there is **no answer**.
- Forward when the phone is **unreachable**.
- Configure all supported **conditional forwarding** modes together.
- Cancel forwarding rules from the same interface.
- Query the current forwarding status where the carrier/device supports the required USSD response flow.
- One-tap forwarding to the **O2 voicemail short code (333)**.
- Support for a **custom destination number**.
- Material 3 interface with system light/dark theme and Android 12+ dynamic color.
- Localized UI for multiple languages, with English fallback.

## How It Works

The app maps each forwarding action to the corresponding GSM supplementary-service code and sends it through Android's telephony APIs.

For example, the O2 voicemail destination uses the short code `333` with the relevant forwarding prefix for the selected condition.

> Carrier support for individual MMI/USSD operations can vary. If a direct request cannot be completed by Android or the network, the app can hand the prepared code to the phone dialer for user confirmation.

## Permissions

The app requests `CALL_PHONE` when required to submit supported USSD/MMI requests directly.

Permissions are requested only for the telephony action that needs them; the project does not require a cloud backend to manage forwarding settings.

## Build

1. Open the project in a recent Android Studio version.
2. Allow Gradle to sync the project.
3. Connect a **real Android device with an active SIM**.
4. Build and run the app.

A physical device is recommended because emulators generally cannot execute real carrier USSD/MMI operations.

## Release Signing

Tagged releases are built through GitHub Actions using repository secrets for the signing identity. The workflow verifies the signing certificate fingerprint before producing release artifacts.

Keep the production keystore and credentials private and preserve the same signing identity for upgrade compatibility.

## Compatibility Notes

- The UI and project are optimized around O2 Germany's voicemail workflow.
- Standard GSM forwarding codes are widely used, but actual behavior remains carrier- and device-dependent.
- Android versions, OEM telephony implementations, dual-SIM behavior, and carrier restrictions can affect direct USSD execution.

## Disclaimer

This is an **independent, unofficial utility** and is not affiliated with or endorsed by Telefónica Germany / O2.

Users should verify their carrier's forwarding rules and any possible call-forwarding charges before enabling a configuration.

---

<div align="center">

**Simple call-forwarding controls without memorizing carrier codes.**

</div>
