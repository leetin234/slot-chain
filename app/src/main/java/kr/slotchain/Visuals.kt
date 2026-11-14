package kr.slotchain

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.atan2
import kotlin.math.sin

val Purple = Color(0xFF7C6CF6)
val PurpleHi = Color(0xFF9A8CFF)
val RouteBlue = Color(0xFF3D8BFF)
val Jam = Color(0xFFF0424F)

/** 경로 미리보기 지도. 실제 지도 타일이 아닌 앱 자체 도식이며, 진행률·정체 구간·목적지는 시나리오 상태를 따른다. */
@Composable
fun RouteMap(modifier: Modifier, progress: Float, jam: Boolean, variant: Int) {
    val t = rememberInfiniteTransition(label = "map")
    val flow by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "flow")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Restart), label = "pulse")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Color(0xFF121821))
        // 강(남쪽)과 공원.
        val river = Path().apply { moveTo(0f, h * 0.72f); lineTo(w, h * 0.5f); lineTo(w, h * 0.64f); lineTo(0f, h * 0.88f); close() }
        drawPath(river, Brush.verticalGradient(listOf(Color(0xFF0F2436), Color(0xFF0C1C2C))))
        drawRoundRect(Color(0xFF173226), Offset(w * 0.8f, h * 0.2f), Size(w * 0.16f, h * 0.18f), CornerRadius(10f))
        // 블록과 도로.
        for (gx in 0 until 9) for (gy in 0 until 12) {
            val x = gx * w / 8.3f - 14f + (gy % 2) * 9f
            val y = gy * h / 11f - 10f
            drawRoundRect(Color(0xFF19202B), Offset(x, y), Size(w / 10.5f, h / 14.5f), CornerRadius(6f))
        }
        val road = Color(0xFF283141)
        listOf(0.12f, 0.42f, 0.73f).forEach { x -> drawLine(road, Offset(w * x, 0f), Offset(w * (x - 0.02f), h), 14f) }
        listOf(0.18f, 0.43f).forEach { y -> drawLine(road, Offset(0f, h * (y + 0.06f)), Offset(w, h * (y - 0.04f)), 16f) }
        drawLine(Color(0xFF2E384A), Offset(0f, h * 0.77f), Offset(w, h * 0.55f), 22f)
        label("성수대로", w * 0.84f, h * 0.31f)
        label("강변북로", w * 0.06f, h * 0.73f, -12f)

        // 경로.
        val pts = if (variant == 0) listOf(0.43f to 1.05f, 0.42f to 0.46f, 0.73f to 0.41f, 0.73f to 0.15f)
        else listOf(0.43f to 1.05f, 0.42f to 0.52f, 0.73f to 0.47f, 0.72f to 0.15f)
        val route = Path().apply { pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(w * x, h * y) else lineTo(w * x, h * y) } }
        drawPath(route, RouteBlue.copy(alpha = 0.22f), style = Stroke(34f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(route, RouteBlue, style = Stroke(13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(route, Color.White.copy(alpha = 0.55f), style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 26f), -flow * 36f), cap = StrokeCap.Round))
        if (jam) {
            val a = Offset(w * pts[1].first, h * pts[1].second)
            val b = Offset(w * (pts[1].first + (pts[2].first - pts[1].first) * 0.55f), h * (pts[1].second + (pts[2].second - pts[1].second) * 0.55f))
            drawLine(Jam.copy(alpha = 0.3f), a, b, 30f, StrokeCap.Round)
            drawLine(Jam, a, b, 13f, StrokeCap.Round)
            pill("정체 +14분", (a.x + b.x) / 2, a.y - 44f, Jam)
        }
        // 목적지 핀.
        val dest = Offset(w * pts.last().first, h * pts.last().second)
        drawCircle(Purple.copy(alpha = 0.3f), 34f, dest)
        drawCircle(Purple, 22f, dest)
        drawCircle(Color.White, 22f, dest, style = Stroke(3f))
        fork(dest)

        // 내 차.
        val pm = PathMeasure().apply { setPath(route, false) }
        val d = pm.length * progress.coerceIn(0.02f, 0.98f)
        val pos = pm.getPosition(d)
        val tan = pm.getTangent(d)
        drawCircle(RouteBlue.copy(alpha = 0.35f * (1 - pulse)), 26f + 30f * pulse, pos)
        drawCircle(Color.White, 25f, pos)
        drawCircle(RouteBlue.copy(alpha = 0.25f), 25f, pos, style = Stroke(4f))
        rotate(Math.toDegrees(atan2(tan.y.toDouble(), tan.x.toDouble())).toFloat() + 90f, pos) {
            val arrow = Path().apply { moveTo(pos.x, pos.y - 14f); lineTo(pos.x + 10f, pos.y + 11f); lineTo(pos.x, pos.y + 5f); lineTo(pos.x - 10f, pos.y + 11f); close() }
            drawPath(arrow, RouteBlue)
        }
    }
}

private fun DrawScope.label(text: String, x: Float, y: Float, rot: Float = -6f) {
    val p = android.graphics.Paint().apply { color = 0xFF5D6A80.toInt(); textSize = 26f; isAntiAlias = true }
    drawContext.canvas.nativeCanvas.apply { save(); rotate(rot, x, y); drawText(text, x, y, p); restore() }
}

private fun DrawScope.pill(text: String, cx: Float, cy: Float, color: Color) {
    val p = android.graphics.Paint().apply { this.color = android.graphics.Color.WHITE; textSize = 27f; isAntiAlias = true; isFakeBoldText = true }
    val tw = p.measureText(text)
    drawRoundRect(color, Offset(cx - tw / 2 - 16f, cy - 24f), Size(tw + 32f, 44f), CornerRadius(10f))
    drawContext.canvas.nativeCanvas.drawText(text, cx - tw / 2, cy + 9f, p)
}

private fun DrawScope.fork(c: Offset) {
    val s = Stroke(3.2f, cap = StrokeCap.Round)
    drawLine(Color.White, Offset(c.x - 5f, c.y - 10f), Offset(c.x - 5f, c.y + 10f), 3.2f, StrokeCap.Round)
    drawLine(Color.White, Offset(c.x + 5f, c.y - 10f), Offset(c.x + 5f, c.y + 10f), 3.2f, StrokeCap.Round)
    drawArc(Color.White, 0f, 180f, false, Offset(c.x - 5f, c.y - 6f), Size(10f, 8f), style = s)
}

/** 체인 고리 로고(보라 그라데이션 사각 + 맞물린 두 고리). */
@Composable
fun Logo(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        drawRoundRect(Brush.linearGradient(listOf(Color(0xFF8E80FF), Color(0xFF5B4CE0))), cornerRadius = CornerRadius(w * 0.26f))
        val st = Stroke(w * 0.085f)
        drawRoundRect(Color.White, Offset(w * 0.16f, w * 0.33f), Size(w * 0.42f, w * 0.34f), CornerRadius(w * 0.17f), style = st)
        drawRoundRect(Color.White, Offset(w * 0.42f, w * 0.33f), Size(w * 0.42f, w * 0.34f), CornerRadius(w * 0.17f), style = st)
    }
}

/** 음성 비서 구슬(회전하는 그라데이션). */
@Composable
fun VoiceOrb(modifier: Modifier, active: Boolean) {
    val t = rememberInfiniteTransition(label = "orb")
    val r by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "rot")
    Canvas(modifier) {
        rotate(if (active) r else 0f) {
            drawCircle(Brush.sweepGradient(listOf(Color(0xFF6FD3FF), Color(0xFF9A8CFF), Color(0xFFFF7AC6), Color(0xFF6FD3FF))))
        }
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent)), radius = size.minDimension * 0.42f)
    }
}

/** 음성 파형. */
@Composable
fun Waveform(modifier: Modifier, color: Color, active: Boolean) {
    val t = rememberInfiniteTransition(label = "wave")
    val ph by t.animateFloat(0f, 6.283f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "ph")
    Canvas(modifier) {
        val n = 15
        val bw = size.width / n
        for (i in 0 until n) {
            val k = if (active) 0.25f + 0.75f * kotlin.math.abs(sin(ph + i * 0.8f) * sin(i * 1.7f + 1f)) else 0.18f
            val bh = size.height * k
            drawRoundRect(color, Offset(i * bw + bw * 0.25f, (size.height - bh) / 2), Size(bw * 0.5f, bh), CornerRadius(bw))
        }
    }
}

/** 진행 중 표시(회전 호). */
@Composable
fun Spinner(modifier: Modifier, color: Color) {
    val t = rememberInfiniteTransition(label = "spin")
    val a by t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "a")
    Canvas(modifier) {
        drawCircle(color.copy(alpha = 0.2f), style = Stroke(size.width * 0.09f))
        drawArc(color, a, 110f, false, style = Stroke(size.width * 0.09f, cap = StrokeCap.Round))
    }
}
