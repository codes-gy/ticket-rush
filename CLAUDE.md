# ticket-rush

Kotlin + Spring Boot(MVC) 콘서트 티켓팅 백엔드. `PLANNING.md` 로드맵 단계로 진행하며, 현재 2단계(대기열)까지 구현됐다. 대기열은 `waiting-api`(현 lineup)를 이식해 콘서트별로 확장한 것이다.

## 먼저 읽을 것

- `GLOSSARY.md`: 클래스·API·문서에 이름을 붙일 때. 특히 예매/예약, 좌석 선점, 입장 토큰처럼 코드·기획서에서 말이 갈리는 개념은 여기 정한 말을 쓴다.
- `docs/adr/`: 대기열 키 구조, Lua 스크립트, 입장 스케줄러를 바꾸려 할 때. 단일 Redis·단일 인스턴스 전제가 여기 적혀 있다.
- `PLANNING.md`: 다음 단계(좌석 선점, 결제)를 설계할 때. 단, 기술 스택 표는 제안일 뿐이고 실제 의존성은 `build.gradle.kts`가 기준이다.

## 빌드 · 테스트

- `./gradlew test` 전에 `docker compose up -d`로 PostgreSQL과 Redis를 띄운다. `contextLoads`가 둘 다 필요하고, `QueueServiceTest`는 실제 Redis의 DB 15(`REDIS_TEST_DATABASE`)를 테스트마다 비운다.
- JDK 21 툴체인, Spring Boot 3.3, Kotlin 1.9.
- 스키마는 `ddl-auto: update`로 엔티티에서 바로 만들어진다. 마이그레이션 도구가 아직 없으니 컬럼 이름을 바꾸면 로컬 DB에 옛 컬럼이 남는다.
- 회원가입은 항상 `USER`로 만든다. 관리자 API를 시험하려면 DB에서 `role`을 `ADMIN`으로 직접 바꾼다.

## 지켜야 할 것

- 대기열 상태를 바꾸는 Redis 작업은 `QueueRedisScripts`의 Lua 스크립트 하나로 끝내고, 키는 `QueueRedisKeys`로만 만든다. 입장 허용 상태는 입장 토큰 키의 TTL로만 판단한다(별도 허용 Set을 두지 않는다).
- 사용자 식별은 JWT에서 꺼낸 `@AuthenticationPrincipal userId: Long`로 한다. 요청 파라미터로 받은 userId를 믿지 않는다.
- 대기열 로직을 바꾸면 실제 Redis에 붙는 `QueueServiceTest`에 동시성 시나리오를 추가한다. 모킹으로는 Lua 원자성을 검증할 수 없다.

## 헷갈리기 쉬운 것

- Kotlin은 블록 주석 중첩을 허용한다. KDoc·주석 안에 `/api/admin/**` 같은 경로 패턴을 쓰면 `/*`가 새 주석을 열어 `Unclosed comment`로 컴파일이 깨진다(5aa3838). 경로 패턴은 주석 밖에 두거나 문장으로 풀어 쓴다.
- `GET /api/queue/{concertId}/rank`의 `rank`는 1부터 시작한다. lineup은 0부터("내 앞 인원")이므로 코드를 옮길 때 주의한다.
- 대기열 등록 API 경로는 `/enter`지만 서비스 메서드와 용어는 "등록"(`register`)이다.
- `Reservation`·`Payment`는 엔티티만 있고 서비스·API가 없다. `Seat.version`(낙관적 락)은 3단계에서 분산 락과 비교하려고 미리 넣어 둔 것이고, Redisson은 아직 의존성에 없다.

## 커밋 · 브랜치

- 기본 브랜치는 `main`이고 CI(`.github/workflows/ci.yaml`)는 `main` 대상 PR·push에서만 돈다.
- 커밋 메시지는 `feat:`/`fix:`/`test:`/`docs:`/`chore:` + 한국어 요약. 로드맵 단계를 끝내는 커밋은 `feat: N단계 - <내용>` 형식을 쓴다.
