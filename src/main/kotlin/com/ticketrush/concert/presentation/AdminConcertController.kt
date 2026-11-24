package com.ticketrush.concert.presentation

import com.ticketrush.concert.application.ConcertService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 관리자 전용 API. SecurityConfig에서 `/api/admin` 하위 전체 경로는 ROLE_ADMIN만 접근 가능하도록 제한되어 있다. */
@RestController
@RequestMapping("/api/admin/concerts")
class AdminConcertController(
    private val concertService: ConcertService,
) {
    @PostMapping
    fun create(@Valid @RequestBody request: ConcertCreateRequest): ResponseEntity<ConcertResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(concertService.create(request))
}
