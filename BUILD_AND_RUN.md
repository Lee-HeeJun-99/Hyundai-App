# Build and run

The project uses AGP 8.7.3, Kotlin 2.0.21, Gradle wrapper 9.6.0, compile SDK 35, min SDK 28, target SDK 34, and JVM target 17. It uses the Android Studio JBR in `/opt/android-studio/jbr` as `JAVA_HOME` because the shell default JDK is 11. Android SDK is configured in local.properties and must match the local machine.

```bash
cd /home/ubuntu/HD/Hyundai-App
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk` after a successful build.

To run on the installed Pleos Connect AVD, start the emulator in Android Studio, verify `adb devices`, then:

```bash
/home/ubuntu/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
/home/ubuntu/Android/Sdk/platform-tools/adb shell am start -n com.rentaltutor.app/.ui.MainActivity
/home/ubuntu/Android/Sdk/platform-tools/adb logcat -s RentalTutor:I
```

Training is blocked if Automotive UX restrictions cannot be read or if distraction optimization is required. Set the emulator to a parked state and reopen the screen.

## Phase 2 Pleos integration

1. In a separate branch, add `Vehicle:2.3.2` and exact method-level permissions only for chosen read APIs.
2. Query capability before access, then test reads and events on the Pleos emulator with Virtual Console logs.
3. Keep `MOCK`, `PLEOS_SDK`, and `UNAVAILABLE` labels distinct. Never silently substitute mock values for a live reading.
4. Verify a parked/gear signal and permissions on the actual platform. Keep all training controls virtual; do not call setters for warnings, drive, brake, steering, ignition, or motion blocking.

## Phase 1 verification on 2026-11-05

- `:app:assembleDebug`: BUILD SUCCESSFUL with Android Studio JBR.
- `:app:testDebugUnitTest`: BUILD SUCCESSFUL; four tests, zero failures/errors.
- APK SHA-256: `e59f1bb3e8ca11197b5f6528fd5e29ffddf6d1eff2d5547911ada563b1ac8bfb`.
- `adb devices -l`: initially no connected devices. Pleos Connect v2.1.0 was started headlessly with `-accel off` because `/dev/kvm` is absent. ADB stayed `emulator-5554 offline`; an additional 120-second `adb wait-for-device` timed out (exit 124). The emulator was then shut down. APK install, app launch, screen behavior, UX gating, and Virtual Console events remain unverified.
