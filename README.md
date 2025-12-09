# 💪 Workout Tracker

A modern, feature-rich Android workout tracking application built with Jetpack Compose and Material Design 3.

<p align="center">
  <img src="app/src/main/res/drawable/workout_logo.png" width="120" alt="Workout Tracker Logo"/>
</p>

## ✨ Features

### 🏋️ Workout Management
- **Custom Workout Plans** - Create personalized workout routines for each day of the week
- **Exercise Library** - 40+ pre-built exercises organized by muscle group
- **Flexible Scheduling** - Choose which days you train, skip rest days automatically
- **Progress Tracking** - Track completed sets and weights for each exercise

### ⏱️ Smart Rest Timer
- **Background Timer** - Timer continues running even when app is minimized
- **Notification Support** - Get notified when rest period ends
- **Set Counter** - Automatically tracks which set you're on
- **Quick Actions** - Skip to next set or finish exercise from notification

### 🔔 Workout Reminders
- **Custom Alarms** - Set reminders for specific days and times
- **Flexible Scheduling** - Different reminders for different days
- **Custom Messages** - Add personalized motivation messages

### 🎨 Modern UI/UX
- **Material Design 3** - Clean, modern interface following latest design guidelines
- **Dark/Light Theme** - Automatic theme switching based on system settings
- **Hebrew & English** - Full RTL support and localization
- **Smooth Animations** - Polished transitions and micro-interactions

## 📱 Screenshots

| Home Screen | Timer Dialog | Exercise Selection |
|:-----------:|:------------:|:------------------:|
| Workout days with exercise cards | Circular timer with set progress | Muscle group filters |

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
│   ├── models/          # Data classes (Exercise, WorkoutDay, TimerState)
│   ├── repository/      # DataStore repository
│   ├── DefaultData.kt   # Default workout structure
│   └── ExerciseBank.kt  # Exercise library
├── service/
│   └── TimerService.kt  # Foreground timer service
├── ui/
│   ├── components/      # Reusable UI components
│   ├── screens/         # Screen composables
│   └── theme/           # Material 3 theming
├── utils/
│   ├── DateUtils.kt     # Date helpers
│   └── NotificationHelper.kt
└── MainActivity.kt      # Entry point & navigation
```

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

## 📋 Requirements

- **Minimum SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
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
