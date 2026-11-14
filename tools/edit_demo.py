"""녹화본(demo/raw.webm)을 시연 영상(demo/SLOT_CHAIN_실동작_시연.mp4)으로 편집한다.

화면·소리는 에뮬레이터 녹화 그대로 쓰고, 그 위에 시연 영상 시안과 같은 구간 제목과
기능 설명 칩만 얹는다. 시각은 tools/record_demo.sh가 남긴 demo/events.txt에서 읽는다.
실행: tools/.venv/bin/python -I tools/edit_demo.py
"""
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
DEMO = ROOT / "demo"
WORK = DEMO / "edit"
FONT = ROOT / "app" / "src" / "main" / "res" / "font"
W, H = 1920, 1080
DEV = (200, 182, 1520, 855)  # 녹화 화면(2560x1440)을 놓을 위치와 크기
INTRO, OUTRO = 3.0, 3.5

TEAL = (100, 168, 255)
WHITE = (244, 246, 250)
SUB = (180, 194, 220)
DIM = (120, 138, 170)


def font(weight, size):
    return ImageFont.truetype(str(FONT / f"pretendard_{weight}.otf"), size)


def background():
    img = Image.new("RGB", (W, H))
    d = ImageDraw.Draw(img)
    for y in range(H):
        k = y / H
        d.line([(0, y), (W, y)], fill=(int(8 + 6 * (1 - k)), int(32 + 20 * (1 - k)), int(70 + 40 * (1 - k))))
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([-300, -500, 1100, 500], fill=(40, 110, 200, 70))
    img.paste(glow.filter(ImageFilter.GaussianBlur(160)), (0, 0), glow.filter(ImageFilter.GaussianBlur(160)))
    # 녹화 화면 뒤 그림자와 테두리.
    x, y, w, h = DEV
    sh = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle([x - 6, y + 4, x + w + 6, y + h + 18], 34, fill=(0, 0, 0, 150))
    img.paste(sh.filter(ImageFilter.GaussianBlur(18)), (0, 0), sh.filter(ImageFilter.GaussianBlur(18)))
    ImageDraw.Draw(img).rounded_rectangle([x - 10, y - 10, x + w + 10, y + h + 10], 30, fill=(20, 24, 34), outline=(52, 60, 80), width=2)
    d = ImageDraw.Draw(img)
    d.text((x, 1052), "SLOT CHAIN · 현대자동차그룹 차량용 앱 개발 공모전 시연 영상 · Pleos Connect Emulator 2.1.0 실제 화면·소리 녹화 · 가상 시설 · 모의 교통·웨이팅 · 음성은 시연용 TTS",
           font=font("medium", 15), fill=DIM)
    img.save(WORK / "bg.png")


def mask():
    x, y, w, h = DEV
    m = Image.new("L", (w, h), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, w - 1, h - 1], 22, fill=255)
    m.save(WORK / "mask.png")


def header(name, no, tag, chip, title_a, title_b, title_c, sub):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    x = DEV[0]
    f_no, f_tag = font("extrabold", 20), font("bold", 21)
    d.rounded_rectangle([x, 26, x + 44, 56], 7, fill=(49, 130, 246))
    d.text((x + 22, 41), no, font=f_no, fill=WHITE, anchor="mm")
    d.text((x + 56, 41), tag, font=f_tag, fill=TEAL, anchor="lm")
    tw = d.textlength(tag, font=f_tag)
    cx = x + 56 + tw + 14
    cw = d.textlength(chip, font=font("medium", 17)) + 26
    d.rounded_rectangle([cx, 26, cx + cw, 56], 15, outline=(90, 120, 170), width=2, fill=(20, 40, 75))
    d.text((cx + cw / 2, 41), chip, font=font("medium", 17), fill=SUB, anchor="mm")
    f_title = font("extrabold", 46)
    tx = x
    for part, color in ((title_a, WHITE), (title_b, TEAL), (title_c, WHITE)):
        d.text((tx, 70), part, font=f_title, fill=color)
        tx += d.textlength(part, font=f_title)
    d.text((x, 130), sub, font=font("medium", 21), fill=SUB)
    img.save(WORK / f"{name}.png")


def callout(name, text, accent=TEAL):
    """화면 오른쪽 위에 뜨는 기능 설명 칩."""
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    f = font("bold", 22)
    tw = d.textlength(text, font=f)
    x2 = DEV[0] + DEV[2]
    x1 = x2 - tw - 64
    d.rounded_rectangle([x1, 92, x2, 140], 24, fill=(10, 22, 44, 235), outline=accent, width=2)
    d.ellipse([x1 + 20, 110, x1 + 32, 122], fill=accent)
    d.text((x1 + 44, 116), text, font=f, fill=WHITE, anchor="lm")
    img.save(WORK / f"{name}.png")


def card(name, lines):
    img = Image.open(WORK / "bg.png").convert("RGB")
    # 카드 화면은 녹화 테두리 대신 가운데 정렬 문구만.
    base = Image.new("RGB", (W, H))
    d0 = ImageDraw.Draw(base)
    for y in range(H):
        k = y / H
        d0.line([(0, y), (W, y)], fill=(int(8 + 6 * (1 - k)), int(32 + 20 * (1 - k)), int(70 + 40 * (1 - k))))
    d = ImageDraw.Draw(base)
    # 로고: 파란 사각 + 맞물린 두 고리.
    cx, cy = W // 2, 360
    d.rounded_rectangle([cx - 56, cy - 56, cx + 56, cy + 56], 32, fill=(49, 130, 246))
    d.rounded_rectangle([cx - 38, cy - 19, cx + 9, cy + 19], 19, outline=WHITE, width=9)
    d.rounded_rectangle([cx - 9, cy - 19, cx + 38, cy + 19], 19, outline=WHITE, width=9)
    y = 470
    for text, size, weight, color in lines:
        d.text((W // 2, y), text, font=font(weight, size), fill=color, anchor="mt")
        y += size + 22
    base.save(WORK / f"{name}.png")


def run(cmd):
    print(" ".join(map(str, cmd))[:200])
    subprocess.run(cmd, check=True)


def main():
    WORK.mkdir(parents=True, exist_ok=True)
    background()
    mask()
    ev = {}
    for line in (DEMO / "events.txt").read_text().split("\n"):
        if line.strip():
            t, name = line.split()
            ev[name] = float(t)
    end = ev["end"]
    header("h1", "01", "출발 전 설정", "정차 상태", "출발 전, ", "예약과 내비", "를 한 번에 연결", "인원 · 메뉴 · 가격대 · 변경 허용 범위를 정해 두면 ETA가 바뀔 때마다 알아서 살펴봐요")
    header("h2", "02", "장면 A · 예약 지각 조정", "주행 중 · 음성 안내", "늦을 것 같으면, ", "예약이 먼저", " 움직여요", "ETA 6:10 · 예약 6:00 → 허용 범위 안에서 6:15 제안 · 같은 식당의 호환 예약끼리 순환 교환")
    header("h3", "03", "장면 B · 만석 식당 대안", "정차 후 비교", "가까운 곳보다, ", "먼저 앉을 수 있는 곳", "", "이동 + 주차·걷기 + 도착 후 대기를 합친 '이용 시작 시각'으로 비교 · 상세 비교는 정차 후에만")
    header("h4", "04", "장면 C · 예약과 길 안내 연결", "단계별 확정", "확정되면, ", "목적지까지", " 바로 바뀌어요", "재확인 → 신규 예약 → 기존 대기 취소 → 내비 목적지 변경 · 하나라도 실패하면 기존 예약 유지")
    header("h5", "05", "장면 D · 이용 후 평가", "정차 상태", "다녀온 뒤 평가가, ", "다음 추천", "을 바꿔요", "음식 · 대기 예측 정확도 · 주차 편의를 자체 평가로 쌓아 추천 순위에 반영")
    callouts = [
        ("c1", "안내 시작 → 실제 Pleos 내비에 목적지 설정(NaviHelper)", ev["guide"], ev["trip_a"], TEAL),
        ("c2", "기어 D · 성수대로 정체 +14분 → 도착 6:10 예상(모의 교통)", ev["drive"] + 0.5, ev["drive"] + 4.4, (255, 168, 38)),
        ("c3", "음성 제안 · 주행 중엔 두 가지 선택만", ev["drive"] + 4.6, ev["swap"], (255, 168, 38)),
        ("c4", "3자 순환 교환 · 각자 허용 범위 안에서 동의 후 일괄 확정", ev["swap"], ev["trip_b"], (21, 196, 126)),
        ("c5", "도착 후 대기 50분 → 이용 시작 약 60분 뒤(모의 웨이팅)", ev["trip_b"] + 0.5, ev["park"], (240, 68, 82)),
        ("c6", "기어 P → 정차 확인 후 상세 비교 자동으로 열림", ev["park"], ev["select"], TEAL),
        ("c7", "해송 해물찜 23분 뒤 · 원래 가던 곳보다 37분 빨라요(가정)", ev["select"], ev["confirm"], (21, 196, 126)),
        ("c8", "4단계 확인 · 하나라도 실패하면 기존 예약·목적지 유지", ev["confirm"], ev["confirm"] + 6.4, TEAL),
        ("c9", "실제 Pleos 내비가 해송 해물찜으로 경로 안내 시작", ev["confirm"] + 6.5, ev["switched"], TEAL),
        ("c10", "예약 6:25 확정 · 기다리지 않고 입장", ev["switched"], ev["visit"], (21, 196, 126)),
        ("c11", "평가 저장 → 다음 추천 순위에 반영", ev["sent"], end, TEAL),
    ]
    for name, text, *_ in callouts:
        callout(name, text, _[2])
    card("intro", [("SLOT CHAIN", 64, "extrabold", WHITE), ("도착 시간에 맞춰 예약을 잇는 차량용 예약 서비스", 28, "medium", SUB),
                   ("실동작 시연 · Pleos Connect Emulator 2.1.0", 22, "semibold", TEAL)])
    card("outro", [("도착 시간에 맞춰, 예약이 움직인다.", 52, "extrabold", WHITE), ("ETA 기반 예약 조정 · 이용 시작 시각 비교 · 내비 목적지 변경", 26, "medium", SUB),
                   ("Kotlin · Jetpack Compose · Pleos NaviHelper · CarUxRestrictions", 21, "semibold", TEAL)])

    sections = [("h1", 0, ev["trip_a"]), ("h2", ev["trip_a"], ev["trip_b"]), ("h3", ev["trip_b"], ev["confirm"]), ("h4", ev["confirm"], ev["visit"]), ("h5", ev["visit"], end + 1)]
    x, y, w, h = DEV
    inputs = ["-i", str(DEMO / "raw.webm"), "-loop", "1", "-i", str(WORK / "bg.png"), "-loop", "1", "-i", str(WORK / "mask.png")]
    overlays = [s[0] for s in sections] + [c[0] for c in callouts]
    for o in overlays:
        inputs += ["-loop", "1", "-i", str(WORK / f"{o}.png")]
    fc = [f"[0:v]scale={w}:{h}:flags=lanczos,format=rgba[dv]", "[2:v]format=gray[m]", "[dv][m]alphamerge[dm]", f"[1:v][dm]overlay={x}:{y}:shortest=1[v0]"]
    cur = "v0"
    times = {s[0]: (s[1], s[2]) for s in sections} | {c[0]: (c[2], c[3]) for c in callouts}
    for i, o in enumerate(overlays):
        a, b = times[o]
        nxt = f"v{i + 1}"
        fc.append(f"[{cur}][{3 + i}:v]overlay=0:0:enable='between(t,{a},{b})'[{nxt}]")
        cur = nxt
    fc.append(f"[{cur}]format=yuv420p[vout]")
    fc.append("[0:a]loudnorm=I=-16:TP=-1.5:LRA=11,aresample=48000[aout]")
    main_mp4 = WORK / "main.mp4"
    run(["ffmpeg", "-v", "error", "-y", *inputs, "-filter_complex", ";".join(fc), "-map", "[vout]", "-map", "[aout]",
         "-r", "30", "-c:v", "libx264", "-preset", "medium", "-crf", "19", "-c:a", "aac", "-b:a", "192k", "-t", f"{end + 0.6:.2f}", str(main_mp4)])
    for name, dur in (("intro", INTRO), ("outro", OUTRO)):
        run(["ffmpeg", "-v", "error", "-y", "-loop", "1", "-i", str(WORK / f"{name}.png"), "-f", "lavfi", "-i", "anullsrc=r=48000:cl=stereo",
             "-t", str(dur), "-vf", f"fade=in:0:12,fade=out:st={dur - 0.4}:d=0.4,format=yuv420p", "-r", "30", "-c:v", "libx264", "-crf", "19", "-c:a", "aac", "-b:a", "192k", str(WORK / f"{name}.mp4")])
    out = DEMO / "SLOT_CHAIN_실동작_시연.mp4"
    run(["ffmpeg", "-v", "error", "-y", "-i", str(WORK / "intro.mp4"), "-i", str(main_mp4), "-i", str(WORK / "outro.mp4"),
         "-filter_complex", "[0:v][0:a][1:v][1:a][2:v][2:a]concat=n=3:v=1:a=1[v][a]", "-map", "[v]", "-map", "[a]",
         "-c:v", "libx264", "-preset", "medium", "-crf", "19", "-c:a", "aac", "-b:a", "192k", "-movflags", "+faststart", str(out)])
    print(out, out.stat().st_size)


if __name__ == "__main__":
    main()
