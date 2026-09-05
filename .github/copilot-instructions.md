# Developer Contract: OpenAODNotify

This document defines the technical constraints and project vision to ensure consistent AI collaboration, regardless of which agent (GitHub Copilot, Gemini, Claude, etc.) is being used. It is the single source of truth — see `AGENTS.md` / `GEMINI.md` for pointers used by other agent tooling.

## Project Vision
A high-performance, privacy-first notification overlay for Android (AOD-focused). Current Version: `1.4.2`.

## Technical Constraints & Standards
- **Zero External Dependencies**: Use standard Android/Material components only. Avoid 3rd-party libraries.
- **Privacy-First**: Accessibility Service must remain restricted (`typeNone`, `feedbackNone`, `canRetrieveWindowContent=false`).
- **Action-Based Lifecycle**: Communication between components must use explicit `ACTION_START` and `ACTION_STOP` intents.
- **Theme Integrity**: No hardcoded hex colors in layouts. Use theme attributes (`?attr/...`) to support system DayNight and Material You.
- **Dynamic Theming**: Support multiple UI styles (e.g., `System` and `Classic`) via theme attributes and runtime style switching.
- **Documentation Standard**: Follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Update `CHANGELOG.md` at the end of each feature block.
- **Architectural Reference**: Store technical rationale ("The Why") and pitfalls in `ARCHITECTURE.md` for future reference.
- **Secrets**: Never hardcode credentials (keystore passwords, API keys, etc.) in any tracked file. Release signing passwords must be loaded only from the untracked/gitignored `local.properties` or CI environment variables (see `build.gradle.kts` / `app/build.gradle.kts`).
- **Git Convention**: Read-only commands (e.g., `status`, `log`, `diff`, `fetch`) may be run freely for context. **STRICT REQUIREMENT**: Explicit user permission is required before executing any command that modifies the repository (e.g., `add`, `commit`, `push`, `reset`, `tag`). Always present the proposed Git CLI commands in a single bash-parseable block when seeking approval.
- **Release Protocol**: When the user requests to "tag a release":
    1. Synchronize `main` with the latest feature branch.
    2. Bump `versionCode` (increment) and `versionName` (remove `-dev`) in `app/build.gradle.kts`.
    3. Update this file's version reference and finalize the latest `CHANGELOG.md` entry (ensure date and version match).
    4. Before building/committing: scan the full working tree diff and, when in doubt, the full `git log --all -p` history for hardcoded secrets, credentials, device identifiers, or other sensitive data that shouldn't be committed. Resolve any findings before proceeding.
    5. Execute a clean build and verify signing: `./gradlew clean assembleRelease verifyReleaseSigning`. This runs lint (wired into `assemble*`), builds the release APK, and fails loudly if the APK isn't signed with the expected release certificate (`expectedReleaseCertSha256` in `app/build.gradle.kts`) — never proceed past a failure here.
    6. Rename the output APK to `OpenAODNotify-[VERSION].apk` in `app/release/`.
    7. Commit, Tag (`v[VERSION]`), and Push to `main`.
    8. Create a GitHub Release using `gh release create` with the APK and changelog notes.
    9. Seek explicit approval for the final bash block containing these steps.

## Service Priority Checklist (The AOD Pierce)
When modifying the overlay, ensure these "Pierce" settings remain:
1. Window Type: `TYPE_ACCESSIBILITY_OVERLAY`.
2. Alpha: `0.99f` (forces translucent compositor pipeline).
3. Cutout Mode: `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`.
4. Insets: `setFitInsetsTypes(0)` (ignore system safe-zones).
5. Rendering: `LAYER_TYPE_HARDWARE` for borders to avoid memory pinning issues.
