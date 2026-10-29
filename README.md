# ParkSpot 🚗📍

[![Android](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-green.svg)](https://developer.android.com)
[![Android Auto](https://img.shields.io/badge/Android%20Auto-Ready-blue.svg)](https://developer.android.com/training/cars)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Release](https://img.shields.io/badge/Release-v1.0.0-brightgreen.svg)](https://github.com/abh1-0/ParkSpot/releases)

**ParkSpot** is a high-speed vehicle parking assistant designed for all Android phones and Android Auto head units. It records your parking spot via high-accuracy GPS and camera photo, features a dedicated home screen widget, and seamlessly navigates you back to your vehicle using Google Maps.

---

## ✨ Key Features

### 1. 📸 Photo & GPS Parking Capture
- **1-Tap Photo Capture:** Take a high-resolution photo of your parking spot, parking pillar, floor level, or surrounding landmarks.
- **Synchronized GPS Location:** When a photo is taken, high-accuracy GPS coordinates and reverse-geocoded street addresses are acquired and associated automatically.
- **Indoor Parking Support:** Record custom floor numbers, bay/pillar letters, and notes (e.g. *"Floor B2 • Pillar 14 near exit"*).

### 2. 📱 Home Screen Widget (Universal Support)
- **Sleek Dark Widget:** Faithful recreation of the beloved OxygenOS Shelf / Vault parking widget card.
- **Two Distinct States:**
  - **Idle State:** Displays the city skyline silhouette and 1-tap buttons for *"Take a picture"* and *"Mark location"*.
  - **Active State:** Displays the parked vehicle's photo thumbnail, exact street address, parked duration (*"Parked 18m ago"*), and quick action buttons.
- **Direct Navigation:** Tap *"Navigate"* on the widget to immediately launch Google Maps walking directions back to your car.
- **Clear & Reset:** Tap *"Clear"* on the widget to mark your car as found and reset the widget to idle.
- **No Emojis:** Strictly crafted with crisp, scalable Android Vector Drawables.

### 3. 🚙 Android Auto Integration
- **Auto-Save on Disconnect:** As soon as you park your car and turn off the engine (Android Auto session disconnects or car Bluetooth disconnects), ParkSpot automatically records your vehicle's parking coordinates in the background!
- **In-Car Display Controls:** When connected to your car's Android Auto screen, save or update your parking location directly with one tap on your vehicle's touchscreen.
- **Heads-Up Notifications:** Sends a notification with 1-tap Google Maps navigation as soon as parking is saved.

### 4. 🗺️ Google Maps Walking Navigation
- **1-Tap Navigation:** Seamlessly launches turn-by-turn walking navigation (`google.navigation:q=lat,lng&mode=w`) directly back to the exact parking coordinates.
- **Live Distance Meter:** Displays real-time walking distance and estimated walk time in the dashboard.
- **Location Sharing:** Share coordinates, street address, and Google Maps links with family and friends.

### 5. ⏱️ Parking Meter Timer & History
- **Meter Expiry Alerts:** Set 15m, 30m, 1h, or 2h timers to avoid parking fines.
- **Parking History:** Review past parking sessions with thumbnails, dates, and locations.

---

## 🏗️ Architecture & Universal Phone Compatibility

- **Universal Device Support:** Works on any phone running Android 7.0 (API 24) through Android 15/16 (API 36).
- **Dual Location Engine:** Uses Google Play Services `FusedLocationProviderClient` for rapid high-accuracy fixes, with automatic fallback to native Android `LocationManager` (`GPS_PROVIDER` and `NETWORK_PROVIDER`) for AOSP or non-GMS devices.
- **Modern Jetpack Stack:**
  - Jetpack Compose with Material 3 Dark theme
  - Android for Cars App Library (`androidx.car.app:app:1.4.0`)
  - Kotlin Coroutines & StateFlow
  - Android native `AppWidgetProvider` with `RemoteViews` for universal launcher compatibility (OnePlus, Pixel, Samsung One UI, Xiaomi HyperOS, Nova Launcher, etc.)

---

## 📲 Installation

Download the latest signed APK from the [Releases](https://github.com/abh1-0/ParkSpot/releases) page:

1. Download `ParkSpot-v1.0.0-release.apk`.
2. Open the file on your Android device and tap **Install**.
3. Add the **Parking Location** widget to your home screen:
   - Long press on an empty home screen area.
   - Tap **Widgets** → select **ParkSpot**.
   - Drag the card widget to your screen.

---

## 🛠️ Building from Source

```bash
git clone https://github.com/abh1-0/ParkSpot.git
cd ParkSpot

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```

---

## 📄 License

Licensed under the [Apache License, Version 2.0](LICENSE).
