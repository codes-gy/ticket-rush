# 입장 토큰의 TTL이 곧 입장 허용 상태다

대기열 설계를 가져온 `waiting-api`(현 lineup)는 입장 허용 여부를 별도 Redis Set 멤버십으로 판단했는데, Set 멤버는 개별 TTL을 가질 수 없어 토큰이 만료된 뒤에도 영원히 `ALLOWED`로 남았다. 이식하면서 허용 Set을 없애고 `queue:{concertId}:user-token:{userId}` 키(값 = 입장 토큰, TTL 5분) 하나를 "허용 상태가 아직 유효한가"의 유일한 근거로 삼았다. 역방향 `queue:entry-token:{token}` → `"{concertId}:{userId}"`도 같은 TTL로 함께 쓰고 지운다.

근거: 58005eb(2단계 대기열 모듈), `QueueRedisKeys` KDoc, README "대기열 동작 방식". lineup도 이후 같은 결정을 했다(lineup ADR 0002).

## Consequences

- 입장 허용 상태를 따로 정리하는 작업이 없다. 대신 허용 기간을 늘리려면 토큰 TTL(`queue.entry-token-ttl-minutes`)을 늘리는 수밖에 없다.
- 좌석 선점 API(3단계)는 역방향 키로 토큰만 보고 콘서트와 회원을 확인할 수 있어야 한다. 역방향 키를 빼먹고 토큰만 지우면 TTL 동안 토큰이 계속 유효하다(b05430e에서 이탈 시 이 누락을 고쳤다).
