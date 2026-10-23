# Powerline
<p align="center">
  <img src="docs/Header.jpg" alt="Powerline — A command launcher for Android" width="100%">
</p>

## Overview
- A command launcher for Android, inspired by Spotlight.
- Keyboard first: open it with Alt+Space.
- Works in desktop mode too.

## Features
- **Apps**: lists your apps and finds them by any text in the app name or package name.
- **Calculator**: `12*3`, `200*10%`, `(1+2)/4`. Enter copies the result.
- **Web search**: search engines defined by a URL query, so you can add your own. Each is reachable by its alias.
- **Share**: create your own aliases for sharing text to an app.
- **Chips and aliases**: reach each feature from its chip or by typing its alias.

## How to Use
1. Install the app.
2. Open Powerline and allow the accessibility permission.
3. Press Alt+Space on a physical keyboard.

## If it doesn't work
- On OSs with strict task management such as Xiaomi HyperOS, even accessibility services may be automatically killed. You may need to uncheck 'Pause app activity if unused' in App Info or set battery restrictions to 'No restrictions'.

## Support for non-Latin users(Optional)
- Search also matches package names, so Powerline is easy to use with an IME.
  - For example, an app named 「電話」 can be found by typing "Phone", from its package name `com.example.phone`.
- Powerline turns the IME off automatically when it opens.
  - Note: tested only with Gboard!

## Supported environments
### Requirements
- Android 8.0 or later
- A physical keyboard (USB or Bluetooth). On-screen keyboards cannot open Powerline.

### Tested on
- Android Studio emulator (virtual)
  - Desktop setup (like Pixel for desktop)
- Xiaomi Pad Mini (physical)
  - Tablet mode and Workstation mode
  - Gboard (US and Japanese)
- That is all so far. If something goes wrong on your device, please report it in [Issues](https://github.com/quartz1216/powerline/issues).

## Privacy
- The accessibility permission is used only to catch Alt+Space and to show the search panel on top of other apps.
- Powerline does not have the `INTERNET` permission. It cannot send anything anywhere.
- It does not read screen content and does not record keystrokes.
- The only data it keeps is your Powerline launch history and search settings, stored on the device.
- It shows no ads and contains no analytics or tracking SDKs.
- See [PRIVACY.md](PRIVACY.md) for details.
