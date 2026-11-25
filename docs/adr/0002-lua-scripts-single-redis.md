# 대기열 상태 전이는 Lua 스크립트 하나씩으로 처리한다 (단일 Redis 전제)

처음 이식한 코드는 `popMin` 뒤에 토큰을 따로 저장해서, 그 사이에 장애가 나면 회원이 대기열과 허용 상태 양쪽에서 사라졌다. 등록도 "토큰 존재 확인 → `ZADD`"가 나뉘어 있어 확인 직후 입장 허용된 회원이 다시 대기열에 들어갈 수 있었다. 그래서 등록·상태 조회·입장 허용·이탈을 각각 `QueueRedisScripts`의 Lua 스크립트 하나(`REGISTER`, `STATUS`, `ALLOW_BATCH`, `LEAVE`)로 묶어 원자적으로 실행한다. 대기열이 비면 같은 스크립트 안에서 활성 콘서트 집합(`queue:active-concerts`)에서도 뺀다.

근거: b05430e, PR #2, `QueueServiceTest`(실제 Redis로 동시 등록·동시 입장 허용 검증).

## Consequences

- 스크립트가 `ARGV`로 받은 접두사로 키를 조합하므로 단일 Redis(또는 Sentinel)에서만 맞다. Redis Cluster로 옮기려면 키에 해시태그(`{concertId}`)를 넣는 등 키 설계를 바꿔야 한다.
- 이 원자성은 모킹으로는 검증되지 않는다. 대기열 로직을 바꾸면 실제 Redis에 붙는 `QueueServiceTest`로 확인한다.
