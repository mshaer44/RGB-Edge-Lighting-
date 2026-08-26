# RGB Edge Lighting - Android Studio Production Project

A high-performance, battery-friendly ambient edge lighting application built with **Jetpack Compose**, **AndroidX Palette**, **NotificationListenerService**, and **TYPE_APPLICATION_OVERLAY**.

---

## 🚀 Quick Setup in Android Studio

1. **Unzip or clone** this repository into your workspace.
2. Open **Android Studio** (Hedgehog 2023.1.1 or Ladybug 2024.2+ recommended).
3. Select **File → Open** and select this directory.
4. Allow Gradle Sync to finish automatically.
5. Connect an Android physical device with USB debugging enabled.
6. Click **Run (Shift + F10)**.

---

## 🔑 Crucial Permissions Required

On first run, two system permissions must be granted for the lighting effect to draw over apps when notifications arrive:

### 1. Appear on Top / Draw Over Other Apps
- Path: **Settings → Apps → Special app access → Display over other apps → RGB Edge Lighting → Allow**.
- **ADB Command**:
```bash
adb shell appops set com.example.edgelighting SYSTEM_ALERT_WINDOW allow
```

### 2. Notification Listener Access
- Path: **Settings → Apps → Special app access → Notification access → RGB Edge Lighting → Allow**.
- **ADB Command**:
```bash
adb shell cmd notification allow_listener com.example.edgelighting/com.example.edgelighting.service.EdgeNotificationListener
```

### 3. Battery Optimization Whitelist (Samsung OneUI / Xiaomi MIUI / OnePlus OxygenOS)
Modern OEM ROMs aggressively kill background listener services. To ensure 100% instant notification triggers:
- Path: **Settings → Battery → Unrestricted / No restrictions**.
- Disable "Put unused apps to sleep".

---

## ⚡ Technical Highlights

- **AMOLED Power Saving**: The overlay canvas is fully transparent except for the glowing stroke. On OLED/AMOLED panels, pure black subpixels consume 0mW.
- **Dynamic Palette Extraction**: Extracts dominant vibrant tones from incoming app icons (e.g. WhatsApp Emerald, Instagram Sunset, Discord Blurple).
- **Hardware-Accelerated Path Animation**: Uses `DashPathEffect` and `SweepGradient` on GPU layers for stutter-free 120Hz rendering.
