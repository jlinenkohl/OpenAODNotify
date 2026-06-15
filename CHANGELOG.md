# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.4-dev] - 2026-06-15

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
