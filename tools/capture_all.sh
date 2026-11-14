#!/bin/bash
# 모든 장면을 순서대로 띄워 화면을 캡처한다(UI 점검용). Virtual Console 탭이 열려 있어야 한다.
# 실행: tools/capture_all.sh  → shots/*.png
set -u
cd "$(dirname "$0")/.."
A=${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}
PKG=kr.slotchain.vehicle/kr.slotchain.MainActivity
VC=http://localhost:8088
mkdir -p shots
gear() { curl -s -X PUT "$VC/vehicle/gear?gear=$1" | grep -q error && echo "gear $1 실패(Virtual Console 탭 확인)"; }
cap() { $A exec-out screencap -p > shots/raw.png; sips -c 1210 1680 --cropOffset 90 856 shots/raw.png --out "shots/$1.png" >/dev/null; }
tap() { $A shell uiautomator dump /sdcard/ui.xml >/dev/null; xy=$($A shell cat /sdcard/ui.xml | python3 -I tools/find_text.py "$1" "${2:-1}"); $A shell input tap $xy; }
go() { $A shell am start -n $PKG "$@" >/dev/null 2>&1; }

gear Park
$A shell am start -S -W -n $PKG >/dev/null; sleep 3; cap setup
gear Drive
go --es scene A; sleep 2.5; cap a1; sleep 5; cap a2
go --es say yes; sleep 1.8; cap a3; sleep 3.4; cap a4
go --es scene B; sleep 3; cap b1
gear Park; sleep 3; cap c1
tap "해송 해물찜"; sleep 1.5; cap c2
tap "예약하고 길 안내 바꾸기"; sleep 2.2; cap c3
sleep 10; $A shell am start -n $PKG >/dev/null 2>&1; sleep 1.5; cap c4
go --es scene D; sleep 1.5; tap "음식 5점"; tap "정확해요"; tap "주차 4점"; sleep 0.5; tap "평가 보내기"; sleep 1.5; cap d1
rm -f shots/raw.png
ls shots
