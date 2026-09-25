package com.ticketrush.queue.presentation

import com.ticketrush.queue.application.QueueService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 로그인한 사용자만 접근 가능 (SecurityConfig의 anyRequest().authenticated() 기본값 적용).
 * userId는 요청 파라미터로 받지 않고 JWT에서 추출한다 (JwtAuthenticationFilter가
 * SecurityContext principal에 userId(Long)를 직접 담아두기 때문에 @AuthenticationPrincipal로 바로 꺼낼 수 있다).
 */
@RestController
@RequestMapping("/api/queue/{concertId}")
class QueueController(
    private val queueService: QueueService,
) {
    @PostMapping("/enter")
    fun enter(
        @PathVariable concertId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<QueueStatusResponse> =
        ResponseEntity.ok(queueService.register(concertId, userId))

    @GetMapping("/rank")
    fun rank(
        @PathVariable concertId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<QueueStatusResponse> =
        ResponseEntity.ok(queueService.getStatus(concertId, userId))

    @DeleteMapping
    fun leave(
        @PathVariable concertId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<Void> {
        queueService.leave(concertId, userId)
        return ResponseEntity.noContent().build()
    }
}
