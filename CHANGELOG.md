# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1-dev] - 2026-02-08

### Added
- **Shape-Scoped Settings**: Visual properties (size, position, rounding) are now stored independently for each shape.
- **Unified Lines System**: Replaced individual line types with a single "Lines" shape featuring a bitmask-based side selector (Top, Bottom, Left, Right).
- **Diagnostics Suite**: Added "Reset & Initialize" button with full debug dump (JSON settings, screen metrics) and "Export Debug Logs" functionality.
- **Rotation Awareness**: Overlay now detects orientation changes and automatically re-aligns itself to prevent distortion.
- **Dynamic Versioning**: Displaying current version and build type (-debug suffix) at the bottom of the main screen.
- **Permission Health Check**: Added `BootReceiver` to notify users via the system tray if Accessibility or Notification permissions are lost after a reboot.

### Changed
- **Sane Defaults**: Updated global defaults (2000ms duration, 0.99 max alpha) and optimized starting positions for draggable shapes.
- **Theme Standardization**: Removed all hardcoded hex colors from layouts in favor of system theme attributes (`?attr/colorSurface`, etc.).
- **AOD Pierce Refinement**: Switched large overlays to hardware rendering and forced translucent blending (`0.99f` alpha) to improve visibility on physical device AOD screens.

### Fixed
- **Ghost Overlay**: Fixed bug where preview shapes would persist after abandoning the settings screen.
- **Timer Stalling**: Implemented timer refreshing so that subsequent notifications extend the active breathing duration.
- **ANR Prevention**: Added state guards to the Accessibility Service to prevent main-thread congestion during high notification traffic.

## [1.0.0] - 2026-02-08

### Added
- **Accessibility Service Architecture**: Migrated to `AccessibilityService` to utilize `TYPE_ACCESSIBILITY_OVERLAY` for AOD visibility.
- **Leashed Handle**: Interactive drag-and-drop system for precise shape positioning.
- **RGB Color Mixer**: Custom slider-based color picker for precise aesthetic control.
- **Numeric Steppers**: UI controls for fine-tuning numeric settings without the keyboard.
- **5-Step Onboarding**: Integrated permission flow for sideloaded app installation.
- **Basic Shapes**: Circle, Square, Rectangle, and Ring support.

[1.1-dev]: https://github.com/jlinenkohl/OpenAODNotify/compare/v1.0.0...v1.1-dev
[1.0.0]: https://github.com/jlinenkohl/OpenAODNotify/releases/tag/v1.0.0
