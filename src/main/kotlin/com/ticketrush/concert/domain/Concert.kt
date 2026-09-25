package com.ticketrush.concert.domain

import com.ticketrush.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "concerts")
class Concert(
    @Column(nullable = false)
    var title: String,

    @Column(nullable = false)
    var artist: String,

    @Column(nullable = false)
    var venue: String,

    @Column(nullable = false)
    var performedAt: LocalDateTime,

    /** 대기열 오픈 및 예매 시작 시각. PLANNING.md 2단계(대기열)에서 이 값을 기준으로 진입 허용 여부를 판단한다. */
    @Column(nullable = false)
    var ticketOpenAt: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ConcertStatus = ConcertStatus.SCHEDULED,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}

enum class ConcertStatus { SCHEDULED, ON_SALE, SOLD_OUT, CLOSED }
