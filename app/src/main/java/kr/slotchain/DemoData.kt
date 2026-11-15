package kr.slotchain

/**
 * 시연용 가상 시설 4곳(성수동 일대 좌표). 실제 상호·예약 정보가 아니다.
 * 웨이팅·이동 시간은 계산 설명용 가정값이며 화면에도 '모의'로 표시한다.
 */
object DemoData {
    val ondam = Facility(
        "ondam", "온담 한식당", "성수동", "한식", 25_000, 8, 6, 4.5, 37.5446, 127.0557,
        slots = listOf(17 * 60 + 45, 18 * 60, 18 * 60 + 15, 18 * 60 + 30, 18 * 60 + 45),
    )
    val badajeong = Facility("badajeong", "바다정 해물찜", "성수동", "해물찜", 27_000, 0, 6, 4.2, 37.5470, 127.0420, waitMin = 50, waitUpdatedMinAgo = 2)
    val haesong = Facility(
        "haesong", "해송 해물찜", "성수동", "해물찜", 26_000, 12, 6, 4.6, 37.5562, 127.0410,
        slots = listOf(18 * 60 + 25, 18 * 60 + 40), waitMin = 5, waitUpdatedMinAgo = 1,
    )
    val pogu = Facility("pogu", "포구집", "뚝섬", "해물찜", 23_000, 6, 8, 4.0, 37.5480, 127.0480, waitMin = null)

    /**
     * 장면 A: 내 예약 6:00(정체로 6:10 도착). 같은 식당의 다른 예약도 ETA를 공유한다.
     * 손님 B는 6:15 예약인데 6:24 도착 예정(늦춤만 가능), 손님 C는 6:30 예약인데 이미 근처라 5:58 도착(앞당김 원함).
     * 그대로면 지각 2팀(테이블 빈 시간 19분)·대기 32분 → 6:00→6:15→6:30→6:00 순환이면 지각 0·대기 13분.
     */
    val myBooking = Booking("me", "ondam", 18 * 60, 4, early = 30, late = 30, owner = "나")
    val ondamOthers = listOf(
        Booking("g1", "ondam", 17 * 60 + 45, 2, early = 0, late = 0, owner = "손님 A", eta = 17 * 60 + 45),
        Booking("g2", "ondam", 18 * 60 + 15, 4, early = 0, late = 15, owner = "손님 B", eta = 18 * 60 + 24),
        Booking("g3", "ondam", 18 * 60 + 30, 4, early = 30, late = 0, owner = "손님 C", eta = 17 * 60 + 58),
        Booking("g4", "ondam", 18 * 60 + 45, 3, early = 15, late = 0, owner = "손님 D", eta = 18 * 60 + 45),
    )

    /** 장면 B: 만석 식당 대안 비교. 기존 목적지는 바다정(웨이팅). */
    val candidates = listOf(
        Candidate(badajeong, travelMin = 10, parkWalkMin = 0, waitMin = 50, current = true, distanceKm = 3.1),
        Candidate(haesong, travelMin = 15, parkWalkMin = 3, waitMin = 5, distanceKm = 5.2, reservableAt = 18 * 60 + 25),
        Candidate(pogu, travelMin = 14, parkWalkMin = 4, waitMin = null, distanceKm = 4.4),
    )
    val preference = Preference(party = 4, menu = "해물찜", priceMin = 20_000, priceMax = 30_000, needParking = true)
}
