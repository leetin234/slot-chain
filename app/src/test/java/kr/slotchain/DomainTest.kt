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
        val plan = SlotEngine.adjust(me, 18 * 60 + 10, f, DemoData.ondamOthers.filter { it.id != "g2" })
        assertEquals(listOf(Move("me", "나", 1080, 1095)), plan.moves)
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
        assertEquals("3자 순환 교환", plan.kind)
        assertEquals(listOf("나", "손님 B", "손님 C"), plan.moves.map { it.owner })
    }

    @Test fun twoWaySwapWhenNeighbourCanMoveEarlier() {
        val others = DemoData.ondamOthers.map { if (it.id == "g2") it.copy(early = 15) else it }
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
