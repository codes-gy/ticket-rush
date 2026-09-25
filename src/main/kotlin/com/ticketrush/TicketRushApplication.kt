package com.ticketrush

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class TicketRushApplication

fun main(args: Array<String>) {
    runApplication<TicketRushApplication>(*args)
}
