package com.ticketrush

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * 스프링 컨텍스트가 정상적으로 로드되는지만 확인하는 최소한의 테스트.
 * 실행하려면 로컬에 PostgreSQL/Redis가 떠 있어야 한다 (docker compose up -d).
 */
@SpringBootTest
class TicketRushApplicationTests {
    @Test
    fun contextLoads() {
    }
}
