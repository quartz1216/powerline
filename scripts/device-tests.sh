#!/usr/bin/env sh
# Runs the instrumented tests on a connected emulator / rooted device.
#
# Powerline only reacts to Alt+Space from a physical keyboard, so OverlayE2ETest writes the
# keys straight to /dev/input. That needs adbd as root and the input nodes writable.
set -e
cd "$(dirname "$0")/.."

adb root >/dev/null
adb wait-for-device
# Git Bash on Windows rewrites /dev/... paths unless told not to
MSYS_NO_PATHCONV=1 adb shell "chmod 666 /dev/input/event*"

./gradlew :app:connectedDebugAndroidTest "$@"
