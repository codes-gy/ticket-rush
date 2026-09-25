package com.ticketrush.seat.presentation

import com.ticketrush.seat.application.SeatService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 관리자 전용 API. SecurityConfig에서 /api/admin/** 는 ROLE_ADMIN만 접근 가능하도록 제한되어 있다. */
@RestController
@RequestMapping("/api/admin/concerts/{concertId}/seats")
class AdminSeatController(
    private val seatService: SeatService,
) {
    @PostMapping
    fun createBulk(
        @PathVariable concertId: Long,
        @Valid @RequestBody request: SeatBulkCreateRequest,
    ): ResponseEntity<List<SeatResponse>> =
        ResponseEntity.status(HttpStatus.CREATED).body(seatService.createBulk(concertId, request))
}
