#!/bin/bash
# SLOT CHAIN 실동작 시연 녹화. 에뮬레이터 자체 녹화기로 화면·소리를 담고,
# 기어는 Pleos Virtual Console HTTP API로 바꾼다. 각 장면의 시작 시각을 demo/events.txt에 남긴다.
# 준비: 에뮬레이터 부팅·앱 설치, `adb forward tcp:8088 tcp:8088`, 브라우저에 http://localhost:8088 탭 열기.
# 실행: tools/record_demo.sh
set -euo pipefail
cd "$(dirname "$0")/.."
A=${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}
PKG=kr.slotchain.vehicle/kr.slotchain.MainActivity
VC=http://localhost:8088
OUT=demo/raw.webm
EV=demo/events.txt
mkdir -p demo

now() { python3 -c 'import time; print(time.time())'; }
gear() { local r; r=$(curl -s -X PUT "$VC/vehicle/gear?gear=$1"); case "$r" in *error*) echo "기어 $1 실패: $r" >&2; $A emu screenrecord stop >/dev/null 2>&1 || true; exit 1;; esac; }
speed() { curl -s -X PUT "$VC/vehicle/controls?speed=$1&accelerator=0&brake=0&steering=0" >/dev/null; }
mark() { python3 -c "import sys; print(f'{float(sys.argv[1]) - float(sys.argv[2]):.2f} {sys.argv[3]}')" "$(now)" "$T0" "$1" >> $EV; echo "[$(date +%T)] $1"; }
tap() { $A shell uiautomator dump /sdcard/ui.xml >/dev/null; local xy; xy=$($A shell cat /sdcard/ui.xml | python3 -I tools/find_text.py "$1" "${2:-1}"); $A shell input tap $xy; }
front() { $A shell am start -n $PKG "$@" >/dev/null 2>&1; }

# 0. 준비: 정차, 설정 화면.
gear Park; speed 0
$A shell am start -S -W -n $PKG >/dev/null
sleep 3
rm -f $OUT $EV
$A emu screenrecord start --time-limit 175 "$PWD/$OUT" >/dev/null
T0=$(now)
mark setup
sleep 4.5

# 01 출발 전 설정 → 안내 시작(실제 Pleos 내비에 목적지 설정).
tap "안내 시작" 2
mark guide
sleep 7.5
front
mark trip_a

# 02 장면 A: 주행 → 정체로 10분 지각 예상 → 음성 제안 → 6:15로 변경(순환 교환 동의).
gear Drive; speed 48
mark drive
sleep 11
tap "6:15로 바꾸기"
mark swap
sleep 9

# 03 장면 B: 만석 식당(50분 대기) → 정차하면 비교.
front --es scene B
mark trip_b
sleep 9
speed 0; gear Park
mark park
sleep 4
tap "해송 해물찜"
mark select
sleep 4

# 04 예약·목적지 변경 4단계 → Pleos 내비가 새 목적지로 안내 시작.
tap "예약하고 길 안내 바꾸기"
mark confirm
sleep 13
front
mark switched
sleep 7

# 05 방문 후 평가.
front --es scene D
mark visit
sleep 2.5
tap "음식 5점"; sleep 0.6
tap "정확해요"; sleep 0.6
tap "주차 4점"; sleep 0.8
tap "평가 보내기"
mark sent
sleep 4
mark end
$A emu screenrecord stop >/dev/null || true
sleep 2
cat $EV
ls -la $OUT
