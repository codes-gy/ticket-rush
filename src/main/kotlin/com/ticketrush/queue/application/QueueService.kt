package com.ticketrush.queue.application

import com.ticketrush.concert.application.ConcertService
import com.ticketrush.queue.infrastructure.QueueRedisKeys
import com.ticketrush.queue.infrastructure.QueueRedisScripts
import com.ticketrush.queue.presentation.QueueStatus
import com.ticketrush.queue.presentation.QueueStatusResponse
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

/**
 * waiting-api의 Redis ZSET 기반 대기열 설계를 콘서트별로 확장한 버전.
 * PLANNING.md 2단계 구현.
 */
@Service
class QueueService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val queueProperties: QueueProperties,
    private val concertService: ConcertService,
) {
    /**
     * 대기열 등록. 이미 대기 중이거나 이미 허용된 사용자가 다시 호출해도 상태만 그대로 반환한다 (멱등).
     * "허용 여부 확인 -> ZADD"를 Lua 스크립트로 묶어, 확인과 등록 사이에 스케줄러가 끼어들어
     * 이미 허용된 사용자가 다시 대기열에 들어가는 일이 없도록 한다.
     */
    fun register(concertId: Long, userId: Long): QueueStatusResponse {
        concertService.getConcertOrThrow(concertId) // 존재하지 않는 콘서트면 여기서 404

        redisTemplate.execute(
            QueueRedisScripts.REGISTER,
            listOf(
                QueueRedisKeys.userTokenKey(concertId, userId),
                QueueRedisKeys.waitKey(concertId),
                QueueRedisKeys.ACTIVE_CONCERTS_KEY,
            ),
            userId.toString(),
            System.currentTimeMillis().toString(),
            concertId.toString(),
        )
        return getStatus(concertId, userId)
    }

    fun getStatus(concertId: Long, userId: Long): QueueStatusResponse {
        val result = redisTemplate.execute(
            QueueRedisScripts.STATUS,
            listOf(QueueRedisKeys.userTokenKey(concertId, userId), QueueRedisKeys.waitKey(concertId)),
            userId.toString(),
        )

        return when (result.firstOrNull()?.toString()) {
            "ALLOWED" ->
                QueueStatusResponse(userId = userId, rank = 0, status = QueueStatus.ALLOWED, entryToken = result[1].toString())
            // Redis rank는 0부터 시작하므로, 사람이 보기 편하도록 1을 더해 "내 앞에 n-1명" 형태로 보여준다.
            "WAITING" ->
                QueueStatusResponse(userId = userId, rank = result[1].toString().toLong() + 1, status = QueueStatus.WAITING, entryToken = null)
            else ->
                QueueStatusResponse(userId = userId, rank = null, status = QueueStatus.NOT_IN_QUEUE, entryToken = null)
        }
    }

    /** 대기열 이탈. 입장 허용 상태였다면 토큰과 역방향 키(entryTokenKey)까지 함께 삭제한다. */
    fun leave(concertId: Long, userId: Long) {
        redisTemplate.execute(
            QueueRedisScripts.LEAVE,
            listOf(
                QueueRedisKeys.waitKey(concertId),
                QueueRedisKeys.userTokenKey(concertId, userId),
                QueueRedisKeys.ACTIVE_CONCERTS_KEY,
            ),
            userId.toString(),
            QueueRedisKeys.ENTRY_TOKEN_KEY_PREFIX,
            concertId.toString(),
        )
    }

    /**
     * 각 콘서트 대기열의 상위 [QueueProperties.allowBatchSize]명을 입장 허용시킨다.
     * 원본 waiting-api는 전역 대기열 하나만 처리했지만, 여기서는 대기열이 생성된 모든 콘서트를
     * ACTIVE_CONCERTS_KEY로 추적해서 각각 처리한다. 대기열이 빈 콘서트는 스크립트가 Set에서 제거한다.
     */
    @Scheduled(fixedRateString = "\${queue.allow-interval-ms:3000}")
    fun allowNextBatch() {
        val activeConcertIds = redisTemplate.opsForSet().members(QueueRedisKeys.ACTIVE_CONCERTS_KEY) ?: emptySet()

        activeConcertIds.forEach { concertIdRaw ->
            val concertId = concertIdRaw.toLongOrNull()
            if (concertId == null) {
                // 잘못된 값이 들어가 있으면 매 주기마다 건너뛰지 않도록 정리한다.
                redisTemplate.opsForSet().remove(QueueRedisKeys.ACTIVE_CONCERTS_KEY, concertIdRaw)
                return@forEach
            }
            allowNextBatch(concertId)
        }
    }

    /** 한 콘서트의 상위 N명을 원자적으로 꺼내 입장 토큰을 발급하고, 허용된 userId 목록을 반환한다. */
    fun allowNextBatch(concertId: Long): List<Long> {
        val batchSize = queueProperties.allowBatchSize
        if (batchSize <= 0) return emptyList()

        val tokens = List(batchSize) { UUID.randomUUID().toString() }
        val ttlMillis = Duration.ofMinutes(queueProperties.entryTokenTtlMinutes).toMillis()

        val allowed = redisTemplate.execute(
            QueueRedisScripts.ALLOW_BATCH,
            listOf(QueueRedisKeys.waitKey(concertId), QueueRedisKeys.ACTIVE_CONCERTS_KEY),
            ttlMillis.toString(),
            QueueRedisKeys.userTokenKeyPrefix(concertId),
            QueueRedisKeys.ENTRY_TOKEN_KEY_PREFIX,
            concertId.toString(),
            *tokens.toTypedArray(),
        )

        return allowed.mapNotNull { it.toString().toLongOrNull() }
    }
}
