package com.ticketrush.seat.presentation

import com.ticketrush.seat.application.SeatService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 공개 조회 API. SecurityConfig에서 GET /api/concerts/** 는 인증 없이 접근 가능하도록 허용되어 있다. */
@RestController
@RequestMapping("/api/concerts/{concertId}/seats")
class SeatController(
    private val seatService: SeatService,
) {
    @GetMapping
    fun findByConcertId(@PathVariable concertId: Long): ResponseEntity<List<SeatResponse>> =
        ResponseEntity.ok(seatService.findByConcertId(concertId))
}
