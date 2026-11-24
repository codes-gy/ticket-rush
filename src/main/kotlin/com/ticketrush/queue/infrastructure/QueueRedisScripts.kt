package com.ticketrush.queue.infrastructure

import org.springframework.data.redis.core.script.DefaultRedisScript

/**
 * 대기열 상태 전이를 원자적으로 처리하는 Lua 스크립트 모음.
 *
 * 여러 Redis 명령을 애플리케이션에서 순서대로 호출하면 명령 사이에 다른 요청/스케줄러가 끼어들 수 있다.
 * (예: popMin 직후 토큰 저장 전에 서버가 죽으면 사용자가 대기열에서도, 허용 목록에서도 사라진다.)
 * Lua 스크립트는 Redis에서 하나의 명령처럼 원자적으로 실행되므로 이런 중간 상태가 외부에 보이지 않는다.
 *
 * 참고: 스크립트 안에서 키 접두사(ARGV)로 키를 조합하므로 Redis Cluster가 아닌 단일 Redis(또는
 * Sentinel) 환경을 전제로 한다. Cluster로 전환할 때는 키에 해시태그({concertId})를 넣어야 한다.
 */
object QueueRedisScripts {
    /**
     * 대기열 등록.
     * KEYS[1]=userTokenKey, KEYS[2]=waitKey, KEYS[3]=ACTIVE_CONCERTS_KEY
     * ARGV[1]=userId, ARGV[2]=score(진입 시각), ARGV[3]=concertId
     * 반환: 1=등록(또는 이미 대기 중), 0=이미 입장 허용 상태라 등록하지 않음
     */
    val REGISTER: DefaultRedisScript<Long> = DefaultRedisScript(
        """
        if redis.call('EXISTS', KEYS[1]) == 1 then
          return 0
        end
        redis.call('ZADD', KEYS[2], 'NX', ARGV[2], ARGV[1])
        redis.call('SADD', KEYS[3], ARGV[3])
        return 1
        """.trimIndent(),
        Long::class.java,
    )

    /**
     * 현재 상태 조회. 토큰 조회와 순번 조회 사이에 스케줄러가 끼어들어 순간적으로
     * NOT_IN_QUEUE가 보이는 일이 없도록 한 번에 읽는다.
     * KEYS[1]=userTokenKey, KEYS[2]=waitKey / ARGV[1]=userId
     * 반환: {"ALLOWED", token} | {"WAITING", rank(0부터)} | {"NOT_IN_QUEUE"}
     */
    @Suppress("UNCHECKED_CAST")
    val STATUS: DefaultRedisScript<List<Any>> = DefaultRedisScript(
        """
        local token = redis.call('GET', KEYS[1])
        if token then
          return {'ALLOWED', token}
        end
        local rank = redis.call('ZRANK', KEYS[2], ARGV[1])
        if rank then
          return {'WAITING', tostring(rank)}
        end
        return {'NOT_IN_QUEUE'}
        """.trimIndent(),
        List::class.java as Class<List<Any>>,
    )

    /**
     * 대기열 상위 N명을 꺼내 입장 토큰을 발급한다 (ZPOPMIN + 토큰 저장을 하나로 묶음).
     * 대기열이 비면 ACTIVE_CONCERTS_KEY에서 해당 콘서트를 제거한다.
     * KEYS[1]=waitKey, KEYS[2]=ACTIVE_CONCERTS_KEY
     * ARGV[1]=토큰 TTL(ms), ARGV[2]=userTokenKey 접두사, ARGV[3]=entryTokenKey 접두사, ARGV[4]=concertId,
     * ARGV[5..]=애플리케이션에서 미리 생성한 토큰(UUID). 토큰 개수만큼 꺼낸다.
     * 반환: 입장 허용된 userId 목록(순번 순)
     */
    @Suppress("UNCHECKED_CAST")
    val ALLOW_BATCH: DefaultRedisScript<List<Any>> = DefaultRedisScript(
        """
        local count = #ARGV - 4
        local allowed = {}
        if count > 0 then
          local popped = redis.call('ZPOPMIN', KEYS[1], count)
          local i = 0
          for idx = 1, #popped, 2 do
            local userId = popped[idx]
            i = i + 1
            local token = ARGV[4 + i]
            local userTokenKey = ARGV[2] .. userId
            local oldToken = redis.call('GET', userTokenKey)
            if oldToken then
              redis.call('DEL', ARGV[3] .. oldToken)
            end
            redis.call('SET', userTokenKey, token, 'PX', ARGV[1])
            redis.call('SET', ARGV[3] .. token, ARGV[4] .. ':' .. userId, 'PX', ARGV[1])
            allowed[#allowed + 1] = userId
          end
        end
        if redis.call('ZCARD', KEYS[1]) == 0 then
          redis.call('SREM', KEYS[2], ARGV[4])
        end
        return allowed
        """.trimIndent(),
        List::class.java as Class<List<Any>>,
    )

    /**
     * 대기열 이탈. 대기 중이면 대기열에서 빼고, 입장 허용 상태면 토큰과 역방향 키를 함께 삭제한다.
     * KEYS[1]=waitKey, KEYS[2]=userTokenKey, KEYS[3]=ACTIVE_CONCERTS_KEY
     * ARGV[1]=userId, ARGV[2]=entryTokenKey 접두사, ARGV[3]=concertId
     */
    val LEAVE: DefaultRedisScript<Long> = DefaultRedisScript(
        """
        redis.call('ZREM', KEYS[1], ARGV[1])
        local token = redis.call('GET', KEYS[2])
        if token then
          redis.call('DEL', ARGV[2] .. token)
        end
        redis.call('DEL', KEYS[2])
        if redis.call('ZCARD', KEYS[1]) == 0 then
          redis.call('SREM', KEYS[3], ARGV[3])
        end
        return 1
        """.trimIndent(),
        Long::class.java,
    )
}
