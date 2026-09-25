package com.ticketrush.seat.presentation

import com.ticketrush.seat.domain.Seat
import com.ticketrush.seat.domain.SeatGrade
import com.ticketrush.seat.domain.SeatStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class SeatCreateItem(
    @field:NotBlank(message = "구역은 필수입니다.")
    val section: String,

    @field:NotBlank(message = "열은 필수입니다.")
    val row: String,

    @field:Positive(message = "좌석 번호는 1 이상이어야 합니다.")
    val number: Int,

    @field:NotNull(message = "등급은 필수입니다.")
    val grade: SeatGrade,

    @field:NotNull(message = "가격은 필수입니다.")
    @field:Positive(message = "가격은 0보다 커야 합니다.")
    val price: BigDecimal,
)

data class SeatBulkCreateRequest(
    @field:NotEmpty(message = "등록할 좌석이 1개 이상 필요합니다.")
    @field:Valid
    val seats: List<SeatCreateItem>,
)

data class SeatResponse(
    val id: Long,
    val section: String,
    val row: String,
    val number: Int,
    val grade: SeatGrade,
    val price: BigDecimal,
    val status: SeatStatus,
) {
    companion object {
        fun from(seat: Seat): SeatResponse =
            SeatResponse(
                id = requireNotNull(seat.id),
                section = seat.section,
                row = seat.row,
                number = seat.number,
                grade = seat.grade,
                price = seat.price,
                status = seat.status,
            )
    }
}
