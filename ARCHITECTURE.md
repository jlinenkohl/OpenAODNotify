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

### Overlay-Visible Time Telemetry (Battery Diagnostics)
- **Decision**: Added a persisted, self-instrumented rolling tally of overlay-visible wall-clock time, surfaced in `MainActivity`'s debug panel — implemented as a **fixed-size 7-slot ring buffer**, not date-keyed entries with periodic pruning.
- **Reasoning**: Android's per-app battery percentage (Settings > Battery) is built almost entirely from CPU time, wakelocks, network, and sensors — display/panel power is tracked as a separate system-level "Screen" bucket and is essentially never attributed to individual apps, even ones (like this one) that force the panel on. There is also no easily-tappable system API for this: `BatteryStats`/`dumpsys batterystats` requires the signature-level `BATTERY_STATS` permission (not grantable to normal apps), and `UsageStatsManager` only tracks foreground app usage, not accessibility-overlay-driven screen wake. Self-instrumentation is the only practical option for a normal app to estimate this app's true contribution to display-on time.
- **Implementation**: Exactly `TELEMETRY_SLOT_COUNT` (7) slots, each keyed by `epochDay % 7`, storing (a) which epoch-day the slot currently represents and (b) accumulated millis for that day — **14 fixed `SharedPreferences` keys, forever**, regardless of how long the app has been installed. No pruning/cleanup pass is needed or exists: a slot's stale data is implicitly discarded (treated as 0 and overwritten) the moment its day-of-week recurs ~7 days later and a new date claims that slot (`PreferenceUtils.getOverlayMillisForDate`/`addOverlayMillisForDate`). `OpenAODOverlayService` tracks a session start timestamp whenever the overlay is actually added to the `WindowManager` (`beginOverlaySessionIfNeeded()`), and flushes elapsed time on `cleanupViews()` (`endOverlaySession()`). A periodic 60s checkpoint (`checkpointOverlaySession()`) flushes progress without ending the session, so an abrupt process kill (force-stop, crash) loses at most one checkpoint interval rather than the whole session. Sessions spanning a local-date boundary are split proportionally (`recordElapsedOverlayTime`).
- **Pitfall**: This is wall-clock (`System.currentTimeMillis()`) based, not monotonic (`SystemClock.elapsedRealtime()`), so a drastic system clock change mid-session (NTP resync, timezone change) could misattribute a small amount of time. Acceptable for a diagnostic tool, not appropriate if this data were ever used for anything billing-grade. Also note the ring-buffer design means requesting more than 7 days via `getRecentOverlayTelemetry` is silently clamped — there is no way to retain more history without changing `TELEMETRY_SLOT_COUNT` (which would grow the fixed key count proportionally).

### Manually-Paced Breathing Animation (Configurable FPS)
- **Decision**: Replaced `ObjectAnimator`/`ValueAnimator`-driven alpha pulsing with a hand-rolled `Handler.postDelayed` loop capped at ~30fps (`BREATHING_FRAME_INTERVAL_MS`).
- **Reasoning**: `ValueAnimator.setFrameDelay()` became a no-op starting at API 35 (our targetSdk), so animation frame pacing can no longer be throttled via the standard animator API — it now always ticks at the display's native refresh rate (60/90/120Hz) via Choreographer. Since the overlay can remain visible for extended periods per notification, every extra frame is a real framebuffer/compositor wakeup that drains battery with no perceptible benefit for a slow, subtle "breathing" pulse.
- **Implementation**: A `BreathingTask` Runnable computes alpha via `AccelerateDecelerateInterpolator` against elapsed wall-clock time (`SystemClock.uptimeMillis()`), and reposts itself every frame interval derived from a runtime-adjustable FPS target (default 30fps, see `PreferenceUtils.getBreathingFps()`/`setBreathingFps()`), instead of every vsync. `cleanupViews()` cancels and removes all pending tasks. A debug-only stepper + "Apply FPS Target & Reset Overlay" control in `MainActivity` (inside the existing `layoutDebugTools` section, hidden in release builds) lets the target be tuned and applied at runtime without a rebuild — applying stops any in-flight overlay so the next display picks up the new value (read fresh in `startBreathing()` each time).
- **Pitfall**: Do not reach for `ValueAnimator.setFrameDelay()` again — it's silently ignored on API 35+ and will not reduce frame rate. Any future animation work needs the same manual-pacing pattern if a sub-refresh-rate cadence is required.

### Power Status Integration (Dual-Overlay Stack)
- **Decision**: Power status and Notifications can now be rendered simultaneously using a "Stacking" approach in the `OverlayService`.
- **Reasoning**: This allows for a clear visual hierarchy (e.g., device borders for battery, center dot for notifications). Reusing the same service ensures perfect synchronization of breathing animations and minimizes system overhead.
- **Implementation**: The `Listener` sends a composite state intent. The `Service` iterates through active flags and renders multiple `View` instances to the `WindowManager`.
- **Pitfall**: If a user selects the same draggable coordinates for both shapes, they will overlap. This is a known trade-off for simplicity, mitigated by suggesting "Borders" for power status.
