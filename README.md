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





# Event Massaging
## 사전 준비
### 1. 환경설정
- Java 21, SpringBoot 3환경에서 동작하며, Kafka와 Redis 및 RDB가 필요합니다.
### 2. Events 클래스 초기화
- 애플리케이션 구동 시점에 Events 클래스에 스프링 빈을 주입하여 초기화해야 합니다. 초기화되지 않은 상태에서 호출 시 IllegalStateException이 발생합니다.
```
@Configuration
public class EventConfig {
    @Bean
    public void initEvents(ApplicationEventPublisher publisher, KafkaTemplate<String, Object> template) {
        Events.init(publisher, template);
    }
}
```

## 사용 방법
### 1. 이벤트 발행(Trigger)
- 비즈니스 로직 수행 후 @Transactional 내에서 이벤트를 트리거합니다. 이 시점에 이벤트는 DB의 Outbox 테이블에 PENDING 상태로 저장됩니다.
```
@Transactional
public void createOrder(OrderRequest request) {
    Order order = orderRepository.save(Order.create(request));
    
    // 이벤트 트리거: Outbox 저장 및 전송 준비
    Events.trigger(new OrderCreatedEvent(order.getId(), order.getCorrelationId()));
}
```
### 2. 메시지 발행 보장 (Relay & Scheduler)
- Listener: 트랜잭션 커밋 직후 Kafka로 메시지를 즉시 전송합니다.
- Scheduler: 전송에 실패한 메시지(FAILED, PENDING)를 5초 주기로 재전송합니다.
- DLT (Dead Letter Topic): 최대 재시도 횟수(3회) 초과 시 메시지를 격리하고 PERMANENT_FAILURE 상태로 변경합니다.

### 3. 중복 소비 방지(Inbox Pattern)
- 수신 측에서는 @InboxLog 또는 InboxAdvice를 통해 메시지 중복 처리를 방지합니다. 헤더의 message_id를 확인하여 이미 처리된 메시지는 건너뜁니다.
```
@KafkaListener(topics = "order.created")
public void handleOrder(@Payload OrderCreatedEvent event, @Header("message_id") String messageId) {
    // InboxAdvice가 message_id를 추출하여 중복 체크를 수행합니다.
}
```
- 테스트 전 초기 버전으로 수정이 될 수 있습니다. 
- (사용 중 변경 사항 및 트러블 슈팅은 발생 시 작성하겠습니다!)
