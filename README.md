# 🎫 ticket-rush

실시간 콘서트 티켓팅 시스템 — 백엔드 채용 포트폴리오용 프로젝트입니다.

전체 기획 배경, 핵심 시나리오, 기술적 과제와 해결 전략, 개발 로드맵은 [`PLANNING.md`](./PLANNING.md)를 참고해주세요.

---

## 기술 스택

- **Language/Framework**: Kotlin, Spring Boot 3.3, Spring Data JPA, Spring Security
- **Database**: PostgreSQL
- **Cache**: Redis
- **Auth**: JWT (jjwt), BCrypt
- **Build**: Gradle (Kotlin DSL)

> Redisson(분산 락)은 아직 사용하는 코드가 없어 의존성에 포함하지 않았습니다.
> `PLANNING.md`의 좌석 선점 단계를 구현할 때 추가할 예정입니다.

---

## 로컬 개발 환경 설정

### 1. 인프라 실행 (PostgreSQL + Redis)
```bash
docker compose up -d
```

### 2. Gradle Wrapper 생성 (최초 1회)
이 저장소에는 아직 Gradle Wrapper(`gradlew`)가 커밋되어 있지 않습니다. 로컬에 Gradle이 설치되어
있다면 아래 명령으로 한 번만 생성해주세요 (이후 `gradlew`, `gradlew.bat`, `gradle/wrapper/` 가
생성되며, 커밋해두면 다음부터는 이 과정이 필요 없습니다).
```bash
gradle wrapper --gradle-version 8.10
```
IntelliJ IDEA로 프로젝트를 열면 이 과정 없이도 IDE가 자동으로 처리해줍니다.

### 3. 애플리케이션 실행
```bash
./gradlew bootRun
```
기동 후 `http://localhost:8080/health` 로 정상 기동 여부를 확인할 수 있습니다.

### 4. 테스트 실행
```bash
./gradlew test
```

---

## 프로젝트 구조

```
src/main/kotlin/com/ticketrush/
├── TicketRushApplication.kt   # 진입점
├── common/
│   ├── BaseTimeEntity.kt       # 생성/수정 시각 공통화
│   ├── security/                # JWT 발급/검증, Spring Security 설정
│   └── exception/                # 커스텀 예외 + 전역 예외 핸들러
├── config/                     # Redis 등 설정
├── user/                        # 회원/인증 (domain, application, presentation, infrastructure)
├── concert/                     # 콘서트(공연) (domain, application, presentation, infrastructure)
├── seat/                        # 좌석 (domain, application, presentation, infrastructure)
├── queue/                       # 대기열 (application, presentation, infrastructure — DB 미영속, Redis만 사용)
├── reservation/domain/          # 예약 (엔티티만 존재, 아직 기능 미구현)
├── payment/domain/              # 결제 (엔티티만 존재, 아직 기능 미구현)
└── health/                     # 헬스체크
```

각 도메인 패키지는 `domain`(엔티티), `application`(서비스), `presentation`(컨트롤러/DTO),
`infrastructure`(레포지토리) 4개 하위 패키지로 구성됩니다. `reservation`/`payment`는 아직
엔티티만 있고 나머지 계층은 해당 기능을 구현하는 단계에서 추가됩니다.

---

## 현재 진행 상태

`PLANNING.md` 로드맵 기준 **2단계(대기열 모듈) 완료**

- 회원가입/로그인 (JWT 발급, BCrypt 비밀번호 해시)
- 콘서트 등록(관리자)/목록·상세 조회(공개)
- 좌석 일괄 등록(관리자)/조회(공개)
- `/api/admin/**`는 `ROLE_ADMIN`만 접근 가능하도록 Spring Security로 보호
- **콘서트별 대기열 진입/순번 조회/이탈** (`waiting-api`의 Redis ZSET 설계를 포팅 및 확장)

아직 좌석 선점(동시성 제어)과 결제는 구현 전입니다.

### API 목록

| Method | Endpoint | 인증 |
| :--- | :--- | :--- |
| POST | `/api/auth/signup` | 불필요 |
| POST | `/api/auth/login` | 불필요 |
| GET | `/api/concerts` | 불필요 |
| GET | `/api/concerts/{concertId}` | 불필요 |
| GET | `/api/concerts/{concertId}/seats` | 불필요 |
| POST | `/api/queue/{concertId}/enter` | 로그인 필요 |
| GET | `/api/queue/{concertId}/rank` | 로그인 필요 |
| DELETE | `/api/queue/{concertId}` | 로그인 필요 |
| POST | `/api/admin/concerts` | ROLE_ADMIN 필요 |
| POST | `/api/admin/concerts/{concertId}/seats` | ROLE_ADMIN 필요 |

> ⚠️ 현재 API로는 ADMIN 계정을 만들 수 없습니다 (회원가입은 항상 USER로 생성됩니다).
> 관리자 기능을 테스트하려면 회원가입 후 DB에서 직접 `role`을 `ADMIN`으로 바꿔주세요.
> (`UPDATE users SET role = 'ADMIN' WHERE email = '...';`) 최초 관리자 시딩 스크립트는
> 추후 단계에서 정리할 예정입니다.

### 대기열 동작 방식

1. `POST /api/queue/{concertId}/enter` — Redis ZSET(`queue:{concertId}:wait`)에 등록 (이미 대기
   중이거나 이미 허용된 상태면 그대로 현재 상태만 반환, 순번을 리셋하지 않음)
2. 3초마다(`queue.allow-interval-ms`) 스케줄러가 각 콘서트 대기열 상위 5명(`queue.allow-batch-size`)을
   꺼내서 입장 토큰을 발급 (5분 TTL, `queue.entry-token-ttl-minutes`)
3. `GET /api/queue/{concertId}/rank` — 대기 중이면 순번을, 허용됐으면 입장 토큰을 반환
4. 입장 토큰은 3단계(좌석 선점)에서 좌석 선택 화면 접근 권한 검증에 사용될 예정

`waiting-api` 원본 설계와 달리, "입장 허용" 상태를 별도의 Set으로 관리하지 않고 **입장 토큰
자체의 Redis TTL을 허용 상태의 유일한 근거로 사용**합니다. 원본은 허용 여부를 Set 멤버십으로
판단했는데, Set 멤버는 개별 TTL을 가질 수 없어서 토큰이 만료된 뒤에도 영원히 "허용됨" 상태로
남는 문제가 있었습니다.
