# SlumberGate 🌙
**Night Lockout & Digital Sunset Engine for Android**

SlumberGate is a 100% offline, privacy-first, free Android application engineered to eliminate late-night smartphone addiction (*revenge bedtime procrastination*). The app bypasses the user's late-night willpower by automating bedtime detection, offering a structured 5-minute wind-down runway, and locking the phone behind a high-friction, pure-black **Simulated Shutdown** overlay.

---

## 🌟 Key Features

### 1. Context Detection Engine (Home Wi-Fi Automation)
- Automatically checks if the user is connected to their designated home Wi-Fi network before initiating wind-down or lockdowns.
- Operates passively without asking disruptive questions like *"Are you home?"*.
- If away from home at bedtime, defers the check by 30 minutes via `WorkManager`.

### 2. 5-Minute Structured Wind-Down Runway
- Wakes up the device 5 minutes before scheduled bedtime using exact `AlarmManager` triggers.
- Runs a low-priority, ongoing `ForegroundService` with a non-intrusive top-screen banner displaying the remaining countdown (`"Wind-down initiated. 5:00 remaining to wrap up."`).
- Automatically transitions into active lockdown when the timer reaches `00:00`.

### 3. Simulated Shutdown Overlay
- Replaces the operating system display with an AMOLED-black (`#000000`) interactive surface rendered directly on `WindowManager` (`TYPE_APPLICATION_OVERLAY`).
- Styled in ultra-dim amber/deep orange (`#8A4B00`) to prevent melatonin suppression.
- Displays a dim digital clock and dynamic sleep rest calculation derived from wake-up time:
  > *"If you sleep now: 7h 24m of rest."*
- Minimalist action bar providing immediate access to the **Emergency Dialer**, **Native Alarms**, and the **Emergency Unlock Gate**.

### 4. Hardened Anti-Bypass Guard
- A dedicated `AccessibilityService` (`LockdownAccessibilityService`) guards against muscle-memory exits (Home, Back, Recent Apps, and Notification Shade).
- Unauthorized applications (browsers, social media, settings) are redirected immediately to the home screen while re-asserting the overlay.
- Safety whitelist: Always allows the default dialer, system phone app, native alarm clock, and up to 3 user-selected emergency apps (e.g., Spotify, Calm).

### 5. Anti-Rage Friction Gate (60-Second Breathing Unlock)
- Instead of an inflexible permanent lock, tapping **Emergency Unlock** starts a 60-second guided breathing canvas:
  - **Inhale (4s)** $\rightarrow$ **Hold (4s)** $\rightarrow$ **Exhale (4s)**.
  - Text: *"Is this truly urgent? Take 60 seconds to breathe."*
- Exiting before the 60 seconds reset the timer to zero.
- Completing the 60 seconds grants a **3-minute temporary session** with a floating sticky timer pill, after which the full Simulated Shutdown Overlay re-engages.

### 6. "Flip to Sleep" Physical Sensor Engine
- Uses `Sensor.TYPE_ACCELEROMETER` and `Sensor.TYPE_PROXIMITY` to detect when the device is placed face-down on a flat surface ($Z < -8.5\text{ m/s}^2$ and proximity covered).
- Issues an audio click and 10ms haptic feedback, then dims the screen backlight to zero for a true restful state.

### 7. Safety First & Life Preservation
- **Emergency Calling:** System emergency dialer and incoming calls (`TelephonyManager.EXTRA_STATE_RINGING`) temporarily suppress the overlay so calls can be answered immediately.
- **Morning Alarms:** Never blocks morning alarms (`AlarmClock.ACTION_SHOW_ALARMS`).
- **Morning Milestones:** Dismisses overlay automatically at wake-up time and increments the **Sleep Protection Streak**.

### 8. Testing Sandbox
- Includes an instant simulation sandbox on the dashboard to test the shutdown overlay, 60s breathing unlock, wind-down banner, and flip-to-sleep sensors at any time.

---

## 🏗 Architecture & Tech Stack

- **Target Platform:** Android (API 26+ / Android 8.0+, targeting Android 14+ / API 34+)
- **Language:** Native Kotlin
- **Architecture Pattern:** Clean Architecture + MVI/MVVM
- **UI Framework:** Jetpack Compose + Material 3
- **Async & Concurrency:** Kotlin Coroutines + `StateFlow` / `Flow`
- **Local Persistence:** Jetpack DataStore Preferences (100% offline, zero cloud calls)
- **Background Tasks:** Android `ForegroundService` (`specialUse`), `AlarmManager.setExactAndAllowWhileIdle()`, and `WorkManager`
- **System Windowing:** Android `WindowManager` overlay rendering with custom Compose `LifecycleOwner`

```
com.heronikostudios.slumbergate/
├── core/
│   ├── context/          # Wi-Fi & WorkManager location automation engines
│   ├── hardware/         # Accelerometer & Proximity listeners (Flip to Sleep)
│   ├── overlay/          # WindowManager overlay managers & lifecycle owners
│   ├── receiver/         # Boot and Alarm exact broadcast receivers
│   └── service/          # Foreground Service & Accessibility Service guard
├── data/
│   ├── datastore/        # Preferences DataStore (Bedtime, Wi-Fi, Whitelist, Streaks)
│   └── model/            # LockdownState, UserSettings, SessionStats, LocationState
├── domain/               # BedtimeScheduler, EvaluateLockConditionUseCase, SleepMath
├── presentation/
│   ├── onboarding/       # Permission setup wizard & status indicators
│   ├── dashboard/        # Main schedule picker, Wi-Fi config, Whitelist modal, Sandbox
│   └── overlay/          # Composable views rendered directly to WindowManager
└── ui/theme/             # AMOLED-black & amber nighttime theme definitions
```

---

## 🔒 Required Permissions & Rationale

| Permission | Purpose |
| :--- | :--- |
| `SYSTEM_ALERT_WINDOW` | Renders the simulated shutdown overlay and wind-down banner across applications. |
| `BIND_ACCESSIBILITY_SERVICE` | Intercepts unauthorized app launches and navigation during active lockdown. |
| `ACCESS_FINE_LOCATION` / `ACCESS_WIFI_STATE` | Verifies home Wi-Fi SSID to ensure lockdown only activates at home. |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keeps sleep timing intact and prevents Android Doze termination. |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Awakens the device exactly 5 minutes before bedtime and at wake-up time. |
| `POST_NOTIFICATIONS` | Displays the persistent status notification and morning streak celebration. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Protects the timing service against aggressive OEM battery killers. |
| `READ_PHONE_STATE` | Suppresses the overlay during incoming emergency telephone calls. |
| `VIBRATE` | Provides brief haptic feedback when entering "Flip to Sleep" stance. |

---

## 🚀 Building & Running

### Prerequisites
- JDK 21 (Eclipse Adoptium / OpenJDK)
- Android SDK with API 34+ platform tools

### Build Commands
```bash
# Set Java environment (if not in default PATH)
export JAVA_HOME=/path/to/jdk-21
export PATH=$JAVA_HOME/bin:$PATH

# Run local unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug
```

The resulting APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🛡 FOSS (Free & Open Source Software) & Privacy Guarantee

SlumberGate is engineered to be **100% Free & Open Source Software (FOSS)**:
- **Zero Network Access:** The application does **not** request `android.permission.INTERNET`. It cannot send data over the internet even if it tried.
- **Zero Proprietary Blobs:** Contains zero Google Play Services (`gms`), Firebase, Crashlytics, or proprietary trackers.
- **F-Droid Anti-Features Audit:** Evaluated against all F-Droid Anti-Features with a 100% clean bill of health. See [foss_audit.md](file:///home/trollo/Projects/Android/SlumberGate/foss_audit.md) for the full report.
- **Application Security & Threat Model:** Audited against OWASP Mobile Top 10, IPC exposure, and safety guarantees. See [security_audit.md](file:///home/trollo/Projects/Android/SlumberGate/security_audit.md) for the full security assessment.

---

## 📄 License
This project is licensed under the **ISC License** (an OSI & FSF approved permissive free software license). See [`LICENSE.txt`](file:///home/trollo/Projects/Android/SlumberGate/LICENSE.txt) for details.
