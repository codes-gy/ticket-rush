package com.ticketrush.queue.application

import com.ticketrush.concert.application.ConcertService
import com.ticketrush.queue.infrastructure.QueueRedisKeys
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
    /** 대기열 등록. 이미 대기 중이거나 이미 허용된 사용자가 다시 호출해도 상태만 그대로 반환한다 (멱등). */
    fun register(concertId: Long, userId: Long): QueueStatusResponse {
        concertService.getConcertOrThrow(concertId) // 존재하지 않는 콘서트면 여기서 404

        val alreadyAllowed = redisTemplate.hasKey(QueueRedisKeys.userTokenKey(concertId, userId))
        if (alreadyAllowed != true) {
            redisTemplate.opsForZSet()
                .addIfAbsent(QueueRedisKeys.waitKey(concertId), userId.toString(), System.currentTimeMillis().toDouble())
            redisTemplate.opsForSet().add(QueueRedisKeys.ACTIVE_CONCERTS_KEY, concertId.toString())
        }
        return getStatus(concertId, userId)
    }

    fun getStatus(concertId: Long, userId: Long): QueueStatusResponse {
        val token = redisTemplate.opsForValue().get(QueueRedisKeys.userTokenKey(concertId, userId))
        if (token != null) {
            return QueueStatusResponse(userId = userId, rank = 0, status = QueueStatus.ALLOWED, entryToken = token)
        }

        val rank = redisTemplate.opsForZSet().rank(QueueRedisKeys.waitKey(concertId), userId.toString())
        return if (rank != null) {
            // Redis rank는 0부터 시작하므로, 사람이 보기 편하도록 1을 더해 "내 앞에 n-1명" 형태로 보여준다.
            QueueStatusResponse(userId = userId, rank = rank + 1, status = QueueStatus.WAITING, entryToken = null)
        } else {
            QueueStatusResponse(userId = userId, rank = null, status = QueueStatus.NOT_IN_QUEUE, entryToken = null)
        }
    }

    fun leave(concertId: Long, userId: Long) {
        redisTemplate.opsForZSet().remove(QueueRedisKeys.waitKey(concertId), userId.toString())
        redisTemplate.delete(QueueRedisKeys.userTokenKey(concertId, userId))
    }

    /**
     * 각 콘서트 대기열의 상위 [QueueProperties.allowBatchSize]명을 입장 허용시킨다.
     * 원본 waiting-api는 전역 대기열 하나만 처리했지만, 여기서는 대기열이 생성된 모든 콘서트를
     * ACTIVE_CONCERTS_KEY로 추적해서 각각 처리한다.
     */
    @Scheduled(fixedRateString = "\${queue.allow-interval-ms:3000}")
    fun allowNextBatch() {
        val activeConcertIds = redisTemplate.opsForSet().members(QueueRedisKeys.ACTIVE_CONCERTS_KEY) ?: emptySet()

        activeConcertIds.forEach { concertIdRaw ->
            val concertId = concertIdRaw.toLongOrNull() ?: return@forEach
            val topUsers = redisTemplate.opsForZSet()
                .popMin(QueueRedisKeys.waitKey(concertId), queueProperties.allowBatchSize.toLong())
                ?: emptySet()

            topUsers.forEach { tuple ->
                val userId = tuple.value?.toLongOrNull() ?: return@forEach
                issueEntryToken(concertId, userId)
            }
        }
    }

    private fun issueEntryToken(concertId: Long, userId: Long) {
        val token = UUID.randomUUID().toString()
        val ttl = Duration.ofMinutes(queueProperties.entryTokenTtlMinutes)

        redisTemplate.opsForValue().set(QueueRedisKeys.userTokenKey(concertId, userId), token, ttl)
        redisTemplate.opsForValue().set(QueueRedisKeys.entryTokenKey(token), "$concertId:$userId", ttl)
    }
}
