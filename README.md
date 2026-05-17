# common

Spring Boot 기반 마이크로서비스 프로젝트에서 공통으로 사용하는 인프라 라이브러리입니다.  
보안, 예외 처리, 응답 형식, 로깅, JPA, Kafka, Feign 등의 설정을 자동으로 제공합니다.

---

## 제공 기능

| 분류 | 내용 |
|---|---|
| **보안** | JWT 기반 인증 필터(`LoginFilter`), 인증 실패 처리(`CustomAuthenticationEntryPoint`), 인가 실패 처리(`CustomAccessDeniedHandler`), `SecurityConfigImpl` 기본 설정 |
| **예외 처리** | `CustomException`, `GlobalExceptionAdviceImpl` 전역 예외 핸들러, `ErrorConfigProperties` YAML 기반 에러 코드 관리 |
| **공통 응답** | `CommonResponse<T>` 표준 응답 포맷, `CommonResponseAdvice` 자동 래핑 |
| **로깅** | `MdcLoggingFilter` MDC 기반 요청 추적 (traceId 자동 주입) |
| **유틸리티** | `SecurityUtil` (현재 로그인 사용자 조회), `JsonUtil`, `MdcTaskDecorator` |
| **도메인** | `BaseEntity` (JPA Auditing 공통 엔티티) |
| **인프라 설정** | `FeignConfig`, `JPAConfig`, `JsonConfig`, `KafkaConfig` 자동 구성 |

---

## 사전 준비 — GitHub Access Token 발급

GitHub Packages는 공개 저장소라도 **인증 없이 패키지를 다운로드할 수 없습니다.**  
아래 절차에 따라 Personal Access Token(PAT)을 발급받아야 합니다.

### 토큰 발급 방법

1. GitHub에 로그인 후 우측 상단 프로필 사진 클릭
2. **Settings** 이동
3. 좌측 메뉴 최하단 **Developer settings** 클릭
4. **Personal access tokens → Tokens (classic)** 클릭
5. **Generate new token → Generate new token (classic)** 클릭
6. 설정 항목 입력:
    - **Note**: 토큰 용도 식별명 입력 (예: `my-project-common`)
    - **Expiration**: 만료 기간 설정
    - **Scopes**: 아래 항목 체크
        - `read:packages` ✅
        - `repo` ✅ (private 저장소 접근 시 필수)
7. **Generate token** 클릭 후 발급된 토큰 값(`ghp_...`) 복사 — **페이지를 닫으면 다시 볼 수 없으므로 반드시 즉시 저장**

---

## 토큰 설정 방법

토큰을 `build.gradle`에 직접 입력하지 않고, **로컬 환경 설정 파일에 저장**합니다.

### macOS / Linux

터미널에서 아래 명령어로 Gradle 전역 설정 파일을 엽니다.

```bash
mkdir -p ~/.gradle
nano ~/.gradle/gradle.properties
```

아래 내용을 추가하고 저장합니다 (`Ctrl+O` → Enter → `Ctrl+X`):

```properties
gpr.user=GitHub아이디
gpr.token=ghp_발급받은토큰값
```

설정 파일이 올바르게 저장되었는지 확인:

```bash
cat ~/.gradle/gradle.properties
```

### Windows

**방법 1. 파일 탐색기로 설정**

1. `Win + R` 키를 누르고 `%USERPROFILE%\.gradle` 입력 후 Enter
2. 해당 폴더 안에 `gradle.properties` 파일이 없으면 새로 만들기 → 텍스트 문서 → 파일명을 `gradle.properties`로 저장
3. 파일을 메모장으로 열고 아래 내용 입력 후 저장:

```properties
gpr.user=GitHub아이디
gpr.token=ghp_발급받은토큰값
```

**방법 2. PowerShell로 설정**

```powershell
# 폴더가 없으면 생성
New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.gradle"

# gradle.properties 파일 생성 및 내용 추가
Add-Content "$env:USERPROFILE\.gradle\gradle.properties" "gpr.user=GitHub아이디"
Add-Content "$env:USERPROFILE\.gradle\gradle.properties" "gpr.token=ghp_발급받은토큰값"
```

---

## 프로젝트 build.gradle 설정

### 1. 저장소 추가

`repositories` 블록에 GitHub Packages 저장소를 추가합니다.

```groovy
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/89-49/common")
        credentials {
            username = findProperty('gpr.user') ?: System.getenv('GPR_USER')
            password = findProperty('gpr.token') ?: System.getenv('GPR_TOKEN')
        }
    }
}
```

### 2. 의존성 추가

`dependencies` 블록에 아래를 추가합니다.

```groovy
dependencies {
    implementation 'org.pgsg:common:0.0.1-SNAPSHOT'
}
```

### 3. 설정 완료 확인

의존성 동기화 후 아래 클래스들이 자동으로 임포트 가능하다면 설정이 완료된 것입니다.

```java
import org.pgsg.common.response.CommonResponse;
import org.pgsg.common.exception.CustomException;
import org.pgsg.common.util.SecurityUtil;
```

---

## 커스터마이징

일부 기능은 직접 Bean을 등록하면 공통 모듈의 기본 구현을 **덮어쓸 수 있습니다.**

### Security 설정 커스터마이징

`SecurityConfig` 인터페이스를 구현하는 Bean을 등록하면 기본 보안 설정 대신 적용됩니다.

```java
@Configuration
public class MySecurityConfig implements SecurityConfig {
    // 프로젝트에 맞는 Security 설정 작성
}
```

### 전역 예외 처리 커스터마이징

`GlobalExceptionAdvice` 인터페이스를 구현하는 Bean을 등록하면 기본 예외 핸들러 대신 적용됩니다.

```java
@RestControllerAdvice
public class MyExceptionAdvice implements GlobalExceptionAdvice {
    // 프로젝트에 맞는 예외 처리 작성
}
```

### 에러 코드 추가

`application.yml`에 `error.configs` 하위에 에러를 추가하면 `ErrorConfigProperties`가 자동으로 읽어옵니다.

```yaml
error:
  configs:
    MyCustomException:
      code: "E001"
      message: "커스텀 에러 메시지입니다."
      status: 400
```

`CustomException`을 발생시킬 때 YAML의 key를 그대로 사용합니다.

```java
throw new CustomException("MyCustomException");
// 또는 특정 필드와 함께
throw new CustomException("MyCustomException", "fieldName");
```

---

## 공통 응답 형식

모든 API 응답은 `CommonResponse<T>` 형식으로 자동 래핑됩니다.

```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": { },
  "traceId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

컨트롤러에서 직접 사용하는 것도 가능합니다.

```java
return ResponseEntity.ok(CommonResponse.success(data));
return ResponseEntity.ok(CommonResponse.success("처리 완료", data));
```

---

## 현재 로그인 사용자 조회

```java
// Optional 방식
Optional<UUID> userId = SecurityUtil.getCurrentUserId();

// 인증 필수 (미인증 시 CustomException 발생)
UUID userId = SecurityUtil.getCurrentUserIdOrThrow();

// 사용자명 조회
Optional<String> username = SecurityUtil.getCurrentUsername();
```

---

## 트러블슈팅

**401 Unauthorized 오류 발생 시**

`gradle.properties` 파일 경로와 키 이름이 정확한지 확인합니다.

```bash
# macOS/Linux
cat ~/.gradle/gradle.properties

# Windows PowerShell
Get-Content "$env:USERPROFILE\.gradle\gradle.properties"
```

출력 내용에 `gpr.user`와 `gpr.token`이 올바르게 설정되어 있어야 합니다. 값이 없거나 오타가 있으면 토큰 발급 절차를 다시 진행하세요.

**토큰 만료 시**

GitHub에서 새 토큰을 재발급한 후 `gradle.properties`의 `gpr.token` 값을 교체합니다.





# Event Messaging

## 사전 준비

### 환경 요구사항

- Java 21, Spring Boot 3
- Kafka, RDB (Outbox / Inbox 테이블 저장용)

### DB 테이블

`Outbox`, `Inbox` 테이블이 자동으로 생성됩니다.  
(`spring.jpa.hibernate.ddl-auto: update` 또는 `create` 설정 필요)

---

## 동작 흐름

```
발행 측                              수신 측
────────────────────────────        ────────────────────────────
@Transactional 내 Events.trigger()  @KafkaListener 메서드
       ↓                                   ↓
Outbox 테이블에 PENDING 저장        @IdempotentConsumer AOP 동작
       ↓                                   ↓
트랜잭션 커밋 후 Kafka 즉시 전송    message_id로 중복 체크 (Inbox)
       ↓                                   ↓
실패 시 5초 주기 재시도 스케줄러     정상 처리 후 Inbox PROCESSED 상태로 변경
       ↓                                   ↓
3회 초과 시 DLT로 격리              acknowledge() 호출 (오프셋 커밋)
```

---

## 사용 방법

### 1. 이벤트 발행

`@Transactional` 내에서 `Events.trigger()`를 호출합니다.  
이벤트는 Outbox 테이블에 `PENDING` 상태로 저장되며, 트랜잭션 커밋 직후 Kafka로 즉시 전송됩니다.

```java
@Transactional
public void createProduct(CreateProductCommand command) {
    Product product = productRepository.save(Product.create(command));

    ProductCreatedEvent payload = mapper.toCreatedEvent(product);
    OutboxEvent event = new OutboxEvent(
        product.getId(),
        product.getId(),
        "Product",
        topicConfig.getProduct().getCreated(),
        payload   // 직렬화 전 DTO 객체 그대로 전달
    );
    eventPublisher.publishEvent(event); // OutboxEvent 객체 그대로 전달
}
```

> `OutboxEvent`에는 직렬화 전 DTO 객체를 넣고, `publishEvent()`에는 `OutboxEvent`를 그대로 넣습니다.  
> `OutboxService.saveEvent()`에서 `JsonUtil.toJson(event.payload())`로 직렬화를 처리합니다.

### 2. 메시지 발행 보장

| 구성 요소 | 역할 |
|---|---|
| `OutboxEventListener` | 트랜잭션 커밋 직후 Kafka로 즉시 전송 |
| `OutboxRelayScheduler` | 전송 실패 메시지(PENDING) 5초 주기 재전송 |
| DLT | 최대 재시도 횟수(3회) 초과 시 `{토픽명}.DLT`로 격리, `PERMANENT_FAILURE` 상태로 변경 |

### 3. 중복 소비 방지 (Inbox Pattern)

`@IdempotentConsumer` 어노테이션을 사용하면 `InboxAdvice` AOP가 자동으로 중복 처리를 방지합니다.

**중복 체크 흐름:**

1. 메시지의 `message_id` 추출
2. Inbox 테이블에서 해당 ID 조회
3. 이미 `PROCESSED` 상태면 스킵 후 `acknowledge()` 호출
4. 신규 메시지면 처리 후 `PROCESSED` 상태로 저장 및 `acknowledge()` 호출

**사용 예시:**

```java
@IdempotentConsumer("product:reservation-cancelled")
@KafkaListener(topics = "${topics.reservation.cancelled}", groupId = "product-group")
public void handleReservationCancelled(ConsumerRecord<String, String> record, Acknowledgment ack) {
    try {
        UUID productId = extractProductId(record);
        productCommandService.pendingSale(productId);
    } catch (IllegalArgumentException e) {
        // 메시지 파싱 불가 - 즉시 스킵
        log.error("메시지 처리 불가 - 스킵 처리: {}", e.getMessage(), e);
        ack.acknowledge();
    } catch (CustomException e) {
        // 재시도해도 실패할 도메인 예외 - 즉시 스킵
        log.error("도메인 예외 발생 - 스킵 처리: error={}", e.getMessage(), e);
        ack.acknowledge();
    } catch (Exception e) {
        // 일시적 오류 - DLT로 라우팅
        log.error("예상치 못한 예외 발생 - DLT 라우팅: record={}, error={}", record.value(), e.getMessage(), e);
        throw e;
    }
}
```

> **주의:** `Acknowledgment ack` 파라미터를 반드시 선언해야 `InboxAdvice`가 정상 동작합니다.

**`@IdempotentConsumer` value 규칙:**
- 서비스 전체에서 **고유한 값**이어야 합니다.
- 권장 형식: `"서비스명:이벤트명"` (예: `"product:reservation-cancelled"`)

### 4. Kafka 수동 커밋 설정

`application-kafka.yml`에서 수동 커밋 모드가 설정되어 있어야 합니다.

```yaml
spring:
  kafka:
    listener:
      ack-mode: MANUAL
```

---

## OutboxStatus

| 상태 | 설명 |
|---|---|
| `PENDING` | 발행 대기 중 |
| `SENDING` | 전송 중 |
| `PROCESSED` | 전송 완료 |
| `PERMANENT_FAILURE` | 최대 재시도 초과, DLT로 격리 |

---

## InboxStatus

| 상태 | 설명 |
|---|---|
| `RECEIVED` | 정상 수신 완료 |
| `PROCESSED` | 정상 처리 완료 |
| `FAILED` | 처리 실패, 직접 처리 필요 |

---

## 주의사항

- `Events.trigger()`는 반드시 `@Transactional` 내에서 호출해야 합니다.
- `@IdempotentConsumer`의 value는 서비스 전체에서 **고유한 값**이어야 합니다.
- `Acknowledgment ack` 파라미터를 선언하지 않으면 `InboxAdvice`가 동작하지 않습니다.
- `OutboxEvent`에 DTO를 넣을 때 미리 직렬화하지 않습니다. 이중 직렬화가 발생합니다.
- 테스트 전 초기 버전으로 수정이 있을 수 있습니다.


# JWT
## 1. 환경 설정 (JwtProperties)
JWT 기능을 사용하려면 `application.yml`에 설정을 추가해야 합니다. 이 설정값은 `JwtProperties` 클래스를 통해 JWT 기능에서 사용됩니다.

```yaml
jwt:
  secret: "${JWT_SECRET_KEY:your-secret-key-at-least-32-chars-long}"
  access-token-expiration: 3600000 # 1시간 (ms)
  refresh-token-expiration: 604800000 # 7일 (ms)
```

필요 시 `JwtProperties`를 직접 주입받아 설정된 만료 시간 등의 값을 참조할 수 있습니다.
```java
@Autowired
private JwtProperties jwtProperties;

Long expiration = jwtProperties.getAccessTokenExpiration();
```

## 2. TokenProvider 사용법

`TokenProvider` 인터페이스(구현체: `JwtTokenProvider`)를 주입받아 토큰 생성 및 검증을 수행합니다.

### 토큰 쌍 생성 (Access & Refresh)

```java
@Autowired
private TokenProvider tokenProvider;

// UserDetailsImpl 객체를 바탕으로 TokenPair 생성
TokenPair tokenPair = tokenProvider.createTokenPair(userDetails);

String accessToken = tokenPair.getAccessToken();   // "Bearer " 접두사 포함
String refreshToken = tokenPair.getRefreshToken();
Long expires = tokenPair.getAccessTokenExpiresIn();
```

### 토큰 검증 및 정보 추출

* 정보 추출 기능(parseClaims() 메서드)의 경우, 해당 기능을 사용할 서비스의 build.gradle에 jjwt-api 의존성을 추가해야 사용 가능

```
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
```

```java
// 유효성 검사 (만료, 서명 등)
boolean isValid = tokenProvider.validateToken(token);

// 사용자 식별값(UUID) 추출
UUID userId = tokenProvider.getUserId(token);

// 남은 유효 시간 확인 (ms)
long remainingTime = tokenProvider.getRemainingTime(token);

// 토큰 파싱 (Claims 추출)
// 토큰 내부의 전체 클레임을 추출하여 상세 정보에 접근할 수 있습니다.
Claims claims = tokenProvider.parseClaims(token);
String role = claims.get(JwtUtils.CLAIM_USER_ROLE, String.class);
String nickname = claims.get(JwtUtils.CLAIM_NICKNAME, String.class);
```

### 만료된 토큰 처리 (재발급 시)
```java
// 만료된 Access Token에서도 Subject(UUID)를 추출할 수 있습니다.
String userIdStr = tokenProvider.getSubjectFromExpiredAccessToken(expiredAccessToken);
```

## 3. JwtUtils 및 상수 활용
`JwtUtils`는 토큰 파싱 유틸리티와 게이트웨이 연동을 위한 상수를 제공합니다.

- **토큰 추출**: `JwtUtils.resolveToken(header)` (Bearer 접두사 제거)
- **표준 헤더 상수**: 게이트웨이에서 전달하는 사용자 정보 헤더 키
    - `JwtUtils.HEADER_USER_ID`: "X-User-Id"
    - `JwtUtils.HEADER_USERNAME`: "X-User-Username"
    - `JwtUtils.HEADER_ROLES`: "X-User-Roles"
    - `JwtUtils.HEADER_USER_NAME`: "X-User-Name"
    - `JwtUtils.HEADER_USER_NICKNAME`: "X-User-Nickname"




