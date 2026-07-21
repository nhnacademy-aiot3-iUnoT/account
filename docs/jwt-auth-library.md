# 공용 JWT 인증 패키지

`com.nhnacademy.auth.jwt` 패키지는 API 서버가 JWT 검증 책임을 수행할 때 필요한 공통 구현을 제공한다.

## 활성화

```properties
nhn.auth.jwt.enabled=true
nhn.auth.jwt.jwk-set-uri=https://auth.example.com/.well-known/jwks.json
nhn.auth.jwt.issuer=https://auth.example.com
nhn.auth.jwt.audiences=account-api
nhn.auth.jwt.allowed-algorithms=RS256
```

기본값은 비활성화다. 활성화하면 JWK Set 기반 `JwtDecoder`, UUID `sub` 및 `kid` 검증,
인증 변환기, 공통 401 응답과 `@AccountUUID` 인자 변환기가 등록된다. JWK Set은 Spring
Security의 Nimbus 구현이 각 애플리케이션 인스턴스 메모리에 캐시한다.

서비스의 `SecurityFilterChain`에는 라이브러리가 제공한 변환기와 진입점을 명시적으로 연결한다.

```java
http.oauth2ResourceServer(resourceServer -> resourceServer
        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
        .authenticationEntryPoint(authenticationEntryPoint));
```

검증된 계정 UUID는 컨트롤러에서 다음과 같이 사용한다.

```java
@GetMapping("/me")
public AccountResponse me(@AccountUUID UUID accountUuid) {
    return accountService.findAccount(accountUuid);
}
```

`@AccountUUID`는 Bearer Token을 다시 파싱하지 않는다. Spring Security가 생성한 인증 완료
`JwtAuthenticationToken`만 허용하므로, 임의 헤더나 검증되지 않은 Claim은 UUID의 출처가 될 수 없다.

현재 저장소에서는 패키지 경계를 분리해 두었다. 여러 서비스에 배포할 때는
`com.nhnacademy.auth.jwt`와 자동 구성 리소스를 별도 Maven 모듈/JAR로 옮기고, API 서버에는
Resource Server 의존성만 노출하면 된다. 개인키와 토큰 발급 코드는 이 라이브러리에 포함하지 않는다.
