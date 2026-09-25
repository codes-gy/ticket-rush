package com.ticketrush.seat.infrastructure

import com.ticketrush.seat.domain.Seat
import org.springframework.data.jpa.repository.JpaRepository

interface SeatRepository : JpaRepository<Seat, Long> {
    fun findByConcertId(concertId: Long): List<Seat>
}
