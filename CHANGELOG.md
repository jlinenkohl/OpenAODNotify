# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Debug: Build Identifier in Version String**: Debug builds now append the short git commit hash (and a `-dirty` flag if the working tree has uncommitted changes) to `versionName`, e.g. `1.4.1-debug+a1b2c3d`. Computed at build time in `app/build.gradle.kts`. Release builds are unaffected — `versionName` stays exactly as set (e.g. `1.4.1`), since releases are already distinguished by git tags.
- **Debug: Notification-Triggered-Overlay Count Telemetry**: Added a second fixed-size 7-slot rolling counter tracking how many valid (filtered) notifications actually triggered the overlay per day, shown alongside the existing overlay-visible-time table in the debug panel. Used as an inferential proxy to separate genuine AOD-on time from this app's own notification-driven activity, since the two can't be directly distinguished with public APIs.
- **Build: Explicit Release Signing Verification**: Added a `verifyReleaseSigning` Gradle task that runs after `assembleRelease` and fails the build loudly if the release APK isn't signed with the expected release certificate. Previously, signing happened silently inside `packageRelease` with no dedicated task or check that the correct keystore/credentials were actually used. Now required as part of the release protocol.

### Fixed
- **Build: Resolved all lint build errors**: Removed an invalid top-level `<uses-permission>` declaration for `BIND_NOTIFICATION_LISTENER_SERVICE` in `AndroidManifest.xml` (signature-level permission, only valid via the `<service android:permission=...>` attribute which was already present and unaffected) and replaced 8 uses of `android:tint` with `app:tint` per AppCompat lint requirements. `./gradlew lintDebug`/`lintRelease` previously failed with 9 errors; both now pass clean.

## [1.4.1] - 2026-09-05

### Security
- **Removed hardcoded release-signing credentials from tracked source**: `RELEASE_STORE_PASSWORD`/`RELEASE_KEY_PASSWORD` were briefly hardcoded in plaintext in the root `build.gradle.kts` during signing-config setup. Never committed to git history (verified), but now loaded exclusively from the untracked, gitignored `local.properties` (or CI environment variables), with no fallback literal.

### Changed
- **Battery: Capped Breathing Animation to a Configurable Frame Rate (default 30fps)**: Replaced the `ObjectAnimator`-based infinite alpha pulse in `OpenAODOverlayService` with a manually-paced `Handler` loop. `ValueAnimator.setFrameDelay()` is a no-op on API 35+, so this was necessary to actually reduce framebuffer/compositor wakeups during long AOD sessions. No visible change to the breathing effect at the default target.

### Added
- **Debug: Runtime-Adjustable Breathing FPS**: Added a stepper + apply control to the debug tools panel (debug builds only) to tune and apply the breathing animation's target frame rate at runtime, for on-device battery/visual-smoothness tuning without a rebuild.
- **Debug: Overlay-Visible Time Telemetry**: Added a persisted, 7-slot fixed-size rolling tally of overlay-visible wall-clock time (14 fixed `SharedPreferences` keys forever, no growth, no pruning needed; survives reset/force-stop/reboot), surfaced in the debug tools panel — a proxy for how much display-on time is actually attributable to this app, since Android doesn't attribute that cost per-app in its own battery stats.

## [1.4] - 2026-06-15

### Added
- **Dedicated Power Settings Activity**: Migrated power status configuration to a standalone UI for better organization.
- **RGB Sliders**: Integrated real-time RGB color mixing for Plugged, Charging, and Low Battery states.
- **Draggable Positioning**: Added support for independent, draggable positioning of the power indicator (separate from notification indicators).
- **Dual-Layer Rendering**: Updated `OpenAODOverlayService` to support simultaneous rendering of notification and power status layers.
- **Intent-Driven Lifecycle**: Improved IPC via `power_preview` and `ACTION_REFRESH` for instant UI feedback.

### Fixed
- **Overlay Persistence**: Resolved an issue where the overlay would skip updates if a notification was already visible when power state changed.
- **Position Sync**: Fixed a bug where dragging the power handle wouldn't persist to the correct `power_` prefixed preference keys.

## [1.3-dev] - 2026-03-16

### Added
- **Intelligent Filtering Engine**: Implemented multi-level notification logic to reduce AOD clutter.
- **Discovery Activity**: New UI to manage whitelisted apps and system categories (Message, Call, Email, etc.).
- **TTL (Time-To-Live)**: Notifications older than 60 minutes (configurable) are now ignored to prevent stale AOD triggers.
- **Clearable Check**: Added option to ignore non-dismissible/ongoing notifications.
- **Automatic Rule Discovery**: System now captures and persists unique notification patterns (Package|Channel|Category) for easy whitelisting.
- **Global Override**: Master toggle to switch between "Allow All Valid" and "Custom Whitelist" modes.
- **Enhanced Debug Export**: Added prompt to choose between saving logs to Downloads or sharing via system sheet.

### Fixed
- **Android 14+ Compatibility**: Added `RECEIVER_EXPORTED` flag to the screen state receiver.
- **UI Integrity**: Improved Z-order management in DiscoveryActivity and added `fitsSystemWindows` support for main layouts.
- **Stability**: Fixed potential crashes in SettingsActivity related to invalid hex color inputs.

### Changed
- **Debug Tooling**: Restricted advanced debug actions (Reset, Export Logs) to debug builds only.

## [1.1-dev] - 2026-02-08
- Initial release with basic shape rendering and accessibility overlay support.
