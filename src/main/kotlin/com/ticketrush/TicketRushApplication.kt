package com.ticketrush

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling // 대기열 입장 허용 스케줄러(QueueService.allowNextBatch)에 필요
class TicketRushApplication

fun main(args: Array<String>) {
    runApplication<TicketRushApplication>(*args)
}
