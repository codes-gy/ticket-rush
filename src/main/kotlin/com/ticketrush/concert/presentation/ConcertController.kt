package com.ticketrush.concert.presentation

import com.ticketrush.concert.application.ConcertService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 공개 조회 API. SecurityConfig에서 `/api/concerts` 하위 전체 경로의 GET 요청은 인증 없이 접근 가능하도록 허용되어 있다. */
@RestController
@RequestMapping("/api/concerts")
class ConcertController(
    private val concertService: ConcertService,
) {
    @GetMapping
    fun findAll(): ResponseEntity<List<ConcertResponse>> =
        ResponseEntity.ok(concertService.findAll())

    @GetMapping("/{concertId}")
    fun findById(@PathVariable concertId: Long): ResponseEntity<ConcertResponse> =
        ResponseEntity.ok(concertService.findById(concertId))
}
