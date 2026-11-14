package kr.slotchain

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Scene { SETUP, TRIP_A, TRIP_B, COMPARE, VISIT }
enum class Alert { NONE, ASK, WORKING, DONE, KEPT }
enum class Toast { NAV, OK, WARN }

data class Bubble(val text: String, val user: Boolean)

/** 화면이 읽는 모든 상태. Activity가 소유하고 Compose가 관찰한다. */
class UiState {
    var mode by mutableStateOf(Mode.PARKED)
    var modeKnown by mutableStateOf(false)
    var scene by mutableStateOf(Scene.SETUP)
    var navState by mutableStateOf("내비 연결 중")
    var navRemain by mutableStateOf<Int?>(null)
    var toast by mutableStateOf<Pair<Toast, String>?>(null)
    var bubble by mutableStateOf<Bubble?>(null)

    // 출발 전 설정
    var party by mutableIntStateOf(4)
    var menu by mutableStateOf("한식")
    var price by mutableIntStateOf(1)
    var toleranceIdx by mutableIntStateOf(2)
    var allowAlt by mutableStateOf(true)
    var parkingOnly by mutableStateOf(true)
    var shareEta by mutableStateOf(true)
    var guiding by mutableStateOf(false)

    // 주행(모의 시나리오 시계)
    var clock by mutableFloatStateOf(17 * 60 + 49f)
    var arrival by mutableIntStateOf(17 * 60 + 56)
    var distanceKm by mutableFloatStateOf(5.8f)
    var progress by mutableFloatStateOf(0f)
    var congestion by mutableStateOf(false)
    var bookingTime by mutableIntStateOf(18 * 60)
    var alert by mutableStateOf(Alert.NONE)
    var plan by mutableStateOf<Plan?>(null)
    val consented = mutableStateListOf<Int>()
    var switched by mutableStateOf(false)

    // 비교·전환
    var selected by mutableStateOf<String?>(null)
    var confirming by mutableStateOf(false)
    val steps = mutableStateListOf<StepState>()
    var flowResult by mutableStateOf("")

    // 방문 후 평가
    var food by mutableIntStateOf(0)
    var waitAcc by mutableIntStateOf(-1)
    var parking by mutableIntStateOf(0)
    var sent by mutableStateOf(false)

    val remainMin get() = (arrival - clock).toInt().coerceAtLeast(0)
}

class MainActivity : ComponentActivity() {
    private val ui = UiState()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var navi: Navi
    private lateinit var driving: DrivingState
    private lateinit var voice: Voice
    private var lastTick = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        voice = Voice(this)
        navi = Navi(this, onState = { ui.navState = it }, onRemain = { ui.navRemain = it })
        navi.start()
        driving = DrivingState(this) { mode, known -> runOnUiThread { onMode(mode, known) } }
        setContent { App(ui, actions) }
        main.post(tick)
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    /** 시연 녹화용: `--es scene A|B|C|D`, `--es say yes|no`. */
    private fun handle(intent: Intent?) {
        when (intent?.getStringExtra("scene")) {
            "A" -> resetTripA()
            "B" -> startTripB()
            "C" -> { startTripB(); openCompare() }
            "D" -> startVisit()
            "SETUP" -> resetSetup()
        }
        when (intent?.getStringExtra("say")) {
            "yes" -> answer(true)
            "no" -> answer(false)
        }
    }

    // ---- 장면 진행 ----
    private val tick = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            val dt = if (lastTick == 0L) 0f else (now - lastTick) / 1000f
            lastTick = now
            advance(dt)
            main.postDelayed(this, 100)
        }
    }

    /** 주행 중에만 시나리오 시계가 흐른다(실제 1.5초 = 1분). */
    private fun advance(dt: Float) {
        val moving = ui.mode == Mode.DRIVING && (ui.scene == Scene.TRIP_A || ui.scene == Scene.TRIP_B) && ui.remainMin > 0
        if (!moving) return
        val before = ui.clock
        ui.clock += dt / 1.5f
        val total = if (ui.scene == Scene.TRIP_A) 5.8f else 3.1f
        val left = ui.remainMin.toFloat().coerceAtLeast(0f)
        ui.distanceKm = (ui.distanceKm - dt / 1.5f * 0.25f).coerceAtLeast(0.3f)
        ui.progress = (1f - ui.distanceKm / total).coerceIn(0f, 0.97f)
        // 장면 A: 출발 3분 뒤 성수대로 정체(+14분) → 도착 6:10 예상 → 조정 제안.
        if (ui.scene == Scene.TRIP_A && !ui.congestion && ui.clock >= 17 * 60 + 53) {
            ui.congestion = true
            ui.arrival = 18 * 60 + 10
            ui.alert = Alert.ASK
            ui.plan = SlotEngine.adjust(DemoData.myBooking, ui.arrival, DemoData.ondam, DemoData.ondamOthers)
            Log.i("SlotFlow", "late eta=${ui.arrival.clock()} plan=${ui.plan}")
            ui.bubble = Bubble("예약보다 10분 늦을 것 같아요. 6시 15분으로 바꿀까요?", false)
            voice.say("prompt_late") { main.postDelayed({ if (ui.alert == Alert.ASK) ui.bubble = null }, 4_000) }
        }
        if (left <= 0f && before < ui.arrival) Log.i("SlotFlow", "arrived")
    }

    private fun onMode(mode: Mode, known: Boolean) {
        val was = ui.mode
        ui.mode = mode
        ui.modeKnown = known
        // 장면 B: 정차하면 이용 시작 시각 비교를 바로 연다.
        if (was == Mode.DRIVING && mode == Mode.PARKED && ui.scene == Scene.TRIP_B && !ui.switched) {
            main.postDelayed({ if (ui.mode == Mode.PARKED && ui.scene == Scene.TRIP_B) openCompare() }, 1_200)
        }
    }

    private fun resetSetup() {
        voice.stop()
        ui.scene = Scene.SETUP; ui.guiding = false; ui.clock = 17 * 60 + 49f; ui.toast = null; ui.bubble = null
    }

    private fun resetTripA() {
        voice.stop()
        ui.scene = Scene.TRIP_A; ui.guiding = true; ui.clock = 17 * 60 + 50f; ui.arrival = 17 * 60 + 56
        ui.distanceKm = 5.8f; ui.progress = 0f; ui.congestion = false; ui.bookingTime = 18 * 60
        ui.alert = Alert.NONE; ui.plan = null; ui.consented.clear(); ui.bubble = null; ui.toast = null
    }

    private fun startTripB() {
        voice.stop()
        ui.scene = Scene.TRIP_B; ui.clock = 18 * 60 + 2f; ui.arrival = 18 * 60 + 12; ui.distanceKm = 3.1f; ui.progress = 0.1f
        ui.switched = false; ui.selected = null; ui.confirming = false; ui.steps.clear(); ui.toast = null
        ui.bubble = Bubble("바다정 해물찜은 지금 50분 대기예요. 정차하면 더 빨리 앉을 수 있는 곳을 비교해 드릴게요.", false)
        voice.say("prompt_full") { main.postDelayed({ ui.bubble = null }, 1_500) }
    }

    private fun openCompare() {
        voice.stop(); ui.bubble = null
        ui.scene = Scene.COMPARE; ui.selected = null
    }

    private fun startVisit() {
        voice.stop()
        ui.scene = Scene.VISIT; ui.food = 0; ui.waitAcc = -1; ui.parking = 0; ui.sent = false; ui.toast = null; ui.bubble = null
    }

    private fun showToast(kind: Toast, text: String, ms: Long = 3_200) {
        ui.toast = kind to text
        main.postDelayed({ if (ui.toast?.second == text) ui.toast = null }, ms)
    }

    // ---- 사용자 응답 ----
    /** 지각 조정 제안에 대한 답(버튼 또는 짧은 음성 응답). */
    private fun answer(yes: Boolean) {
        if (ui.alert != Alert.ASK) return
        voice.stop()
        ui.bubble = Bubble(if (yes) "응, 바꿔줘" else "그대로 둘게", true)
        if (!yes) {
            ui.alert = Alert.KEPT
            main.postDelayed({ ui.bubble = null; voice.say("keep_late") }, 900)
            return
        }
        val plan = ui.plan ?: return
        ui.alert = Alert.WORKING
        ui.consented.clear()
        // 관련 고객 → 사업자 순으로 동의를 받고, 마지막에 ETA를 다시 확인한 뒤 일괄 확정한다.
        plan.moves.indices.forEach { i -> main.postDelayed({ ui.consented += i }, 700L + i * 650L) }
        main.postDelayed({ ui.consented += plan.moves.size }, 700L + plan.moves.size * 650L)
        main.postDelayed({
            val newTime = plan.moves.first().to
            if (ui.arrival <= newTime) {
                ui.bookingTime = newTime
                ui.alert = Alert.DONE
                ui.bubble = null
                showToast(Toast.OK, "예약 변경 완료 · ${newTime.ampm()}")
                voice.say("done_late")
                Log.i("SlotFlow", "committed ${plan.kind} -> ${newTime.clock()}")
            } else {
                ui.alert = Alert.ASK
            }
        }, 1_100L + (plan.moves.size + 1) * 650L)
    }

    private fun startGuide() {
        ui.guiding = true
        showToast(Toast.NAV, "온담 한식당으로 안내를 시작해요 · ETA 공유 중")
        navi.route(DemoData.ondam) { ok, msg -> Log.i("SlotFlow", "route ondam ok=$ok $msg") }
        main.postDelayed({ if (ui.scene == Scene.SETUP) resetTripA() }, 2_400)
    }

    /** 대안 예약 전환: 재확인 → 신규 예약 → 기존 웨이팅 취소 → 내비 목적지 변경. */
    private fun confirmSwitch() {
        val target = DemoData.haesong
        val labels = listOf("대안 시설 예약 가능 여부 재확인", "신규 예약 확정", "기존 웨이팅 취소 · 수수료 확인", "내비게이션 목적지 변경")
        ui.confirming = true
        ui.steps.clear(); ui.steps.addAll(List(labels.size) { StepState.WAIT })
        ui.flowResult = ""
        fun runStep(i: Int) {
            if (i == labels.size) {
                ui.flowResult = "완료"
                main.postDelayed({
                    ui.confirming = false; ui.switched = true; ui.scene = Scene.TRIP_B
                    ui.arrival = 18 * 60 + 20; ui.bookingTime = 18 * 60 + 25; ui.distanceKm = 5.2f; ui.progress = 0.05f
                    showToast(Toast.OK, "해송 해물찜 6:25 예약 · 길 안내 변경")
                    voice.say("done_switch")
                }, 1_400)
                return
            }
            ui.steps[i] = StepState.RUN
            val finish = { ok: Boolean ->
                if (ok) { ui.steps[i] = StepState.DONE; main.postDelayed({ runStep(i + 1) }, 350) }
                else {
                    ui.steps[i] = StepState.FAIL
                    for (j in i - 1 downTo 0) ui.steps[j] = StepState.ROLLED_BACK
                    ui.flowResult = "'${labels[i]}' 실패 · 기존 예약과 목적지를 유지해요"
                }
            }
            if (i == 3) navi.route(target) { ok, msg -> Log.i("SlotFlow", "route haesong ok=$ok $msg"); main.postDelayed({ finish(ok) }, 500) }
            else main.postDelayed({ finish(true) }, 900)
        }
        runStep(0)
    }

    private val actions by lazy {
        Actions(
            startGuide = ::startGuide,
            answer = ::answer,
            compare = ::openCompare,
            select = { ui.selected = it },
            confirm = ::confirmSwitch,
            closeConfirm = { ui.confirming = false },
            send = { ui.sent = true; showToast(Toast.OK, "평가가 다음 추천에 반영돼요") },
            scene = { s -> when (s) { "A" -> resetTripA(); "B" -> startTripB(); "D" -> startVisit(); else -> resetSetup() } },
        )
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        voice.stop(); navi.close(); driving.close()
        super.onDestroy()
    }
}

class Actions(
    val startGuide: () -> Unit,
    val answer: (Boolean) -> Unit,
    val compare: () -> Unit,
    val select: (String) -> Unit,
    val confirm: () -> Unit,
    val closeConfirm: () -> Unit,
    val send: () -> Unit,
    val scene: (String) -> Unit,
)
