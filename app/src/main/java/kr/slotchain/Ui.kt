package kr.slotchain

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Wait = Color(0xFFFF9447)

@Composable
fun App(ui: UiState, act: Actions) {
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density * 1.3f, d.fontScale), LocalTextStyle provides DS.body2) {
        Box(Modifier.fillMaxSize().background(DS.bg)) {
            AnimatedContent(
                targetState = ui.scene,
                transitionSpec = { (fadeIn(tween(320)) + slideInHorizontally(tween(320)) { it / 20 }) togetherWith fadeOut(tween(180)) },
                label = "scene", modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp),
            ) { scene ->
                when (scene) {
                    Scene.SETUP -> Setup(ui, act)
                    Scene.TRIP_A, Scene.TRIP_B -> Trip(ui, act)
                    Scene.COMPARE -> Compare(ui, act)
                    Scene.VISIT -> Visit(ui, act)
                }
            }
            ToastPill(ui, Modifier.align(Alignment.TopCenter).padding(top = 14.dp))
            VoiceBar(ui, Modifier.align(if (ui.scene == Scene.TRIP_A || ui.scene == Scene.TRIP_B) Alignment.BottomStart else Alignment.BottomCenter).padding(start = 40.dp, bottom = 124.dp))
            if (ui.scene == Scene.COMPARE && ui.confirming) ConfirmModal(ui, act)
        }
    }
}

// ---------------- 공통 ----------------
@Composable
private fun Brand(ui: UiState, act: Actions, size: Dp = 40.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { act.scene(nextDemo(ui.scene)) }) }) { Logo(Modifier.size(size)) }
        Spacer(Modifier.width(12.dp))
        T("SLOT CHAIN", DS.title3)
    }
}

private fun nextDemo(s: Scene) = when (s) { Scene.SETUP -> "A"; Scene.TRIP_A -> "B"; Scene.TRIP_B, Scene.COMPARE -> "D"; Scene.VISIT -> "SETUP" }

@Composable
private fun SectionLabel(text: String, right: String? = null, rightColor: Color = DS.blueText) {
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        T(text, DS.caption.copy(fontWeight = FontWeight.Bold), DS.text2, Modifier.weight(1f))
        if (right != null) T(right, DS.caption.copy(fontWeight = FontWeight.Bold), rightColor)
    }
}

@Composable
private fun Rolling(value: String, style: androidx.compose.ui.text.TextStyle, color: Color) {
    AnimatedContent(
        targetState = value,
        transitionSpec = { (slideInVertically(tween(400)) { it } + fadeIn(tween(400))) togetherWith (slideOutVertically(tween(400)) { -it } + fadeOut(tween(250))) },
        label = "roll",
    ) { v -> T(v, style, color) }
}

@Composable
private fun FacilityIcon(kind: Int, size: Dp = 56.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(16.dp)).background(DS.surfaceHi), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size * 0.46f)) {
            val w = this.size.width
            val c = DS.text2
            if (kind == 0) {
                drawLine(c, Offset(w * 0.3f, 0f), Offset(w * 0.3f, w), w * 0.1f, StrokeCap.Round)
                drawLine(c, Offset(w * 0.17f, 0f), Offset(w * 0.17f, w * 0.34f), w * 0.07f, StrokeCap.Round)
                drawLine(c, Offset(w * 0.43f, 0f), Offset(w * 0.43f, w * 0.34f), w * 0.07f, StrokeCap.Round)
                drawOval(c, Offset(w * 0.6f, 0f), Size(w * 0.28f, w * 0.42f))
                drawLine(c, Offset(w * 0.74f, w * 0.4f), Offset(w * 0.74f, w), w * 0.1f, StrokeCap.Round)
            } else {
                val body = Path().apply { moveTo(w * 0.05f, w * 0.5f); quadraticBezierTo(w * 0.45f, w * 0.05f, w * 0.78f, w * 0.5f); quadraticBezierTo(w * 0.45f, w * 0.95f, w * 0.05f, w * 0.5f); close() }
                drawPath(body, c, style = Stroke(w * 0.09f))
                drawPath(Path().apply { moveTo(w * 0.78f, w * 0.5f); lineTo(w, w * 0.28f); lineTo(w, w * 0.72f); close() }, c, style = Stroke(w * 0.09f))
            }
        }
    }
}

@Composable
private fun NavArrow(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        drawPath(Path().apply { moveTo(w / 2, 0f); lineTo(w, w); lineTo(w / 2, w * 0.74f); lineTo(0f, w); close() }, color)
    }
}

// ---------------- 01 출발 전 설정 ----------------
@Composable
private fun Setup(ui: UiState, act: Actions) {
    val parked = ui.mode == Mode.PARKED
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Brand(ui, act)
            Spacer(Modifier.weight(1f))
            Badge(if (parked) "정차 중에만 바꿀 수 있어요" else "주행 중 · 설정 잠김", if (parked) DS.text2 else DS.orange)
        }
        Gap(28.dp)
        T("오늘 예약, 출발 전에 맞춰둘게요", DS.title1)
        Gap(6.dp)
        T("늦을 것 같으면 같은 식당 안에서 먼저 시간을 맞추고, 안 되면 더 빨리 앉을 곳을 찾아요", DS.body2, DS.text2)
        Gap(28.dp)
        Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(Modifier.weight(1f).fillMaxHeight()) {
                SectionLabel("오늘 예약", "가상 시설", DS.text3)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FacilityIcon(0)
                    Column(Modifier.padding(start = 16.dp)) {
                        T(DemoData.ondam.name, DS.title2)
                        T("성수동 · 한식 · 주차 8대", DS.caption, DS.text2)
                    }
                }
                Gap(18.dp)
                ListRow("예약", "${ui.bookingTime.ampm()} · ${ui.party}인")
                ListRow("현재 도착 예정", (17 * 60 + 56).ampm(), DS.green)
                ListRow("같은 식당 · 도착 시간 공유 중", "${DemoData.ondamOthers.size}팀")
                Gap(6.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NavArrow(DS.blueText, 16.dp)
                    T(if (ui.guiding) "  내비 목적지로 설정했어요" else "  안내를 시작하면 내비 목적지로 설정해요", DS.caption, DS.blueText)
                }
            }
            Card(Modifier.weight(1f).fillMaxHeight()) {
                SectionLabel("대안 추천 기준")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T("인원", DS.body1, modifier = Modifier.weight(1f))
                    Stepper("${ui.party}명", parked, { if (ui.party > 1) ui.party-- }, { if (ui.party < 8) ui.party++ })
                }
                Gap(22.dp)
                T("메뉴", DS.caption, DS.text2)
                Gap(10.dp)
                val menus = listOf("한식", "해물", "고기", "양식")
                Chips(menus, menus.indexOf(ui.menu), parked) { ui.menu = menus[it] }
                Gap(20.dp)
                T("1인 가격대", DS.caption, DS.text2)
                Gap(10.dp)
                Chips(listOf("2만 원 이하", "2~3만 원", "3만 원 이상"), ui.price, parked) { ui.price = it }
            }
            Card(Modifier.weight(1f).fillMaxHeight()) {
                val stops = listOf("정시", "±10분", "±20분", "±30분")
                SectionLabel("예약 시간 변경 허용", stops[ui.toleranceIdx])
                StepSlider(stops, ui.toleranceIdx, parked) { ui.toleranceIdx = it }
                Gap(18.dp)
                SettingRow("대안 장소 추천", null, ui.allowAlt, parked) { ui.allowAlt = it }
                SettingRow("주차 가능한 곳만", null, ui.parkingOnly, parked) { ui.parkingOnly = it }
                SettingRow("위치 · 목적지 공유", "예약한 시설에만 · 언제든 끌 수 있어요", ui.shareEta, parked) { ui.shareEta = it }
            }
        }
        Gap(16.dp)
        FlowStrip(Modifier.fillMaxWidth().weight(1f))
        Gap(18.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                T("주행 중에는 짧은 음성 응답이나 두 가지 선택만 할 수 있어요", DS.caption, DS.text2)
                T("상세 비교와 예약 전환은 차를 세운 뒤에 열려요", DS.caption, DS.text3)
            }
            if (ui.guiding) Button("안내 중 · 도착 시간 공유 중", Modifier.width(440.dp), Btn.WEAK, DS.green) {}
            else Button("안내 시작", Modifier.width(440.dp), enabled = parked && ui.shareEta) { act.startGuide() }
        }
    }
}

@Composable
private fun SettingRow(title: String, sub: String?, on: Boolean, enabled: Boolean, set: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            T(title, DS.body1)
            if (sub != null) T(sub, DS.caption, DS.text3)
        }
        Toggle(on, enabled, set)
    }
}

/** 서비스 흐름 5단계: 숫자 원 + 가는 선, 현재 단계만 파랑. */
@Composable
private fun FlowStrip(modifier: Modifier) {
    val steps = listOf(
        "안내 시작" to "예약한 시설에만 도착 시간 공유",
        "도착 시간 확인" to "정체로 늦어지는지 계속 확인",
        "같은 식당 안 조정" to "빈 시간 → 2팀 · 여러 팀 교환",
        "대안 비교" to "차를 세우면 이용 시작 시각 순",
        "예약 · 목적지 변경" to "하나라도 실패하면 그대로 유지",
    )
    Card(modifier, padding = 28.dp) {
        SectionLabel("안내를 시작하면 이렇게 도와드려요")
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Row(Modifier.fillMaxWidth()) {
                steps.forEachIndexed { i, (t, s) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxWidth().height(40.dp)) {
                                val y = size.height / 2
                                if (i > 0) drawLine(DS.surfaceHi, Offset(0f, y), Offset(size.width / 2, y), 4f)
                                if (i < steps.lastIndex) drawLine(DS.surfaceHi, Offset(size.width / 2, y), Offset(size.width, y), 4f)
                            }
                            Box(Modifier.size(40.dp).clip(CircleShape).background(if (i == 0) DS.blue else DS.surfaceHi), contentAlignment = Alignment.Center) {
                                T("${i + 1}", DS.body1, if (i == 0) Color.White else DS.text2)
                            }
                        }
                        Gap(14.dp)
                        T(t, DS.body1, align = TextAlign.Center)
                        Gap(4.dp)
                        T(s, DS.caption, DS.text3, align = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ---------------- 주행: 장면 A · B ----------------
@Composable
private fun Trip(ui: UiState, act: Actions) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        MapCard(ui, Modifier.weight(0.46f).fillMaxHeight())
        Card(Modifier.weight(0.54f).fillMaxHeight(), padding = 28.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Brand(ui, act, 34.dp)
                Spacer(Modifier.weight(1f))
                Badge(if (ui.mode == Mode.DRIVING) "주행 중 · 간단 안내" else "정차 중", if (ui.mode == Mode.DRIVING) DS.blue else DS.green)
            }
            Gap(28.dp)
            if (ui.scene == Scene.TRIP_A) TripA(ui, act) else TripB(ui, act)
        }
    }
}

@Composable
private fun MapCard(ui: UiState, modifier: Modifier) {
    val dest = when { ui.scene == Scene.TRIP_A -> DemoData.ondam.name; ui.switched -> DemoData.haesong.name; else -> DemoData.badajeong.name }
    Box(modifier.clip(DS.rCard)) {
        RouteMap(Modifier.fillMaxSize(), ui.progress, ui.scene == Scene.TRIP_A && ui.congestion, if (ui.scene == Scene.TRIP_A) 0 else 1, dest)
        Row(Modifier.padding(18.dp).clip(RoundedCornerShape(20.dp)).background(DS.blue).padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            TurnIcon(ui.scene == Scene.TRIP_A)
            Spacer(Modifier.width(16.dp))
            Column {
                val km = (ui.distanceKm * 0.32f).coerceAtLeast(0.1f)
                T(if (km < 1f) "${(km * 1000).toInt() / 100 * 100}m" else "%.1fkm".format(km), DS.title1, Color.White)
                T(if (ui.scene == Scene.TRIP_A) "우회전 · 성수대로" else "직진 · 성수대로", DS.caption, Color.White.copy(alpha = 0.85f))
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(DS.surface).padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column { T("도착 예정", DS.small, DS.text3); Rolling(ui.arrival.ampm(), DS.body1, if (ui.scene == Scene.TRIP_A && ui.arrival > ui.bookingTime) DS.orange else DS.text) }
            Spacer(Modifier.width(28.dp))
            Column { T("남은 거리", DS.small, DS.text3); T("%.1fkm".format(ui.distanceKm), DS.body1) }
            Spacer(Modifier.weight(1f))
            T(dest, DS.body2, DS.text2)
        }
        T(if (ui.navState.startsWith("NaviHelper")) "모의 교통 · 목적지는 Pleos 내비로 설정" else "모의 교통 · ${ui.navState}", DS.small, DS.text3,
            Modifier.align(Alignment.TopStart).padding(top = 124.dp, start = 20.dp).clip(RoundedCornerShape(8.dp)).background(DS.bg.copy(alpha = 0.8f)).padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

@Composable
private fun TurnIcon(right: Boolean) {
    Canvas(Modifier.size(44.dp)) {
        val w = size.width
        val p = Path().apply {
            if (right) { moveTo(w * 0.25f, w); lineTo(w * 0.25f, w * 0.45f); quadraticBezierTo(w * 0.25f, w * 0.3f, w * 0.42f, w * 0.3f); lineTo(w * 0.78f, w * 0.3f) }
            else { moveTo(w * 0.5f, w); lineTo(w * 0.5f, w * 0.12f) }
        }
        drawPath(p, Color.White, style = Stroke(w * 0.12f, cap = StrokeCap.Round))
        val head = if (right) Path().apply { moveTo(w * 0.64f, w * 0.12f); lineTo(w * 0.9f, w * 0.3f); lineTo(w * 0.64f, w * 0.48f) }
        else Path().apply { moveTo(w * 0.3f, w * 0.3f); lineTo(w * 0.5f, w * 0.08f); lineTo(w * 0.7f, w * 0.3f) }
        drawPath(head, Color.White, style = Stroke(w * 0.12f, cap = StrokeCap.Round))
    }
}

@Composable
private fun PlaceHeader(kind: Int, name: String, sub: String, badge: String, badgeColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FacilityIcon(kind)
        Column(Modifier.padding(start = 16.dp).weight(1f)) {
            T(name, DS.title2)
            T(sub, DS.caption, DS.text2)
        }
        Badge(badge, badgeColor)
    }
}

/** 큰 시각 두 개(라벨 + 숫자). 칸 배경 없이 숫자 크기로 위계를 만든다. */
@Composable
private fun TwoTimes(l1: String, v1: String, c1: Color, l2: String, v2: String, c2: Color) {
    Row {
        Column(Modifier.weight(1f)) { T(l1, DS.body2, DS.text2); Rolling(v1, DS.display, c1) }
        Column(Modifier.weight(1f)) { T(l2, DS.body2, DS.text2); Rolling(v2, DS.display, c2) }
    }
}

@Composable
private fun ColumnScope.TripA(ui: UiState, act: Actions) {
    val late = ui.arrival > ui.bookingTime
    PlaceHeader(0, DemoData.ondam.name, "${ui.party}인 · 예약",
        when { ui.alert == Alert.DONE -> "${ui.bookingTime.clock()} 확정"; late -> "${ui.arrival - ui.bookingTime}분 늦을 예정"; else -> "제시간 도착" },
        when { ui.alert == Alert.DONE -> DS.green; late -> DS.orange; else -> DS.green })
    Gap(28.dp)
    TwoTimes("예약 시간", ui.bookingTime.clock(), DS.text, "도착 예정", ui.arrival.clock(), if (late) DS.orange else DS.green)
    Gap(24.dp)
    Timeline(ui.bookingTime, ui.arrival, late)
    Gap(24.dp)
    Box(Modifier.weight(1f).fillMaxWidth()) {
        AnimatedContent(
            targetState = ui.alert,
            transitionSpec = { (fadeIn(tween(300)) + slideInVertically(tween(380)) { it / 8 }) togetherWith fadeOut(tween(150)) },
            label = "alert", modifier = Modifier.fillMaxSize(),
        ) { a ->
            when (a) {
                Alert.NONE -> Calm(ui)
                Alert.ASK -> Ask(ui, act)
                Alert.WORKING -> Working(ui)
                Alert.DONE -> Done(ui)
                Alert.KEPT -> StateBlock(DS.orange) { T("예약은 그대로 두었어요", DS.title3); Gap(6.dp); T("식당에 늦는다고 알리고 도착 시간을 계속 공유해요", DS.body2, DS.text2) }
            }
        }
    }
}

@Composable
private fun StateBlock(color: Color, content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.10f)).padding(26.dp), content = content)

/** 5:50~6:30 축. 파란 막대 = 예약, 점 = 도착 예정. */
@Composable
private fun Timeline(booking: Minute, eta: Minute, late: Boolean) {
    val start = 17 * 60 + 50
    val b by animateFloatAsState((booking - start) / 40f, tween(700, easing = FastOutSlowInEasing), label = "b")
    val e by animateFloatAsState((eta - start) / 40f, tween(900, easing = FastOutSlowInEasing), label = "e")
    val dot by animateColorAsState(if (late) DS.orange else DS.green, tween(400), label = "dot")
    Column {
        Canvas(Modifier.fillMaxWidth().height(30.dp)) {
            val y = size.height / 2
            drawLine(DS.surfaceHi, Offset(0f, y), Offset(size.width, y), 8f, StrokeCap.Round)
            val bx = size.width * b
            val ex = size.width * e
            if (late) drawLine(dot, Offset(bx, y), Offset(ex, y), 8f, StrokeCap.Round)
            drawRoundRect(DS.blue, Offset(bx - 4f, y - 22f), Size(8f, 44f), CornerRadius(4f))
            drawCircle(Color.Black.copy(alpha = 0.3f), 18f, Offset(ex, y + 2f))
            drawCircle(Color.White, 16f, Offset(ex, y))
            drawCircle(dot, 10f, Offset(ex, y))
        }
        Gap(6.dp)
        Row { listOf("5:50", "6:00", "6:10", "6:20", "6:30").forEachIndexed { i, s -> T(s, DS.small, DS.text3, Modifier.weight(1f), align = when (i) { 0 -> TextAlign.Start; 4 -> TextAlign.End; else -> TextAlign.Center }) } }
    }
}

@Composable
private fun Calm(ui: UiState) = StateBlock(DS.green) {
    T("예약 시간에 맞춰 가고 있어요", DS.title3)
    Gap(6.dp)
    T("늦어질 것 같으면 같은 식당 안에서 먼저 시간을 맞춰볼게요", DS.body2, DS.text2)
    Spacer(Modifier.weight(1f))
    Row(verticalAlignment = Alignment.Bottom) {
        Rolling("${(ui.bookingTime - ui.arrival).coerceAtLeast(0)}분", DS.display, DS.green)
        T("  여유 있어요", DS.title3, DS.text2, Modifier.padding(bottom = 10.dp))
    }
}

@Composable
private fun Ask(ui: UiState, act: Actions) {
    val plan = ui.plan
    val newT = plan?.moves?.firstOrNull()?.to ?: (18 * 60 + 15)
    StateBlock(DS.orange) {
        T("${ui.arrival - ui.bookingTime}분 늦을 것 같아요", DS.title2)
        Gap(2.dp)
        T("늦는 손님 · 일찍 온 손님과 시간을 맞바꾸면 모두 덜 기다려요", DS.body2, DS.text2)
        Gap(14.dp)
        ChainBoard(ui, Modifier.fillMaxWidth().weight(1f), progress = List(plan?.moves?.size ?: 0) { 0f }, preview = true)
        Gap(10.dp)
        if (plan != null) Deltas(plan)
        Gap(6.dp)
        T("${(plan?.checked ?: 0) + (plan?.explored ?: 0)}가지 경우를 계산해 지각·대기가 가장 적은 ${plan?.kind ?: ""}을 골랐어요", DS.caption, DS.text3)
        Gap(14.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button("${newT.clock()}로 바꾸기", Modifier.weight(1.4f)) { act.answer(true) }
            Button("그대로 둘게요", Modifier.weight(1f), Btn.SECONDARY) { act.answer(false) }
        }
    }
}

/** 지각 팀 · 대기 합계 · 빈 테이블 시간의 전후 비교. */
@Composable
private fun Deltas(plan: Plan, big: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Delta(Modifier.weight(1f), "늦는 팀", "${plan.before.lateTeams}팀", "${plan.after.lateTeams}팀", big)
        Delta(Modifier.weight(1f), "모두의 대기", "${plan.before.waitMin}분", "${plan.after.waitMin}분", big)
        Delta(Modifier.weight(1f), "빈 테이블", "${plan.before.lateMin}분", "${plan.after.lateMin}분", big)
    }
}

@Composable
private fun Delta(modifier: Modifier, label: String, before: String, after: String, big: Boolean) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0x33000000)).padding(horizontal = 16.dp, vertical = if (big) 14.dp else 10.dp)) {
        T(label, DS.caption, DS.text2)
        Row(verticalAlignment = Alignment.Bottom) {
            T(before, DS.body1, DS.text3, maxLines = 1)
            T(" → ", DS.body2, DS.text3)
            T(after, if (big) DS.title2 else DS.title3, DS.green, maxLines = 1)
        }
    }
}

/**
 * 식당 한 곳의 시간대(5:45~6:45)를 가로로 놓고, 각 예약 팀을 토큰으로 그린다.
 * 토큰 아래는 공유된 도착 예정(늦음=빨강, 일찍=회색 대기, 맞음=초록).
 * 이동은 위로 휘는 화살표(사슬)로 그리고, progress에 따라 토큰이 새 자리로 미끄러진다.
 */
@Composable
private fun ChainBoard(ui: UiState, modifier: Modifier, progress: List<Float>, preview: Boolean) {
    val me = DemoData.myBooking
    val all = DemoData.ondamOthers + me
    val moves = ui.plan?.moves.orEmpty()
    val slots = DemoData.ondam.slots
    val arrival = DemoData.ondamOthers.associate { it.id to (it.eta ?: it.time) } + (me.id to ui.arrival)
    val anim = progress.mapIndexed { i, p -> animateFloatAsState(p, tween(900, easing = FastOutSlowInEasing), label = "mv$i").value }
    val inf = androidx.compose.animation.core.rememberInfiniteTransition(label = "chain")
    val flow by inf.animateFloat(0f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(1200, easing = androidx.compose.animation.core.LinearEasing)), label = "flow")
    Canvas(modifier) {
        val n = slots.size
        val pad = 40f
        val sx = { t: Int -> pad + (size.width - 2 * pad) * slots.indexOf(t) / (n - 1) }
        val baseY = size.height * 0.78f
        val tokY = size.height * 0.46f
        val r = minOf(30f, size.height * 0.13f)
        val txt = android.graphics.Paint().apply { isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        // 시간대 칸.
        slots.forEach { t ->
            val x = sx(t)
            drawRoundRect(DS.surfaceHi, Offset(x - 56f, baseY - 6f), Size(112f, 40f), CornerRadius(12f))
            txt.color = DS.text2.toArgb(); txt.textSize = 24f
            drawContext.canvas.nativeCanvas.drawText(t.clock(), x, baseY + 22f, txt)
        }
        // 사슬 화살표.
        moves.forEachIndexed { i, m ->
            val x0 = sx(m.from); val x1 = sx(m.to)
            val lift = size.height * (0.30f + 0.06f * (i % 2))
            val path = Path().apply { moveTo(x0, tokY - r - 4f); quadraticBezierTo((x0 + x1) / 2, tokY - r - lift, x1, tokY - r - 4f) }
            val c = if (i == 0) DS.blue else Color(0xFF8B7BFF)
            drawPath(path, c.copy(alpha = 0.25f), style = Stroke(10f, cap = StrokeCap.Round))
            drawPath(path, c, style = Stroke(4f, cap = StrokeCap.Round, pathEffect = if (preview) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 12f), -flow * 26f) else null))
            // 화살촉.
            val dir = if (x1 > x0) 1f else -1f
            val hx = x1; val hy = tokY - r - 4f
            drawPath(Path().apply { moveTo(hx, hy + 2f); lineTo(hx - 12f * dir, hy - 14f); lineTo(hx + 6f * dir, hy - 12f); close() }, c)
        }
        // 토큰.
        all.forEach { b ->
            val mi = moves.indexOfFirst { it.bookingId == b.id }
            val p = if (mi >= 0) anim.getOrElse(mi) { 0f } else 0f
            val from = sx(b.time)
            val to = if (mi >= 0) sx(moves[mi].to) else from
            val x = from + (to - from) * p
            val y = tokY - kotlin.math.sin(Math.PI * p).toFloat() * size.height * 0.22f
            val slot = if (p > 0.5f && mi >= 0) moves[mi].to else b.time
            val arr = arrival.getValue(b.id)
            val state = when { arr > slot -> DS.red; slot - arr > 10 -> DS.text3; else -> DS.green }
            val fill = when { b.id == "me" -> DS.blue; mi >= 0 -> Color(0xFF6E5CF0); else -> Color(0xFF3A3A44) }
            drawCircle(Color.Black.copy(alpha = 0.3f), r + 2f, Offset(x, y + 3f))
            drawCircle(fill, r, Offset(x, y))
            txt.color = android.graphics.Color.WHITE; txt.textSize = r * 0.8f
            drawContext.canvas.nativeCanvas.drawText(if (b.id == "me") "나" else b.owner.takeLast(1), x, y + r * 0.28f, txt)
            // 도착 예정 꼬리표.
            val label = when { arr > slot -> "${arr - slot}분 늦음"; slot - arr > 10 -> "${slot - arr}분 대기"; else -> "도착 ${arr.clock()}" }
            txt.textSize = 20f; txt.color = state.toArgb()
            drawContext.canvas.nativeCanvas.drawText(label, x, y + r + 26f, txt)
        }
    }
}

@Composable
private fun Working(ui: UiState) {
    val moves = ui.plan?.moves.orEmpty()
    val biz = moves.size in ui.consented
    StateBlock(DS.blue) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spinner(Modifier.size(26.dp), DS.blueText)
            T("  ${moves.size}팀이 함께 시간을 맞바꾸는 중", DS.title3)
        }
        Gap(4.dp)
        T("동의한 팀부터 새 자리로 옮겨요 · 각자 정해둔 변경 허용 범위 안에서만", DS.body2, DS.text2)
        Gap(10.dp)
        ChainBoard(ui, Modifier.fillMaxWidth().weight(1f), progress = moves.indices.map { if (it in ui.consented) 1f else 0f }, preview = false)
        Gap(8.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            moves.forEachIndexed { i, m ->
                val ok = i in ui.consented
                Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (ok) DS.weak(DS.green) else DS.surfaceHi).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (ok) CheckCircle(DS.green, 18.dp) else Spinner(Modifier.size(16.dp), DS.text3)
                    T("  ${m.owner} 동의", DS.caption.copy(fontWeight = FontWeight.Bold), if (ok) DS.green else DS.text2)
                }
            }
        }
        Gap(10.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (biz) CheckCircle(DS.green, 22.dp) else Spinner(Modifier.size(22.dp), DS.text3)
            T("  식당 승인 · 도착 시간을 다시 확인한 뒤 한 번에 확정", DS.body2, if (biz) DS.green else DS.text2)
        }
    }
}

@Composable
private fun Done(ui: UiState) {
    val s = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) { s.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f)) }
    val plan = ui.plan
    StateBlock(DS.green) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.scale(s.value)) { CheckCircle(DS.green, 48.dp) }
            Spacer(Modifier.width(14.dp))
            Column {
                T("예약을 ${ui.bookingTime.clock()}로 바꿨어요 · 아무도 늦지 않아요", DS.title3)
                T("${plan?.kind ?: ""} · ${plan?.moves?.size ?: 0}팀과 식당이 모두 동의 · 도착 ${ui.arrival.clock()} · ${ui.bookingTime - ui.arrival}분 여유", DS.body2, DS.text2)
            }
        }
        Gap(10.dp)
        ChainBoard(ui, Modifier.fillMaxWidth().weight(1f), progress = List(plan?.moves?.size ?: 0) { 1f }, preview = false)
        Gap(8.dp)
        if (plan != null) Deltas(plan, big = true)
    }
}

@Composable
private fun Fact(modifier: Modifier, k: String, v: String, color: Color) {
    Column(modifier) { T(k, DS.caption, DS.text2); T(v, DS.title1, color) }
}

@Composable
private fun ColumnScope.TripB(ui: UiState, act: Actions) {
    val f = if (ui.switched) DemoData.haesong else DemoData.badajeong
    PlaceHeader(1, f.name, if (ui.switched) "${ui.party}인 · 예약" else "${ui.party}인 · 현장 대기",
        if (ui.switched) "${ui.bookingTime.clock()} 확정" else "만석 · 대기 길어요", if (ui.switched) DS.green else DS.red)
    Gap(28.dp)
    if (ui.switched) TwoTimes("도착 예정", ui.arrival.clock(), DS.text, "예약 시간", ui.bookingTime.clock(), DS.green)
    else TwoTimes("도착까지", "${ui.remainMin}분", DS.text, "도착 후 대기", "${f.waitMin}분", DS.red)
    Gap(8.dp)
    T(if (ui.switched) "예약을 확정해서 기다리지 않고 들어가요" else "대기 정보 ${f.waitUpdatedMinAgo}분 전 · 모의 데이터", DS.caption, DS.text3)
    Gap(24.dp)
    StateBlock(if (ui.switched) DS.green else DS.surfaceHi) {
        T("예상 이용 시작", DS.body2, DS.text2)
        val startIn = if (ui.switched) 23 else ui.remainMin + (f.waitMin ?: 0)
        Row(verticalAlignment = Alignment.Bottom) {
            Rolling("${startIn}분 뒤", DS.display, if (ui.switched) DS.green else DS.text)
            T("  " + (ui.clock.toInt() + startIn).ampm(), DS.title3, DS.text2, Modifier.padding(bottom = 10.dp))
        }
        Spacer(Modifier.weight(1f))
        if (ui.switched) {
            StartBar("바다정", 10, 50, 60, DS.text2)
            Gap(10.dp)
            StartBar("해송", 18, 5, 23, DS.green)
            Gap(14.dp)
            T("원래 가던 곳보다 약 37분 빨리 시작해요 · 가정한 수치예요", DS.caption, DS.text2)
        } else {
            StartBar("이동 · 대기", ui.remainMin, f.waitMin ?: 0, ui.remainMin + (f.waitMin ?: 0), DS.text)
            Gap(18.dp)
            if (ui.mode == Mode.PARKED) Button("더 빨리 앉을 수 있는 곳 보기", Modifier.fillMaxWidth()) { act.compare() }
            else T("차를 세우면 더 빨리 앉을 수 있는 곳을 비교해 드릴게요", DS.body2, DS.text2)
        }
    }
}

/** 이용 시작 분해 막대: 이동(파랑) + 대기(주황). */
@Composable
private fun StartBar(label: String, travel: Int, wait: Int, total: Int, valueColor: Color, max: Float = 64f) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        T(label, DS.caption, DS.text2, Modifier.width(92.dp))
        Canvas(Modifier.weight(1f).height(16.dp)) {
            drawRoundRect(DS.surfaceHi, cornerRadius = CornerRadius(size.height / 2))
            val u = size.width / max * grow.value
            val r = CornerRadius(size.height / 2)
            drawRoundRect(Wait, Offset.Zero, Size((travel + wait) * u, size.height), r)
            drawRoundRect(DS.blue, Offset.Zero, Size(travel * u, size.height), r)
        }
        T("${total}분", DS.body1, valueColor, Modifier.width(72.dp), align = TextAlign.End)
    }
}

// ---------------- 장면 B: 이용 시작 시각 비교 ----------------
@Composable
private fun Compare(ui: UiState, act: Actions) {
    val ranked = remember { StartTimeRanker.rank(DemoData.candidates, DemoData.preference) }
    val best = remember { StartTimeRanker.best(ranked) }
    val current = ranked.first { it.current }
    val rows = listOf(current) + ranked.filter { !it.current }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(Modifier.weight(0.62f).fillMaxHeight(), padding = 28.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Brand(ui, act, 34.dp)
                Spacer(Modifier.weight(1f))
                Badge("정차 중 · 자세히 비교", DS.green)
            }
            Gap(24.dp)
            T("더 빨리 앉을 수 있는 곳", DS.title1)
            Gap(4.dp)
            T("이용 시작 = 이동 + 주차·걷기 + 도착 후 대기 · 4인 · 해물찜 · 1인 2~3만 원 · 주차 필요", DS.body2, DS.text2)
            Gap(10.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Legend(DS.blue, "이동"); Legend(DS.text3, "주차·걷기"); Legend(Wait, "대기")
            }
            Gap(14.dp)
            rows.forEachIndexed { i, c ->
                CompareRow(c, i, c == best, ui.selected == c.facility.id, Modifier.weight(1f)) { if (c.known && !c.current) act.select(c.facility.id) }
                if (i < rows.lastIndex) Gap(8.dp)
            }
            Gap(10.dp)
            T("예시 수치는 계산을 보여주기 위한 가정이에요 · 실시간 대기 정보가 없는 곳은 추천에서 빼요", DS.small, DS.text3)
        }
        Detail(ui, act, current, Modifier.weight(0.38f).fillMaxHeight())
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Dot(color, 10.dp)
    T(" $text    ", DS.caption, DS.text2)
}

@Composable
private fun CompareRow(c: Candidate, index: Int, best: Boolean, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(120L + index * 180L); grow.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val bg by animateColorAsState(if (selected) DS.weak(DS.blue) else Color.Transparent, tween(250), label = "row")
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bg).press(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(c.facility.name, DS.title3)
            Spacer(Modifier.width(10.dp))
            when {
                c.current -> Badge("가던 곳", DS.text2)
                best -> Badge("가장 빨라요", DS.green)
                !c.known -> Badge("대기 정보 없음", DS.orange)
            }
            Spacer(Modifier.weight(1f))
            val color = when { c.current -> DS.red; best -> DS.green; else -> DS.text2 }
            T(if (c.known) "${c.startIn}분 뒤" else "${c.startIn}분+", DS.title2, color)
        }
        Gap(4.dp)
        T("이동 ${c.travelMin}분 · 주차·걷기 ${c.parkWalkMin}분 · 대기 ${c.waitMin?.let { "${it}분" } ?: "알 수 없음"}" +
            " · ${if (c.facility.parkingSpaces > 0) "주차 ${c.facility.parkingSpaces}대" else "주차 불가"} · 평가 ${c.facility.rating}", DS.caption, DS.text3)
        Gap(12.dp)
        Canvas(Modifier.fillMaxWidth().height(14.dp)) {
            val u = size.width / 72f * grow.value
            val r = CornerRadius(size.height / 2)
            drawRoundRect(DS.surfaceHi, cornerRadius = r)
            val total = c.travelMin + c.parkWalkMin + (c.waitMin ?: 0)
            if (c.waitMin != null) drawRoundRect(Wait, Offset.Zero, Size(total * u, size.height), r)
            else { var k = (c.travelMin + c.parkWalkMin) * u; while (k < (c.travelMin + c.parkWalkMin + 26) * u) { drawRoundRect(Wait.copy(alpha = 0.5f), Offset(k, 0f), Size(u * 0.5f, size.height), CornerRadius(3f)); k += u * 1.2f } }
            drawRoundRect(DS.text3, Offset.Zero, Size((c.travelMin + c.parkWalkMin) * u, size.height), r)
            drawRoundRect(DS.blue, Offset.Zero, Size(c.travelMin * u, size.height), r)
        }
    }
}

@Composable
private fun Detail(ui: UiState, act: Actions, current: Candidate, modifier: Modifier) {
    val sel = DemoData.candidates.firstOrNull { it.facility.id == ui.selected }
    Card(modifier, padding = 28.dp) {
        AnimatedContent(targetState = sel, transitionSpec = { (fadeIn(tween(300)) + slideInHorizontally(tween(320)) { it / 12 }) togetherWith fadeOut(tween(120)) }, label = "detail", modifier = Modifier.fillMaxSize()) { c ->
            if (c == null) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.size(72.dp).clip(CircleShape).background(DS.surfaceHi), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(32.dp)) {
                            drawCircle(DS.text2, size.width * 0.34f, Offset(size.width * 0.42f, size.height * 0.42f), style = Stroke(size.width * 0.09f))
                            drawLine(DS.text2, Offset(size.width * 0.68f, size.height * 0.68f), Offset(size.width * 0.92f, size.height * 0.92f), size.width * 0.1f, StrokeCap.Round)
                        }
                    }
                    Gap(18.dp)
                    T("왼쪽에서 장소를 고르면", DS.title3, align = TextAlign.Center)
                    T("예약 조건을 보여드려요", DS.title3, align = TextAlign.Center)
                    Gap(8.dp)
                    T("메뉴 · 가격대 · 인원 · 주차 조건을 모두 맞춘 곳만 보여요", DS.caption, DS.text3, align = TextAlign.Center)
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    T("가상 시설", DS.small, DS.text3)
                    T(c.facility.name, DS.title1)
                    T("${c.facility.menu} · 1인 %.1f만 원 · 평가 ${c.facility.rating}".format(c.facility.pricePerPerson / 10_000f), DS.body2, DS.text2)
                    Gap(18.dp)
                    ListRow("입장 예약", "${c.reservableAt?.ampm() ?: "확인 필요"} · ${ui.party}인")
                    ListRow("더 가는 거리", "+${c.travelMin - current.travelMin}분 · %.1fkm".format(c.distanceKm - current.distanceKm))
                    ListRow("지금 대기", "취소 · 수수료 없음")
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.Bottom) {
                        T("${current.startIn - c.startIn}분", DS.display, DS.green)
                        T("  더 빨리 앉아요", DS.title3, DS.text2, Modifier.padding(bottom = 10.dp))
                    }
                    Gap(12.dp)
                    StartBar("가던 곳", current.travelMin + current.parkWalkMin, current.waitMin ?: 0, current.startIn, DS.text2)
                    Gap(8.dp)
                    StartBar(c.facility.name.take(2), c.travelMin + c.parkWalkMin, c.waitMin ?: 0, c.startIn, DS.green)
                    Gap(22.dp)
                    Button("예약하고 길 안내 바꾸기", Modifier.fillMaxWidth()) { act.confirm() }
                }
            }
        }
    }
}

@Composable
private fun ConfirmModal(ui: UiState, act: Actions) {
    val labels = listOf("예약할 수 있는지 다시 확인", "새 예약 확정", "지금 대기 취소 · 수수료 확인", "내비 목적지 변경")
    val doneLabels = listOf("가능", "확정", "취소됨", "변경됨")
    Box(Modifier.fillMaxSize().background(Color(0xB3000000)).clickable(remember { MutableInteractionSource() }, null) {}, contentAlignment = Alignment.Center) {
        val s = remember { Animatable(0.94f) }
        LaunchedEffect(Unit) { s.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 400f)) }
        Column(Modifier.width(700.dp).scale(s.value).clip(RoundedCornerShape(28.dp)).background(DS.surface).padding(32.dp)) {
            val allDone = ui.flowResult == "완료"
            val failed = ui.steps.any { it == StepState.FAIL }
            T("해송 해물찜 · ${ui.party}인 · 오후 6:25", DS.caption, DS.text2)
            Gap(4.dp)
            T(when { allDone -> "예약과 목적지를 바꿨어요"; failed -> "바꾸지 않고 그대로 뒀어요"; else -> "예약과 목적지를 바꾸고 있어요" }, DS.title1)
            Gap(20.dp)
            labels.forEachIndexed { i, l ->
                val st = ui.steps.getOrNull(i) ?: StepState.WAIT
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                        when (st) {
                            StepState.RUN -> Spinner(Modifier.size(30.dp), DS.blueText)
                            StepState.DONE -> { val p = remember { Animatable(0.4f) }; LaunchedEffect(Unit) { p.animateTo(1f, spring(dampingRatio = 0.5f)) }; Box(Modifier.scale(p.value)) { CheckCircle(DS.green, 30.dp) } }
                            StepState.FAIL -> Box(Modifier.size(30.dp).clip(CircleShape).background(DS.red), contentAlignment = Alignment.Center) { T("!", DS.body1, Color.White) }
                            StepState.ROLLED_BACK -> Box(Modifier.size(30.dp).clip(CircleShape).background(DS.weak(DS.orange)), contentAlignment = Alignment.Center) { T("↺", DS.body2, DS.orange) }
                            else -> Box(Modifier.size(30.dp).clip(CircleShape).background(DS.surfaceHi))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    T(l, DS.body1, if (st == StepState.WAIT) DS.text3 else DS.text, Modifier.weight(1f))
                    T(when (st) { StepState.RUN -> "진행 중"; StepState.DONE -> doneLabels[i]; StepState.FAIL -> "실패"; StepState.ROLLED_BACK -> "되돌림"; else -> "" },
                        DS.caption.copy(fontWeight = FontWeight.Bold), when (st) { StepState.RUN -> DS.blueText; StepState.DONE -> DS.green; StepState.FAIL -> DS.red; else -> DS.orange })
                }
            }
            Gap(16.dp)
            T(if (failed) ui.flowResult else "하나라도 실패하면 자동으로 멈추고 지금 예약과 목적지를 그대로 둬요", DS.caption, if (failed) DS.orange else DS.text3)
            if (failed) { Gap(16.dp); Button("닫기", Modifier.fillMaxWidth(), Btn.SECONDARY) { act.closeConfirm() } }
        }
    }
}

// ---------------- 장면 D: 방문 후 평가 ----------------
@Composable
private fun Visit(ui: UiState, act: Actions) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(Modifier.weight(0.4f).fillMaxHeight(), padding = 28.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Brand(ui, act, 34.dp)
                Spacer(Modifier.weight(1f))
                Badge("정차 중", DS.green)
            }
            Gap(28.dp)
            T("방문 완료", DS.caption, DS.text2)
            T(DemoData.haesong.name, DS.title1)
            T("오늘 · ${ui.party}인 · 오후 6:24 입장", DS.body2, DS.text2)
            Spacer(Modifier.weight(1f))
            Stat("이용 시작", "6:24", "예상 6:25 · 1분 차이", DS.text)
            Spacer(Modifier.weight(1f))
            Stat("실제 대기", "4분", "예상 5분", DS.text)
            Spacer(Modifier.weight(1f))
            Stat("원래 가던 곳보다", "약 37분", "빨리 앉았어요 · 가정한 수치", DS.green)
            Spacer(Modifier.weight(1f))
        }
        Card(Modifier.weight(0.6f).fillMaxHeight(), padding = 32.dp) {
            T("30초면 끝나요 · 다음 추천에 반영돼요", DS.caption, DS.text2)
            Gap(4.dp)
            T("오늘 식사, 어떠셨나요?", DS.title1)
            Spacer(Modifier.weight(1f))
            RateRow("음식") { Stars("음식", ui.food) { ui.food = it } }
            Spacer(Modifier.weight(0.6f))
            RateRow("대기 시간 예측") { Segmented(listOf("정확해요", "조금 달라요", "많이 달라요"), ui.waitAcc) { ui.waitAcc = it } }
            Spacer(Modifier.weight(0.6f))
            RateRow("주차") { Stars("주차", ui.parking) { ui.parking = it } }
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(ui.sent, enter = fadeIn() + slideInVertically { it / 2 }) {
                T("다음 검색부터 대기 예측과 주차 편의를 추천 순서에 반영해요", DS.body2, DS.text2, Modifier.padding(bottom = 16.dp))
            }
            if (ui.sent) Button("반영했어요 · 고마워요", Modifier.fillMaxWidth(), Btn.WEAK, DS.green) {}
            else Button("평가 보내기", Modifier.fillMaxWidth(), enabled = ui.food > 0 && ui.waitAcc >= 0 && ui.parking > 0) { act.send() }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, sub: String, color: Color) {
    Column {
        T(label, DS.body2, DS.text2)
        Row(verticalAlignment = Alignment.Bottom) {
            T(value, DS.display, color)
            T("  $sub", DS.caption, DS.text3, Modifier.padding(bottom = 10.dp))
        }
    }
}

@Composable
private fun RateRow(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        T(label, DS.title3, modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
private fun Stars(name: String, n: Int, set: (Int) -> Unit) {
    Row {
        (1..5).forEach { i ->
            val on = i <= n
            val s by animateFloatAsState(if (on) 1.12f else 1f, spring(dampingRatio = 0.4f), label = "s$i")
            val c by animateColorAsState(if (on) Color(0xFFFFC342) else DS.surfaceHi, tween(200), label = "c$i")
            Canvas(Modifier.padding(horizontal = 4.dp).size(46.dp).scale(s).semantics { contentDescription = "$name ${i}점" }.press { set(i) }) {
                val w = size.width
                val p = Path()
                for (k in 0 until 10) {
                    val r = if (k % 2 == 0) w * 0.5f else w * 0.22f
                    val a = Math.toRadians(-90.0 + k * 36.0)
                    val x = w / 2 + r * kotlin.math.cos(a).toFloat()
                    val y = w / 2 + r * kotlin.math.sin(a).toFloat()
                    if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
                drawPath(p, c)
            }
        }
    }
}

@Composable
private fun Segmented(items: List<String>, sel: Int, set: (Int) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(14.dp)).background(DS.surfaceHi).padding(4.dp)) {
        items.forEachIndexed { i, s ->
            val on = i == sel
            val bg by animateColorAsState(if (on) Color(0xFF45454F) else Color.Transparent, tween(200), label = "seg$i")
            T(s, DS.body2.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium), if (on) DS.text else DS.text2,
                Modifier.clip(RoundedCornerShape(11.dp)).background(bg).press { set(i) }.padding(horizontal = 22.dp, vertical = 12.dp))
        }
    }
}

// ---------------- 오버레이 ----------------
@Composable
private fun ToastPill(ui: UiState, modifier: Modifier) {
    val t = ui.toast
    AnimatedVisibility(t != null, modifier, enter = fadeIn(tween(220)) + slideInVertically(tween(320)) { -it }, exit = fadeOut(tween(220)) + slideOutVertically(tween(280)) { -it }) {
        val (kind, text) = t ?: (Toast.OK to "")
        Row(Modifier.clip(CircleShape).background(Color(0xFF3A3A44)).padding(start = 10.dp, end = 26.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (kind == Toast.NAV) Box(Modifier.size(36.dp).clip(CircleShape).background(DS.blue), contentAlignment = Alignment.Center) { NavArrow(Color.White, 15.dp) }
            else CheckCircle(DS.green, 36.dp)
            T("  $text", DS.body1)
        }
    }
}

@Composable
private fun VoiceBar(ui: UiState, modifier: Modifier) {
    val b = ui.bubble
    AnimatedVisibility(b != null, modifier, enter = fadeIn(tween(220)) + slideInVertically(tween(340)) { it / 2 }, exit = fadeOut(tween(250))) {
        val bub = b ?: Bubble("", false)
        Row(
            Modifier.widthIn(max = 520.dp).clip(RoundedCornerShape(32.dp)).background(Color(0xF22C2C35)).padding(start = 12.dp, end = 22.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VoiceOrb(Modifier.size(52.dp), active = true)
            Column(Modifier.weight(1f, fill = false).padding(start = 14.dp)) {
                T(if (bub.user) "내 대답" else "SLOT CHAIN", DS.small.copy(fontWeight = FontWeight.Bold), if (bub.user) DS.blueText else DS.text3)
                T("“${bub.text}”", DS.body1.copy(lineHeight = 28.sp), if (bub.user) DS.blueText else DS.text)
            }
            Spacer(Modifier.width(14.dp))
            Waveform(Modifier.size(86.dp, 28.dp), if (bub.user) DS.blueText else DS.text2, true)
        }
    }
}
