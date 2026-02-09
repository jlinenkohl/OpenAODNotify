# Collaboration Log: Gemini & OpenAODNotify

This document summarizes the development journey and collaboration between the developer and Gemini (AI Assistant) to help maintain context for future sessions.

## Project Vision
Create a "1.0" lightweight, high-performance notification overlay for Android (AOD-focused).

## Key Achievements & Collaborations

### 1. High-Priority Architecture (The AOD Pierce)
- **Problem**: Standard `APPLICATION_OVERLAY` windows were occluded by the system's AOD/Lockscreen layers on physical devices (Android 15+).
- **Solution**: Refactored the renderer into an **`AccessibilityService`** using `TYPE_ACCESSIBILITY_OVERLAY`. This provides the highest possible Z-order to remain visible over system layers.
- **Lifecycle Fix**: Implemented an explicit `ACTION_START` and `ACTION_STOP` protocol to manage the persistent service.
- **Translucent Hack**: Set window alpha to `0.99f` to force inclusion in the system's translucent AOD blending stack.

### 2. Robust Notification & Timer Logic
- **Problem**: Notification events were sometimes missed, and the timeout didn't refresh for concurrent messages.
- **Solution**: 
    - **Timer Extension**: Subsequent notifications now reset the countdown timer, keeping the indicator active for the full duration from the *last* message received.
    - **Notification Grace Period**: Implemented a 5s window to ignore system "wake pulses," preventing premature indicator cleanup.
    - **System Filtering**: Added logic to ignore permanent system notifications (e.g., `android`, `com.android.systemui`).

### 3. Performance & Stability
- **ANR Prevention**: Implemented state-based guards in the service to prevent redundant layout calculations and main-thread congestion during high notification traffic.
- **Hardware Acceleration**: Re-enabled hardware rendering for large borders to resolve `ashmem` pinning errors while maintaining battery-friendly performance.

### 4. Interactive UX & Onboarding
- **Leashed Handle**: Created a gesture-safe drag-and-drop system using a "MOVE" tab that hangs below the shape.
- **Integrated Setup**: Expanded the `MainActivity` onboarding to 5 steps, including direct deep-linking to Accessibility settings.
- **Custom UI**: Built a lightweight RGB Mixer and numeric steppers to eliminate 3rd-party library dependencies.

## Developer Notes for Gemini
- **Architecture**: Logic is split between `OpenAODListener` (detection) and `OpenAODOverlayService` (rendering).
- **Control Flow**: Communication between detection and rendering is strictly action-based (`ACTION_START`/`ACTION_STOP`).
- **Targeting**: Min SDK 34 (Android 14), Target SDK 35 (Android 15).
