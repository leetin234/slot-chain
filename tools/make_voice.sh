#!/bin/bash
# 음성 안내 문구를 macOS 유나 음성으로 만든다(시연용 TTS). 실행: tools/make_voice.sh
set -e
cd "$(dirname "$0")/.."
OUT=app/src/main/assets/voice; W=tools/.work; mkdir -p $OUT $W
say_to() { say -v Yuna -r 200 -o "$W/$1.aiff" "$2"; ffmpeg -v error -y -i "$W/$1.aiff" -af "silenceremove=start_periods=1:start_threshold=-50dB,areverse,silenceremove=start_periods=1:start_threshold=-50dB,areverse,highpass=f=120,loudnorm=I=-16:TP=-1.5" -ar 22050 -ac 1 "$OUT/$1.wav"; }
say_to prompt_late "예약보다 10분 늦을 것 같아요. 6시 15분으로 바꿀까요?"
say_to done_late "예약을 6시 15분으로 바꿨어요. 5분 여유가 있어요."
say_to keep_late "알겠어요. 예약은 그대로 두고 식당에 지각 소식을 전할게요."
say_to prompt_full "바다정 해물찜은 지금 50분 대기예요. 정차하면 더 빨리 앉을 수 있는 곳을 비교해 드릴게요."
say_to done_switch "해송 해물찜 6시 25분으로 예약하고 길 안내를 바꿨어요."
ls -la $OUT
