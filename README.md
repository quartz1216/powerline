# Powerline

A keyboard-first launcher for Android. Press **Alt+Space** on a physical keyboard from any app and a search panel opens on top of it. Type, then press Enter.

- **Apps**: matches name, package name and common English names ("phone" finds 電話)
- **Calculator**: `12*3`, `200*10%`, `(1+2)/4`. Enter copies the result.
- **Web**: plain text searches Google. Start with an alias to use another engine: `yt`, `ytm`, `maps`, `x`, `wiki`, `play`. URLs open directly.
- **Share**: sends the text to any app through the share sheet

With an empty search field, the panel shows engine shortcuts, your recent apps and all apps.

Powerline 2 is not a home screen app any more. Keep your usual launcher; Powerline sits on top of it.

## Setup

1. Install Powerline and open it from the app drawer. The status screen shows what still needs to be turned on.
2. **Accessibility**: tap the chip, then turn on Powerline under *Downloaded apps* (or *Installed apps*). Powerline uses the accessibility service only to detect Alt+Space; see [PRIVACY.md](PRIVACY.md).
3. **Power option**: tap the chip and allow Powerline to run without battery restrictions. Some systems (Xiaomi HyperOS especially) stop accessibility services in the background otherwise. On HyperOS, also turn off *Pause app activity if unused* in App info.
4. Press Alt+Space.

Alt+Space needs a physical keyboard. Keys sent by software (on-screen keyboards, `adb shell input`) do not reach accessibility services.

## Keys

| Key | In the search panel |
|---|---|
| Alt+Space | Open / close |
| ↑ ↓ ← → | Move the selection |
| Tab / Shift+Tab | Next / previous section (engine chips, recent apps, all apps; or result groups) |
| Enter | Run the selected item |
| Esc / Back | Close |

## What Powerline keeps

Only your launch history (the last 100 apps opened from Powerline) and the search settings on the status screen. There are no backup files; Android backup includes only the search settings. Powerline has no internet permission. Details in [PRIVACY.md](PRIVACY.md).

## Development

Requirements: Android Studio / JDK 17, Android SDK 34.

```sh
./gradlew :app:assembleDebug        # build
./gradlew :app:testDebugUnitTest    # JVM tests (search pipeline, providers, history)
scripts/device-tests.sh             # instrumented + end-to-end tests on an emulator or device
scripts/measure.sh                  # startup, open time, typing and memory numbers (release build)
```

The end-to-end tests press Alt+Space as a physical keyboard: on an emulator or rooted device by writing to `/dev/input` (the script runs `adb root` for that), on other devices through a virtual USB keyboard made with Android's `hid` tool (Android 12+). The app stays installed after the run.

The plan and status of the V2 rework is in [ROADMAP.md](ROADMAP.md).
