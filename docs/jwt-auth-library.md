# 사용자 JWT 전달 기반 API 간 인증 구조

## 1. 문서 목적

이 문서는 하나의 사용자 JWT를 `Account API`와 `Inventory API`에서 함께 검증하고, `Account API`가 `Inventory API`를 호출할 때 동일한 JWT를 그대로 전달하는 구조를 정리합니다.

목표는 다음과 같습니다.

```text
- 각 API가 JWT를 독립적으로 검증한다.
- 사용자 UUID는 JWT의 sub를 기준으로 통일한다.
- 컨트롤러와 서비스 계층은 JWT 형식을 직접 알지 않는다.
- 각 API는 자신의 DB와 업무 규칙을 기준으로 인가를 수행한다.
- API 간 동기 호출에서는 사용자의 JWT를 그대로 전달한다.
- 프론트엔드는 신뢰 경계가 아닌 API 클라이언트로 취급한다.
- 사용자가 Gateway에 동일한 요청을 직접 보내는 것을 별도로 차단하지 않는다.
```

이 구조에서 API 간 호출의 의미는 다음과 같습니다.

> Account API가 자신의 서비스 권한으로 Inventory API를 호출하는 것이 아니라, 인증된 사용자의 JWT를 전달해 동일한 사용자 권한으로 Inventory API를 호출합니다.

Inventory API는 Account API의 호출 여부가 아니라 JWT로 인증된 최종 사용자를 기준으로 인가를 수행합니다.

---

## 2. JWT 구조

사용자 로그인 후 발급되는 JWT는 다음과 같은 Claim을 가집니다.

```json
{
  "iss": "account-api",
  "aud": [
    "account-api",
    "inventory-api"
  ],
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "iat": 1785110400,
  "exp": 1785112200
}
```

| Claim | 설명                   |
| ----- | -------------------- |
| `iss` | JWT를 발급한 인증 서버       |
| `aud` | JWT를 사용할 수 있는 API 목록 |
| `sub` | 인증된 사용자의 UUID        |
| `iat` | JWT 발급 시각            |
| `exp` | JWT 만료 시각            |

JWT에는 비밀번호, 이메일, 이름과 같은 계정 상세 정보를 포함하지 않습니다.

JWT는 사용자의 신원을 증명하기 위한 값이며, 조직 소속이나 특정 자원에 대한 접근 권한을 직접 증명하지 않습니다.

---

## 3. 전체 인증 흐름

```text
Client
  ↓ Authorization: Bearer 사용자 JWT

API Gateway
  ↓ Authorization: Bearer 사용자 JWT

Account API
  1. JWT 서명 검증
  2. issuer 검증
  3. 만료 시간 검증
  4. aud에 account-api가 포함되어 있는지 확인
  5. sub 존재 및 UUID 형식 검증
  6. JwtAuthenticationToken을 SecurityContext에 저장
  ↓

Account API
  1. 사용자의 요청을 처리
  2. Inventory API 호출이 필요하면
     Authorization 헤더의 JWT를 그대로 전달
  ↓

Inventory API
  1. 전달받은 JWT를 다시 검증
  2. issuer 검증
  3. 만료 시간 검증
  4. aud에 inventory-api가 포함되어 있는지 확인
  5. sub 존재 및 UUID 형식 검증
  6. JwtAuthenticationToken을 SecurityContext에 저장
  7. 자체 DB와 업무 규칙으로 인가 수행
```

각 API는 앞선 API나 Gateway에서 JWT를 검증했다는 사실에 의존하지 않습니다.

---

## 4. Account API의 처리

Account API는 클라이언트가 전달한 JWT를 독립적으로 검증합니다.

```text
JWT
  ↓
JwtDecoder
  ↓
서명 및 Claim 검증
  ↓
sub UUID 검증
  ↓
JwtAuthenticationToken 생성
  ↓
SecurityContext 저장
```

컨트롤러는 JWT를 직접 다루지 않습니다.

```java
@GetMapping("/me")
public AccountResponse getMyAccount(
        @AccountUUID UUID accountUuid
) {
    return accountService.getMyAccount(accountUuid);
}
```

`@AccountUUID`는 `SecurityContext`에 저장된 인증 완료
`JwtAuthenticationToken`의 `sub`를 UUID로 변환합니다. JWT가 아닌 인증 객체나
UUID 형식이 아닌 `sub`는 거부합니다.

현재 인증 컨텍스트에서 애플리케이션에 제공할 값은 계정 UUID 하나이므로 별도의
`AccountPrincipal`은 도입하지 않습니다. UUID 외에 여러 인증 방식에서 공유해야 할
추가 정보가 생기면 그때 전용 Principal 도입을 다시 검토합니다.

컨트롤러와 서비스 계층은 JWT Claim 이름, 토큰 형식, 서명 알고리즘과 같은 인증 세부 사항을 알 필요가 없습니다.

---

## 5. Account API에서 Inventory API 호출

Account API가 Inventory API를 호출할 때 사용자의 JWT를 그대로 전달합니다.

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

개념적인 호출 흐름은 다음과 같습니다.

```text
Client
  → API Gateway
    → Account API
      Authorization: Bearer 사용자 JWT

Account API
  → Inventory API
    Authorization: Bearer 동일한 사용자 JWT
```

Account API는 JWT Payload에서 사용자 UUID를 꺼내 별도의 사용자 식별 헤더를 만들지 않습니다.

피해야 하는 방식은 다음과 같습니다.

```http
X-Account-UUID: 550e8400-e29b-41d4-a716-446655440000
```

일반 헤더는 요청자가 임의로 작성할 수 있으므로 사용자 식별 정보는 서명 검증이 가능한 JWT를 통해 전달합니다.

동일한 JWT를 전달한다는 것은 Account API가 사용자를 대신해 요청 흐름을 이어간다는 의미입니다.

다만 JWT 자체가 다음 사실까지 증명하는 것은 아닙니다.

```text
- Account API가 실제 호출자였는가?
- 프론트엔드에서 시작된 요청인가?
- 사용자가 같은 요청을 직접 구성했는가?
```

현재 구조에서는 이러한 호출 경로를 인가 기준으로 사용하지 않습니다.

---

## 6. Inventory API의 처리

Inventory API는 Account API나 Gateway가 이미 JWT를 검증했다는 사실만 믿지 않습니다.

전달받은 JWT를 다시 독립적으로 검증합니다.

```text
Authorization 헤더에서 JWT 추출
  ↓
JWT 서명 검증
  ↓
iss 검증
  ↓
exp 및 nbf 검증
  ↓
aud에 inventory-api가 포함되는지 확인
  ↓
sub 존재 및 UUID 형식 확인
  ↓
JwtAuthenticationToken을 SecurityContext에 저장
```

Inventory API에서도 동일한 `@AccountUUID`를 사용할 수 있습니다.

```java
@GetMapping("/inventories/me")
public InventoryResponse getMyInventory(
        @AccountUUID UUID accountUuid
) {
    return inventoryService.getMyInventory(accountUuid);
}
```

Account API와 Inventory API에서 추출되는 사용자 UUID는 동일합니다.

```text
Account API의 @AccountUUID 결과
=
Inventory API의 @AccountUUID 결과
=
JWT의 sub
```

Inventory API는 요청이 Account API를 거쳤는지 여부가 아니라 인증된 사용자의 UUID와 자체 업무 규칙을 기준으로 인가합니다.

---

## 7. 인증과 인가의 구분

### 7.1 인증

JWT 검증은 요청자가 누구인지 확인합니다.

```text
- 신뢰할 수 있는 인증 서버가 발급했는가?
- JWT가 위조되지 않았는가?
- JWT가 만료되지 않았는가?
- 이 API를 대상으로 발급된 JWT인가?
- 사용자 UUID는 무엇인가?
```

인증 결과는 Spring Security의 `JwtAuthenticationToken`으로 표현하며,
애플리케이션 계층에는 `@AccountUUID`를 통해 UUID만 제공합니다.

---

### 7.2 인가

각 API는 인증된 사용자 UUID를 기준으로 자신의 DB와 업무 규칙을 확인합니다.

Inventory API의 인가 예시는 다음과 같습니다.

```text
- 사용자가 해당 조직에 소속되어 있는가?
- 사용자의 조직원 상태가 ACTIVE인가?
- 해당 재고가 사용자의 조직 소유인가?
- 사용자가 해당 재고를 조회하거나 수정할 권한이 있는가?
```

예시는 다음과 같습니다.

```java
public InventoryResponse getInventory(
        UUID accountUuid,
        UUID inventoryUuid
) {
    OrganizationMember member =
            organizationMemberRepository
                    .findByAccountUuid(accountUuid)
                    .orElseThrow(OrganizationMemberNotFoundException::new);

    if (!member.isActive()) {
        throw new AccessDeniedException(
                "활성 상태의 조직원이 아닙니다."
        );
    }

    Inventory inventory =
            inventoryRepository.findByUuid(inventoryUuid)
                    .orElseThrow(InventoryNotFoundException::new);

    if (!inventory.belongsTo(member.getOrganizationUuid())) {
        throw new AccessDeniedException(
                "해당 재고에 접근할 수 없습니다."
        );
    }

    return InventoryResponse.from(inventory);
}
```

JWT가 유효하다는 사실만으로 조직 소속이나 자원 접근 권한이 증명되는 것은 아닙니다.

```text
JWT 검증
→ 사용자가 누구인지 확인

업무 인가
→ 해당 사용자가 무엇을 할 수 있는지 확인
```

---

## 8. 이 구조가 적합한 경우

동일한 사용자 JWT 전달 방식은 다음 조건에서 적합합니다.

```text
- Account API와 Inventory API가 같은 사용자 인증 체계를 사용한다.
- JWT의 aud에 두 API가 모두 포함되어 있다.
- API 간 호출이 사용자를 대신한 동기 호출이다.
- Inventory API가 최종 사용자 UUID를 기준으로 인가한다.
- Inventory API가 호출 서비스보다 최종 사용자를 중요하게 본다.
- 각 API가 JWT를 직접 다시 검증한다.
- 사용자의 Gateway 직접 호출을 보안상 허용한다.
- 프론트엔드와 직접 API 호출을 별도로 구분할 필요가 없다.
```

예시는 다음과 같습니다.

```text
사용자가 자신의 계정 정보를 조회한다.
  ↓
Account API가 사용자 정보를 조회한다.
  ↓
Account API가 Inventory API에 같은 JWT를 전달한다.
  ↓
Inventory API가 같은 사용자 UUID로 조직 및 재고 권한을 확인한다.
```

---

## 9. 프론트엔드와 직접 API 호출 정책

### 9.1 프론트엔드는 API 클라이언트이다

프론트엔드는 사용자의 요청을 화면과 상호작용으로 제공하는 API 클라이언트입니다.

브라우저에서 실행되는 프론트엔드 코드도 내부적으로는 사용자의 JWT를 포함해 API를 호출합니다.

```text
사용자
  ↓
프론트엔드
  ↓ Authorization: Bearer 사용자 JWT
API Gateway
  ↓
Account API 또는 Inventory API
```

사용자가 `curl`, Postman 또는 별도의 클라이언트를 이용해 동일한 요청을 직접 구성하는 경우에도 인증 구조는 같습니다.

```text
사용자
  ↓ Authorization: Bearer 사용자 JWT
API Gateway
  ↓
Account API 또는 Inventory API
```

두 호출 모두 다음 정보를 기준으로 처리됩니다.

```text
- JWT가 유효한가?
- JWT의 Audience에 현재 API가 포함되어 있는가?
- JWT의 sub에 해당하는 사용자는 누구인가?
- 해당 사용자가 요청한 자원에 접근할 권한이 있는가?
```

프론트엔드를 통해 호출했다는 사실 자체는 신뢰 근거로 사용하지 않습니다.

브라우저에서 실행되는 요청의 URL, 헤더, Payload는 사용자가 확인하고 재현할 수 있으므로, 프론트엔드 호출만 허용하고 동일한 직접 요청을 차단하는 것은 실질적인 보안 경계가 되기 어렵습니다.

따라서 사용자가 동일한 JWT와 요청 형식으로 API Gateway를 직접 호출하는 것은 별도로 차단하지 않습니다.

```text
프론트엔드를 통한 호출
=
사용자가 동일한 요청을 직접 구성한 호출
```

---

### 9.2 프론트엔드의 권한 처리는 보안 인가가 아니다

프론트엔드에서 특정 버튼을 숨기거나 화면 접근을 제한하는 것은 사용자 경험을 위한 처리입니다.

```text
프론트엔드 권한 처리
→ 화면 및 사용자 경험 제어

백엔드 권한 처리
→ 실제 자원 접근 통제
```

사용자는 프론트엔드의 화면 제한과 관계없이 API 요청을 직접 구성할 수 있습니다.

따라서 모든 보안상 중요한 인가는 각 API에서 반드시 다시 수행합니다.

---

### 9.3 Gateway 직접 호출과 내부 API 직접 호출의 구분

여기서 허용하는 직접 호출은 사용자가 외부에 공개된 API Gateway 엔드포인트를 직접 호출하는 것을 의미합니다.

```text
허용되는 호출

사용자
  → API Gateway
    → 내부 API
```

Inventory API와 같은 내부 서비스의 주소를 외부에 직접 공개한다는 의미는 아닙니다.

```text
권장 네트워크 구조

외부 사용자
  → API Gateway
    → 사설 네트워크의 Account API
    → 사설 네트워크의 Inventory API
```

내부 API는 사설 네트워크에 배치하고 외부 요청은 Gateway를 통해서만 전달되도록 구성할 수 있습니다.

이 제한은 프론트엔드에서 발생한 요청만 허용하기 위한 것이 아니라 다음과 같은 네트워크 및 운영 정책을 적용하기 위한 것입니다.

```text
- 외부 공격 표면 축소
- 공통 로깅
- 요청 크기 제한
- Rate Limit
- TLS 종료
- 라우팅 통제
- 공통 보안 정책 적용
```

---

### 9.4 별도의 서비스 인증을 적용하지 않는 이유

현재 요구사항에서는 Inventory API가 다음 정보를 확인할 필요가 없습니다.

```text
- Account API가 호출했는가?
- 프론트엔드가 호출했는가?
- 사용자가 직접 요청을 구성했는가?
```

Inventory API에 필요한 정보는 최종 사용자의 신원과 권한입니다.

```text
누가 요청했는가?
→ JWT의 sub

해당 사용자가 접근할 수 있는가?
→ Inventory API의 DB와 업무 규칙
```

따라서 현재 구조에서는 다음 목적을 위한 별도의 서비스 인증이나 OAuth Token Exchange를 적용하지 않습니다.

```text
- 호출 서비스 식별
- 다운스트림 전용 권한
- 프론트엔드와 직접 호출의 구분
- API별 최소 권한 토큰 재발급
- Account API 경유 여부 증명
```

이러한 기능은 현재 보안 요구사항에 포함되지 않으며, 도입할 경우 인증 구조와 운영 복잡도가 증가합니다.

향후 실제 요구사항이 변경되는 경우에만 별도 구조를 검토합니다.

---

## 10. 이 구조가 적합하지 않은 경우

다음 상황에서는 사용자 JWT 전달만으로 충분하지 않을 수 있습니다.

### 10.1 서비스 자체 권한으로 수행하는 작업

```text
- 배치 작업
- 스케줄러 작업
- 이벤트 소비
- 운영 자동화
- 특정 내부 서비스만 수행할 수 있는 관리 작업
```

이런 작업은 최종 사용자 권한이 아니라 서비스 자체 권한으로 수행됩니다.

현재 사용자 JWT 전달 구조와 별도의 인증 흐름으로 설계해야 합니다.

---

### 10.2 호출 서비스 식별이 반드시 필요한 작업

Inventory API가 다음을 보안상 확인해야 한다면 사용자 JWT만으로는 부족합니다.

```text
- Account API가 호출했는가?
- 다른 내부 API가 호출했는가?
- 사용자가 Inventory API를 직접 호출했는가?
- 특정 서비스에서 시작된 요청만 허용해야 하는가?
```

사용자 JWT의 `sub`에는 사용자 UUID만 있으므로 호출한 서비스를 증명하지 못합니다.

현재 요구사항에서는 이러한 구분이 필요하지 않으므로 별도의 서비스 인증을 적용하지 않습니다.

---

### 10.3 비동기 작업

```text
사용자 요청
  ↓
메시지 큐
  ↓ 일정 시간 후 처리
Inventory API
```

비동기 처리 시점에는 JWT가 만료될 수 있습니다.

또한 메시지를 처리하는 주체는 사용자의 현재 HTTP 요청과 분리되어 있으므로, 동일 JWT를 그대로 장기간 전달하는 방식은 적절하지 않을 수 있습니다.

따라서 동일 JWT 전달 방식은 기본적으로 동기 요청에 적용합니다.

---

## 11. 회원가입과 조직원 생성

회원가입 과정의 조직원 생성은 일반적인 사용자 위임 호출과 다릅니다.

회원가입이 완료되기 전에는 정상 사용자 JWT가 아직 없을 수 있습니다.

권장 흐름은 다음과 같습니다.

```text
Client
  → Account API
    회원가입 정보 + 초대 UUID

Account API
  1. Account UUID 생성
  2. 계정을 PENDING 상태로 저장
  3. 조직관리 API에 초대 UUID와 Account UUID 전달

조직관리 API
  하나의 트랜잭션에서:
  1. 초대 UUID 검증
  2. 만료 여부 확인
  3. 사용 여부 확인
  4. 조직원 생성
  5. 초대 UUID 사용 완료 처리

조직관리 API
  → 성공 응답

Account API
  1. 계정을 ACTIVE로 변경
  2. JWT 발급
  3. 회원가입 성공 응답
```

회원가입은 다음 순서로 완료되어야 합니다.

```text
조직원 생성 성공
  ↓
계정 활성화
  ↓
JWT 발급
  ↓
회원가입 성공
```

회원가입 성공 후 조직원을 생성하면 다음 불일치가 생길 수 있습니다.

```text
계정 생성 성공
  ↓
조직원 생성 실패
  ↓
조직원이 없는 계정 발생
```

조직관리 API에서는 초대 UUID 검증, 조직원 생성, 초대 사용 완료 처리를 하나의 트랜잭션으로 처리하는 것이 좋습니다.

또한 네트워크 오류나 재시도로 동일한 요청이 중복 실행될 수 있으므로 다음 사항을 고려해야 합니다.

```text
- 초대 UUID 중복 사용 방지
- 동일 Account UUID의 조직원 중복 생성 방지
- 회원가입 요청의 멱등성 보장
- PENDING 상태 계정 복구 정책
- 조직원 생성 후 Account 활성화 실패 시 재시도 정책
```

---

## 12. JWT Audience 정책

현재 구조에서는 JWT가 다음 Audience를 가집니다.

```json
{
  "aud": [
    "account-api",
    "inventory-api"
  ]
}
```

각 API는 자신의 이름이 `aud`에 포함되어 있는지 확인합니다.

```text
Account API
→ account-api 포함 여부 확인

Inventory API
→ inventory-api 포함 여부 확인
```

현재 API가 두 개이고 모든 사용자 JWT가 항상 두 API에서 사용된다면 보안 범위는 플랫폼 공통 Audience와 사실상 유사할 수 있습니다.

```json
{
  "aud": ["inventory-platform-api"]
}
```

두 방식의 차이는 API가 추가될 때 나타납니다.

### API별 Audience 목록

```text
기존 JWT:
aud = account-api, inventory-api

새 Order API 추가:
기존 JWT에는 order-api가 없으므로 접근 불가
```

### 플랫폼 공통 Audience

```text
기존 JWT:
aud = inventory-platform-api

새 Order API도 같은 Audience 사용:
기존 JWT로 접근 가능
```

현재 설계에서는 API별 Audience 목록을 사용합니다.

```text
account-api
inventory-api
```

모든 사용자 JWT에 두 API가 항상 포함될 예정이라면 공통 Audience를 선택해도 기능적 차이는 크지 않지만, API별 Audience 목록은 새로운 API가 추가될 때 기존 JWT의 사용 범위를 자동으로 확장하지 않는다는 장점이 있습니다.

---

## 13. 각 API의 JWT 인증 책임

공용 인증 Starter를 사용하지 않고 각 API가 자신의 코드에 JWT 검증 구성을 직접 둡니다.
Account API는 `com.nhnacademy.account.config`와
`com.nhnacademy.account.security` 패키지에서 다음 기능을 구성합니다.

```text
- JwtDecoder
- JWK Set 공개키 조회 및 캐시
- JWT 서명 검증
- issuer 검증
- audience 검증
- exp 및 nbf 검증
- sub UUID 검증
- JwtAuthenticationToken 생성
- SecurityContext 저장
- @AccountUUID ArgumentResolver 등록
- 401 및 403 응답 처리
```

Inventory API 등 다른 API도 같은 검증 원칙을 해당 API 내부 코드로 구현하되,
자신에게 해당하는 audience를 설정합니다. 한 API의 Spring Bean이나 내부 패키지를
다른 API가 런타임 라이브러리로 참조하지 않습니다.

```yaml
security:
  jwt:
    issuer: https://auth.example.com
    audiences:
      - inventory-api
```

기대 Audience는 명시적인 보안 설정으로 관리하는 것을 기본으로 합니다.

```text
security.jwt.audiences = inventory-api
  ↓
기대 Audience = inventory-api
```

동일한 서비스의 여러 인스턴스는 같은 Audience를 사용하므로 이중화에도 문제가 없습니다.

---

## 14. 키 관리

개인키는 JWT를 발급하는 인증 서버만 보유합니다.

```text
Account/Auth 서버
- RSA 개인키 보유
- JWT 발급
- JWK Set으로 공개키 제공
```

각 API 서버는 개인키를 보유하지 않습니다.

```text
각 API 서버
- JWK Set에서 공개키 조회
- 공개키를 캐시
- JWT 서명 검증
```

```text
개인키
→ JWT 발급 가능
→ 인증 서버만 보관

공개키
→ JWT 검증 가능
→ JWT 발급 불가능
→ 여러 API가 조회 가능
```

공개키 회전을 위해 JWT Header의 `kid`와 JWK Set의 키 식별자를 사용할 수 있습니다.

다음 상황을 고려해야 합니다.

```text
- 새로운 공개키 추가
- 기존 공개키와 새로운 공개키의 동시 제공
- 기존 JWT 만료 후 이전 공개키 제거
- 알 수 없는 kid 처리
- JWK Set 조회 실패 시 캐시 정책
```

---

## 15. 실패 응답 원칙

### 15.1 401 Unauthorized

다음은 인증 실패로 처리합니다.

```text
- JWT가 없음
- JWT 서명이 올바르지 않음
- JWT가 만료됨
- JWT가 아직 유효하지 않음
- issuer가 올바르지 않음
- 현재 API가 audience에 없음
- sub가 없음
- sub가 UUID 형식이 아님
- 허용하지 않은 서명 알고리즘 사용
```

### 15.2 403 Forbidden

다음은 인증은 성공했지만 인가에 실패한 경우입니다.

```text
- 조직에 소속되어 있지 않음
- 조직원 상태가 비활성 상태임
- 해당 자원의 소유자가 아님
- 필요한 역할이나 권한이 없음
```

사용자가 프론트엔드가 아닌 별도의 클라이언트로 요청했다는 이유만으로 `403 Forbidden`을 반환하지 않습니다.

호출 방식과 관계없이 동일한 인증 및 인가 규칙을 적용합니다.

---

## 16. 테스트 항목

최소한 다음 통합 테스트가 필요합니다.

### JWT 검증

```text
- 정상 JWT로 Account API 요청 성공
- 정상 JWT로 Inventory API 요청 성공
- 만료된 JWT는 401
- 아직 유효하지 않은 JWT는 401
- 잘못된 서명은 401
- 잘못된 issuer는 401
- account-api Audience가 없는 토큰은 Account API에서 401
- inventory-api Audience가 없는 토큰은 Inventory API에서 401
- sub가 없는 토큰은 401
- sub가 UUID 형식이 아니면 401
- 허용하지 않은 서명 알고리즘은 401
- 알 수 없는 kid는 401
```

### API 간 JWT 전달

```text
- Account API에서 Inventory API로 JWT 전달 성공
- 전달된 JWT가 원본 JWT와 동일함
- 동일한 Account UUID가 두 API에서 추출됨
- Inventory API가 전달받은 JWT를 독립적으로 다시 검증함
- 다운스트림 호출 로그에 JWT 원문이 노출되지 않음
```

### 인가

```text
- 조직에 속하지 않은 사용자는 403
- 비활성 조직원은 403
- 다른 조직 자원 접근은 403
- 접근 가능한 조직 자원 요청은 성공
```

### Gateway 및 직접 호출

```text
- 프론트엔드를 통한 Gateway 호출 성공
- 동일한 JWT로 Gateway API를 직접 호출해도 동일하게 처리됨
- 호출 도구에 따라 인가 결과가 달라지지 않음
- 내부 API 주소는 외부 네트워크에서 접근할 수 없음
- Gateway를 통한 내부 API 접근은 성공
```

### 회원가입

```text
- 정상 초대 UUID로 회원가입 성공
- 만료된 초대 UUID는 실패
- 이미 사용한 초대 UUID는 실패
- 동일 초대 UUID의 동시 사용은 하나만 성공
- 동일 회원가입 요청이 재전송되어도 조직원이 중복 생성되지 않음
- 조직원 생성 실패 시 계정이 ACTIVE로 변경되지 않음
- 조직원 생성 후 Account 활성화 실패 시 재시도 가능
```

---

## 17. 최종 구조

```text
JWT
- iss = account-api
- aud = account-api, inventory-api
- sub = 사용자 UUID

Client
  → API Gateway
    → Account API
      JWT 독립 검증
      JwtAuthenticationToken 생성
      @AccountUUID 사용
    ↓
Account API
  → Inventory API
    동일한 Authorization Bearer JWT 전달
    ↓
Inventory API
  JWT 독립 검증
  inventory-api Audience 확인
  JwtAuthenticationToken 생성
  @AccountUUID로 동일한 사용자 UUID 추출
  자체 DB로 조직 및 자원 인가 수행
```

프론트엔드를 사용하지 않고 사용자가 직접 Gateway 요청을 구성하는 경우에도 같은 보안 규칙을 적용합니다.

```text
프론트엔드 호출
  → Gateway
  → API

직접 구성한 호출
  → Gateway
  → API

두 호출 모두
  → JWT 검증
  → 사용자 식별
  → 자체 DB 기반 인가
```

내부 API 주소는 외부에 직접 공개하지 않고 Gateway 뒤의 사설 네트워크에 배치합니다.

---

## 18. 최종 판단

이 구조는 다음 전제에서 적절합니다.

```text
- API 간 호출이 사용자를 대신한 동기 호출이다.
- Inventory API는 호출 서비스보다 최종 사용자를 기준으로 인가한다.
- JWT에 Account API와 Inventory API Audience가 모두 포함된다.
- 각 API는 전달받은 JWT를 독립적으로 다시 검증한다.
- 조직 소속과 자원 접근 권한은 각 API가 자신의 DB에서 판단한다.
- 프론트엔드는 신뢰 경계가 아니라 API 클라이언트로 취급한다.
- 사용자가 Gateway에 동일한 요청을 직접 보내는 것을 허용한다.
- 프론트엔드 호출과 직접 호출을 보안적으로 구분하지 않는다.
- 내부 API는 외부에 직접 공개하지 않고 Gateway 뒤에 배치한다.
- 호출 서비스를 식별하기 위한 별도의 서비스 인증은 적용하지 않는다.
- OAuth Token Exchange나 API별 최소 권한 토큰은 현재 요구사항에 포함하지 않는다.
```

핵심 원칙은 다음과 같습니다.

```text
JWT
→ 사용자가 누구인지 증명

@AccountUUID
→ 검증된 JwtAuthenticationToken의 sub를 UUID로 애플리케이션에 제공

동일 JWT 전달
→ Account API가 동일한 사용자 권한으로 Inventory API 호출을 이어감

Inventory API
→ 동일한 사용자 UUID를 기준으로 자체 업무 인가 수행

프론트엔드
→ API 사용을 편리하게 제공하는 클라이언트

API Gateway
→ 외부 요청의 공통 진입점과 네트워크 경계

직접 API 호출
→ 프론트엔드 호출과 동일한 인증 및 인가 규칙 적용
```

프론트엔드는 신뢰할 수 있는 보안 주체가 아닙니다.

사용자가 동일한 요청을 직접 구성해 API Gateway를 호출하는 것은 프론트엔드를 통한 호출과 본질적으로 동일하므로 별도로 차단하지 않습니다.

각 API는 호출 방식이나 호출 도구가 아니라 JWT 검증 결과와 자체 업무 인가 규칙을 기준으로 요청을 처리합니다.

서비스 자체 권한이 필요한 내부 작업, 비동기 작업 또는 호출 서비스 식별이 실제로 필요한 작업은 현재 사용자 JWT 전달 흐름과 별도로 설계합니다.
