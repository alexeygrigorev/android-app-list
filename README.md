# android-app-list

A minimal Android home-screen replacement that shows nothing but an
alphabetical list of every installed app. There is no home screen, no
widgets, and no app drawer to swipe into: the list is the home screen.

It's meant for phones that are driven programmatically (adb, Appium,
UI Automator), where a predictable, plain screen is more useful than a
regular launcher.

What it does:

- Lists every launchable app (icon + name), sorted by name
- Tap a row to open the app
- Home always returns to the list; Back does nothing on the list
- When the screen turns off, the list comes back to the front, so
  waking the phone always lands on the list
- Updates itself when apps are installed or removed

No Gradle, no dependencies: one activity, built with the plain SDK tools.

## Build

Requirements: JDK 17+, and an Android SDK with build-tools and the
`android-35` platform installed.

```bash
./build.sh
```

The SDK is taken from `ANDROID_HOME` (or `ANDROID_SDK_ROOT`, falling back
to `~/Android/Sdk`). You can override `BUILD_TOOLS_VERSION`, `PLATFORM`
and `KEYSTORE`. The APK is signed with the standard debug keystore
(`~/.android/debug.keystore`), which is created if missing.

The output is `build/app-list.apk`. The script works in Git Bash on
Windows as well as on Linux/macOS.

## Install and set as home

```bash
adb install -r build/app-list.apk
adb shell cmd package set-home-activity com.alexeygrigorev.applist/.MainActivity
adb shell input keyevent KEYCODE_HOME
```

## Revert to another launcher

List the available launchers:

```bash
adb shell cmd package query-activities --brief \
  -a android.intent.action.MAIN -c android.intent.category.HOME
```

Then set the one you want, for example the stock launcher on a Motorola
phone:

```bash
adb shell cmd package set-home-activity com.motorola.launcher3/com.android.launcher3.CustomizationPanelLauncher
```

Or uninstall this one completely:

```bash
adb uninstall com.alexeygrigorev.applist
```

## License

MIT
