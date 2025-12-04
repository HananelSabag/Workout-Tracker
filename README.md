# 🏋️ Workout Tracker - Android App

A professional, feature-rich Android workout tracking application built with Jetpack Compose and Material 3. Designed for complete flexibility and ease of use.

## 🎯 Core Philosophy

**Your Workout, Your Way** - Complete control over your training schedule with a beautiful, intuitive interface.

## ✨ Key Features

### 🚀 Dynamic Workout Builder (v2.1)
- **Interactive Day Selection** - Choose which days you train (Sunday through Saturday)
- **Custom Workout Titles** - Name each day's workout (e.g., "Chest Day", "Back & Biceps", "Leg Day")
- **Flexible Exercise Management** - Add, remove, or reorder exercises anytime
- **40+ Pre-Made Exercises** - Comprehensive exercise library organized by muscle groups
- **Smart Filtering** - Find exercises by category (Chest, Back, Shoulders, Arms, Legs, Core, Cardio)
- **Custom Exercises** - Create your own exercises with custom names and parameters
- **Access from Settings** - Modify your workout plan anytime without starting over

### 📋 Exercise Catalog System
- **Comprehensive Library** - 40+ exercises covering all major muscle groups:
  - **Chest** (6): Dumbbell press, barbell press, cable press, flyes, push-ups
  - **Back** (6): Lat pulldowns, rows, pullups, deadlifts, pullovers
  - **Shoulders** (5): Presses, lateral raises, front raises, rear delts
  - **Arms** (6): Bicep curls, tricep extensions, hammer curls, dips
  - **Legs** (9): Squats, lunges, leg press, curls, extensions, calf raises
  - **Core** (4): Planks, crunches, Russian twists, leg raises
  - **Cardio** (5): HIIT, running, cycling, rowing, elliptical
- **Quick Add** - One-tap exercise addition from catalog
- **Smart Defaults** - Pre-configured sets, reps, and rest times for each exercise
- **Visual Organization** - Clean card-based UI with muscle group tags

### 🎨 Enhanced Onboarding Experience
- **Two-Step Setup** - Streamlined onboarding process
  - **Page 1**: Feature showcase with notification permissions
  - **Page 2**: Three setup options:
    1. **Default Program** - 5-day workout plan ready to go
    2. **Build Your Own** - Custom workout with day-by-day exercise selection
    3. **Skip** - Start with empty schedule, add exercises as you go
- **No Scrolling** - Everything visible without scrolling
- **Swipe-Protected** - Prevents accidental navigation between pages
- **Clean Design** - Modern, professional interface

### ⏱️ Smart Rest Timer
- **Foreground Service** - Timer never stops, even during phone calls
- **Background Notifications** - See countdown in status bar
- **In-App Banner** - Orange indicator shows active timer with time remaining
- **Pause/Resume** - Take bathroom breaks without losing progress
- **Set Tracking** - Automatic set completion and progression
- **Click to Open** - Tap banner to open full timer dialog
- **Proper Close Behavior** - X and Background buttons work correctly now

### 📊 Workout Tracking
- **Set-Based System** - Track individual sets per exercise
- **Weight History** - Record and persist weight for each exercise
- **Completion Tracking** - Visual checkmarks for completed exercises
- **Progress Indicators** - See exactly where you are in your workout
- **Weekly Reset** - Manual or automatic progress reset (preserves weights)

### ⚙️ Flexible Settings
- **Language Support** - Hebrew (RTL) and English
- **Theme Options** - Light, Dark, or System Default
- **Day Management** - Enable/disable any days of the week
- **Auto-Open Control** - Choose to jump to today's workout or remember last viewed
- **Notification Control** - Customize workout reminders
- **Workout Builder Access** - Modify your plan directly from settings

### 🔔 Smart Notifications
- **Workout Reminders** - Schedule notifications for training times
- **Timer Alerts** - Get notified when rest period ends
- **Weekly Reset** - Optional notification on automatic reset
- **Persistent Timer** - See countdown in notification shade

## 🏗️ Architecture

### Clean Architecture with Modern Android Best Practices

```
app/src/main/java/com/hananel/workouttracker2/
├── MainActivity.kt              # Entry point & navigation
├── data/
│   ├── models/                  # Data models
│   │   ├── Exercise.kt
│   │   ├── WorkoutDay.kt
│   │   ├── TimerState.kt
│   │   └── UserPreferences.kt
│   ├── repository/              # Data layer
│   │   └── WorkoutRepository.kt
│   ├── ExerciseBank.kt          # 40+ exercises library
│   └── DefaultData.kt           # Default workout structure
├── ui/
│   ├── screens/                 # Screen composables
│   │   ├── WorkoutScreen.kt
│   │   ├── OnboardingScreen.kt
│   │   ├── SettingsScreen.kt
│   │   └── ExerciseCatalogScreen.kt
│   ├── components/              # Reusable components
│   │   ├── ExerciseCard.kt
│   │   ├── TimerDialog.kt
│   │   └── DaySelector.kt
│   └── theme/                   # Material 3 theme
├── service/
│   └── TimerService.kt          # Foreground timer service
└── utils/                       # Helpers
    ├── DateUtils.kt
    └── NotificationHelper.kt
```

## 🔧 Technology Stack

- **Kotlin** - 100% Kotlin codebase
- **Jetpack Compose** - Modern declarative UI
- **Material 3** - Latest Material Design components
- **DataStore** - Type-safe data persistence
- **Kotlinx Serialization** - Efficient JSON serialization
- **Navigation Compose** - Type-safe navigation
- **Coroutines & Flow** - Asynchronous & reactive programming
- **ViewModel** - MVVM architecture pattern
- **Foreground Service** - Reliable background timer
- **AlarmManager** - Scheduled notifications

## 📦 Dependencies

```kotlin
// Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.05.00"))
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.1.1")

// Serialization
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.7")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
```

## 🚀 User Flow

### First Launch - Onboarding
1. Welcome screen with feature overview
2. Notification permission request
3. Choose setup option:
   - Use default program OR
   - Build custom workout plan OR
   - Skip and add exercises later
4. Ready to train! 💪

### Daily Workout
1. App opens → Today's workout displayed
2. Tap day selector to change days
3. Select exercise → Input weight
4. Complete set → Tap "Start Rest"
5. Timer runs → Rest period countdown
6. Tap "Next Set" or wait for auto-complete
7. Repeat until all sets done → Exercise marked complete ✓
8. Move to next exercise
9. Finish workout → Feel accomplished! 🎉

### Modifying Your Plan
1. Open Settings
2. Tap "Manage workout days and exercises"
3. Select which days to train
4. For each day:
   - Set workout title
   - Add exercises from catalog
   - Remove unwanted exercises
5. Save → Plan updated instantly

## 🎨 Design Highlights

- **Material 3** design system
- **Adaptive theming** - Beautiful light and dark modes
- **Smooth animations** and transitions
- **Responsive layouts** for all screen sizes
- **Full RTL support** for Hebrew
- **Accessible** with proper contrast and labels
- **Clean typography** with clear hierarchy
- **Intuitive interactions** with visual feedback

## 🌐 Internationalization

### Supported Languages
- 🇮🇱 **Hebrew (עברית)** - Full RTL support, native interface
- 🇺🇸 **English** - Complete translation
- 🌍 **System Default** - Follows device language

### Fully Localized
- All UI text and labels
- Exercise names and categories
- Day names and abbreviations
- Notification messages
- Error messages and dialogs

## 💾 Data Persistence

### What's Saved
- ✅ Workout progress (completed sets, exercises)
- ✅ Exercise weights and notes
- ✅ Custom workout structure
- ✅ User preferences (theme, language, enabled days)
- ✅ Timer state (survives app restart)

### Weekly Reset
- **Clears**: Completion marks, set progress
- **Preserves**: Weights, notes, custom exercises, workout structure

## 📱 Permissions

```xml
<!-- Required -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

## 🔨 Building the App

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- Kotlin 1.9.22+
- Android SDK 33+ (Android 13+)
- Gradle 8.2+

### Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on device
./gradlew installDebug

# Run tests
./gradlew test
```

### Build Steps
1. Clone repository: `git clone https://github.com/HananelSabag/Workout-Tracker.git`
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run on device or emulator (API 33+)

## 🎯 Default Workout Program

### Sunday - Chest & Shoulders
- Dumbbell Chest Press (5×10-12, 60s rest)
- Cable Chest Press (4×12-15, 45s rest)
- Machine Flyes (4×15-20, 45s rest)
- Dumbbell Shoulder Press (4×10-12, 60s rest)
- Lateral Raises (4×15-20, 45s rest)

### Monday - Cardio
- HIIT Training (1×30min)

### Tuesday - Legs
- Squats (5×10-12, 60s rest)
- Cossack Squats (4×10 each, 45s rest)
- Walking Lunges (4×12 steps, 45s rest)
- Leg Curls (4×15-20, 45s rest)
- Step Ups (4×15 each, 45s rest)

### Wednesday - Cardio
- Low HR Running (1×30-40min)

### Thursday - Back & Arms
- Wide Lat Pulldown (5×10-12, 60s rest)
- Seated Cable Row (4×12-15, 45s rest)
- Cable Pullover (4×15-20, 45s rest)
- Dumbbell Curls (4×12-15, 45s rest)
- Cable Tricep Extension (4×15-20, 45s rest)

*All days and exercises are fully customizable!*

## 🧪 Quality Assurance

### Manual Testing Checklist
- ✅ Onboarding flow completion
- ✅ Workout builder day selection
- ✅ Exercise addition from catalog
- ✅ Custom exercise creation
- ✅ Timer functionality (start/pause/resume/stop)
- ✅ Timer persistence across app restart
- ✅ Set tracking and auto-completion
- ✅ Weight input and persistence
- ✅ Weekly reset (manual and automatic)
- ✅ Theme switching (light/dark/system)
- ✅ Language switching (Hebrew/English)
- ✅ Notification delivery
- ✅ Background timer operation
- ✅ Settings modifications

### Edge Cases Handled
- Mid-workout app closure → Progress saved
- Phone call during timer → Timer continues
- Battery optimization → Foreground service exemption
- Low memory → State persisted to disk
- Network unavailable → Fully offline capable
- Screen rotation → State preserved
- Language change → UI updates immediately
- Theme change → Smooth transition

## 📈 Version History

### v2.1 (December 2025) - Complete Overhaul
- ✨ **NEW**: Dynamic Workout Builder with day selection
- ✨ **NEW**: Workout title customization per day
- ✨ **NEW**: Access builder from Settings
- ✨ **NEW**: Enhanced onboarding (2 pages, 3 options)
- ✨ **NEW**: Complete exercise catalog with filters
- ✨ **NEW**: All 7 days enabled by default
- 🐛 **FIX**: Timer dialog auto-reopen issue
- 🐛 **FIX**: Exercise catalog limited to 10 items
- 🐛 **FIX**: Swipe navigation in onboarding
- 🎨 **UI**: Removed blue backgrounds, cleaner design
- 🎨 **UI**: Compact day selector (3-letter abbreviations)

### v2.0 (December 2025)
- Exercise Bank System
- Enhanced Rest Timer with notifications
- Onboarding Experience
- Auto-open current day toggle
- Data preservation on updates

### v1.0 (November 2025)
- Initial release
- Core workout tracking
- Timer functionality
- Weekly reset
- Bilingual support

## 🔮 Future Enhancements

### Planned Features
- 📊 Progress charts and statistics
- 📈 Weight progression tracking
- 🏆 Achievement system
- 📸 Exercise demonstration videos
- 📝 Workout notes and history
- ☁️ Cloud backup (optional)
- 🔔 Smart reminder suggestions
- 👥 Workout sharing (optional)

### Under Consideration
- 🎵 Rest timer music integration
- 📱 Wear OS companion app
- 🤝 Social features
- 🎯 Workout templates marketplace
- 🧠 AI workout suggestions

## 👨‍💻 Developer

**Hananel Sabag**  
Fitness Enthusiast & Android Developer

### Contact
- GitHub: [@HananelSabag](https://github.com/HananelSabag)
- Project: [Workout Tracker](https://github.com/HananelSabag/Workout-Tracker)

### Development Stats
- **Lines of Code**: 6,000+
- **Development Time**: 2 months
- **Architecture**: Clean Architecture + MVVM
- **UI Framework**: Jetpack Compose
- **Languages**: Kotlin 100%

## 📄 License

This project is a personal workout tracking solution built with modern Android development best practices.

## 🙏 Acknowledgments

- **Material Design 3** - Beautiful, accessible design system
- **Jetpack Compose** - Modern, declarative UI toolkit
- **Android Community** - Countless resources and best practices
- **Fitness Community** - Inspiration and workout knowledge

---

## 💪 Built for Athletes, by an Athlete

Whether you're a beginner starting your fitness journey or an experienced athlete tracking advanced programs, Workout Tracker adapts to your needs with complete flexibility and a beautiful interface.

**No ads. No subscriptions. No data collection. Just your workouts, your way.**

---

**Made with ❤️ and 💪 for fitness enthusiasts worldwide**

*Version 2.1 - December 2025*
