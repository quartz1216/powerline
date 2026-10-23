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

median() { tr ' ' '\n' | sort -n | awk 'NF { v[++n] = $1 } END { print (n ? v[int((n + 1) / 2)] : "n/a") }'; }

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

# "Indexed N apps in X ms" from AppIndex, first in the log
index_time() { adb logcat -d -s AppIndex:I | sed -n 's/.*apps in \([0-9]*\) ms.*/\1/p' | head -1; }

# UI-thread time per frame (HandleInputStart to SyncQueued) since the last gfxinfo reset, as in
# OverlayE2ETest, one per line
frame_times() {
    adb shell dumpsys gfxinfo "$PKG" framestats | tr -d '\r' | awk -F, '
        /^Flags,/ { for (i = 1; i <= NF; i++) col[$i] = i; next }
        col["Flags"] && $1 == "0" && $col["SyncQueued"] > 0 {
            printf "%.1f\n", ($col["SyncQueued"] - $col["HandleInputStart"]) / 1000000
        }'
}

# Value at percentile $1 of numbers on stdin, one per line
percentile() { sort -n | awk -v p="$1" 'NF { v[++n] = $1 } END { print (n ? v[int((n * p + 99) / 100)] : "n/a") }'; }

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
# The index loads when the service connects, or on the first open if it is not ready by then
firsts=""
indexed=""
ready=0
for _ in $(seq "$RUNS"); do
    adb shell am force-stop "$PKG"
    adb logcat -c
    enable_service
    sleep 2
    at_connect=$(index_time)
    firsts="$firsts $(open_time)"
    adb logcat -d -s SearchOverlay:I | grep -q "([1-9][0-9]* apps ready)" && ready=$((ready + 1))
    indexed="$indexed ${at_connect:-$(index_time)}"
done

# --- later opens ---
laters=""
for _ in $(seq "$RUNS"); do laters="$laters $(open_time)"; done

# --- typing "powerline" a key at a time into the open overlay ---
typing_p50=""
typing_p90=""
for _ in 1 2 3; do
    alt_space
    sleep 1
    adb shell dumpsys gfxinfo "$PKG" reset >/dev/null
    for c in p o w e r l i n e; do adb shell input text "$c"; sleep 0.06; done
    sleep 0.5
    frames=$(frame_times)
    typing_p50="$typing_p50 $(echo "$frames" | percentile 50)"
    typing_p90="$typing_p90 $(echo "$frames" | percentile 90)"
    esc
    sleep 0.5
done

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
echo "Release APK size              ${apk_kb} KB"
echo "MainActivity cold start       median $(echo $starts | median) ms   ($starts )"
echo "Overlay first open            median $(echo $firsts | median) ms   ($firsts )"
echo "First opens with apps ready   $ready / $RUNS"
echo "AppIndex first load           median $(echo $indexed | median) ms   ($indexed )"
echo "Overlay later opens           median $(echo $laters | median) ms   ($laters )"
echo "Typing UI-thread p50 / frame  median $(echo $typing_p50 | median) ms   ($typing_p50 )"
echo "Typing UI-thread p90 / frame  median $(echo $typing_p90 | median) ms   ($typing_p90 )"
echo "Idle memory (PSS)             ${pss_kb} KB"
echo "AppIndex reloads, 10 package events  $reloads"
