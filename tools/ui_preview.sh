#!/usr/bin/env bash
# Runs the built SmartTube VR APK in an Android emulator and records the VR interface
# (screenshots + video) so it can be compared with docs/interfaz/*.jpg.
# Usage: ui_preview.sh APK_DIR VIDEO_URL OUT_DIR   (run inside reactivecircus/android-emulator-runner)
set -u
APK_DIR=$1; URL=$2; OUT=$3
mkdir -p "$OUT"
BT=$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)
APK=$(ls "$APK_DIR"/*.apk | head -1)
PKG=$("$BT/aapt" dump badging "$APK" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
echo "APK: $APK  package: $PKG"
adb install -r -g "$APK" || { echo "install failed"; exit 1; }

# Landscape 1280x720 dp, like the Quest 2D panel.
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
adb shell wm size 1920x1080
adb shell wm density 240

shot() { sleep "${2:-2}"; adb exec-out screencap -p > "$OUT/$1.png" && echo "captura: $1"; }

# Tap the view whose text (or content-desc) matches, using a UI Automator dump.
tap_node() {
  local attr=$1 value=$2 xy=""
  for i in 1 2 3; do
    rm -f /tmp/ui.xml
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 && adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
    xy=$(python3 - "$attr" "$value" /tmp/ui.xml <<'PY'
import re, sys, xml.etree.ElementTree as ET
attr, value, path = sys.argv[1:]
try:
    root = ET.parse(path).getroot()
except Exception:
    sys.exit(0)
for n in root.iter('node'):
    if n.get(attr, '') == value:
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
)
    [ -n "$xy" ] && break
    sleep 1
  done
  if [ -z "$xy" ]; then echo "no encontrado: $value"; cp /tmp/ui.xml "$OUT/ui_$(echo "$value" | tr -c 'A-Za-z0-9' _).xml" 2>/dev/null; return 1; fi
  adb shell input tap $xy
}

adb logcat -c
adb shell screenrecord --time-limit 150 --bit-rate 2500000 /sdcard/demo.mp4 &
adb shell am start -a android.intent.action.VIEW -d "$URL" -p "$PKG"
shot 01_abriendo_video 30
adb shell input keyevent KEYCODE_DPAD_CENTER
shot 02_hud_barra_y_pildora 3
adb shell input keyevent KEYCODE_MENU
shot 03_reproductor_vr 3
tap_node text "Escenarios" && shot 04_escenarios 3
tap_node text "Cyberpunk" && shot 05_cyberpunk_elegido 3
adb shell input keyevent KEYCODE_BACK; sleep 2
adb shell input keyevent KEYCODE_DPAD_CENTER
shot 06_hud_tema_cyberpunk 3
adb shell input keyevent KEYCODE_MENU; sleep 2
tap_node text "Pantalla virtual" && shot 07_pantalla_virtual 3
adb shell input keyevent KEYCODE_BACK; sleep 2
adb shell input keyevent KEYCODE_MENU; sleep 2
tap_node text "Escenarios" && sleep 2 && tap_node text "Playa tropical"; sleep 2
adb shell input keyevent KEYCODE_BACK; sleep 2
adb shell input keyevent KEYCODE_DPAD_CENTER
shot 08_hud_tema_oceano 3
sleep 8
adb shell pkill -INT screenrecord; sleep 4
adb pull /sdcard/demo.mp4 "$OUT/demo.mp4" || true
adb logcat -d -b crash > "$OUT/crash.txt" 2>/dev/null || true
adb logcat -d 2>/dev/null | grep -E "AndroidRuntime|QuestHud|cinema" | tail -300 > "$OUT/logcat.txt" || true
ls -la "$OUT"
