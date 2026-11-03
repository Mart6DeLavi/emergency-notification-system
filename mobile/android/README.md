# SENSA Android

Native Android client for the SENSA emergency notification system.

## Stack

- Kotlin 2.0.21
- Jetpack Compose (Material 3)
- Retrofit + Gson
- OkHttp
- Coil
- Android Photo Picker

## Requirements

- JDK 17+
- Android SDK 36
- An emulator or physical device (API 26+)

The SDK location is read from `local.properties` (not committed):

```properties
sdk.dir=/path/to/Android/sdk
```

## Build

```bash
cd mobile/android
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Run

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.sensa.mobile -c android.intent.category.LAUNCHER 1
```

The app talks to the production API through the gateway:

```text
https://panel.sensanotify.com/api/
```

## Features

- Login and registration with address data
- Rescue-service alerts
- User reports with status
- Report details with presigned attachment URLs
- New report flow:
  - select photos/videos with the Android Photo Picker
  - upload all attachments
  - show per-file moderation status and confidence
  - submit the event and link approved files
- Profile editing
- Email and push notification preferences (in-app alerts only; push delivery is not enabled)

## Notes

- JWT is stored in SharedPreferences for the prototype. Use EncryptedSharedPreferences/DataStore for production.
- Push notifications are not part of this client yet; alerts are loaded from the in-app endpoint.
- Video attachments are uploaded and moderated server-side; the app shows a video tile and an external-open action.
