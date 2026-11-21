# PulseSpan — Google Play Deployment Guide

Follow this checklist to publish **PulseSpan** (`com.pulsespan.app`) to the Google Play Store with Android Health Connect approval.

---

## 1. Pre-Production Security (Gemini AI)
Before uploading a public release `.aab`, **do not ship a raw `GEMINI_API_KEY` inside the app**:
- Recommended: Switch `GeminiCoachRepository` to **Firebase AI Logic** (with Firebase App Check enabled) or a lightweight Cloud Run / Cloud Functions proxy endpoint.
- Keep `local.properties` `GEMINI_API_KEY` blank for release builds unless proxied.

---

## 2. Generate a Signed Release App Bundle (`.aab`)
1. In Android Studio: **Build -> Generate Signed App Bundle / APK -> Android App Bundle**.
2. Create a new upload keystore (store it safely outside Git; never commit `*.jks` or `*.keystore`).
3. Build the `release` variant (R8 minification and resource shrinking are already enabled in `app/build.gradle.kts`).
4. Enroll in **Play App Signing** in Google Play Console when uploading your first `.aab`.

---

## 3. Health Connect Declaration (Required by Google Play)
Because PulseSpan declares `android.permission.health.READ_*` permissions:
1. In Google Play Console, go to **Policy and programmes -> App content -> Health apps**.
2. Select **Activity and fitness** and **Sleep**.
3. In the **Health Connect permissions** declaration, justify each read-only permission:
   - `READ_SLEEP`: Computes Sleep Score and displays Deep/REM/Light/Awake architecture.
   - `READ_HEART_RATE_VARIABILITY` & `READ_RESTING_HEART_RATE`: Computes 14-day personal baseline and daily Recovery/Readiness Score.
   - `READ_HEART_RATE`, `READ_STEPS`, `READ_ACTIVE_CALORIES_BURNED`, `READ_TOTAL_CALORIES_BURNED`, `READ_EXERCISE`: Computes Daily Strain Score and displays Fitbit workouts.
4. Confirm that `PermissionsRationaleActivity` is reachable from Health Connect settings (already configured in `AndroidManifest.xml`).

---

## 4. Privacy Policy & Data Safety Form
1. Host `docs/PRIVACY_POLICY.md` at a public HTTPS URL (e.g., GitHub Pages or your domain) and paste it into Play Console -> **App content -> Privacy policy**.
2. In **Data safety**, declare:
   - Health & Fitness data is read on-device from Health Connect and not sold or shared with third-party advertisers.
   - If AI Coaching is enabled, summarized daily metrics and user chat prompts are sent over HTTPS to the AI provider solely to generate coaching responses.

---

## 5. Testing & Rollout
1. Upload your `.aab` to **Internal testing** first and test on a real Android phone with Fitbit syncing to Health Connect.
2. Promote to **Closed testing** (personal Play developer accounts require 12 testers opted in for 14 days).
3. Submit for **Production** review.
