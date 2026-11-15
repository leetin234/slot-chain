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
    /** 공유된 도착 예정. 없으면 예약 시간에 맞춰 온다고 본다. */
    val eta: Minute? = null,
) {
    fun accepts(t: Minute) = t - time in -early..late
}

data class Move(val bookingId: String, val owner: String, val from: Minute, val to: Minute)

/** 한 식당의 배치 품질. 지각 = 테이블이 비어 있는 시간, 대기 = 손님이 먼저 와서 기다리는 시간. */
data class Score(val lateTeams: Int, val lateMin: Int, val waitMin: Int) {
    val cost get() = lateMin * 3 + waitMin
    companion object { val ZERO = Score(0, 0, 0) }
}

/** 예약 조정안. moves가 비면 조정 불가. explored는 비교한 배치 수. */
data class Plan(
    val moves: List<Move>,
    val reason: String,
    val before: Score = Score.ZERO,
    val after: Score = Score.ZERO,
    val explored: Int = 0,
    val valid: Int = 0,
    /** 엔진이 실제로 따져 본 이동 경우의 수(허용 범위·자리 충돌 검사 포함). */
    val checked: Int = 0,
) {
    val ok get() = moves.isNotEmpty()
    val kind get() = when (moves.size) { 0 -> "불가"; 1 -> "빈 슬롯"; 2 -> "2인 교환"; else -> "${moves.size}팀 순환 교환" }
}

/**
 * 같은 식당 안에서 내 예약을 ETA 이후·허용 범위 안의 시간으로 옮기는 모든 방법을 만들어 본다.
 * 빈 슬롯, 2인 교환, 여러 팀 순환 교환을 전부 나열한 뒤 각 팀의 공유 ETA로
 * 지각(테이블 빈 시간)과 대기를 계산해 가장 적은 배치를 고른다.
 * 제약: 각 팀의 허용 범위 안, 원래 제시간이던 팀을 늦게 만들지 않음, 내 새 시간은 내 ETA 이후.
 */
object SlotEngine {
    private var checked = 0

    fun adjust(mine: Booking, eta: Minute, facility: Facility, others: List<Booking>, maxChain: Int = 4): Plan {
        val here = others.filter { it.facilityId == facility.id }
        val arrival = (here.associate { it.id to (it.eta ?: it.time) }) + (mine.id to eta)
        val all = here + mine
        val before = score(all, emptyList(), arrival)
        if (eta <= mine.time) return Plan(emptyList(), "제시간 도착 예상", before, before)
        val taken = here.associateBy { it.time }
        val targets = facility.slots.filter { it >= eta && it > mine.time && mine.accepts(it) }
        val found = mutableListOf<List<Move>>()
        checked = facility.slots.size
        for (t in targets) {
            val chain = mutableListOf(Move(mine.id, mine.owner, mine.time, t))
            if (t !in taken) found += chain.toList() else collect(chain, taken, facility, maxChain, found)
        }
        val ok = found.filter { plan ->
            plan.drop(1).all { m -> val arr = arrival.getValue(m.bookingId); arr > m.from || m.to >= arr }
        }
        val best = ok.minWithOrNull(compareBy({ score(all, it, arrival).cost }, { it.size }))
            ?: return Plan(emptyList(), if (targets.isEmpty()) "허용 범위 안에 도착 가능한 슬롯 없음" else "호환 예약 없음", before, before, found.size, 0, checked)
        return Plan(best, if (best.size == 1) "빈 슬롯" else "호환 예약 교환", before, score(all, best, arrival), found.size, ok.size, checked)
    }

    fun score(all: List<Booking>, moves: List<Move>, arrival: Map<String, Minute>): Score {
        var teams = 0; var late = 0; var wait = 0
        for (b in all) {
            val slot = moves.firstOrNull { it.bookingId == b.id }?.to ?: b.time
            val arr = arrival.getValue(b.id)
            if (arr > slot) { teams++; late += arr - slot } else wait += slot - arr
        }
        return Score(teams, late, wait)
    }

    /** 밀려난 손님을 비워진 자리·빈 슬롯·다른 예약 자리로 옮기는 모든 사슬을 모은다. */
    private fun collect(chain: MutableList<Move>, taken: Map<Minute, Booking>, f: Facility, maxChain: Int, out: MutableList<List<Move>>) {
        val displaced = taken[chain.last().to] ?: return run { out += chain.toList() }
        if (chain.size >= maxChain || chain.any { it.bookingId == displaced.id } || displaced.party > f.maxParty) return
        val used = chain.map { it.to }.toSet()
        val vacated = chain.map { it.from }.toSet()
        for (s in f.slots) {
            checked++
            if (s == displaced.time || s in used || !displaced.accepts(s)) continue
            chain += Move(displaced.id, displaced.owner, displaced.time, s)
            if (s in vacated || s !in taken) out += chain.toList() else collect(chain, taken, f, maxChain, out)
            chain.removeLast()
        }
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
