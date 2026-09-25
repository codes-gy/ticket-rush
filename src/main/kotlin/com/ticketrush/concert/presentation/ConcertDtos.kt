package com.ticketrush.concert.presentation

import com.ticketrush.concert.domain.Concert
import com.ticketrush.concert.domain.ConcertStatus
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class ConcertCreateRequest(
    @field:NotBlank(message = "공연명은 필수입니다.")
    val title: String,

    @field:NotBlank(message = "아티스트명은 필수입니다.")
    val artist: String,

    @field:NotBlank(message = "공연장은 필수입니다.")
    val venue: String,

    @field:NotNull(message = "공연 일시는 필수입니다.")
    @field:Future(message = "공연 일시는 미래여야 합니다.")
    val performedAt: LocalDateTime,

    @field:NotNull(message = "티켓 오픈 시각은 필수입니다.")
    @field:Future(message = "티켓 오픈 시각은 미래여야 합니다.")
    val ticketOpenAt: LocalDateTime,
)

data class ConcertResponse(
    val id: Long,
    val title: String,
    val artist: String,
    val venue: String,
    val performedAt: LocalDateTime,
    val ticketOpenAt: LocalDateTime,
    val status: ConcertStatus,
) {
    companion object {
        fun from(concert: Concert): ConcertResponse =
            ConcertResponse(
                id = requireNotNull(concert.id),
                title = concert.title,
                artist = concert.artist,
                venue = concert.venue,
                performedAt = concert.performedAt,
                ticketOpenAt = concert.ticketOpenAt,
                status = concert.status,
            )
    }
}
