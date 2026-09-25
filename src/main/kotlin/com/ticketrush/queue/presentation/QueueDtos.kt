package com.ticketrush.queue.presentation

enum class QueueStatus { WAITING, ALLOWED, NOT_IN_QUEUE }

data class QueueStatusResponse(
    val userId: Long,
    /** WAITING일 때만 값이 있다 (1부터 시작하는 사람이 보기 편한 순번). */
    val rank: Long?,
    val status: QueueStatus,
    /** ALLOWED일 때만 값이 있다. 좌석 선택 화면 진입 및 좌석 선점 API 호출 시 필요하다 (3단계에서 검증 추가 예정). */
    val entryToken: String?,
)
