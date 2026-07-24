# 💪 Workout Tracker

A modern Android workout tracker built with Jetpack Compose — liquid-glass UI, automatic session
timing, and a full training history.

<p align="center">
  <img src="app/src/main/res/drawable/workout_logo.png" width="120" alt="Workout Tracker Logo"/>
</p>

**Latest build:** [`WorkoutTracker-v2.0.apk`](WorkoutTracker-v2.0.apk) — install directly on any
Android 13+ device.

## ✨ Features

### 🏋️ Workout Management
- **Custom Workout Plans** - Create personalized workout routines for each day of the week
- **Exercise Library** - 40+ pre-built exercises organized by muscle group
- **Flexible Scheduling** - Choose which days you train, skip rest days automatically
- **One-tap set logging** - A single "set done" button per exercise; the last set closes the
  exercise automatically, with an undo for mis-taps

### ⏱️ Session Timing
- **Automatic workout clock** - Starts on your first logged set, not when the app opens
- **10-minute idle close** - Walk away and the session ends by itself, backdated to your last
  action, so the recorded length is real training time
- **Live stats** - Sets, exercises finished and total volume while you train

### 📊 Workout History
- **Every session recorded** - Date, start/end time, duration, sets and volume
- **Exercise-by-exercise log** - What you actually completed vs. what was planned, and the weight used
- **"Last workout" card** - Straight from the home screen

### ⏱️ Smart Rest Timer
- **Background Timer** - Timer continues running even when app is minimized
- **Notification Support** - Get notified when rest period ends, with a "set done" action
- **Inline countdown** - Rest shows on the exercise card and in a banner, never blocking you

### 🔔 Workout Reminders
- **Custom Alarms** - Set reminders for specific days and times
- **Flexible Scheduling** - Different reminders for different days
- **Custom Messages** - Add personalized motivation messages

### 🎨 Liquid-glass UI
- **One design system** - Frosted panels over a gradient backdrop (`ui/theme/LiquidGlass.kt`)
- **Segmented day bar** - A single glass tab row with a sliding gradient pill
- **Steppers instead of keyboards** - Sets, rest and weight are all ± controls
- **Dark/Light Theme** - Automatic theme switching based on system settings
- **Hebrew & English** - Full RTL support and localization

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Architecture:** MVVM + Clean Architecture
- **Data Persistence:** DataStore (Preferences)
- **Background Processing:** Foreground Service
- **Navigation:** Jetpack Navigation Compose
- **Design System:** Material Design 3

## 📦 Project Structure

```
app/src/main/java/com/hananel/workouttracker2/
├── data/
│   ├── models/          # Exercise, WorkoutDay, TimerState, WorkoutSession
│   ├── repository/      # DataStore repository — owns set counting & sessions
│   ├── DefaultData.kt   # Default workout structure
│   └── ExerciseBank.kt  # Exercise library
├── service/
│   └── TimerService.kt  # Foreground rest countdown (no set counting)
├── ui/
│   ├── components/      # DayTabBar, ExerciseCard, SessionBar, GlassControls
│   ├── screens/         # Workout, History, Settings, Alarms, Catalog, Onboarding
│   └── theme/           # LiquidGlass design system + Material 3 theming
├── utils/
│   ├── DateUtils.kt     # Date/duration helpers
│   └── NotificationHelper.kt
└── MainActivity.kt      # Entry point & navigation
```

### Set counting — the one invariant

Sets advance **only** through `WorkoutRepository.completeSet()`, which writes the set count and the
exercise's completed flag in a single DataStore edit. The rest timer is a countdown and nothing
more. An earlier design let the timer own the counter, so finishing the last set silently failed
whenever the timer was stopped, the service was killed, or rest was set to zero. Don't move set
counting back into the service.

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17+
- Android SDK 34

### Installation

1. Clone the repository
```bash
git clone https://github.com/HananelSabag/Workout-Tracker.git
```

2. Open in Android Studio

3. Sync Gradle and run on device/emulator

### Build

```bash
./gradlew assembleDebug
```

The release APK checked in at the repo root is signed with the auto-generated debug key so it
installs straight from GitHub. Swap in a real keystore before any Play Store upload.

### Tests

```bash
./gradlew testDebugUnitTest connectedDebugAndroidTest
```

JVM tests cover the session model and date/duration helpers. The instrumented tests run against a
real DataStore and guard set counting, session rotation and the idle close.

## 📋 Requirements

- **Minimum SDK:** 33 (Android 13)
- **Target SDK:** 35 (Android 15)
- **Permissions:**
  - `POST_NOTIFICATIONS` - Timer and reminder notifications
  - `FOREGROUND_SERVICE` - Background timer
  - `SCHEDULE_EXACT_ALARM` - Workout reminders

## 🤝 Contributing

Contributions are welcome! Feel free to:
- Report bugs
- Suggest features
- Submit pull requests

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Hananel Sabag**

- GitHub: [@HananelSabag](https://github.com/HananelSabag)

---

<p align="center">
  Made with ❤️ and Kotlin
</p>
