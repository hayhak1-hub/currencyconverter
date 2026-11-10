#!/usr/bin/env bash
# Smoke test for the R8-minified release build on an emulator or device.
#
# Debug builds hide release-only crashes (R8 stripping generic signatures, missing keep rules).
# This installs the release APK, walks the main screens, creates an alert, and fails on any crash.
#
#   scripts/release_smoke_test.sh [path/to/currencyconverter.apk]
#
# Needs: adb on PATH (or ANDROID_HOME), python3, one running emulator/device (set ANDROID_SERIAL
# if several are attached). The app language is forced to English so labels can be found by text.
set -u
# Git Bash on Windows rewrites /sdcard/... arguments into local paths; keep them as typed.
export MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL="*"

APK="${1:-app/build/outputs/apk/release/currencyconverter.apk}"
PKG="com.hayhak.currencyConverter"
ACTIVITY="$PKG/com.hayhak.currencyconverter.MainActivity"
command -v adb >/dev/null 2>&1 || export PATH="$PATH:${ANDROID_HOME:-$HOME/AppData/Local/Android/Sdk}/platform-tools"
PY="$(command -v python3 || command -v python)"
TMP="$(mktemp -d)"
# A Windows python cannot open MSYS-style /tmp paths; hand it the native form.
TMPPY="$(command -v cygpath >/dev/null 2>&1 && cygpath -m "$TMP" || echo "$TMP")"
FAILS=0

fail() { echo "FAIL: $*"; FAILS=$((FAILS + 1)); }
pass() { echo "ok:   $*"; }

[ -f "$APK" ] || { echo "APK not found: $APK (run ./gradlew assembleRelease first)"; exit 2; }

dump_ui() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb exec-out cat /sdcard/ui.xml > "$TMP/ui.xml" 2>/dev/null
}

# Prints "x y" of the first node whose text or content-desc contains $1 (case-insensitive).
find_node() {
  "$PY" - "$1" "$TMPPY/ui.xml" "${2:-}" "${3:-1}" <<'PYEOF'
import re, sys
import xml.etree.ElementTree as ET
needle = sys.argv[1].lower()
want_class = sys.argv[3] if len(sys.argv) > 3 else ""
nth = int(sys.argv[4]) if len(sys.argv) > 4 else 1
seen = 0
for n in ET.parse(sys.argv[2]).iter("node"):
    label = (n.get("text") or "") + " " + (n.get("content-desc") or "")
    if want_class:
        match = n.get("class") == want_class
    else:
        match = needle in label.lower()
    if match:
        seen += 1
        if seen < nth:
            continue
        m = re.findall(r"\d+", n.get("bounds", ""))
        if len(m) == 4:
            x1, y1, x2, y2 = map(int, m)
            print((x1 + x2) // 2, (y1 + y2) // 2)
            sys.exit(0)
sys.exit(1)
PYEOF
}

tap_text() {
  local label="$1" i xy
  for i in 1 2 3 4; do
    dump_ui
    if xy="$(find_node "$label")"; then
      adb shell input tap $xy
      sleep 1.5
      return 0
    fi
    sleep 1.5
  done
  return 1
}

has_text() {
  dump_ui
  find_node "$1" >/dev/null 2>&1
}

crashed() { adb logcat -d -b crash 2>/dev/null | grep -q "$PKG"; }

echo "== installing $APK"
adb uninstall "$PKG" >/dev/null 2>&1
adb install "$APK" >/dev/null || { echo "install failed"; exit 2; }
adb shell cmd locale set-app-locales "$PKG" --user 0 --locales en >/dev/null 2>&1
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS >/dev/null 2>&1
adb logcat -b all -c
adb shell am start -n "$ACTIVITY" >/dev/null
sleep 8

has_text "Converter" && pass "app starts on the converter" || fail "converter screen not shown"

open_screen() { # drawer item label
  tap_text "Open menu" || { fail "menu button not found"; return 1; }
  tap_text "$1" || { fail "drawer item '$1' not found"; return 1; }
}

open_screen "Rates"      && { has_text "Base" && pass "Rates opens" || fail "Rates content missing"; }
open_screen "Statistics" && { has_text "Today" && pass "Statistics opens" || fail "Statistics content missing"; }
open_screen "History"    && { has_text "Compare" && pass "History opens" || fail "History content missing"; }
open_screen "Metals" && {
  ok=0
  for _ in 1 2 3 4 5 6 7 8; do has_text "XAU" && { ok=1; break; }; sleep 3; done
  [ "$ok" -eq 1 ] && pass "Metals loads prices" || fail "Metals prices missing"
}

# The alarm flow is what crashed in release (Gson TypeToken under R8).
if open_screen "Alerts"; then
  if tap_text "Add alert"; then
    sleep 1; dump_ui
    if xy="$(find_node "" android.widget.EditText)"; then
      adb shell input tap $xy; sleep 1
      adb shell input text 1.5; sleep 1
      # Hide the keyboard only if it is up; BACK would otherwise close the dialog.
      adb shell dumpsys input_method | grep -q "mInputShown=true" && { adb shell input keyevent BACK; sleep 1; }
      tap_text "Set alert" || fail "Set alert button not found"
      sleep 2
      has_text "1.5000" && pass "alert is saved and listed" || fail "alert not listed after saving"
    else
      fail "alert value field not found"
    fi
  else
    fail "add alert button not found"
  fi
fi

open_screen "Portfolio"  && { has_text "Not investment advice" && pass "Portfolio opens" || fail "Portfolio content missing"; }
# Portfolio entries are stored as JSON, which is the other place R8 can break Gson.
if tap_text "Add holding"; then
  sleep 1; dump_ui
  if xy="$(find_node "" android.widget.EditText 1)"; then
    adb shell input tap $xy; sleep 1; adb shell input text 100; sleep 1
    adb shell dumpsys input_method | grep -q "mInputShown=true" && { adb shell input keyevent BACK; sleep 1; }
    dump_ui
    if xy="$(find_node "" android.widget.EditText 2)"; then
      adb shell input tap $xy; sleep 1; adb shell input text 35; sleep 1
      adb shell dumpsys input_method | grep -q "mInputShown=true" && { adb shell input keyevent BACK; sleep 1; }
      tap_text "Save" || fail "portfolio Save button not found"
      sleep 2
      has_text "Total value" && pass "portfolio entry is saved and valued" || fail "portfolio entry not shown after saving"
    else
      fail "portfolio price field not found"
    fi
  else
    fail "portfolio amount field not found"
  fi
else
  fail "add holding button not found"
fi

open_screen "Trips"      && { has_text "Create a trip" && pass "Trips opens" || fail "Trips content missing"; }
open_screen "Settings" && { has_text "Appearance" && pass "Settings opens" || fail "Settings content missing"; }

if crashed; then
  fail "crash detected:"; adb logcat -d -b crash | grep -A12 "$PKG" | head -30
else
  pass "no crash in the crash log"
fi

adb shell cmd locale set-app-locales "$PKG" --user 0 --locales "" >/dev/null 2>&1
rm -rf "$TMP"
echo
[ "$FAILS" -eq 0 ] && { echo "SMOKE TEST PASSED"; exit 0; } || { echo "SMOKE TEST FAILED ($FAILS)"; exit 1; }
