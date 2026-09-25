package com.ticketrush.concert.application

import com.ticketrush.common.exception.NotFoundException
import com.ticketrush.concert.domain.Concert
import com.ticketrush.concert.infrastructure.ConcertRepository
import com.ticketrush.concert.presentation.ConcertCreateRequest
import com.ticketrush.concert.presentation.ConcertResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ConcertService(
    private val concertRepository: ConcertRepository,
) {
    @Transactional
    fun create(request: ConcertCreateRequest): ConcertResponse {
        val concert = Concert(
            title = request.title,
            artist = request.artist,
            venue = request.venue,
            performedAt = request.performedAt,
            ticketOpenAt = request.ticketOpenAt,
        )
        return ConcertResponse.from(concertRepository.save(concert))
    }

    @Transactional(readOnly = true)
    fun findAll(): List<ConcertResponse> =
        concertRepository.findAll().map { ConcertResponse.from(it) }

    @Transactional(readOnly = true)
    fun findById(concertId: Long): ConcertResponse =
        ConcertResponse.from(getConcertOrThrow(concertId))

    @Transactional(readOnly = true)
    fun getConcertOrThrow(concertId: Long): Concert =
        concertRepository.findByIdOrNull(concertId)
            ?: throw NotFoundException("존재하지 않는 콘서트입니다. (id=$concertId)")
}
