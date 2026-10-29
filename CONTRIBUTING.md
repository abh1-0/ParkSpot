# Contributing to ParkSpot

Thank you for your interest in contributing to **ParkSpot**! We welcome all bug reports, feature suggestions, UI/UX polish, and code contributions.

---

## 🛠️ Development Setup

1. **Prerequisites:**
   - JDK 17+
   - Android Studio Meerkat or later (or command-line Android SDK)
   - Android SDK Platform 34+ (Build-Tools 34+)

2. **Clone the Repository:**
   ```bash
   git clone https://github.com/abh1-0/ParkSpot.git
   cd ParkSpot
   ```

3. **Build the Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on Connected Device / Emulator:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📌 Code Style & Guidelines

- **Vector Drawables Only:** We strictly enforce the use of real Android Vector Drawables (`res/drawable/ic_*.xml`) and Material Vector Icons. Do not use emoji glyphs as replacements for UI icons.
- **Universal Phone Compatibility:** The app targets Android API 24+ and must work across all manufacturers and AOSP forks (including devices without Google Play Services via native `LocationManager` fallbacks).
- **Architecture:** Keep UI code in Jetpack Compose, business logic in ViewModels, and state in Kotlin Flows/StateFlows. App Widgets use native `RemoteViews` for maximum reliability across home screen launchers.

---

## 🚀 Submitting a Pull Request

1. Fork the repo and create your branch from `main`:
   ```bash
   git checkout -b feature/my-new-feature
   ```
2. Verify that `./gradlew assembleDebug` and `./gradlew assembleRelease` pass without errors.
3. Commit your changes with clear, descriptive commit messages.
4. Push to your branch and open a Pull Request against `main`.

---

## 📄 License
By contributing to ParkSpot, you agree that your contributions will be licensed under the project's [Apache 2.0 License](LICENSE).
