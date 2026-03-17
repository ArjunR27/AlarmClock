# AlarmClock

Tempo is a Jetpack Compose Android app that combines a live analog clock, 
an alarm management system, and a custom sound library in one, unified interface. 
Users can create one-time or repeating alarms, choose built-in or upload custom audios,
preview sounds before using them, customize snooze length, and dismiss or snooze active alarms
from both a full-screen ringing view and a high-priority notification. The app utilizes
Compose UI patterns that we learned in class and classic Android alarm components so that
alarms can ring reliably if the app is closed, phone is locked, restarts, or the system time changes. 

## Figma Design

Here is the interactive Figma design that I created (using Figma Make)
Link: https://casual-snake-17916706.figma.site

## Main Features
- Live clock screen displaying current date and next alarm countdown
- Alarm list with create, edit, enable/disable, multi-select delete, and repeat-day customization
- Further alarm customization for label, AM/PM time, sound selection, and snooze duration
- Default sound catalog plus support for uploading custom alarm audio files
- Sound preview playback before assigning a sound to an alarm
- Full-screen ringing activity that appears over the lock screen
- High-priority alarm notification with `Dismiss` and `Snooze` actions
- Automatic alarm rescheduling after reboot, time change, timezone change, app update, or exact-alarm permission changes

## Android and Jetpack Compose Features Used

- Kotlin for the full application codebase
- Jetpack Compose for all major app UI
- Material 3 components for cards, tabs, dialogs, text fields, switches, sheets, and theming
- Navigation Compose for the top-level `Clock`, `Alarms`, and `Sounds` tabs
- `ViewModel` for screen state and business logic
- `StateFlow` and `collectAsStateWithLifecycle` for lifecycle-aware UI state observation
- `remember`, `rememberSaveable`, `LaunchedEffect`, and `DisposableEffect` for Compose state and side effects
- `Canvas` for drawing the analog clock face and hands
- `LazyColumn` for efficient alarm and sound lists
- `BoxWithConstraints` for responsive portrait and landscape layouts
- `rememberLauncherForActivityResult` with `OpenDocument` and runtime permission contracts
- Downloadable Google Fonts through `androidx.compose.ui:ui-text-google-fonts`
- DataStore Preferences for storing alarm data locally
- Room for storing custom uploaded sounds
- KSP for Room code generation
- Android `AlarmManager` with exact alarm scheduling
- `PendingIntent`, `BroadcastReceiver`, and a foreground `Service` for reliable alarm delivery
- `NotificationCompat` and full-screen intents for alarm controls while ringing
- `MediaPlayer` for sound preview playback and looping alarm playback

## Libraries and Dependencies

Most dependencies are from AndroidX / Jetpack:

- `androidx.activity:activity-compose`
- `androidx.navigation:navigation-compose`
- `androidx.lifecycle:lifecycle-runtime-ktx`
- `androidx.lifecycle:lifecycle-viewmodel-compose`
- Compose BOM, UI, Foundation, and Material 3
- `androidx.datastore:datastore-preferences`
- `androidx.room:room-runtime`
- `androidx.room:room-ktx`
- `androidx.compose.ui:ui-text-google-fonts`

Additional libraries:

- `com.google.code.gson:gson`

Things that I had to figure out:

- Exact alarm behavior and permissions on newer Android versions: 
  - I used `AlarmManager.setAlarmClock(...)` to schedule alarms
  Cchecked `canScheduleExactAlarms()` so alarms are only enabled when exact scheduling is allowed.
- Foreground service + notification flow for active alarms: 
  - I start a foreground `AlarmPlaybackService` when an alarm fires. 
  - Show a high-priority notification with `Dismiss` and `Snooze` actions
- Full-screen ringing UI over the lock screen
  - I launch a dedicated `AlarmRingingActivity` with full-screen intent behavior and lock-screen 
    - The ringing screen appears even when the device is locked.
- Persistable URI access for uploaded custom sound files: 
  - I used the Android document picker and called `takePersistableUriPermission(...)`
    - Uploaded audio files stay accessible after the app closes or the phone restarts.
- Re-registering alarms after reboot and system time changes
  - I added a broadcast receiver that listens for boot, timezone, and permission changes. 
  - This then reloads saved alarms and schedules them again.

## Device / SDK Requirements

- Minimum SDK: `24` (Android 7.0)
- Target SDK: `36`
- Compile SDK: `36`
- Java target: `11`
- Kotlin JVM target: `11`

Important runtime requirements:

- On Android 12+ (`API 31+`), exact alarms may require the user to allow exact alarm scheduling
- On Android 13+ (`API 33+`), notifications may require runtime permission so alarm controls can appear properly
- The device or emulator should have working audio output so alarms and sound previews can be tested

Declared permissions used by the project:

- `SCHEDULE_EXACT_ALARM`
- `RECEIVE_BOOT_COMPLETED`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `USE_FULL_SCREEN_INTENT`
- `POST_NOTIFICATIONS`

## Architecture Notes

- The app uses a shared `AlarmViewModel` for alarm state and a separate `AudioViewModel` for sound data
- Alarm data is stored simply in DataStore as JSON, while custom sounds use Room because they behave more like a small database table
- Actual ringing behavior depends on Android platform components outside Compose
- The app uses a lightweight manual dependency setup through a custom `Application` class instead of a DI framework like Hilt

## Above and Beyond / Extra Work

- Lock-screen alarm experience with full-screen intent support
- Heads-up notification actions for snooze and dismiss
- Uploadable custom alarm sounds with persistent URI access
- Automatic alarm recovery after reboot or time changes
- Responsive clock layout for portrait and landscape
- Unit tests for alarm time calculations and alarm data normalization

## Device/Emulator Testing
- Consistently tested on Pixel 6
