#!/usr/bin/env sh
# Measures the numbers in ROADMAP.md §6 on a connected emulator / rooted device, using the
# release build (R8) signed with the debug key. Replaces any installed Powerline build.
#
# Alt+Space is written to the keyboard's evdev node, as in scripts/device-tests.sh, so it
# needs adbd as root. The search benchmark runs on the JVM: SearchBenchmarkTest.
#
#   scripts/measure.sh            # RUNS=10 by default
set -e
cd "$(dirname "$0")/.."

PKG=dev.quxomiya.powerline
SERVICE="$PKG/$PKG.services.PowerlineAccessibilityService"
RUNS=${RUNS:-10}
# Git Bash on Windows rewrites /dev/... paths unless told not to
export MSYS_NO_PATHCONV=1

sdk_dir() {
    if [ -n "$ANDROID_HOME" ]; then echo "$ANDROID_HOME"; return; fi
    sed -n 's/^sdk.dir=//p' local.properties | sed 's/\\:/:/; s/\\\\/\//g'
}

median() { sort -n | awk '{ v[NR] = $1 } END { print (NR ? v[int((NR + 1) / 2)] : "n/a") }'; }

# --- build and install ---
./gradlew -q :app:assembleRelease
UNSIGNED=app/build/outputs/apk/release/app-release-unsigned.apk
APK=app/build/outputs/apk/release/app-release-measure.apk
BUILD_TOOLS="$(sdk_dir)/build-tools/$(ls "$(sdk_dir)/build-tools" | sort -V | tail -1)"
APKSIGNER="$BUILD_TOOLS/apksigner"
[ -x "$APKSIGNER" ] || APKSIGNER="$BUILD_TOOLS/apksigner.bat"
KEYSTORE="$HOME/.android/debug.keystore"
# Windows tools need a Windows path (path conversion is off, see above)
command -v cygpath >/dev/null && KEYSTORE=$(cygpath -m "$KEYSTORE")
"$APKSIGNER" sign --ks "$KEYSTORE" --ks-pass pass:android --ks-key-alias androiddebugkey \
    --out "$APK" "$UNSIGNED"

adb root >/dev/null
adb wait-for-device
adb shell "chmod 666 /dev/input/event*"
adb install -r "$APK" >/dev/null 2>&1 || { adb uninstall "$PKG" >/dev/null; adb install "$APK" >/dev/null; }

KEYBOARD=$(adb shell getevent -p | awk '
    /add device/ { dev = $NF }
    / 0039 / && dev != "" { print dev; exit }' | tr -d '\r')
[ -n "$KEYBOARD" ] || { echo "No keyboard with KEY_SPACE under /dev/input" >&2; exit 1; }

key() { # code value
    adb shell "sendevent $KEYBOARD 1 $1 $2; sendevent $KEYBOARD 0 0 0"
}
alt_space() { key 56 1; key 57 1; key 57 0; key 56 0; }
esc() { key 1 1; key 1 0; }

enable_service() {
    adb shell settings delete secure enabled_accessibility_services >/dev/null
    adb shell settings put secure enabled_accessibility_services "$SERVICE"
    adb shell settings put secure accessibility_enabled 1
}

# "Shown N ms" from SearchOverlay after one Alt+Space; the overlay is closed again afterwards
open_time() {
    adb logcat -c
    alt_space
    for _ in 1 2 3 4 5 6 7 8 9 10; do
        ms=$(adb logcat -d -s SearchOverlay:I | sed -n 's/.*Shown \([0-9]*\) ms.*/\1/p' | tail -1)
        [ -n "$ms" ] && break
        sleep 0.3
    done
    sleep 0.5
    esc
    sleep 0.5
    echo "${ms:-n/a}"
}

adb shell input keyevent KEYCODE_HOME

# --- APK size ---
apk_kb=$(( $(wc -c < "$APK") / 1024 ))

# --- MainActivity cold start ---
starts=""
for _ in $(seq "$RUNS"); do
    adb shell am force-stop "$PKG"
    sleep 0.5
    t=$(adb shell am start -W -n "$PKG/.MainActivity" | sed -n 's/TotalTime: //p' | tr -d '\r')
    starts="$starts $t"
done
adb shell input keyevent KEYCODE_HOME

# --- overlay first open: fresh process, service just bound ---
firsts=""
indexed=""
for _ in $(seq "$RUNS"); do
    adb shell am force-stop "$PKG"
    enable_service
    sleep 2
    firsts="$firsts $(open_time)"
    indexed="$indexed $(adb logcat -d -s AppIndex:I | sed -n 's/.*apps in \([0-9]*\) ms.*/\1/p' | head -1)"
done

# --- later opens ---
laters=""
for _ in $(seq "$RUNS"); do laters="$laters $(open_time)"; done

# --- idle memory, overlay closed ---
sleep 3
pss_kb=$(adb shell dumpsys meminfo "$PKG" | awk '/TOTAL PSS:/ { print $3; exit } /^ *TOTAL / { print $2; exit }')

# --- AppIndex reloads while one app is disabled / enabled 5 times (10 package events) ---
adb logcat -c
for _ in 1 2 3 4 5; do
    adb shell pm disable-user --user current com.android.camera2 >/dev/null
    adb shell pm enable --user current com.android.camera2 >/dev/null
done
sleep 5
reloads=$(adb logcat -d -s AppIndex:I | grep -c "Indexed" || true)

echo
echo "Release APK size        ${apk_kb} KB"
echo "MainActivity cold start median $(echo $starts | tr ' ' '\n' | median) ms   ($starts )"
echo "Overlay first open      median $(echo $firsts | tr ' ' '\n' | median) ms   ($firsts )"
echo "AppIndex load (first)   median $(echo $indexed | tr ' ' '\n' | median) ms   ($indexed )"
echo "Overlay later opens     median $(echo $laters | tr ' ' '\n' | median) ms   ($laters )"
echo "Idle memory (PSS)       ${pss_kb} KB"
echo "AppIndex reloads, 10 package events  $reloads"
