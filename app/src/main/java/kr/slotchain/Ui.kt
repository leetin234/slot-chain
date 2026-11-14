package kr.slotchain

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---- 디자인 토큰(시연 영상 기준) ----
private val Bg = Color(0xFF0B0E14)
private val Card = Color(0xFF161A23)
private val CardHi = Color(0xFF1E2330)
private val Line = Color(0xFF272D3B)
private val TextHi = Color(0xFFF4F6FA)
private val TextMid = Color(0xFFC3C9D6)
private val TextLo = Color(0xFF858EA3)
private val Mint = Color(0xFF2ED3A0)
private val Amber = Color(0xFFF2A33A)
private val Red = Color(0xFFF0424F)
private val Blue = Color(0xFF2F6BFF)
private val Gray = Color(0xFF6B7385)

private val Pretendard = FontFamily(
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
)

private val PurpleGrad = Brush.horizontalGradient(listOf(Color(0xFF8A7CFF), Color(0xFF6A5AF0)))

@Composable
fun App(ui: UiState, act: Actions) {
    val d = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(d.density * 1.3f, d.fontScale),
        LocalTextStyle provides TextStyle(fontFamily = Pretendard, fontWeight = FontWeight.Medium, color = TextHi),
    ) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF10141E), Bg)))) {
            Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 16.dp)) {
                AnimatedContent(
                    targetState = ui.scene,
                    transitionSpec = { (fadeIn(tween(380)) + slideInHorizontally(tween(420)) { it / 14 }) togetherWith fadeOut(tween(220)) },
                    label = "scene", modifier = Modifier.fillMaxSize(),
                ) { scene ->
                    when (scene) {
                        Scene.SETUP -> Setup(ui, act)
                        Scene.TRIP_A, Scene.TRIP_B -> Trip(ui, act)
                        Scene.COMPARE -> Compare(ui, act)
                        Scene.VISIT -> Visit(ui, act)
                    }
                }
            }
            ToastPill(ui, Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
            VoiceBar(ui, Modifier.align(if (ui.scene == Scene.TRIP_A || ui.scene == Scene.TRIP_B) Alignment.BottomStart else Alignment.BottomCenter).padding(start = 36.dp, bottom = 118.dp))
            if (ui.scene == Scene.COMPARE && ui.confirming) ConfirmModal(ui, act)
        }
    }
}

// ---------------- 공통 ----------------
@Composable
private fun AppHeader(page: String, ui: UiState, act: Actions, right: @Composable RowScope.() -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { act.scene(nextDemo(ui.scene)) }) }) { Logo(Modifier.size(40.dp)) }
        Spacer(Modifier.width(14.dp))
        Text("SLOT CHAIN", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
        Text("   ›   ", fontSize = 18.sp, color = TextLo)
        Text(page, fontSize = 19.sp, color = TextMid, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        right()
    }
}

private fun nextDemo(s: Scene) = when (s) { Scene.SETUP -> "A"; Scene.TRIP_A -> "B"; Scene.TRIP_B, Scene.COMPARE -> "D"; Scene.VISIT -> "SETUP" }

@Composable
private fun Tag(text: String, color: Color, big: Boolean = false) {
    Text(
        text, color = color, fontSize = if (big) 16.sp else 14.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).padding(horizontal = if (big) 16.dp else 12.dp, vertical = if (big) 8.dp else 6.dp),
    )
}

@Composable
private fun SmallTag(text: String, bg: Color, fg: Color) {
    Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp))
}

@Composable
private fun Panel(modifier: Modifier = Modifier, padding: Dp = 24.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(24.dp)).padding(padding), content = content)
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) = Text(text, fontSize = 16.sp, color = TextMid, fontWeight = FontWeight.SemiBold, modifier = modifier.padding(bottom = 10.dp))

@Composable
private fun PrimaryButton(text: String, modifier: Modifier = Modifier, done: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val bg by animateColorAsState(if (done) Mint else if (enabled) Purple else CardHi, tween(400), label = "btn")
    Box(
        modifier.clip(RoundedCornerShape(18.dp))
            .background(if (done || !enabled) Brush.linearGradient(listOf(bg, bg)) else PurpleGrad)
            .clickable(enabled = enabled && !done, onClick = onClick).padding(vertical = 22.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (done) Color(0xFF052019) else if (enabled) Color.White else TextLo) }
}

@Composable
private fun InfoIcon(color: Color = TextLo) {
    Canvas(Modifier.size(24.dp)) {
        drawCircle(color, style = Stroke(size.width * 0.08f))
        drawLine(color, Offset(size.width / 2, size.height * 0.45f), Offset(size.width / 2, size.height * 0.72f), size.width * 0.09f, StrokeCap.Round)
        drawCircle(color, size.width * 0.06f, Offset(size.width / 2, size.height * 0.3f))
    }
}

@Composable
private fun CheckDot(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        drawCircle(color)
        val p = Path().apply { moveTo(this@Canvas.size.width * 0.28f, this@Canvas.size.height * 0.52f); lineTo(this@Canvas.size.width * 0.44f, this@Canvas.size.height * 0.67f); lineTo(this@Canvas.size.width * 0.73f, this@Canvas.size.height * 0.36f) }
        drawPath(p, Color(0xFF07231A), style = Stroke(this.size.width * 0.11f, cap = StrokeCap.Round))
    }
}

@Composable
private fun FacilityIcon(kind: Int, size: Dp = 48.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(Color(0xFF2A2F55)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size * 0.5f)) {
            val w = this.size.width
            val c = Color(0xFFD9D5FF)
            if (kind == 0) { // 수저(한식)
                drawLine(c, Offset(w * 0.3f, 0f), Offset(w * 0.3f, w), w * 0.1f, StrokeCap.Round)
                drawLine(c, Offset(w * 0.18f, 0f), Offset(w * 0.18f, w * 0.35f), w * 0.07f, StrokeCap.Round)
                drawLine(c, Offset(w * 0.42f, 0f), Offset(w * 0.42f, w * 0.35f), w * 0.07f, StrokeCap.Round)
                drawOval(c, Offset(w * 0.6f, 0f), Size(w * 0.28f, w * 0.42f))
                drawLine(c, Offset(w * 0.74f, w * 0.4f), Offset(w * 0.74f, w), w * 0.1f, StrokeCap.Round)
            } else { // 물고기(해물)
                val body = Path().apply { moveTo(w * 0.05f, w * 0.5f); quadraticBezierTo(w * 0.45f, w * 0.05f, w * 0.78f, w * 0.5f); quadraticBezierTo(w * 0.45f, w * 0.95f, w * 0.05f, w * 0.5f); close() }
                drawPath(body, c, style = Stroke(w * 0.08f))
                val tail = Path().apply { moveTo(w * 0.78f, w * 0.5f); lineTo(w, w * 0.28f); lineTo(w, w * 0.72f); close() }
                drawPath(tail, c, style = Stroke(w * 0.08f))
            }
        }
    }
}

@Composable
private fun BigTime(value: String, color: Color, size: TextUnit = 64.sp) {
    AnimatedContent(
        targetState = value,
        transitionSpec = { (slideInVertically(tween(420)) { it } + fadeIn(tween(420))) togetherWith (slideOutVertically(tween(420)) { -it } + fadeOut(tween(300))) },
        label = "time",
    ) { v -> Text(v, fontSize = size, fontWeight = FontWeight.ExtraBold, color = color, letterSpacing = 0.5.sp) }
}

// ---------------- 01 출발 전 설정 ----------------
@Composable
private fun Setup(ui: UiState, act: Actions) {
    val parked = ui.mode == Mode.PARKED
    Column(Modifier.fillMaxSize()) {
        AppHeader("출발 전 설정", ui, act) {
            Row(Modifier.clip(RoundedCornerShape(50)).background(CardHi).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(22.dp).clip(RoundedCornerShape(5.dp)).background(Color.White), contentAlignment = Alignment.Center) { Text("P", color = Bg, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }
                Text("  정차 중에만 변경할 수 있어요", fontSize = 15.sp, color = TextMid)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            // 오늘 예약
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Label("오늘 예약")
                Column(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(22.dp))) {
                    Box(Modifier.fillMaxWidth().height(118.dp).background(Brush.linearGradient(listOf(Color(0xFF3B3480), Color(0xFF262A55)))).drawBehind {
                        drawCircle(Color.White.copy(alpha = 0.06f), size.height * 0.9f, Offset(size.width * 0.86f, size.height * 0.15f))
                    }) {
                        Row(Modifier.align(Alignment.CenterStart).padding(start = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                            FacilityIcon(0, 50.dp)
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(DemoData.ondam.name, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                                Text("성수동 · 한식 · 주차 8대", fontSize = 15.sp, color = TextMid)
                            }
                        }
                        Box(Modifier.align(Alignment.TopEnd).padding(14.dp)) { SmallTag("가상 시설", Color(0x33FFFFFF), TextMid) }
                    }
                    Column(Modifier.padding(22.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("예약", fontSize = 16.sp, color = TextMid, modifier = Modifier.weight(1f))
                            Text("${ui.bookingTime.ampm()} · ${ui.party}인", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("현재 ETA", fontSize = 16.sp, color = TextMid, modifier = Modifier.weight(1f))
                            Text((17 * 60 + 56).ampm(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Mint)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NavArrow(Blue, 18.dp)
                            Text(if (ui.guiding) "  내비 목적지로 설정됨 · NaviHelper" else "  안내 시작 시 내비 목적지로 설정 · NaviHelper", fontSize = 15.sp, color = Color(0xFF7FA6FF), fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(18.dp))
                        SwapExplainer()
                    }
                }
            }
            // 선호 조건
            Column(Modifier.weight(0.92f).fillMaxHeight()) {
                Label("선호 조건 · 대안 추천 기준")
                Panel(Modifier.weight(1f).fillMaxWidth(), padding = 22.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("인원", fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Stepper("−", parked) { if (ui.party > 1) ui.party-- }
                        Text("${ui.party}명", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 14.dp))
                        Stepper("+", parked) { if (ui.party < 8) ui.party++ }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("메뉴 유형", fontSize = 15.sp, color = TextMid)
                    Spacer(Modifier.height(10.dp))
                    Chips(listOf("한식", "해물", "고기", "양식"), listOf("한식", "해물", "고기", "양식").indexOf(ui.menu), parked) { ui.menu = listOf("한식", "해물", "고기", "양식")[it] }
                    Spacer(Modifier.height(20.dp))
                    Text("1인 가격대", fontSize = 15.sp, color = TextMid)
                    Spacer(Modifier.height(10.dp))
                    Chips(listOf("~2만", "2~3만", "3만~"), ui.price, parked) { ui.price = it }
                    Spacer(Modifier.height(22.dp))
                    Text("필수 조건(인원·메뉴·가격·주차)을 먼저 거른 뒤\n이용 시작이 빠른 순으로 추천해요", fontSize = 14.sp, color = TextLo, lineHeight = 20.sp)
                }
            }
            // 허용 범위 + 동의
            Column(Modifier.weight(1f).fillMaxHeight()) {
                val stops = listOf("정시", "±10분", "±20분", "±30분")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Label("예약 시간 변경 허용 범위", Modifier.weight(1f))
                    Text(stops[ui.toleranceIdx], color = PurpleHi, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
                }
                Panel(Modifier.fillMaxWidth(), padding = 22.dp) {
                    StopSlider(stops.size, ui.toleranceIdx, parked) { ui.toleranceIdx = it }
                    Spacer(Modifier.height(12.dp))
                    Row { stops.forEachIndexed { i, s -> Text(s, fontSize = 15.sp, color = if (i == ui.toleranceIdx) TextHi else TextLo, modifier = Modifier.weight(1f), textAlign = if (i == 0) TextAlign.Start else if (i == stops.lastIndex) TextAlign.End else TextAlign.Center) } }
                }
                Spacer(Modifier.height(16.dp))
                Panel(Modifier.weight(1f).fillMaxWidth(), padding = 0.dp) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp), verticalArrangement = Arrangement.Center) {
                        ToggleRow("대안 장소 추천 허용", null, ui.allowAlt, parked) { ui.allowAlt = it }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                        ToggleRow("주차 가능한 곳만", null, ui.parkingOnly, parked) { ui.parkingOnly = it }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                        ToggleRow("위치 · 목적지 연동 동의", "예약한 시설에만 ETA 공유 · 언제든 해제", ui.shareEta, parked) { ui.shareEta = it }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Label("안내를 시작하면 이렇게 움직여요")
        FlowStrip(Modifier.weight(1f).fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            InfoIcon()
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text("주행 중에는 상세 비교와 예약 변경 조작이 제한돼요.", fontSize = 16.sp, color = TextMid)
                Text("승인은 짧은 음성 응답 또는 정차 후 화면에서 진행해요.", fontSize = 16.sp, color = TextMid)
            }
            PrimaryButton(if (ui.guiding) "✓  안내 시작 · ETA 공유 중" else "안내 시작", Modifier.width(440.dp), done = ui.guiding, enabled = parked && ui.shareEta) { act.startGuide() }
        }
    }
}

/** 서비스 흐름 5단계. 선을 따라 빛이 흘러가며 순서를 보여 준다. */
@Composable
private fun FlowStrip(modifier: Modifier) {
    val steps = listOf(
        Triple("안내 시작", "예약 시설에만 ETA 공유", Blue),
        Triple("ETA 감시", "정체·지각 가능성 감지", Color(0xFF52B6F0)),
        Triple("같은 식당 안 조정", "빈 슬롯 → 2인·순환 교환", Purple),
        Triple("대안 비교", "정차 시 이용 시작 시각 순", Amber),
        Triple("확정 · 목적지 변경", "실패하면 기존 예약 유지", Mint),
    )
    val inf = rememberInfiniteTransition(label = "flow")
    val run by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = androidx.compose.animation.core.LinearEasing)), label = "run")
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(22.dp)).padding(horizontal = 26.dp, vertical = 22.dp), verticalArrangement = Arrangement.Center) {
        Box(Modifier.fillMaxWidth().height(64.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height / 2
                val x0 = size.width * 0.1f
                val x1 = size.width * 0.9f
                drawLine(Line, Offset(x0, y), Offset(x1, y), 6f, StrokeCap.Round)
                drawLine(Brush.horizontalGradient(steps.map { it.third }, startX = x0, endX = x1), Offset(x0, y), Offset(x1, y), 6f, StrokeCap.Round, alpha = 0.55f)
                val px = x0 + (x1 - x0) * run
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), Color.Transparent), center = Offset(px, y), radius = 40f), 40f, Offset(px, y))
                steps.forEachIndexed { i, (_, _, c) ->
                    val x = x0 + (x1 - x0) * i / (steps.size - 1)
                    val near = 1f - (kotlin.math.abs(px - x) / 120f).coerceIn(0f, 1f)
                    drawCircle(c.copy(alpha = 0.18f + 0.3f * near), 30f + 8f * near, Offset(x, y))
                    drawCircle(Color(0xFF10141C), 20f, Offset(x, y))
                    drawCircle(c, 20f, Offset(x, y), style = Stroke(5f))
                    drawCircle(c, 7f + 3f * near, Offset(x, y))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row {
            steps.forEachIndexed { i, (t, sub, c) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("0${i + 1}", fontSize = 13.sp, color = c, fontWeight = FontWeight.ExtraBold)
                    Text(t, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(sub, fontSize = 14.sp, color = TextLo, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** 예약 카드 아래: 순환 교환 개념을 한눈에. */
@Composable
private fun SwapExplainer() {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardHi).padding(16.dp)) {
        Text("늦을 것 같으면, 같은 식당의 호환 예약끼리 시간을 맞바꿔요", fontSize = 14.sp, color = TextMid, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            listOf("나 6:00→6:15", "B 6:15→6:30", "C 6:30→6:00").forEachIndexed { i, s ->
                Text(s, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (i == 0) PurpleHi else TextMid,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (i == 0) Purple.copy(alpha = 0.18f) else Color(0xFF262C3A)).padding(horizontal = 9.dp, vertical = 6.dp))
                if (i < 2) Text(" ↻ ", fontSize = 14.sp, color = TextLo)
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

@Composable
private fun Stepper(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(CardHi).border(1.dp, Line, RoundedCornerShape(12.dp)).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 22.sp, color = TextMid)
    }
}

@Composable
private fun Chips(items: List<String>, sel: Int, enabled: Boolean, set: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { i, s ->
            val on = i == sel
            Text(
                s, fontSize = 18.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = if (on) TextHi else TextLo,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) Purple.copy(alpha = 0.22f) else CardHi)
                    .border(if (on) 2.dp else 1.dp, if (on) PurpleHi else Line, RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled) { set(i) }.padding(horizontal = 16.dp, vertical = 11.dp),
            )
        }
    }
}

@Composable
private fun StopSlider(stops: Int, index: Int, enabled: Boolean, set: (Int) -> Unit) {
    val anim by animateFloatAsState(index.toFloat(), spring(dampingRatio = 0.7f), label = "slider")
    Canvas(
        Modifier.fillMaxWidth().height(40.dp)
            .pointerInput(enabled) { if (enabled) detectTapGestures { set(((it.x / size.width) * (stops - 1) + 0.5f).toInt().coerceIn(0, stops - 1)) } },
    ) {
        val y = size.height / 2
        val pad = 18f
        val x = { f: Float -> pad + (size.width - pad * 2) * f / (stops - 1) }
        drawLine(Line, Offset(pad, y), Offset(size.width - pad, y), 9f, StrokeCap.Round)
        drawLine(Brush.horizontalGradient(listOf(Color(0xFF8A7CFF), Purple)), Offset(pad, y), Offset(x(anim), y), 9f, StrokeCap.Round)
        for (i in 0 until stops) drawCircle(if (i <= index) Purple else Line, 9f, Offset(x(i.toFloat()), y))
        drawCircle(Purple.copy(alpha = 0.28f), 36f, Offset(x(anim), y))
        drawCircle(Color.White, 24f, Offset(x(anim), y))
    }
}

@Composable
private fun ToggleRow(title: String, sub: String?, on: Boolean, enabled: Boolean, set: (Boolean) -> Unit) {
    val knob by animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.7f), label = "knob")
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled) { set(!on) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            if (sub != null) Text(sub, fontSize = 14.sp, color = TextLo)
        }
        Box(Modifier.size(62.dp, 36.dp).clip(RoundedCornerShape(50)).background(if (on) Purple else Color(0xFF2B3140))) {
            Box(Modifier.offset(x = 4.dp + 26.dp * knob).align(Alignment.CenterStart).size(28.dp).clip(CircleShape).background(Color.White))
        }
    }
}

// ---------------- 주행: 장면 A(지각 조정) · 장면 B(만석) ----------------
@Composable
private fun Trip(ui: UiState, act: Actions) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        MapCard(ui, Modifier.weight(0.47f).fillMaxHeight())
        Panel(Modifier.weight(0.53f).fillMaxHeight(), padding = 26.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Logo(Modifier.size(36.dp))
                Text("  SLOT CHAIN", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                if (ui.mode == Mode.DRIVING) Tag("주행 중 · 간단 안내", Color(0xFF8FA8FF)) else Tag("정차 중", Mint)
            }
            Spacer(Modifier.height(22.dp))
            if (ui.scene == Scene.TRIP_A) TripA(ui, act) else TripB(ui, act)
        }
    }
}

@Composable
private fun MapCard(ui: UiState, modifier: Modifier) {
    val dest = when { ui.scene == Scene.TRIP_A -> DemoData.ondam.name; ui.switched -> DemoData.haesong.name; else -> DemoData.badajeong.name }
    Box(modifier.clip(RoundedCornerShape(24.dp)).border(1.dp, Line, RoundedCornerShape(24.dp))) {
        RouteMap(Modifier.fillMaxSize(), ui.progress, ui.scene == Scene.TRIP_A && ui.congestion && ui.alert != Alert.DONE || ui.scene == Scene.TRIP_A && ui.congestion, if (ui.scene == Scene.TRIP_A) 0 else 1)
        // 회전 안내.
        Row(
            Modifier.padding(18.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Color(0xFF3A7BFF), Color(0xFF2456D6)))).padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TurnIcon(ui.scene == Scene.TRIP_A)
            Spacer(Modifier.width(18.dp))
            Column {
                val turnKm = (ui.distanceKm * 0.32f).coerceAtLeast(0.1f)
                Text(if (turnKm < 1f) "${(turnKm * 1000).toInt() / 100 * 100}m" else "%.1fkm".format(turnKm), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (ui.scene == Scene.TRIP_A) "우회전 · 성수대로" else "직진 · 성수대로", fontSize = 16.sp, color = Color(0xFFDDE6FF))
            }
        }
        // 하단 도착 정보.
        Row(
            Modifier.align(Alignment.BottomCenter).padding(14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xE60E121A)).padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column { Text("도착 예정", fontSize = 13.sp, color = TextLo); BigTime(ui.arrival.ampm(), if (ui.scene == Scene.TRIP_A && ui.arrival > ui.bookingTime) Amber else TextHi, 22.sp) }
            Spacer(Modifier.width(26.dp))
            Column { Text("남은 거리", fontSize = 13.sp, color = TextLo); Text("%.1fkm".format(ui.distanceKm), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.weight(1f))
            Text(dest, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB9C7FF))
        }
        // ETA 출처(정직하게 표시).
        Text(
            if (ui.navState.startsWith("NaviHelper")) "모의 교통 시나리오 · 목적지는 NaviHelper로 설정" else "모의 교통 시나리오 · ${ui.navState}",
            fontSize = 12.sp, color = TextLo, modifier = Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = 18.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xCC0E121A)).padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun TurnIcon(right: Boolean) {
    Canvas(Modifier.size(46.dp)) {
        val w = size.width
        val p = Path().apply {
            if (right) { moveTo(w * 0.25f, w); lineTo(w * 0.25f, w * 0.45f); quadraticBezierTo(w * 0.25f, w * 0.3f, w * 0.42f, w * 0.3f); lineTo(w * 0.78f, w * 0.3f) }
            else { moveTo(w * 0.5f, w); lineTo(w * 0.5f, w * 0.12f) }
        }
        drawPath(p, Color.White, style = Stroke(w * 0.12f, cap = StrokeCap.Round))
        val head = if (right) Path().apply { moveTo(w * 0.66f, w * 0.12f); lineTo(w * 0.92f, w * 0.3f); lineTo(w * 0.66f, w * 0.48f) }
        else Path().apply { moveTo(w * 0.3f, w * 0.3f); lineTo(w * 0.5f, w * 0.08f); lineTo(w * 0.7f, w * 0.3f) }
        drawPath(head, Color.White, style = Stroke(w * 0.12f, cap = StrokeCap.Round))
    }
}

@Composable
private fun ColumnScope.TripA(ui: UiState, act: Actions) {
    val late = ui.arrival > ui.bookingTime
    Row(verticalAlignment = Alignment.CenterVertically) {
        FacilityIcon(0)
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(DemoData.ondam.name, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("${ui.party}인 · 예약", fontSize = 15.sp, color = TextLo)
        }
        when {
            ui.alert == Alert.DONE -> Tag("예약 ${ui.bookingTime.clock()} 확정", Mint, big = true)
            late -> Tag("${ui.arrival - ui.bookingTime}분 지각 예상", Amber, big = true)
            else -> Tag("제시간 도착", Mint, big = true)
        }
    }
    Spacer(Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        TimeTile(Modifier.weight(1f), "예약 시간", ui.bookingTime.clock(), TextHi)
        TimeTile(Modifier.weight(1f), "도착 예정", ui.arrival.clock(), if (late) Amber else Mint)
    }
    Spacer(Modifier.height(24.dp))
    Timeline(ui.bookingTime, ui.arrival, late)
    Spacer(Modifier.height(18.dp))
    Box(Modifier.weight(1f).fillMaxWidth()) {
        AnimatedContent(
            targetState = ui.alert,
            transitionSpec = { (fadeIn(tween(350)) + slideInVertically(tween(420)) { it / 6 }) togetherWith fadeOut(tween(200)) },
            label = "alert", modifier = Modifier.fillMaxSize(),
        ) { a ->
            when (a) {
                Alert.NONE -> CalmCard(ui)
                Alert.ASK -> AskCard(ui, act)
                Alert.WORKING -> SwapCard(ui)
                Alert.DONE -> DoneCard(ui)
                Alert.KEPT -> InfoCard("예약은 그대로 두었어요", "식당에 지각 소식을 전하고 도착 시간을 계속 공유해요", Amber)
            }
        }
    }
}

@Composable
private fun TimeTile(modifier: Modifier, label: String, value: String, color: Color, bg: Color = Color(0xFF10141C), border: Color = Line) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(bg).border(1.dp, border, RoundedCornerShape(18.dp)).padding(horizontal = 22.dp, vertical = 18.dp)) {
        Text(label, fontSize = 15.sp, color = TextMid)
        BigTime(value, color)
    }
}

/** 5:50~6:30 시간 축: 보라 막대 = 예약, 점 = 도착 예정. */
@Composable
private fun Timeline(booking: Minute, eta: Minute, late: Boolean) {
    val start = 17 * 60 + 50
    val span = 40f
    val b by animateFloatAsState((booking - start) / span, tween(700, easing = FastOutSlowInEasing), label = "b")
    val e by animateFloatAsState((eta - start) / span, tween(900, easing = FastOutSlowInEasing), label = "e")
    val dotColor by animateColorAsState(if (late) Amber else Mint, tween(500), label = "dot")
    val inf = rememberInfiniteTransition(label = "tl")
    val glow by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "g")
    Column {
        Canvas(Modifier.fillMaxWidth().height(34.dp)) {
            val y = size.height * 0.5f
            drawLine(Line, Offset(0f, y), Offset(size.width, y), 7f, StrokeCap.Round)
            val bx = size.width * b
            val ex = size.width * e
            if (late) drawLine(dotColor.copy(alpha = 0.5f), Offset(bx, y), Offset(ex, y), 7f, StrokeCap.Round)
            drawRoundRect(Purple, Offset(bx - 4f, y - 26f), Size(8f, 52f), CornerRadius(4f))
            drawCircle(dotColor.copy(alpha = 0.25f * glow), 30f, Offset(ex, y))
            drawCircle(Color.White, 17f, Offset(ex, y))
            drawCircle(dotColor, 12f, Offset(ex, y))
        }
        Spacer(Modifier.height(6.dp))
        Row { listOf("5:50", "6:00", "6:10", "6:20", "6:30").forEachIndexed { i, s -> Text(s, fontSize = 13.sp, color = TextLo, modifier = Modifier.weight(1f), textAlign = when (i) { 0 -> TextAlign.Start; 4 -> TextAlign.End; else -> TextAlign.Center }) } }
    }
}

@Composable
private fun CalmCard(ui: UiState) {
    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(Brush.verticalGradient(listOf(Mint.copy(alpha = 0.10f), Mint.copy(alpha = 0.03f)))).border(1.dp, Mint.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).padding(26.dp)) {
        Text("예약 시간에 맞춰 가고 있어요", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("도착 예정이 바뀌면 같은 식당 안에서 먼저 조정을 찾아볼게요", fontSize = 16.sp, color = TextMid)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            BigTime("${(ui.bookingTime - ui.arrival).coerceAtLeast(0)}분", Mint, 60.sp)
            Text("  여유", fontSize = 22.sp, color = TextMid, modifier = Modifier.padding(bottom = 12.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(Mint))
            Text("  온담 한식당에만 ETA 공유 중 · 변경 허용 ±20분", fontSize = 15.sp, color = TextMid)
        }
    }
}

@Composable
private fun InfoCard(title: String, sub: String, color: Color) {
    Row(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(Brush.verticalGradient(listOf(color.copy(alpha = 0.10f), color.copy(alpha = 0.03f)))).border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).padding(26.dp), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(sub, fontSize = 16.sp, color = TextMid)
        }
    }
}

@Composable
private fun AskCard(ui: UiState, act: Actions) {
    val inf = rememberInfiniteTransition(label = "ask")
    val glow by inf.animateFloat(0.35f, 0.9f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "glow")
    val newT = ui.plan?.moves?.firstOrNull()?.to ?: (18 * 60 + 15)
    Column(
        Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(Brush.verticalGradient(listOf(Color(0xFF2A2214), Color(0xFF17150F))))
            .border(1.5.dp, Amber.copy(alpha = glow), RoundedCornerShape(20.dp)).padding(26.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WarnIcon()
            Text("  ${ui.arrival - ui.bookingTime}분 늦을 것 같아요. ${newT.clock()}로 바꿀까요?", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text("허용 범위 안 · 같은 식당 슬롯 확인 · 수수료 없음", fontSize = 16.sp, color = TextMid)
        Spacer(Modifier.weight(1f))
        Text("찾은 조정안 · ${ui.plan?.kind ?: ""}", fontSize = 15.sp, color = Amber, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        MovesRow(ui.plan?.moves.orEmpty(), emptyList(), preview = true)
        Spacer(Modifier.height(8.dp))
        Text("모두 각자 동의한 변경 허용 범위 안이에요 · 승인 후 관련 고객·사업자 동의를 받아요", fontSize = 14.sp, color = TextLo)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.weight(1.3f).clip(RoundedCornerShape(16.dp)).background(PurpleGrad).clickable { act.answer(true) }.padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
                Text("${newT.clock()}로 변경 요청", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF2B2A28)).clickable { act.answer(false) }.padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
                Text("그대로 둘게요", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMid)
            }
        }
    }
}

@Composable
private fun WarnIcon() {
    Canvas(Modifier.size(26.dp)) {
        val w = size.width
        drawPath(Path().apply { moveTo(w / 2, w * 0.06f); lineTo(w * 0.98f, w * 0.92f); lineTo(w * 0.02f, w * 0.92f); close() }, Amber)
        drawLine(Color(0xFF231A08), Offset(w / 2, w * 0.38f), Offset(w / 2, w * 0.64f), w * 0.11f, StrokeCap.Round)
        drawCircle(Color(0xFF231A08), w * 0.06f, Offset(w / 2, w * 0.78f))
    }
}

/** 순환 교환 진행: 관련 고객 → 사업자 동의 → ETA 재검증 후 일괄 확정. */
@Composable
private fun SwapCard(ui: UiState) {
    val moves = ui.plan?.moves.orEmpty()
    Column(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(Color(0xFF17162A)).border(1.5.dp, Purple.copy(alpha = 0.7f), RoundedCornerShape(20.dp)).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spinner(Modifier.size(24.dp), PurpleHi)
            Text("  ${ui.plan?.kind ?: "교환"} 동의를 받는 중", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        MovesRow(moves, ui.consented, preview = false)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val biz = moves.size in ui.consented
            if (biz) CheckDot(Mint, 20.dp) else Spinner(Modifier.size(20.dp), TextLo)
            Text("  사업자 승인 · 최종 ETA 재검증 후 일괄 확정", fontSize = 16.sp, color = if (biz) Mint else TextMid, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MovesRow(moves: List<Move>, consented: List<Int>, preview: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        moves.forEachIndexed { i, m ->
            val ok = !preview && i in consented
            val s by animateFloatAsState(if (ok || preview) 1f else 0.95f, spring(dampingRatio = 0.5f), label = "c$i")
            Column(
                Modifier.weight(1f).scale(s).clip(RoundedCornerShape(14.dp)).background(if (ok) Mint.copy(alpha = 0.12f) else if (i == 0) Purple.copy(alpha = 0.16f) else Color(0xFF232836))
                    .border(1.dp, if (ok) Mint else if (i == 0) Purple.copy(alpha = 0.6f) else Line, RoundedCornerShape(14.dp)).padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.owner, fontSize = 15.sp, color = if (i == 0) PurpleHi else TextMid, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (!preview) { if (ok) CheckDot(Mint, 18.dp) else Spinner(Modifier.size(16.dp), TextLo) }
                }
                Text("${m.from.clock()} → ${m.to.clock()}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (i < moves.lastIndex) Text("↻", fontSize = 18.sp, color = TextLo)
        }
    }
}

@Composable
private fun DoneCard(ui: UiState) {
    val s = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { s.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
    Row(
        Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(Brush.verticalGradient(listOf(Color(0xFF0F2C22), Color(0xFF0E1A17)))).border(1.5.dp, Mint.copy(alpha = 0.6f), RoundedCornerShape(20.dp)).padding(26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.scale(s.value)) { CheckDot(Mint, 62.dp) }
        Spacer(Modifier.width(20.dp))
        Column {
            Text("예약이 ${ui.bookingTime.clock()}로 바뀌었어요", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text("${ui.plan?.kind ?: ""} · 고객·사업자 동의 완료 · ETA 재검증 ${ui.arrival.clock()} · ${ui.bookingTime - ui.arrival}분 여유", fontSize = 16.sp, color = TextMid)
        }
    }
}

@Composable
private fun ColumnScope.TripB(ui: UiState, act: Actions) {
    val f = if (ui.switched) DemoData.haesong else DemoData.badajeong
    Row(verticalAlignment = Alignment.CenterVertically) {
        FacilityIcon(1)
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(f.name, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text(if (ui.switched) "${ui.party}인 · 예약" else "${ui.party}인 · 현장 웨이팅", fontSize = 15.sp, color = TextLo)
        }
        if (ui.switched) Tag("예약 ${ui.bookingTime.clock()} 확정", Mint, big = true) else Tag("만석 · 긴 대기", Red, big = true)
    }
    Spacer(Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        TimeTile(Modifier.weight(1f), "도착 예정", if (ui.switched) ui.arrival.clock() else "${ui.remainMin}분 후", TextHi)
        if (ui.switched) TimeTile(Modifier.weight(1f), "예약 시간", ui.bookingTime.clock(), Mint)
        else TimeTile(Modifier.weight(1f), "도착 후 대기", "${f.waitMin}분", Red, Color(0xFF2A1418), Red.copy(alpha = 0.6f))
    }
    Spacer(Modifier.height(12.dp))
    Text(if (ui.switched) "예약 확정 · 대기 없이 입장 예정" else "대기 정보 ${f.waitUpdatedMinAgo}분 전 갱신 · 모의 웨이팅 데이터", fontSize = 14.sp, color = TextLo)
    Spacer(Modifier.height(18.dp))
    Column(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF10141C)).border(1.dp, Line, RoundedCornerShape(20.dp)).padding(26.dp)) {
        Text("예상 이용 시작", fontSize = 16.sp, color = TextMid)
        Spacer(Modifier.height(4.dp))
        val startIn = if (ui.switched) 23 else ui.remainMin + (f.waitMin ?: 0)
        Row(verticalAlignment = Alignment.Bottom) {
            BigTime("${startIn}분 후", if (ui.switched) Mint else TextHi, 60.sp)
            Text("  " + (ui.clock.toInt() + startIn).ampm(), fontSize = 20.sp, color = TextMid, modifier = Modifier.padding(bottom = 12.dp))
        }
        Spacer(Modifier.weight(1f))
        if (ui.switched) {
            Text("기존 목적지보다 약 37분 빨리 시작해요 · 가정 기준", fontSize = 17.sp, color = Mint, fontWeight = FontWeight.SemiBold)
        } else if (ui.mode == Mode.PARKED) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PurpleGrad).clickable { act.compare() }.padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                Text("더 빨리 앉을 수 있는 곳 비교하기", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(22.dp)) { drawRoundRect(TextMid, cornerRadius = CornerRadius(5f), style = Stroke(3f)); drawCircle(TextMid, 3f) }
                Text("  정차하면 더 빨리 앉을 수 있는 곳을 비교해 드릴게요", fontSize = 17.sp, color = TextMid, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------------- 장면 B: 이용 시작 시각 비교 ----------------
@Composable
private fun Compare(ui: UiState, act: Actions) {
    val ranked = remember { StartTimeRanker.rank(DemoData.candidates, DemoData.preference) }
    val best = remember { StartTimeRanker.best(ranked) }
    val rows = listOf(ranked.first { it.current }) + ranked.filter { !it.current }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Panel(Modifier.weight(0.62f).fillMaxHeight(), padding = 26.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Logo(Modifier.size(36.dp))
                Text("  이용 시작 시각 비교", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                Tag("정차 중 · 상세 비교", Mint)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf("4인", "해물찜", "1인 2~3만", "주차 필요").forEach { SmallTag(it, CardHi, TextMid); Spacer(Modifier.width(8.dp)) }
                Spacer(Modifier.weight(1f))
                Legend(Blue, "이동"); Legend(Gray, "주차·도보"); Legend(Color(0xFFF08A3C), "대기")
            }
            Spacer(Modifier.height(18.dp))
            rows.forEachIndexed { i, c ->
                CompareRow(c, i, c == best, ui.selected == c.facility.id, Modifier.weight(1f)) { if (c.known && !c.current) act.select(c.facility.id) }
                if (i < rows.lastIndex) Spacer(Modifier.height(14.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("예시 수치는 계산 설명용 가정 · 가상 식당 3곳 · 실시간 정보가 없는 후보는 확정 추천에서 제외", fontSize = 13.sp, color = TextLo)
        }
        DetailPanel(ui, act, best, ranked.first { it.current }, Modifier.weight(0.38f).fillMaxHeight())
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
    Text(" $text   ", fontSize = 14.sp, color = TextMid)
}

@Composable
private fun CompareRow(c: Candidate, index: Int, best: Boolean, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(150L + index * 220L); grow.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val border by animateColorAsState(if (selected) Mint else Line, tween(300), label = "rb")
    val maxMin = 70f
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (selected) Brush.verticalGradient(listOf(Color(0xFF10291F), Color(0xFF0F1A18))) else Brush.verticalGradient(listOf(Color(0xFF10141C), Color(0xFF10141C))))
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(c.facility.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(10.dp))
            when {
                c.current -> SmallTag("기존 목적지", CardHi, TextMid)
                best -> { SmallTag("가장 먼저 시작", Mint.copy(alpha = 0.16f), Mint); Spacer(Modifier.width(6.dp)); SmallTag("주차 ${c.facility.parkingSpaces}대", CardHi, TextMid) }
                !c.known -> SmallTag("이용 가능 미확인", Amber.copy(alpha = 0.16f), Amber)
            }
            Spacer(Modifier.weight(1f))
            Text("이동 ${c.travelMin} + 주차·도보 ${c.parkWalkMin} + 대기 ${c.waitMin ?: "?"}", fontSize = 15.sp, color = TextMid)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.weight(1f).height(44.dp)) {
                val unit = size.width / maxMin * grow.value
                var x = 0f
                drawRoundRect(Color(0xFF1A1F2B), cornerRadius = CornerRadius(10f))
                fun seg(min: Int, color: Color) { if (min > 0) { drawRect(Brush.verticalGradient(listOf(color.lighten(0.12f), color)), Offset(x, 0f), Size(min * unit, size.height)); x += min * unit } }
                seg(c.travelMin, Blue)
                seg(c.parkWalkMin, Gray)
                if (c.waitMin != null) seg(c.waitMin, Color(0xFFF08A3C))
                else { // 미확인 대기: 빗금.
                    var k = 0f
                    while (k < 26 * unit) { drawRect(Color(0xFFF08A3C).copy(alpha = 0.75f), Offset(x + k, 0f), Size(unit * 0.45f, size.height)); k += unit * 1.1f }
                }
            }
            Spacer(Modifier.width(22.dp))
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.width(150.dp), horizontalArrangement = Arrangement.End) {
                val color = when { c.current -> Color(0xFFFF6B6B); best -> Mint; else -> TextMid }
                Text(if (c.known) "${c.startIn}분" else "${c.startIn}분+", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = color)
                Text(if (c.known) "  " + (18 * 60 + 2 + c.startIn).clock() else "  미확인", fontSize = 15.sp, color = TextLo, modifier = Modifier.padding(bottom = 6.dp))
            }
        }
    }
}

@Composable
private fun DetailPanel(ui: UiState, act: Actions, best: Candidate?, current: Candidate, modifier: Modifier) {
    val sel = DemoData.candidates.firstOrNull { it.facility.id == ui.selected }
    Panel(modifier, padding = 22.dp) {
        AnimatedContent(targetState = sel, transitionSpec = { (fadeIn(tween(350)) + slideInHorizontally(tween(420)) { it / 8 }) togetherWith fadeOut(tween(150)) }, label = "detail", modifier = Modifier.fillMaxSize()) { c ->
            if (c == null) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Canvas(Modifier.size(54.dp)) {
                        drawCircle(TextLo, size.width * 0.32f, Offset(size.width * 0.42f, size.height * 0.42f), style = Stroke(size.width * 0.07f))
                        drawLine(TextLo, Offset(size.width * 0.66f, size.height * 0.66f), Offset(size.width * 0.9f, size.height * 0.9f), size.width * 0.08f, StrokeCap.Round)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("후보를 선택하면 예약 조건을 보여드려요", fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("동일 메뉴 유형 · 가격대 · 인원 · 주차 조건 충족", fontSize = 15.sp, color = TextLo, textAlign = TextAlign.Center)
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().height(126.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xFF1C4A5E), Color(0xFF1B2A48)))).drawBehind {
                        drawCircle(Color.White.copy(alpha = 0.06f), size.height * 0.95f, Offset(size.width * 0.85f, size.height * 0.1f))
                    }) {
                        Column(Modifier.align(Alignment.CenterStart).padding(start = 20.dp)) {
                            Text(c.facility.name, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                            Text("${c.facility.menu} · 1인 %.1f만 · 자체 평가 ${c.facility.rating}".format(c.facility.pricePerPerson / 10_000f), fontSize = 15.sp, color = TextMid)
                        }
                        Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) { SmallTag("가상 시설", Color(0x33FFFFFF), TextMid) }
                    }
                    Spacer(Modifier.height(14.dp))
                    val save = current.startIn - c.startIn
                    DetailRow("입장 예약", "${c.reservableAt?.ampm() ?: "확인 필요"} · ${ui.party}인 테이블")
                    DetailRow("추가 이동", "+${c.travelMin - current.travelMin}분 · %.1fkm".format(c.distanceKm - current.distanceKm))
                    DetailRow("기존 웨이팅", "취소 · 수수료 없음")
                    DetailRow("이용 시작 단축", "${save}분 빨라요", Mint)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(PurpleGrad).clickable { act.confirm() }.padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
                        Text("예약하고 길 안내 변경", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(k: String, v: String, color: Color = TextHi) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(k, fontSize = 16.sp, color = TextMid, modifier = Modifier.weight(1f))
        Text(v, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
}

@Composable
private fun ConfirmModal(ui: UiState, act: Actions) {
    val labels = listOf("대안 시설 예약 가능 여부 재확인", "신규 예약 확정", "기존 웨이팅 취소 · 수수료 확인", "내비게이션 목적지 변경")
    val doneLabels = listOf("가능", "확정됨", "취소됨", "변경됨")
    Box(Modifier.fillMaxSize().background(Color(0xB3050709)).clickable(remember { MutableInteractionSource() }, null) {}, contentAlignment = Alignment.Center) {
        val s = remember { Animatable(0.92f) }
        LaunchedEffect(Unit) { s.animateTo(1f, spring(dampingRatio = 0.6f)) }
        Column(Modifier.width(720.dp).scale(s.value).clip(RoundedCornerShape(26.dp)).background(Color(0xFF181C26)).border(1.dp, Line, RoundedCornerShape(26.dp)).padding(32.dp)) {
            Text("해송 해물찜 · ${ui.party}인 · 오후 6:25", fontSize = 16.sp, color = TextMid)
            val allDone = ui.flowResult == "완료"
            val failed = ui.steps.any { it == StepState.FAIL }
            Text(when { allDone -> "예약과 목적지를 모두 바꿨어요"; failed -> "변경을 멈췄어요"; else -> "예약과 목적지를 바꾸는 중" }, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(18.dp))
            labels.forEachIndexed { i, l ->
                val st = ui.steps.getOrNull(i) ?: StepState.WAIT
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                        when (st) {
                            StepState.RUN -> Spinner(Modifier.size(32.dp), PurpleHi)
                            StepState.DONE -> { val p = remember { Animatable(0.4f) }; LaunchedEffect(Unit) { p.animateTo(1f, spring(dampingRatio = 0.45f)) }; Box(Modifier.scale(p.value)) { CheckDot(Mint, 32.dp) } }
                            StepState.FAIL -> Box(Modifier.size(32.dp).clip(CircleShape).background(Red), contentAlignment = Alignment.Center) { Text("!", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) }
                            StepState.ROLLED_BACK -> Box(Modifier.size(32.dp).clip(CircleShape).border(2.dp, Amber, CircleShape), contentAlignment = Alignment.Center) { Text("↺", fontSize = 16.sp, color = Amber) }
                            else -> Box(Modifier.size(32.dp).clip(CircleShape).border(2.dp, Line, CircleShape))
                        }
                    }
                    Text("   $l", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = if (st == StepState.WAIT) TextLo else TextHi, modifier = Modifier.weight(1f))
                    Text(when (st) { StepState.RUN -> "진행 중"; StepState.DONE -> doneLabels[i]; StepState.FAIL -> "실패"; StepState.ROLLED_BACK -> "되돌림"; else -> "" },
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = when (st) { StepState.RUN -> PurpleHi; StepState.DONE -> Mint; StepState.FAIL -> Red; else -> Amber })
                }
                if (i < labels.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(18.dp)) { drawPath(Path().apply { moveTo(size.width / 2, 0f); lineTo(size.width, size.height * 0.25f); lineTo(size.width * 0.85f, size.height * 0.8f); lineTo(size.width / 2, size.height); lineTo(size.width * 0.15f, size.height * 0.8f); lineTo(0f, size.height * 0.25f); close() }, TextLo, style = Stroke(2.5f)) }
                Text("  " + (if (failed) ui.flowResult else "한 단계라도 실패하면 자동 변경을 멈추고 기존 예약 · 목적지를 유지해요"), fontSize = 15.sp, color = if (failed) Amber else TextLo)
            }
            if (failed) {
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardHi).clickable { act.closeConfirm() }.padding(vertical = 18.dp), contentAlignment = Alignment.Center) { Text("닫기", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ---------------- 장면 D: 방문 후 평가 ----------------
@Composable
private fun Visit(ui: UiState, act: Actions) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Panel(Modifier.weight(0.4f).fillMaxHeight(), padding = 26.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Logo(Modifier.size(36.dp))
                Text("  방문 완료", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                Tag("정차 중", Mint)
            }
            Spacer(Modifier.height(18.dp))
            Text(DemoData.haesong.name, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text("오늘 · ${ui.party}인 · 오후 6:24 입장", fontSize = 16.sp, color = TextLo)
            Spacer(Modifier.height(18.dp))
            StatTile("이용 시작 시각", "6:24", "예상 6:25 · 오차 1분", TextHi, Modifier.weight(1f))
            Spacer(Modifier.height(12.dp))
            StatTile("실제 대기", "4분", "예상 5분", TextHi, Modifier.weight(1f))
            Spacer(Modifier.height(12.dp))
            StatTile("기존 목적지였다면", "약 37분 절약", "가정 기준", Mint, Modifier.weight(1f))
        }
        Panel(Modifier.weight(0.6f).fillMaxHeight(), padding = 30.dp) {
            Text("30초 평가 · 다음 추천에 반영돼요", fontSize = 15.sp, color = TextMid)
            Text("오늘 식사, 어떠셨나요?", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(0.4f))
            RateRow("음식 만족") { Stars(ui.food) { ui.food = it } }
            Spacer(Modifier.weight(0.3f))
            RateRow("대기 시간 정확도") { Segmented(listOf("정확해요", "조금 달라요", "많이 달라요"), ui.waitAcc) { ui.waitAcc = it } }
            Spacer(Modifier.weight(0.3f))
            RateRow("주차 편의") { Stars(ui.parking) { ui.parking = it } }
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(ui.sent, enter = fadeIn() + slideInVertically { it / 2 }) {
                Row(Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(14.dp)).background(CardHi).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    InfoIcon(PurpleHi)
                    Text("  다음 검색부터 대기 정확도 · 주차 편의를 추천 순위에 반영해요", fontSize = 16.sp, color = TextMid)
                }
            }
            PrimaryButton(if (ui.sent) "반영 완료 · 고마워요!" else "평가 보내기", Modifier.fillMaxWidth(), done = ui.sent, enabled = ui.food > 0 && ui.waitAcc >= 0 && ui.parking > 0) { act.send() }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, sub: String, color: Color, modifier: Modifier) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF10141C)).border(1.dp, Line, RoundedCornerShape(18.dp)).padding(horizontal = 22.dp, vertical = 14.dp), verticalArrangement = Arrangement.Center) {
        Text(label, fontSize = 15.sp, color = TextMid)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text("  $sub", fontSize = 15.sp, color = TextLo, modifier = Modifier.padding(bottom = 6.dp))
        }
    }
}

@Composable
private fun RateRow(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
private fun Stars(n: Int, set: (Int) -> Unit) {
    Row {
        (1..5).forEach { i ->
            val on = i <= n
            val s by animateFloatAsState(if (on) 1.15f else 1f, spring(dampingRatio = 0.4f), label = "s$i")
            Text("★", fontSize = 44.sp, color = if (on) Color(0xFFF6C443) else Color(0xFF39404F), modifier = Modifier.scale(s).padding(horizontal = 4.dp).clickable { set(i) })
        }
    }
}

@Composable
private fun Segmented(items: List<String>, sel: Int, set: (Int) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF10141C)).padding(5.dp)) {
        items.forEachIndexed { i, s ->
            val on = i == sel
            Text(s, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (on) Bg else TextMid,
                modifier = Modifier.clip(RoundedCornerShape(11.dp)).background(if (on) Color.White else Color.Transparent).clickable { set(i) }.padding(horizontal = 22.dp, vertical = 12.dp))
        }
    }
}

// ---------------- 오버레이: 토스트 · 음성 ----------------
@Composable
private fun ToastPill(ui: UiState, modifier: Modifier) {
    val t = ui.toast
    AnimatedVisibility(t != null, modifier, enter = fadeIn(tween(250)) + slideInVertically(tween(350)) { -it }, exit = fadeOut(tween(250)) + slideOutVertically(tween(300)) { -it }) {
        val (kind, text) = t ?: (Toast.OK to "")
        Row(Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFF4F6FA)).padding(start = 10.dp, end = 26.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(if (kind == Toast.NAV) Blue else Mint), contentAlignment = Alignment.Center) {
                if (kind == Toast.NAV) NavArrow(Color.White, 16.dp) else Canvas(Modifier.size(18.dp)) {
                    drawPath(Path().apply { moveTo(size.width * 0.12f, size.height * 0.52f); lineTo(size.width * 0.4f, size.height * 0.8f); lineTo(size.width * 0.9f, size.height * 0.22f) }, Color.White, style = Stroke(size.width * 0.16f, cap = StrokeCap.Round))
                }
            }
            Text("  $text", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Bg)
        }
    }
}

@Composable
private fun VoiceBar(ui: UiState, modifier: Modifier) {
    val b = ui.bubble
    AnimatedVisibility(b != null, modifier, enter = fadeIn(tween(250)) + slideInVertically(tween(380)) { it / 2 } + scaleIn(initialScale = 0.96f), exit = fadeOut(tween(300))) {
        val bub = b ?: Bubble("", false)
        Row(
            Modifier.widthIn(max = 660.dp).clip(RoundedCornerShape(40.dp)).background(Brush.horizontalGradient(listOf(Color(0xF21A1F33), Color(0xF2141826))))
                .border(1.dp, Color(0x557C6CF6), RoundedCornerShape(50)).padding(start = 12.dp, end = 28.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VoiceOrb(Modifier.size(54.dp), active = true)
            Text("  “${bub.text}”", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (bub.user) Color(0xFFCFC8FF) else TextHi, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(20.dp))
            Waveform(Modifier.size(130.dp, 34.dp), if (bub.user) PurpleHi else Color(0xFFB9C2DA), true)
        }
    }
}

private fun Color.lighten(k: Float) = Color(red + (1 - red) * k, green + (1 - green) * k, blue + (1 - blue) * k, alpha)
