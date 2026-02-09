# OpenAODNotify

OpenAODNotify is a lightweight Android utility designed to simulate a notification LED/dot for devices without one, specifically targeting Always-On Display (AOD) environments.

## Features
- **Customizable Overlays**: Choose between Circles, Squares, Rectangles, or Edge-based lines and borders.
- **Breathing Effect**: High-efficiency, hardware-accelerated alpha pulsing (breathing) to minimize battery impact.
- **Interactive Setup**: Drag the notification dot directly on your screen to position it perfectly (e.g., around a camera cutout).
- **Material You Design**: Respects system light/dark themes and dynamic styling.
- **Battery Mindful**: Uses minimal CPU by leveraging system-level animations and foreground service optimization.

## Getting Started
1. **Permissions**: The app requires Overlay Permission (System Alert Window) and Notification Listener access to function.
2. **Restricted Settings**: If sideloading on newer Android versions, you may need to "Allow Restricted Settings" in the Android App Info page.
3. **Setup**: Follow the 4-step guide on the main screen to ensure all background services are correctly configured.

## Usage
Tap the **Settings Gear** to customize:
- Shape and Size
- Breathing speed and opacity range
- Global timeout (how long the dot stays active)
- Exact screen coordinates (via text or drag-and-drop)
