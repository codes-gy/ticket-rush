package com.ticketrush.seat.application

import com.ticketrush.concert.application.ConcertService
import com.ticketrush.seat.domain.Seat
import com.ticketrush.seat.infrastructure.SeatRepository
import com.ticketrush.seat.presentation.SeatBulkCreateRequest
import com.ticketrush.seat.presentation.SeatResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SeatService(
    private val seatRepository: SeatRepository,
    private val concertService: ConcertService,
) {
    @Transactional
    fun createBulk(concertId: Long, request: SeatBulkCreateRequest): List<SeatResponse> {
        val concert = concertService.getConcertOrThrow(concertId)
        val seats = request.seats.map { item ->
            Seat(
                concert = concert,
                section = item.section,
                row = item.row,
                number = item.number,
                grade = item.grade,
                price = item.price,
            )
        }
        return seatRepository.saveAll(seats).map { SeatResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun findByConcertId(concertId: Long): List<SeatResponse> {
        concertService.getConcertOrThrow(concertId) // 존재하지 않는 콘서트면 여기서 404
        return seatRepository.findByConcertId(concertId).map { SeatResponse.from(it) }
    }
}
