package kr.slotchain

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.app.Activity
import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Pleos NaviHelper 연결. 목적지 변경(requestRoute)과 남은 시간(onDrivingInfo)을 다룬다.
 * 연결이 안 되면 앱은 모의 ETA로 계속 동작하고 화면에 그 사실을 표시한다.
 */
class Navi(context: Context, private val onState: (String) -> Unit, private val onRemain: (Int?) -> Unit) {
    private val helper = runCatching { NaviHelper(context.applicationContext) }.getOrNull()
    private val main = Handler(Looper.getMainLooper())
    var connected = false
        private set
    var destination: String? = null
        private set
    private var pending: ((Boolean, String) -> Unit)? = null

    private val listener = object : NaviHelperEventListener {
        override fun onDestinationChanged() {
            log("onDestinationChanged")
            helper?.getDestinationInfo()
        }

        override fun onDestinationInfo(info: DestinationInfo) {
            destination = info.name
            log("destination=${info.name}")
            // 빈 목적지 응답(안내 전 조회)은 성공으로 치지 않는다.
            if (info.name.isNotBlank()) main.post { pending?.invoke(true, info.name); pending = null }
        }

        override fun onRouteStarted(info: ai.pleos.playground.navi.data.RouteStartInfo) {
            log("route started")
            main.post { pending?.invoke(true, "안내 시작"); pending = null }
        }

        override fun onRoutingCompleted() {
            log("routing completed")
            helper?.getDestinationInfo()
        }

        override fun onDrivingInfo(info: DrivingInfo) {
            // duration: 목적지까지 남은 시간(초).
            val min = info.destination?.duration?.let { (it + 59) / 60 }
            main.post { onRemain(min) }
        }

        override fun onError(errorCode: NaviErrorCode) {
            log("error $errorCode")
            main.post { pending?.invoke(false, errorCode.toString()); pending = null }
        }
    }

    fun start() {
        val h = helper ?: return onState("내비 연동 불가 · 모의 ETA")
        runCatching {
            h.initialize(
                onConnected = {
                    connected = true
                    runCatching { h.addListener(listener); h.getDestinationInfo() }
                    main.post { onState("NaviHelper 연결") }
                },
                onFailed = { main.post { onState("내비 연결 실패 · 모의 ETA") } },
            )
        }.onFailure { onState("내비 연동 불가 · 모의 ETA") }
    }

    /** 목적지를 바꾼다. 내비 앱의 안내 시작(onRouteStarted) 또는 목적지 응답으로 확인하고, 12초 안에 응답이 없으면 실패로 본다. */
    fun route(f: Facility, done: (Boolean, String) -> Unit) {
        val h = helper
        if (h == null || !connected) return done(false, "내비 미연결")
        pending = done
        log("requestRoute ${f.name}")
        runCatching {
            h.requestRoute(RouteInfo(latitude = f.lat, longitude = f.lng, poiName = f.name, poiId = "slotchain-${f.id}", address = "서울 성동구 ${f.area}", poiSubId = "", routeOption = RouteOption.RECOMMENDED))
        }.onFailure { Log.w("SlotNavi", "requestRoute failed", it); pending = null; return done(false, it.javaClass.simpleName) }
        main.postDelayed({ pending?.let { pending = null; it(false, "응답 없음") } }, 12_000)
    }

    fun close() {
        runCatching { helper?.removeListener(listener); helper?.release() }
    }

    private fun log(s: String) = Log.i("SlotNavi", s)
}

enum class Mode { PARKED, DRIVING }

/** AAOS CarUxRestrictions로 주행·정차를 판단. 정보가 없으면 주행과 같이 제한한다. */
class DrivingState(context: Activity, private val update: (Mode, Boolean) -> Unit) {
    private var closed = false
    private var manager: CarUxRestrictionsManager? = null

    private fun apply(rule: CarUxRestrictions?) {
        if (closed) return
        val parked = rule != null && !rule.isRequiresDistractionOptimization && rule.activeRestrictions == CarUxRestrictions.UX_RESTRICTIONS_BASELINE
        Log.i("SlotUX", "known=${rule != null} parked=$parked")
        update(if (parked) Mode.PARKED else Mode.DRIVING, rule != null)
    }

    private val car = runCatching {
        Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { car, ready ->
            if (closed) return@createCar
            apply(null)
            if (ready) runCatching {
                manager = car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
                manager?.registerListener(::apply)
                apply(manager?.currentCarUxRestrictions)
            }
        }
    }.getOrNull()

    fun close() {
        closed = true
        runCatching { manager?.unregisterListener() }
        car?.disconnect()
    }
}

/** 짧은 음성 안내. 내비·미디어 위에 잠깐 겹치도록 어시스턴트 용도로 포커스를 요청한다. */
class Voice(private val ctx: Context) {
    private val am = ctx.getSystemService(AudioManager::class.java)
    private val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private var player: MediaPlayer? = null
    private var req: AudioFocusRequest? = null

    fun say(clip: String, done: () -> Unit = {}) {
        stop()
        val r = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attrs).setOnAudioFocusChangeListener {}.build()
        req = r
        am.requestAudioFocus(r)
        player = MediaPlayer().apply {
            setAudioAttributes(attrs)
            ctx.assets.openFd("voice/$clip.wav").use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            setOnCompletionListener { stop(); done() }
            prepare()
            start()
        }
    }

    fun stop() {
        player?.runCatching { stop(); release() }
        player = null
        req?.let { am.abandonAudioFocusRequest(it) }
        req = null
    }
}
