# 🏋️ Workout Tracker - Android App

A professional, clean, and maintainable Android workout tracking application built with Jetpack Compose and Material 3.

## 📱 Features

### Core Functionality
- ✅ **Weekly Workout Tracking** - Default Sunday-Thursday schedule (customizable in settings)
- ✅ **Set-Based Workout System** - Track individual sets per exercise
- ✅ **Rest Timer** - Automatic countdown timer between sets with set tracking
- ✅ **Exercise Completion** - Auto-mark exercises as complete when all sets are done
- ✅ **Weight Tracking** - Track weight per exercise with persistent history
- ✅ **Weekly Reset** - Manual button (required), optional automatic with notification
- ✅ **Bilingual Support** - Hebrew & English with full RTL support for Hebrew
- ✅ **Theme Support** - Light/Dark/System with beautiful design for both
- ✅ **Smart Notifications** - Workout reminders with custom times and days

### State Persistence
- All progress is saved immediately to DataStore
- Timer state persists across app restarts
- Completed sets are tracked and restored
- Weights and notes are preserved across weekly resets

## 🏗️ Architecture

### Clean Architecture with Separation of Concerns

```
app/src/main/java/com/hananel/workouttracker2/
├── MainActivity.kt              # Main entry and navigation
├── data/
│   ├── models/                  # Data models (Exercise, WorkoutDay, etc.)
│   ├── repository/              # DataStore operations
│   └── DefaultData.kt           # Default workout structure
├── ui/
│   ├── screens/                 # Screen composables and ViewModels
│   ├── components/              # Reusable UI components
│   └── theme/                   # Theme, colors, typography
├── utils/                       # Utility classes (DateUtils, NotificationHelper)
└── receiver/                    # Broadcast receivers
```

## 🎨 Design

- **Material 3** design system
- **Professional color schemes** for both light and dark themes
- **Smooth animations** and transitions
- **Responsive** layout for all screen sizes
- **RTL support** for Hebrew language
- **Accessible** with clear labels and good contrast

## 🔧 Technology Stack

- **Kotlin** - Modern programming language
- **Jetpack Compose** - Modern UI toolkit
- **Material 3** - Latest Material Design
- **DataStore** - Data persistence
- **Kotlinx Serialization** - JSON serialization
- **Navigation Compose** - Navigation between screens
- **Coroutines** - Asynchronous programming
- **ViewModel** - State management
- **AlarmManager** - Scheduled notifications

## 📦 Dependencies

```kotlin
// Compose & Material 3
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

## 🏋️ Default Workout Structure

### Sunday - Chest & Shoulders
- Dumbbell Chest Press (5 sets, 10-12 reps, 60s rest)
- Cable Chest Press (4 sets, 12-15 reps, 45s rest)
- Machine Flyes (4 sets, 15-20 reps, 45s rest)
- Dumbbell Shoulder Press (4 sets, 10-12 reps, 60s rest)
- Lateral Raises (4 sets, 15-20 reps, 45s rest)

### Monday - Cardio
- HIIT Training (1 set, 30 min)

### Tuesday - Legs
- Squats (5 sets, 10-12 reps, 60s rest)
- Cossack Squats (4 sets, 10 each leg, 45s rest)
- Walking Lunges (4 sets, 12 steps each, 45s rest)
- Leg Curls (4 sets, 15-20 reps, 45s rest)
- Step Ups (4 sets, 15 each leg, 45s rest)

### Wednesday - Cardio
- Low HR Running (1 set, 30-40 min)

### Thursday - Back & Arms
- Wide Lat Pulldown (5 sets, 10-12 reps, 60s rest)
- Seated Cable Row (4 sets, 12-15 reps, 45s rest)
- Cable Pullover (4 sets, 15-20 reps, 45s rest)
- Dumbbell Curls (4 sets, 12-15 reps, 45s rest)
- Cable Tricep Extension (4 sets, 15-20 reps, 45s rest)

## 🚀 User Flow

1. App opens → Today's workout displayed automatically
2. User selects exercise → Performs set
3. Timer starts automatically after clicking "Start Rest"
4. User rests → Completes set → Clicks "Next Set"
5. Timer resets for next set
6. All sets completed → Exercise auto-marked complete ✓
7. User moves to next exercise
8. Finishes workout → Feels accomplished! 💪

## ⚙️ Settings

- **Language Selection** - Hebrew, English, or System Default
- **Theme Selection** - Light, Dark, or System Default
- **Notifications** - Enable/disable workout reminders
- **Auto Reset** - Optional automatic weekly reset on Sundays
- **Manual Reset** - Button to manually reset weekly progress
- **Enabled Days** - Customize which days appear in workout list

## 🔔 Notifications

### Channels
1. **Workout Reminders** - High priority, alarm sound for scheduled workouts
2. **Timer Alerts** - High priority for rest timer completion
3. **Weekly Updates** - Default priority for weekly reset notifications

### Permissions
- `POST_NOTIFICATIONS` - Display notifications
- `SCHEDULE_EXACT_ALARM` - Schedule precise workout reminders
- `VIBRATE` - Vibrate on timer completion
- `WAKE_LOCK` - Keep timer running in background

## 💾 Data Persistence

All data is stored using DataStore Preferences with JSON serialization:

- **Workout Progress** - Completed exercises, sets, weights, notes
- **User Preferences** - Language, theme, notifications, enabled days
- **Workout Structure** - Custom workout days and exercises
- **Timer State** - Current timer state (persists across app restarts)

### Weekly Reset Behavior
- **Clears:** Completion marks, completed sets
- **Preserves:** Exercise weights, notes, custom exercises

## 🎯 Key Features Implementation

### Timer Logic
```kotlin
1. User clicks "Start Rest"
2. Load existing completedSets from WorkoutProgress
3. Create TimerState with currentSet = completedSets + 1
4. Show timer dialog and save state to DataStore
5. On "Next Set" or timer completion:
   - Increment completedSets
   - Save to WorkoutProgress
   - Auto-complete exercise if all sets done
6. On close (X button):
   - Save completedSets to WorkoutProgress
   - Clear TimerState from DataStore
```

### State Management
- **ViewModel** handles all business logic
- **Repository** manages DataStore operations
- **Flows** provide reactive state updates
- **Coroutines** handle async operations

## 📱 Screens

### 1. Workout Screen
- Day selector (horizontal tabs)
- Exercise list with cards
- Set progress indicators
- Weight input fields
- Timer button per exercise
- Completion checkboxes

### 2. Settings Screen
- Language selection dialog
- Theme selection dialog
- Notification toggles
- Auto-reset toggle
- Manual reset button with confirmation

### 3. Timer Dialog
- Large circular timer display
- Exercise name
- Set progress dots (completed=green, current=blue, pending=gray)
- Set counter badge
- Close button (X)
- "Next Set" and "Stop" buttons

## 🌐 Internationalization

### Supported Languages
- **Hebrew (עברית)** - Default, full RTL support
- **English** - Full translation
- **System Default** - Uses device language

### String Resources
- All UI text is localized
- Exercise names translated
- Day names localized
- Messages and notifications translated

## 🎨 Themes

### Light Theme
- Primary: Blue (#2563EB)
- Success: Green (#059669)
- Background: Light Gray (#F9FAFB)
- Surface: White (#FFFFFF)
- Text: Dark Gray (#1F2937)

### Dark Theme
- Primary: Light Blue (#3B82F6)
- Success: Light Green (#10B981)
- Background: Very Dark Gray (#0F172A)
- Surface: Dark Gray (#1E293B)
- Text: Light Gray (#F1F5F9)

## 🧪 Testing

### Manual Testing Checklist
- ✅ App launches without errors
- ✅ All 5 workout days load correctly
- ✅ Timer saves and restores sets properly
- ✅ Exercise auto-completes when all sets done
- ✅ Weekly reset works (manual button)
- ✅ Both themes look great
- ✅ Hebrew and English both work with proper RTL
- ✅ Notifications send at right times
- ✅ No data loss on app close
- ✅ Timer persists across app restart

### Edge Cases Handled
1. Mid-workout app exit → Sets saved immediately
2. Timer during phone call → State persists
3. Bathroom break mid-set → Progress saved
4. All sets completed → Auto-mark complete
5. Weekly reset → Confirmation dialog, preserve weights
6. No exercises for a day → Friendly rest day message
7. Language change → UI updates immediately
8. Theme change → Immediate update without restart

## 🚀 Building the App

### Requirements
- Android Studio Hedgehog or newer
- Kotlin 1.9.22 or newer
- Android SDK 33+
- Gradle 8.0+

### Build Steps
```bash
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Run on device or emulator (API 33+)
```

### Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

## 📝 Future Enhancements (Nice-to-Have)

- 📊 Statistics dashboard with charts
- 🏆 Achievement system for milestones
- 📈 Weight progression graphs
- 📝 Exercise notes with history
- 🔊 Sound effects for timer completion
- 📸 Exercise images/videos
- ☁️ Cloud backup (Firebase/Google Drive)
- 👥 Social features (share workouts)
- 🔔 Smart reminders based on patterns

## 👨‍💻 Developer

**Created by:** Hananel Sabag  
**Project:** Workout Tracker v4.0  
**Date:** November 2025  
**Architecture:** Clean Architecture with MVVM  
**UI Framework:** Jetpack Compose  

## 📄 License

This project is built as a personal workout tracking solution.

## 🙏 Acknowledgments

- Material Design 3 for the beautiful design system
- Jetpack Compose team for the modern UI toolkit
- Android developer community for best practices and patterns

---

**Built with ❤️ and 💪 for fitness enthusiasts**

