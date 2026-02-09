# OpenAODNotify Architecture (v1.0)

## Core Components

### 1. OpenAODListener (NotificationListenerService)
The "Detection Engine". Monitors incoming system notifications.
- **Responsibility**: Detects valid notifications (ignoring system/ongoing), tracks the "hasNotification" state, and manages the lifecycle of the overlay via Intent Actions (`START`/`STOP`).
- **Lifecycle**: System-managed. Uses `requestRebind` for stability.

### 2. OpenAODOverlayService (AccessibilityService)
The "High-Priority Renderer". Draws the visual indicator over all system layers.
- **Why Accessibility?**: Provides the `TYPE_ACCESSIBILITY_OVERLAY` window type, which has the necessary Z-order to "pierce" through the Android AOD and Lockscreen layers.
- **Responsibility**: Manages the `WindowManager` overlay. Renders hardware-accelerated shapes and handles the "breathing" pulse.
- **Lifecycle**: Persistent. Responds to explicit `ACTION_START` and `ACTION_STOP` intents to manage view visibility.

### 3. SettingsActivity & MainActivity
The "User Interface".
- **MainActivity**: Guides the user through the 5-step permission process (Restricted Settings, Overlay, Notification, Accessibility, and Battery).
- **SettingsActivity**: Provides a precision RGB mixer, numeric steppers, and a "Leashed Handle" for easy drag-and-drop alignment.

### 4. PreferenceUtils
The "Persistence Layer".
- **Logic**: Centralized storage for 3 independent settings profiles (Default, Profile 1, Profile 2).
- **Enums**: Uses a type-safe `ShapeType` enum to eliminate magic integers.

## Data Flow
1. User configures settings in `SettingsActivity`.
2. `OpenAODListener` detects a notification.
3. If screen is off, `OpenAODListener` sends `ACTION_START` to `OpenAODOverlayService`.
4. `OpenAODOverlayService` draws the high-priority overlay and begins the pulse animation.
5. On phone unlock or notification dismissal, an `ACTION_STOP` is sent to clear the screen.
