# AGENTS.md

## Cursor Cloud specific instructions

### Overview

Stock Tracker Wear is a standalone Wear OS (Android smartwatch) app built with Kotlin, Jetpack Compose for Wear OS, Hilt, Room, Retrofit, and WorkManager. It has no backend or Docker services — it's a single-module Android Gradle project under `:wear`.

### Environment requirements

- **JDK 17** (set as default via `update-alternatives`)
- **Android SDK** installed at `/opt/android-sdk` with `platforms;android-35`, `build-tools;35.0.0`, and `platform-tools`
- Environment variables: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`, `ANDROID_SDK_ROOT=/opt/android-sdk`
- `local.properties` in project root must contain `sdk.dir=/opt/android-sdk` and optionally `STOCK_API_KEY=<key>` (empty key is fine for builds/tests)

### Key commands

| Task | Command |
|---|---|
| Build debug APK | `./gradlew :wear:assembleDebug` |
| Run unit tests | `./gradlew :wear:testDebugUnitTest` |
| Run lint | `./gradlew :wear:lintDebug` |
| Clean build | `./gradlew clean` |

### Gotchas

- The `com.google.android.wearable:wearable:2.9.0` `compileOnly` dependency is required in `wear/build.gradle.kts` to provide the `android:ambientEnabled` manifest attribute definition. Without it, AAPT will fail with "attribute android:ambientEnabled not found."
- The `android:ambientEnabled` attribute was removed from the manifest because it's deprecated and absent from the Android SDK platform 35. The project uses `AmbientLifecycleObserver` from AndroidX Wear instead.
- The `Configuration.Provider` interface for WorkManager uses a `val workManagerConfiguration` property (not the older `getWorkManagerConfiguration()` method) in WorkManager 2.9.0+.
- `scrollBy` for `ScalingLazyListState` requires `import androidx.compose.foundation.gestures.scrollBy`.
- The `CachedQuoteEntityMapper.kt` defines top-level extension functions (not inside an object), so imports use `com.stocktracker.wear.data.local.toDomain` / `toEntity`.
- There is no emulator/device available in the cloud VM, so the app cannot be run on a device. Build verification and unit tests are the primary validation methods.
- Test reports are generated at `wear/build/reports/tests/testDebugUnitTest/` and lint reports at `wear/build/reports/lint-results-debug.html`.
