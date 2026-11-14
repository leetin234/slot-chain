package kr.slotchain

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** docs/DESIGN.md의 토큰. 화면 코드는 이 값만 쓴다. */
object DS {
    val bg = Color(0xFF17171C)
    val surface = Color(0xFF202027)
    val surfaceHi = Color(0xFF2C2C35)
    val divider = Color(0xFF2C2C35)
    val text = Color(0xFFECECEF)
    val text2 = Color(0xFF9E9EA6)
    val text3 = Color(0xFF6B6B76)
    val blue = Color(0xFF3182F6)
    val blueText = Color(0xFF64A8FF)
    val red = Color(0xFFF04452)
    val green = Color(0xFF15C47E)
    val orange = Color(0xFFFFA826)
    fun weak(c: Color) = c.copy(alpha = 0.16f)

    val font = FontFamily(
        Font(R.font.pretendard_medium, FontWeight.Medium),
        Font(R.font.pretendard_semibold, FontWeight.SemiBold),
        Font(R.font.pretendard_bold, FontWeight.Bold),
        Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
    )
    private fun s(size: Int, w: FontWeight, lh: Float = 1.35f) =
        TextStyle(fontFamily = font, fontSize = size.sp, fontWeight = w, letterSpacing = (-0.3).sp, lineHeight = (size * lh).sp, color = text)
    val display = s(64, FontWeight.Bold, 1.1f)
    val title1 = s(34, FontWeight.Bold, 1.25f)
    val title2 = s(28, FontWeight.Bold, 1.25f)
    val title3 = s(23, FontWeight.Bold)
    val body1 = s(20, FontWeight.SemiBold)
    val body2 = s(18, FontWeight.Medium)
    val caption = s(15, FontWeight.Medium)
    val small = s(13, FontWeight.Medium)

    val rCard = RoundedCornerShape(24.dp)
    val rButton = RoundedCornerShape(16.dp)
    val rChip = RoundedCornerShape(10.dp)
}

/** 누르면 0.97로 줄었다가 스프링으로 돌아온다. */
fun Modifier.press(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val k by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 0.6f, stiffness = 800f), label = "press")
    this.scale(k).clickable(src, null, enabled = enabled, onClick = onClick)
}

@Composable
fun T(text: String, style: TextStyle, color: Color = style.color, modifier: Modifier = Modifier, align: TextAlign? = null, maxLines: Int = Int.MAX_VALUE) =
    Text(text, style = style.copy(color = color), modifier = modifier, textAlign = align, maxLines = maxLines)

@Composable
fun Card(modifier: Modifier = Modifier, padding: Dp = 24.dp, color: Color = DS.surface, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.clip(DS.rCard).background(color).padding(padding), content = content)

enum class Btn { PRIMARY, SECONDARY, WEAK }

@Composable
fun Button(text: String, modifier: Modifier = Modifier, kind: Btn = Btn.PRIMARY, color: Color = DS.blue, enabled: Boolean = true, onClick: () -> Unit) {
    val bg by animateColorAsState(
        when { !enabled -> DS.surfaceHi; kind == Btn.PRIMARY -> color; kind == Btn.SECONDARY -> DS.surfaceHi; else -> DS.weak(color) }, tween(250), label = "btn",
    )
    val fg = when { !enabled -> DS.text3; kind == Btn.WEAK -> if (color == DS.blue) DS.blueText else color; else -> Color.White }
    Box(modifier.height(64.dp).clip(DS.rButton).background(bg).press(enabled, onClick), contentAlignment = Alignment.Center) {
        T(text, DS.body1, fg)
    }
}

@Composable
fun Toggle(on: Boolean, enabled: Boolean = true, set: (Boolean) -> Unit) {
    val x by animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.75f, stiffness = 500f), label = "toggle")
    val track by animateColorAsState(if (on) DS.blue else DS.surfaceHi, tween(200), label = "track")
    Box(
        Modifier.size(64.dp, 38.dp).clip(CircleShape).background(if (enabled) track else track.copy(alpha = 0.5f))
            .clickable(remember { MutableInteractionSource() }, null, enabled = enabled) { set(!on) },
    ) {
        Box(Modifier.offset(x = 4.dp + 26.dp * x).align(Alignment.CenterStart).size(30.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
fun Badge(text: String, color: Color) {
    T(text, DS.caption.copy(fontWeight = FontWeight.Bold), if (color == DS.blue) DS.blueText else color,
        Modifier.clip(DS.rChip).background(DS.weak(color)).padding(horizontal = 12.dp, vertical = 6.dp))
}

@Composable
fun Chips(items: List<String>, sel: Int, enabled: Boolean, set: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { i, s ->
            val on = i == sel
            val bg by animateColorAsState(if (on) DS.blue else DS.surfaceHi, tween(200), label = "chip$i")
            T(s, DS.body2.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium), if (on) Color.White else DS.text2,
                Modifier.clip(DS.rChip).background(bg).press(enabled) { set(i) }.padding(horizontal = 18.dp, vertical = 12.dp))
        }
    }
}

/** 라벨-값 한 줄. */
@Composable
fun ListRow(label: String, value: String, valueColor: Color = DS.text) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        T(label, DS.body2, DS.text2, Modifier.weight(1f))
        T(value, DS.body1, valueColor)
    }
}

@Composable
fun Stepper(value: String, enabled: Boolean, minus: () -> Unit, plus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepBtn("−", enabled, minus)
        T(value, DS.title3, modifier = Modifier.width(80.dp), align = TextAlign.Center)
        StepBtn("+", enabled, plus)
    }
}

@Composable
private fun StepBtn(s: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(46.dp).clip(CircleShape).background(DS.surfaceHi).press(enabled, onClick), contentAlignment = Alignment.Center) { T(s, DS.title3, DS.text2) }
}

@Composable
fun StepSlider(labels: List<String>, index: Int, enabled: Boolean, set: (Int) -> Unit) {
    val n = labels.size
    val a by animateFloatAsState(index.toFloat(), spring(dampingRatio = 0.75f), label = "slider")
    Column {
        Canvas(
            Modifier.fillMaxWidth().height(44.dp)
                .pointerInput(enabled) { if (enabled) detectTapGestures { set(((it.x / size.width) * (n - 1) + 0.5f).toInt().coerceIn(0, n - 1)) } },
        ) {
            val y = size.height / 2
            val pad = 20f
            val x = { f: Float -> pad + (size.width - pad * 2) * f / (n - 1) }
            drawLine(DS.surfaceHi, Offset(pad, y), Offset(size.width - pad, y), 10f, StrokeCap.Round)
            drawLine(DS.blue, Offset(pad, y), Offset(x(a), y), 10f, StrokeCap.Round)
            for (i in 0 until n) drawCircle(if (i <= index) DS.blue else DS.text3, 6f, Offset(x(i.toFloat()), y))
            drawCircle(Color.Black.copy(alpha = 0.25f), 25f, Offset(x(a), y + 2f))
            drawCircle(Color.White, 23f, Offset(x(a), y))
        }
        Spacer(Modifier.height(8.dp))
        Row { labels.forEachIndexed { i, s -> T(s, DS.caption, if (i == index) DS.text else DS.text3, Modifier.weight(1f), align = when (i) { 0 -> TextAlign.Start; n - 1 -> TextAlign.End; else -> TextAlign.Center }) } }
    }
}

@Composable
fun CheckCircle(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        drawCircle(color)
        val p = Path().apply { moveTo(this@Canvas.size.width * 0.29f, this@Canvas.size.height * 0.52f); lineTo(this@Canvas.size.width * 0.44f, this@Canvas.size.height * 0.67f); lineTo(this@Canvas.size.width * 0.72f, this@Canvas.size.height * 0.37f) }
        drawPath(p, Color.White, style = Stroke(this.size.width * 0.1f, cap = StrokeCap.Round))
    }
}

@Composable
fun Dot(color: Color, size: Dp = 8.dp) = Box(Modifier.size(size).clip(CircleShape).background(color))

@Composable
fun Gap(h: Dp) = Spacer(Modifier.height(h))
