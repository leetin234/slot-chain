package kr.slotchain

/** 시각은 하루 기준 분(18:00 = 1080). */
typealias Minute = Int

fun Minute.clock(): String = "%d:%02d".format((this / 60 - 1) % 12 + 1, this % 60)
fun Minute.ampm(): String = (if (this >= 12 * 60) "오후 " else "오전 ") + clock()

data class Facility(
    val id: String,
    val name: String,
    val area: String,
    val menu: String,
    val pricePerPerson: Int,
    val parkingSpaces: Int,
    val maxParty: Int,
    val rating: Double,
    val lat: Double,
    val lng: Double,
    /** 예약 시간대(분). 각 시간대는 테이블 하나. */
    val slots: List<Minute> = emptyList(),
    /** 현장 웨이팅 예상(분). null이면 실시간 정보 없음. */
    val waitMin: Int? = null,
    val waitUpdatedMinAgo: Int? = null,
)

data class Booking(
    val id: String,
    val facilityId: String,
    val time: Minute,
    val party: Int,
    /** 고객이 허용한 변경 폭: 앞당김 최대(분), 늦춤 최대(분). */
    val early: Int,
    val late: Int,
    val owner: String,
) {
    fun accepts(t: Minute) = t - time in -early..late
}

data class Move(val bookingId: String, val owner: String, val from: Minute, val to: Minute)

/** 예약 조정안. moves가 비면 조정 불가. */
data class Plan(val moves: List<Move>, val reason: String) {
    val ok get() = moves.isNotEmpty()
    val kind get() = when (moves.size) { 0 -> "불가"; 1 -> "빈 슬롯"; 2 -> "2인 교환"; else -> "${moves.size}자 순환 교환" }
}

/**
 * 같은 시설 안에서 내 예약을 ETA 이후·허용 범위 안의 시간으로 옮긴다.
 * 빈 슬롯이 없으면 호환 예약(같거나 큰 테이블에 맞는 인원, 각자의 허용 범위 안)끼리
 * 2인 교환 또는 순환 교환을 찾는다. 모든 이동은 각 고객의 허용 범위를 넘지 않는다.
 */
object SlotEngine {
    fun adjust(mine: Booking, eta: Minute, facility: Facility, others: List<Booking>, maxChain: Int = 4): Plan {
        if (eta <= mine.time) return Plan(emptyList(), "제시간 도착 예상")
        val taken = others.filter { it.facilityId == facility.id }.associateBy { it.time }
        val targets = facility.slots.filter { it >= eta && it > mine.time && mine.accepts(it) }.sorted()
        if (targets.isEmpty()) return Plan(emptyList(), "허용 범위 안에 도착 가능한 슬롯 없음")
        for (t in targets) {
            if (t !in taken) return Plan(listOf(Move(mine.id, mine.owner, mine.time, t)), "빈 슬롯")
        }
        // 순환 교환: 내가 t로 가면 t의 손님은 다른 시간으로, … 마지막 손님이 내 원래 시간(또는 빈 슬롯)으로.
        for (t in targets) {
            val chain = mutableListOf(Move(mine.id, mine.owner, mine.time, t))
            if (extend(chain, taken, facility, mine, maxChain)) return Plan(chain.toList(), "호환 예약 교환")
        }
        return Plan(emptyList(), "호환 예약 없음")
    }

    private fun extend(chain: MutableList<Move>, taken: Map<Minute, Booking>, f: Facility, mine: Booking, maxChain: Int): Boolean {
        val displaced = taken[chain.last().to] ?: return true
        if (chain.size >= maxChain || chain.any { it.bookingId == displaced.id }) return false
        if (displaced.party > f.maxParty) return false
        val used = chain.map { it.to }.toSet()
        val vacated = chain.map { it.from }.toSet()
        // 비워진 자리(교환 완성) → 빈 슬롯 → 다른 예약 자리(사슬 연장) 순으로 시도.
        val options = f.slots
            .filter { s -> s != displaced.time && s !in used && displaced.accepts(s) }
            .sortedWith(compareBy({ if (it in vacated) 0 else if (it !in taken) 1 else 2 }, { kotlin.math.abs(it - displaced.time) }))
        for (s in options) {
            chain += Move(displaced.id, displaced.owner, displaced.time, s)
            if (s in vacated || s !in taken) return true
            if (extend(chain, taken, f, mine, maxChain)) return true
            chain.removeLast()
        }
        return false
    }
}

/** 비교 후보. 이용 시작 = 이동 + 주차·도보 + 도착 후 대기. */
data class Candidate(
    val facility: Facility,
    val travelMin: Int,
    val parkWalkMin: Int,
    val waitMin: Int?,
    val current: Boolean = false,
    val distanceKm: Double = 0.0,
    val reservableAt: Minute? = null,
) {
    val known get() = waitMin != null
    val startIn get() = travelMin + parkWalkMin + (waitMin ?: 0)
}

data class Preference(val party: Int, val menu: String, val priceMin: Int, val priceMax: Int, val needParking: Boolean)

object StartTimeRanker {
    /** 필수 조건으로 거른 뒤 이용 시작이 빠른 순. 대기 정보가 없는 후보는 '미확인'으로 뒤에 둔다. */
    fun rank(cands: List<Candidate>, p: Preference): List<Candidate> =
        cands.filter { c ->
            c.current || (c.facility.menu == p.menu && c.facility.maxParty >= p.party &&
                c.facility.pricePerPerson in p.priceMin..p.priceMax && (!p.needParking || c.facility.parkingSpaces > 0))
        }.sortedWith(compareBy<Candidate>({ !it.known }, { it.startIn }))

    /** 확정 추천은 실시간 정보가 있는 후보 중 기존 목적지보다 빠른 곳만. */
    fun best(ranked: List<Candidate>): Candidate? {
        val cur = ranked.firstOrNull { it.current } ?: return null
        return ranked.firstOrNull { !it.current && it.known && it.startIn < cur.startIn }
    }
}

/** 대안 예약 전환: 각 단계가 끝나야 다음으로. 하나라도 실패하면 앞 단계를 되돌리고 기존 예약·목적지를 유지한다. */
enum class StepState { WAIT, RUN, DONE, FAIL, ROLLED_BACK }

class SwitchFlow(val steps: List<String>) {
    val state = MutableList(steps.size) { StepState.WAIT }
    var result: String = ""
        private set

    /** step(i)가 true면 성공. 실패 시 이미 끝난 단계는 역순으로 되돌린다. */
    fun run(step: (Int) -> Boolean, undo: (Int) -> Unit = {}): Boolean {
        for (i in steps.indices) {
            state[i] = StepState.RUN
            if (!step(i)) {
                state[i] = StepState.FAIL
                for (j in i - 1 downTo 0) { undo(j); state[j] = StepState.ROLLED_BACK }
                result = "'${steps[i]}' 실패 · 기존 예약과 목적지를 유지해요"
                return false
            }
            state[i] = StepState.DONE
        }
        result = "완료"
        return true
    }
}
