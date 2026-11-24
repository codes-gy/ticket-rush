package com.ticketrush.queue.application

import com.ticketrush.common.exception.NotFoundException
import com.ticketrush.concert.application.ConcertService
import com.ticketrush.queue.infrastructure.QueueRedisKeys
import com.ticketrush.queue.presentation.QueueStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * QueueService를 실제 Redis에 대고 검증한다 (Lua 스크립트의 원자성까지 확인하려면 모킹으로는 부족하다).
 *
 * - 로컬: `docker compose up -d`로 Redis를 띄운 뒤 실행. CI: GitHub Actions의 redis 서비스 컨테이너 사용.
 * - 애플리케이션(DB 0)과 섞이지 않도록 별도 DB 인덱스(기본 15)를 사용하고, 테스트마다 비운다.
 *   (같은 JVM에서 뜬 스프링 컨텍스트의 스케줄러가 DB 0의 대기열을 처리하더라도 이 테스트에는 영향이 없다.)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueueServiceTest {
    private val concertId = 1L

    private lateinit var connectionFactory: LettuceConnectionFactory
    private lateinit var redisTemplate: RedisTemplate<String, String>
    private lateinit var concertService: ConcertService
    private lateinit var properties: QueueProperties
    private lateinit var queueService: QueueService

    @BeforeAll
    fun setUpRedis() {
        val config = RedisStandaloneConfiguration(
            System.getenv("REDIS_HOST") ?: "localhost",
            (System.getenv("REDIS_PORT") ?: "6379").toInt(),
        )
        config.database = (System.getenv("REDIS_TEST_DATABASE") ?: "15").toInt()
        connectionFactory = LettuceConnectionFactory(config)
        connectionFactory.afterPropertiesSet()
        connectionFactory.start()

        redisTemplate = RedisTemplate<String, String>().apply {
            this.connectionFactory = this@QueueServiceTest.connectionFactory
            keySerializer = StringRedisSerializer()
            valueSerializer = StringRedisSerializer()
            afterPropertiesSet()
        }
    }

    @AfterAll
    fun tearDownRedis() {
        flushTestDatabase()
        connectionFactory.destroy()
    }

    @BeforeEach
    fun setUp() {
        flushTestDatabase()
        concertService = mock(ConcertService::class.java)
        properties = QueueProperties().apply {
            allowBatchSize = 2
            entryTokenTtlMinutes = 5
        }
        queueService = QueueService(redisTemplate, properties, concertService)
    }

    private fun flushTestDatabase() {
        redisTemplate.execute { connection -> connection.serverCommands().flushDb() }
    }

    @Test
    fun `등록 순서대로 순번이 매겨지고, 같은 사용자가 다시 등록해도 순번이 바뀌지 않는다`() {
        assertThat(queueService.register(concertId, 10).rank).isEqualTo(1)
        assertThat(queueService.register(concertId, 20).rank).isEqualTo(2)

        val again = queueService.register(concertId, 10)

        assertThat(again.status).isEqualTo(QueueStatus.WAITING)
        assertThat(again.rank).isEqualTo(1)
        assertThat(redisTemplate.opsForZSet().size(QueueRedisKeys.waitKey(concertId))).isEqualTo(2)
        assertThat(redisTemplate.opsForSet().isMember(QueueRedisKeys.ACTIVE_CONCERTS_KEY, "$concertId")).isTrue()
    }

    @Test
    fun `존재하지 않는 콘서트에는 등록되지 않는다`() {
        doThrow(NotFoundException("없음")).`when`(concertService).getConcertOrThrow(999)

        assertThatThrownBy { queueService.register(999, 10) }.isInstanceOf(NotFoundException::class.java)
        assertThat(redisTemplate.hasKey(QueueRedisKeys.waitKey(999))).isFalse()
    }

    @Test
    fun `스케줄러는 상위 N명에게만 토큰을 발급하고, 토큰과 역방향 키에 TTL을 건다`() {
        listOf(10L, 20L, 30L).forEach { queueService.register(concertId, it) }

        queueService.allowNextBatch()

        val first = queueService.getStatus(concertId, 10)
        val second = queueService.getStatus(concertId, 20)
        val third = queueService.getStatus(concertId, 30)

        assertThat(first.status).isEqualTo(QueueStatus.ALLOWED)
        assertThat(second.status).isEqualTo(QueueStatus.ALLOWED)
        assertThat(third.status).isEqualTo(QueueStatus.WAITING)
        assertThat(third.rank).isEqualTo(1)
        assertThat(first.entryToken).isNotEqualTo(second.entryToken)

        val reverseKey = QueueRedisKeys.entryTokenKey(first.entryToken!!)
        assertThat(redisTemplate.opsForValue().get(reverseKey)).isEqualTo("$concertId:10")
        assertThat(redisTemplate.getExpire(reverseKey, TimeUnit.SECONDS)).isBetween(1L, 300L)
        assertThat(redisTemplate.getExpire(QueueRedisKeys.userTokenKey(concertId, 10), TimeUnit.SECONDS))
            .isBetween(1L, 300L)
    }

    @Test
    fun `이미 입장 허용된 사용자가 다시 등록해도 대기열에 다시 들어가지 않는다`() {
        queueService.register(concertId, 10)
        queueService.allowNextBatch()
        val token = queueService.getStatus(concertId, 10).entryToken

        val again = queueService.register(concertId, 10)

        assertThat(again.status).isEqualTo(QueueStatus.ALLOWED)
        assertThat(again.entryToken).isEqualTo(token)
        assertThat(redisTemplate.opsForZSet().rank(QueueRedisKeys.waitKey(concertId), "10")).isNull()
    }

    @Test
    fun `입장 허용 상태에서 이탈하면 토큰과 역방향 키가 모두 삭제된다`() {
        queueService.register(concertId, 10)
        queueService.allowNextBatch()
        val token = queueService.getStatus(concertId, 10).entryToken!!

        queueService.leave(concertId, 10)

        assertThat(queueService.getStatus(concertId, 10).status).isEqualTo(QueueStatus.NOT_IN_QUEUE)
        assertThat(redisTemplate.hasKey(QueueRedisKeys.userTokenKey(concertId, 10))).isFalse()
        assertThat(redisTemplate.hasKey(QueueRedisKeys.entryTokenKey(token))).isFalse()
    }

    @Test
    fun `대기열이 비면 활성 콘서트 목록에서 제거된다`() {
        queueService.register(concertId, 10)
        queueService.register(2, 10)

        queueService.allowNextBatch() // 콘서트 1, 2 모두 1명씩이라 한 번에 비워진다
        assertThat(redisTemplate.opsForSet().members(QueueRedisKeys.ACTIVE_CONCERTS_KEY)).isEmpty()

        queueService.register(3, 10)
        queueService.leave(3, 10) // 마지막 대기자가 이탈해도 제거된다
        assertThat(redisTemplate.opsForSet().members(QueueRedisKeys.ACTIVE_CONCERTS_KEY)).isEmpty()
    }

    @Test
    fun `동시에 등록하고 동시에 입장 허용해도 순번이 중복되지 않고 토큰은 1인당 하나만 발급된다`() {
        val users = (1L..60L).toList()
        val pool = Executors.newFixedThreadPool(16)
        try {
            runConcurrently(pool, users.map { userId -> Callable { queueService.register(concertId, userId) } })

            val ranks = users.map { queueService.getStatus(concertId, it).rank }
            assertThat(ranks).containsExactlyInAnyOrderElementsOf((1L..60L).toList())

            // 배치 크기 2 x 40번 = 최대 80명 허용 가능 -> 60명 전원이 정확히 한 번씩 허용되어야 한다
            val allowed = runConcurrently(pool, List(40) { Callable { queueService.allowNextBatch(concertId) } })
                .flatten()

            assertThat(allowed).containsExactlyInAnyOrderElementsOf(users)
            val tokens = users.map { queueService.getStatus(concertId, it) }
            assertThat(tokens).allMatch { it.status == QueueStatus.ALLOWED }
            assertThat(tokens.map { it.entryToken }.toSet()).hasSize(60)
            assertThat(redisTemplate.keys(QueueRedisKeys.entryTokenKey("*"))).hasSize(60)
            assertThat(redisTemplate.opsForSet().members(QueueRedisKeys.ACTIVE_CONCERTS_KEY)).isEmpty()
        } finally {
            pool.shutdownNow()
        }
    }

    private fun <T> runConcurrently(pool: java.util.concurrent.ExecutorService, tasks: List<Callable<T>>): List<T> {
        val start = CountDownLatch(1)
        val futures = tasks.map { task -> pool.submit(Callable { start.await(); task.call() }) }
        start.countDown()
        return futures.map { it.get(10, TimeUnit.SECONDS) }
    }
}
