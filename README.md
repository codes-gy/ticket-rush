# 🎫 ticket-rush

실시간 콘서트 티켓팅 시스템 — 백엔드 채용 포트폴리오용 프로젝트입니다.

전체 기획 배경, 핵심 시나리오, 기술적 과제와 해결 전략, 개발 로드맵은 [`PLANNING.md`](./PLANNING.md)를 참고해주세요.

---

## 기술 스택 (0단계 기준)

- **Language/Framework**: Kotlin, Spring Boot 3.3, Spring Data JPA
- **Database**: PostgreSQL
- **Cache**: Redis
- **Build**: Gradle (Kotlin DSL)

> Redisson(분산 락), JWT 인증 등은 아직 사용하는 코드가 없어 의존성에 포함하지 않았습니다.
> `PLANNING.md`의 해당 단계(좌석 선점, 회원 인증)를 구현할 때 추가할 예정입니다.

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
├── common/                     # BaseTimeEntity 등 공용 컴포넌트
├── config/                     # Redis 등 설정
├── user/domain/                # 회원
├── concert/domain/              # 콘서트(공연)
├── seat/domain/                 # 좌석
├── reservation/domain/          # 예약
├── payment/domain/              # 결제
└── health/                     # 헬스체크
```

도메인별 패키지는 추후 `domain`(엔티티) 외에 `application`(서비스), `presentation`(컨트롤러),
`infrastructure`(레포지토리 구현체) 하위 패키지로 확장할 예정입니다.

---

## 현재 진행 상태

`PLANNING.md` 로드맵 기준 **0단계(프로젝트 초기 설정) 완료** — 기본 골격, 엔티티, 인프라 설정까지
반영되어 있고, 실제 도메인 로직(대기열, 좌석 선점, 결제)은 아직 구현 전입니다.
