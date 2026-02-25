# Developer Contract: Gemini & OpenAODNotify

This document defines the technical constraints and project vision to ensure consistent AI collaboration.

## Project Vision
A high-performance, privacy-first notification overlay for Android (AOD-focused). Current Version: `1.2-dev`.

## Technical Constraints & Standards
- **Zero External Dependencies**: Use standard Android/Material components only. Avoid 3rd-party libraries.
- **Privacy-First**: Accessibility Service must remain restricted (`typeNone`, `feedbackNone`, `canRetrieveWindowContent=false`).
- **Action-Based Lifecycle**: Communication between components must use explicit `ACTION_START` and `ACTION_STOP` intents.
- **Theme Integrity**: No hardcoded hex colors in layouts. Use theme attributes (`?attr/...`) to support system DayNight and Material You.
- **Dynamic Theming**: Support multiple UI styles (e.g., `System` and `Classic`) via theme attributes and runtime style switching.
- **Documentation Standard**: Follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Update `CHANGELOG.md` at the end of each feature block.
- **Architectural Reference**: Store technical rationale ("The Why") and pitfalls in `ARCHITECTURE.md` for future reference.
- **Git Convention**: Always output the proper Git CLI commands using [Conventional Commits](https://www.conventionalcommits.org/) (e.g., `feat:`, `fix:`, `docs:`) when ready to commit, build, or test. Use a single bash-parseable block.

## Service Priority Checklist (The AOD Pierce)
When modifying the overlay, ensure these "Pierce" settings remain:
1. Window Type: `TYPE_ACCESSIBILITY_OVERLAY`.
2. Alpha: `0.99f` (forces translucent compositor pipeline).
3. Cutout Mode: `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`.
4. Insets: `setFitInsetsTypes(0)` (ignore system safe-zones).
5. Rendering: `LAYER_TYPE_HARDWARE` for borders to avoid memory pinning issues.
