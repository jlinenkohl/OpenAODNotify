# OpenAODNotify Architecture (v1.1-dev)

## Core Components

### 1. OpenAODListener (NotificationListenerService)
The "Detection Engine". Monitors incoming system notifications.
- **Responsibility**: Detects valid notifications (ignoring system/ongoing), tracks the "hasNotification" state, and manages the lifecycle of the overlay via Intent Actions (`START`/`STOP`).
- **Metadata Logging**: Upgraded to log deep metadata (Title, Category, Channel) for discovery of app-specific notification patterns.
- **Lifecycle**: System-managed. Uses `requestRebind` for stability.

### 2. OpenAODOverlayService (AccessibilityService)
The "High-Priority Renderer". Draws the visual indicator over all system layers.
- **Why Accessibility?**: Provides the `TYPE_ACCESSIBILITY_OVERLAY` window type, which has the necessary Z-order to "pierce" through the Android AOD and Lockscreen layers.
- **Orientation Support**: Automatically detects device rotation via `onConfigurationChanged` and refreshes overlay layout/dimensions to prevent distortion.
- **Lifecycle**: Persistent. Responds to explicit `ACTION_START` and `ACTION_STOP` intents to manage view visibility.

### 3. SettingsActivity & MainActivity
The "User Interface".
- **MainActivity**: Guides the user through the 5-step setup. Displays dynamic versioning (with `-debug` suffix for dev builds).
- **SettingsActivity**: Precision configuration with RGB Mixer, numeric steppers, and "Leashed Handle" positioning.

### 4. BootReceiver
The "Health Monitor".
- **Responsibility**: Listens for `RECEIVE_BOOT_COMPLETED`. Automatically verifies permissions and restarts services after a device reboot. Posts high-priority alerts if critical access is lost.

### 5. PreferenceUtils
The "Persistence Layer".
- **Logic**: Centralized storage for 3 independent settings profiles (Default, Profile 1, Profile 2).
- **Type Safety**: Uses the `ShapeType` Enum to manage visual configurations without magic integers.

## Data Flow
1. User configures settings in `SettingsActivity`.
2. `OpenAODListener` detects a notification and sends `ACTION_START` to `OpenAODOverlayService`.
3. `OpenAODOverlayService` draws the high-priority overlay and begin the pulse.
4. Subsequent notifications refresh the active timer.
5. On phone unlock or notification dismissal, an `ACTION_STOP` is sent to clear the screen.
