# PulseSpan — Fitbit Health & Longevity Coach (Android)

**PulseSpan** is a native Android (**Kotlin + Jetpack Compose + Material 3**) health & longevity companion inspired by Vora, built for **Google Fitbit** trackers via **Android Health Connect** and powered by **Gemini AI**.

## Features Included
- **Unified Longevity Score (0–100)** + Readiness Zones (**Prime / Balanced / Restore**)
- **Sleep & Recovery**: Sleep stages timeline (Deep, REM, Light, Awake), Sleep Efficiency, Overnight HRV (RMSSD) & Resting Heart Rate vs. 14-day personal baseline
- **Strain & Activity**: Daily Strain score, steps, active calories, elevated HR minutes, and Fitbit workout history
- **Gemini AI Longevity Coach**: Daily readiness briefing and interactive Q&A grounded in your actual Fitbit metrics (with automatic local fallback when offline or before adding an API key)
- **Built-in Demo Mode**: Realistic 14-day Fitbit sample dataset so every screen works immediately on an emulator or phone
- **Google Play & Health Connect Compliant**: Includes `PermissionsRationaleActivity` and Android 14+ `VIEW_PERMISSION_USAGE` declaration

## Quick Start (Android Studio)

1. Install **Android Studio** (Ladybug / Meerkat or newer — includes JDK 17+ and Android SDK 35).
2. Open this folder (`pulsespan-android`) in Android Studio and let Gradle sync.
3. *(Optional)* Copy `local.properties.example` to `local.properties` (or append to Android Studio's generated `local.properties`) and add your Gemini API key:
   ```properties
   GEMINI_API_KEY=your_key_here
   ```
4. Click **Run** on an Android 14+ emulator or physical phone.
5. Run unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```

## Connecting Your Google Fitbit Tracker
1. Open the **Fitbit** app on your Android phone.
2. Go to **You -> Settings -> Health Connect** and enable syncing for Sleep, Heart Rate, HRV, Steps, Calories, and Exercise.
3. Open **PulseSpan**, tap **Connect** on the Today screen (or go to the **Coach** tab), grant Health Connect read permissions, and toggle off **Demo Fitbit Data Mode**.

## Deploying to Google Play
See [docs/GOOGLE_PLAY_LAUNCH_GUIDE.md](docs/GOOGLE_PLAY_LAUNCH_GUIDE.md) and [docs/PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md).
