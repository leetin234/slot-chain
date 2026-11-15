package kr.slotchain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {
    private val f = DemoData.ondam
    private val me = DemoData.myBooking

    @Test fun onTimeNeedsNoChange() {
        assertFalse(SlotEngine.adjust(me, 17 * 60 + 56, f, DemoData.ondamOthers).ok)
    }

    @Test fun freeSlotIsTakenDirectly() {
        // 6:15가 비어 있고 다른 손님이 모두 제시간이면 빈 슬롯으로 바로 옮긴다.
        val others = DemoData.ondamOthers.filter { it.id != "g2" }.map { it.copy(eta = null) }
        val plan = SlotEngine.adjust(me, 18 * 60 + 10, f, others)
        assertEquals(listOf(Move("me", "나", 1080, 1095)), plan.moves)
    }

    @Test fun swapBeatsFreeSlotWhenItHelpsSomeoneElse() {
        // 6:15가 비어 있어도 C가 5:58에 와서 32분 기다릴 상황이면, C와 바꾸는 편이 전체 대기가 적다.
        val plan = SlotEngine.adjust(me, 18 * 60 + 10, f, DemoData.ondamOthers.filter { it.id != "g2" })
        assertTrue(plan.after.cost < SlotEngine.score(DemoData.ondamOthers.filter { it.id != "g2" } + me, listOf(Move("me", "나", 1080, 1095)), mapOf("me" to 1090, "g1" to 1065, "g3" to 1078, "g4" to 1125)).cost)
        assertEquals("2인 교환", plan.kind)
    }

    @Test fun scenarioAUsesCycleWithinEveryonesTolerance() {
        val plan = SlotEngine.adjust(me, 18 * 60 + 10, f, DemoData.ondamOthers)
        assertTrue(plan.ok)
        assertEquals(1095, plan.moves.first().to) // 6:15
        // 모두 허용 범위 안, 도착 시간 위반 없음, 슬롯 중복 없음.
        val all = DemoData.ondamOthers + me
        plan.moves.forEach { m -> assertTrue(all.first { it.id == m.bookingId }.accepts(m.to)) }
        assertEquals(plan.moves.size, plan.moves.map { it.to }.toSet().size)
        // 비워진 자리로만 이동해 최종 배치가 겹치지 않는다.
        val finalTimes = all.map { b -> plan.moves.firstOrNull { it.bookingId == b.id }?.to ?: b.time }
        assertEquals(finalTimes.size, finalTimes.toSet().size)
        assertEquals("3팀 순환 교환", plan.kind)
        assertEquals(listOf("나", "손님 B", "손님 C"), plan.moves.map { it.owner })
    }

    @Test fun cycleIsChosenBecauseEveryoneGainsNotJustMe() {
        val plan = SlotEngine.adjust(me, 18 * 60 + 10, f, DemoData.ondamOthers)
        // 그대로 두면 나(10분)·B(9분) 지각, C는 32분 대기.
        assertEquals(Score(2, 19, 32), plan.before)
        // 순환 후: 아무도 늦지 않고 대기 합계 13분(나 5 · B 6 · C 2).
        assertEquals(Score(0, 0, 13), plan.after)
        assertTrue("explored=${plan.explored} valid=${plan.valid}", plan.explored >= plan.valid && plan.valid >= 2) // 여러 배치 중 비교해 골랐다
    }

    @Test fun neverMakesAnOnTimeGuestLate() {
        val plan = SlotEngine.adjust(me, 18 * 60 + 10, f, DemoData.ondamOthers)
        val all = DemoData.ondamOthers.associateBy { it.id }
        plan.moves.drop(1).forEach { m -> val b = all.getValue(m.bookingId); val arr = b.eta ?: b.time; if (arr <= m.from) assertTrue(m.to >= arr) }
    }

    @Test fun twoWaySwapWhenNeighbourCanMoveEarlier() {
        // B가 일찍 도착(5:58)하고 앞당김도 허용, C는 제시간이면 2인 교환이 가장 좋다.
        val others = DemoData.ondamOthers.map { when (it.id) { "g2" -> it.copy(early = 15, eta = 17 * 60 + 58); "g3" -> it.copy(eta = null); else -> it } }
        val plan = SlotEngine.adjust(me, 18 * 60 + 10, f, others)
        assertEquals("2인 교환", plan.kind)
        assertEquals(1080, plan.moves[1].to)
    }

    @Test fun noChangeBeyondTolerance() {
        val plan = SlotEngine.adjust(me.copy(late = 10), 18 * 60 + 10, f, DemoData.ondamOthers)
        assertFalse(plan.ok)
    }

    @Test fun lockedNeighboursBlockSwap() {
        val locked = DemoData.ondamOthers.map { it.copy(early = 0, late = 0) }
        assertFalse(SlotEngine.adjust(me, 18 * 60 + 10, f, locked).ok)
    }

    @Test fun rankerSortsByStartTimeAndHoldsUnknownBack() {
        val ranked = StartTimeRanker.rank(DemoData.candidates, DemoData.preference)
        assertEquals(listOf("haesong", "badajeong", "pogu"), ranked.map { it.facility.id })
        assertEquals(23, ranked[0].startIn)
        assertEquals(60, ranked[1].startIn)
        val best = StartTimeRanker.best(ranked)!!
        assertEquals("haesong", best.facility.id)
        assertEquals(37, ranked.first { it.current }.startIn - best.startIn)
    }

    @Test fun rankerFiltersRequiredConditions() {
        val ranked = StartTimeRanker.rank(DemoData.candidates, DemoData.preference.copy(party = 7))
        assertEquals(listOf("badajeong", "pogu"), ranked.map { it.facility.id }) // 해송은 6인까지
        assertEquals(null, StartTimeRanker.best(ranked))
    }

    @Test fun switchFlowRollsBackOnFailure() {
        val flow = SwitchFlow(listOf("재확인", "신규 예약", "기존 취소", "목적지 변경"))
        val undone = mutableListOf<Int>()
        assertFalse(flow.run(step = { it != 3 }, undo = { undone += it }))
        assertEquals(listOf(2, 1, 0), undone)
        assertEquals(StepState.FAIL, flow.state[3])
        assertTrue(flow.result.contains("기존 예약과 목적지를 유지"))
        val ok = SwitchFlow(listOf("a", "b"))
        assertTrue(ok.run({ true }))
        assertTrue(ok.state.all { it == StepState.DONE })
    }
}
