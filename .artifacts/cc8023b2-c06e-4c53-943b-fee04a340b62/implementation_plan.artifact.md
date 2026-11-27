# Implementation Plan - Alarm System & Daily Routines with Vibration

Add a robust study routine alarm system with time-of-day scheduling, system notifications, and device vibration when study time arrives.

## User Review Required

> [!IMPORTANT]
> - **Permissions**: The app will request `POST_NOTIFICATIONS` (Android 13+) and handling for exact alarms (`SCHEDULE_EXACT_ALARM`).
> - **Background Alarms**: Uses `AlarmManager` and a `BroadcastReceiver` to trigger alarms even when the app is in the background or closed.

## Proposed Changes

### [app]

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/roshan/AndroidStudioProjects/PadhaiPlus2/app/src/main/AndroidManifest.xml)
- Add permissions: `VIBRATE`, `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`.
- Register `AlarmReceiver` `BroadcastReceiver`.

#### [NEW] [AlarmReceiver.kt](file:///C:/Users/roshan/AndroidStudioProjects/PadhaiPlus2/app/src/main/java/com/ps/padhaiplus/AlarmReceiver.kt)
- BroadcastReceiver triggered by `AlarmManager`.
- Plays system notification and vibrates device using `Vibrator` / `VibratorManager`.

#### [NEW] [AlarmScheduler.kt](file:///C:/Users/roshan/AndroidStudioProjects/PadhaiPlus2/app/src/main/java/com/ps/padhaiplus/AlarmScheduler.kt)
- Helper object/class to schedule and cancel exact alarms via `AlarmManager`.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/roshan/AndroidStudioProjects/PadhaiPlus2/app/src/main/java/com/ps/padhaiplus/MainActivity.kt)
- Update `RoutineItem` data model to include `hour` (Int), `minute` (Int), `isEnabled` (Boolean).
- Add time selection UI (Hour/Minute inputs or TimePicker dialog/inputs) when creating routines.
- Integrate `AlarmScheduler` calls when routines are added, toggled, or deleted.
- Request runtime permissions (`POST_NOTIFICATIONS`, exact alarm permission check).

## Verification Plan

### Automated Tests
- Build verification via `gradle_build` (`app:assembleDebug`).

### Manual Verification
- Deploy app to emulator/device, add a routine with a specific time, and verify alarm scheduling and vibration handling.
