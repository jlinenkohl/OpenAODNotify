# OpenAODNotify

OpenAODNotify is a lightweight Android utility designed to simulate a notification LED/dot for devices without one, specifically targeting Always-On Display (AOD) environments.

## Features
- **Customizable Overlays**: Circles, Squares, Rectangles, Rings, or Edge-based lines and full-screen borders.
- **Privacy-First Design**: Minimal technical configuration ensures no user data, input, or screen content is monitored.
- **Interactive Setup**: "Leashed Handle" system for precise drag-and-drop positioning, especially around camera cutouts.
- **Settings Profiles**: Support for 3 independent configurations (Default, Profile 1, Profile 2).
- **RGB Color Mixer**: Built-in, zero-dependency color picker for exact aesthetic control.
- **Battery Mindful**: Uses hardware acceleration and efficient foreground management to minimize power impact.

## Permissions & Privacy
To function reliably on modern Android versions (14, 15+), the app requires three core permissions:

1. **Notification Access**: To detect incoming messages and trigger the indicator.
2. **Overlay Permission**: To draw the indicator over other apps.
3. **Accessibility Service**: Required to "pierce" through the system's Always-on Display and Lockscreen layers.

### Why Accessibility?
Standard overlays are often hidden by the system when the screen is locked or in AOD mode. An Accessibility Overlay is the only reliable way to ensure your notification dot stays visible on top of the system's low-power screen.

### Your Data is Safe
The app is technically configured with `accessibilityEventTypes="typeNone"` and `accessibilityFeedbackType="feedbackNone"`. This means **it is technically incapable** of:
- Reading your screen content or passwords.
- Monitoring your typing or clicks.
- Tracking your usage data.

## Getting Started
1. **Restricted Settings**: If sideloading, go to App Info > (⋮) Three dots > "Allow Restricted Settings" first.
2. **Onboarding**: Follow the 5-step guide on the main screen to configure permissions.
3. **Customize**: Tap the Settings Gear to mix your color, choose a shape, and nudge it into position.
