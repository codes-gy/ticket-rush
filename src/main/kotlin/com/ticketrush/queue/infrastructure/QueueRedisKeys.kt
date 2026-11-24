package com.ticketrush.queue.infrastructure

/**
 * waiting-api(Redis ZSET 기반 대기열)의 키 설계를 콘서트별로 확장한 것이다.
 *
 * 원본 설계와 달리, "입장 허용" 상태를 별도의 만료되지 않는 Set으로 관리하지 않는다.
 * (원본은 ALLOW_KEY Set 멤버십을 "허용됨"의 근거로 삼았는데, Set 멤버는 개별 TTL을 가질 수 없어서
 * 입장 토큰이 만료된 뒤에도 영원히 ALLOWED 상태로 남는 문제가 있었다.) 대신 userTokenKey 자체의
 * TTL을 "허용 상태가 아직 유효한지"의 유일한 기준으로 사용한다 — 토큰이 사라지면 곧 허용 상태도
 * 함께 사라진다.
 */
object QueueRedisKeys {
    fun waitKey(concertId: Long): String = "queue:$concertId:wait"

    /** 유저별 발급된 입장 토큰. 값 자체가 토큰 문자열이며, TTL이 곧 "허용 상태 유효 기간"이다. */
    fun userTokenKey(concertId: Long, userId: Long): String = userTokenKeyPrefix(concertId) + userId

    /** Lua 스크립트 안에서 userId를 붙여 userTokenKey를 만들 때 사용하는 접두사. */
    fun userTokenKeyPrefix(concertId: Long): String = "queue:$concertId:user-token:"

    /** 토큰 -> "concertId:userId" 역방향 조회. 좌석 선점 API(3단계)가 토큰만으로 사용자를 검증할 때 사용한다. */
    fun entryTokenKey(token: String): String = ENTRY_TOKEN_KEY_PREFIX + token

    /** Lua 스크립트 안에서 토큰을 붙여 entryTokenKey를 만들 때 사용하는 접두사. */
    const val ENTRY_TOKEN_KEY_PREFIX = "queue:entry-token:"

    /**
     * 스케줄러가 매번 전체 콘서트를 스캔하지 않도록, 대기열이 생성된 콘서트 ID만 추적한다.
     * 대기열이 비면(스케줄러가 마지막 인원을 꺼내거나 마지막 인원이 이탈하면) 이 Set에서 제거된다.
     */
    const val ACTIVE_CONCERTS_KEY = "queue:active-concerts"
}
