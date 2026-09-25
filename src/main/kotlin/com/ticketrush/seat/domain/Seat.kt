package com.ticketrush.seat.domain

import com.ticketrush.common.BaseTimeEntity
import com.ticketrush.concert.domain.Concert
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal

@Entity
@Table(name = "seats")
class Seat(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    val concert: Concert,

    @Column(nullable = false)
    val section: String,

    @Column(name = "seat_row", nullable = false)
    val row: String,

    @Column(nullable = false)
    val number: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val grade: SeatGrade,

    @Column(nullable = false)
    val price: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SeatStatus = SeatStatus.AVAILABLE,

    /**
     * 낙관적 락(버전 컬럼) 방식. PLANNING.md 5장에서 제안한 "낙관적 락 vs 분산 락(Redisson) 비교"
     * 실험을 위해 우선 반영해두었다. 분산 락 방식은 좌석 선점 기능 구현 시 별도로 추가한다.
     */
    @Version
    var version: Long = 0,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}

enum class SeatGrade { VIP, R, S, A }
enum class SeatStatus { AVAILABLE, HELD, SOLD }
