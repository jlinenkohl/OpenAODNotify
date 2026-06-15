# OpenAODNotify Architecture

## Core Components

### 1. OpenAODListener (NotificationListenerService)
The "Detection Engine". Monitors incoming system notifications.
- **Responsibility**: Detects valid notifications (filtering system/ongoing), tracks "hasNotification" state, and manages the overlay lifecycle via `ACTION_START`/`ACTION_STOP`.
- **Metadata Logging**: Upgraded to log deep metadata (Title, Category, Channel) for discovery of app-specific notification patterns.

### 2. OpenAODOverlayService (AccessibilityService)
The "High-Priority Renderer". Draws the visual indicator over all system layers.
- **Z-Order**: Uses `TYPE_ACCESSIBILITY_OVERLAY` to pierce through AOD and Lockscreen layers.
- **Orientation Support**: Automatically refreshes dimensions and layout upon device rotation via `onConfigurationChanged`.
- **Performance**: Uses `LAYER_TYPE_HARDWARE` for borders and state-based guards to prevent main-thread congestion.

### 3. BootReceiver
The "Health Monitor".
- **Responsibility**: Automatically verifies permissions and restarts services after a device reboot. Posts high-priority alerts if critical access is lost.

## Design Rationale & Decision Log (The "Why")

### Accessibility Service vs. Foreground Service
- **Decision**: Migrated renderer from Foreground Service to Accessibility Service.
- **Reasoning**: In Android 15+, standard `APPLICATION_OVERLAY` windows are occluded by the system's AOD/Lockscreen layer. `TYPE_ACCESSIBILITY_OVERLAY` provides the necessary Z-order to stay on top.
- **Pitfall**: Accessibility Services are persistent; required implementing a custom `ACTION_STOP` protocol to manually clear views since the service doesn't "stop" in the traditional sense.

### Translucent Alpha Hack (0.99f)
- **Decision**: Setting window alpha to `0.99f` instead of `1.0f`.
- **Reasoning**: The Android compositor often optimizes opaque `1.0f` windows out of the AOD blending stack. Forcing `0.99f` ensures the window is included in the translucent drawing pipeline, piercing the AOD layer.

### Leashed Handle for Drag-and-Drop
- **Decision**: Added a "MOVE" handle leashed below the actual notification shape.
- **Reasoning**: Small shapes near screen edges are difficult to grab because system gestures (status bar) take priority. The handle provides a safe touch target in a non-gesture zone.

### Logic vs. Rendering Split
- **Decision**: Separated notification detection (`Listener`) from visual rendering (`Service`).
- **Reasoning**: Keeps the complex logic of notification filtering and state management isolated from the low-level WindowManager calls, making the app easier to test and port.

### Power Status Integration (Dual-Overlay Stack)
- **Decision**: Power status and Notifications can now be rendered simultaneously using a "Stacking" approach in the `OverlayService`.
- **Reasoning**: This allows for a clear visual hierarchy (e.g., device borders for battery, center dot for notifications). Reusing the same service ensures perfect synchronization of breathing animations and minimizes system overhead.
- **Implementation**: The `Listener` sends a composite state intent. The `Service` iterates through active flags and renders multiple `View` instances to the `WindowManager`.
- **Pitfall**: If a user selects the same draggable coordinates for both shapes, they will overlap. This is a known trade-off for simplicity, mitigated by suggesting "Borders" for power status.
