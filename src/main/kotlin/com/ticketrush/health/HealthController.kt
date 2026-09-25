package com.ticketrush.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** 배포 환경에서 애플리케이션이 정상 기동했는지 확인하기 위한 최소한의 헬스체크 엔드포인트. */
@RestController
class HealthController {
    @GetMapping("/health")
    fun health(): Map<String, String> = mapOf("status" to "OK")
}
