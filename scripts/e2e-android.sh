#!/usr/bin/env bash
# End-to-end check of the release APK on an emulator: self-test, then a scripted walk through
# the UI with screenshots. Usage: scripts/e2e-android.sh <phone|tv> <apk> <output-dir>
set -uo pipefail
MODE=$1 APK=$2 OUT=$3
PKG=app.anitv
ACTIVITY=$PKG/app.anitv.android.MainActivity
mkdir -p "$OUT"
step=0

shot() { step=$((step + 1)); adb exec-out screencap -p > "$OUT/$MODE-$(printf %02d $step)-$1.png"; echo "screenshot $1"; }
key() { adb shell input keyevent "$@"; sleep 1; }

# Taps the center of the first UI node whose text or content-description matches a regex.
tap() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$OUT/ui.xml" >/dev/null 2>&1
  local xy
  xy=$(python3 - "$OUT/ui.xml" "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
pattern = re.compile(sys.argv[2])
for node in ET.parse(sys.argv[1]).iter("node"):
    label = node.get("text") or node.get("content-desc") or ""
    if pattern.search(label):
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
)
  if [ -z "$xy" ]; then echo "tap: nothing matches '$1'"; return 1; fi
  echo "tap '$1' at $xy"; adb shell input tap $xy; sleep 1
}

adb wait-for-device
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb install -r "$APK" || { echo "install failed"; exit 1; }

# 1. Self-test: network, AniList, images, AniSkip, source, stream.
adb logcat -c
adb shell am start -W -n "$ACTIVITY" --ez selftest true
for _ in $(seq 1 60); do
  adb logcat -d -s ANITV_SELFTEST:I | grep -q "SELFTEST DONE" && break
  sleep 2
done
adb logcat -d -s ANITV_SELFTEST:I | tee "$OUT/$MODE-selftest.txt"
adb shell am force-stop $PKG

# 2. Scripted UI walk.
adb logcat -c
adb shell am start -W -n "$ACTIVITY"
sleep 15; shot home
if [ "$MODE" = tv ]; then
  key KEYCODE_DPAD_RIGHT; key KEYCODE_DPAD_RIGHT; shot home-focus-moved
  key KEYCODE_DPAD_CENTER; sleep 8; shot details
  key KEYCODE_DPAD_CENTER; sleep 20; shot player
  key KEYCODE_DPAD_CENTER; sleep 2; shot player-controls
  key KEYCODE_BACK; sleep 3; shot back-to-details
  key KEYCODE_BACK; sleep 2; key KEYCODE_DPAD_UP; key KEYCODE_DPAD_UP; key KEYCODE_DPAD_UP; shot search-button
else
  tap "^Watch now$" && sleep 8; shot details
  tap "^(Play|Resume) episode" && sleep 20; shot player
  size=$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1)
  adb shell input tap $(( ${size%x*} / 2 )) $(( ${size#*x} / 2 )); sleep 1; shot player-controls
  key KEYCODE_BACK; sleep 3; shot back-to-details
  key KEYCODE_BACK; sleep 2
  tap "^Search$" && adb shell input text "frieren" && sleep 6; shot search
fi

adb logcat -d > "$OUT/$MODE-logcat.txt"
grep -E "FATAL EXCEPTION|ANITV_PLAYER|AndroidRuntime" -A 20 "$OUT/$MODE-logcat.txt" > "$OUT/$MODE-errors.txt" || true
rm -f "$OUT/ui.xml"

# Fail the job when the self-test reported failures or the app crashed.
grep -q "SELFTEST DONE pass=[0-9]* fail=0" "$OUT/$MODE-selftest.txt" || { echo "self-test failed"; exit 1; }
! grep -q "FATAL EXCEPTION" "$OUT/$MODE-logcat.txt" || { echo "app crashed"; exit 1; }
echo "e2e $MODE OK"
