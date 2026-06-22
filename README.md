# Badamoyeo API

바다모여 서비스의 Spring Boot 백엔드 API입니다. 해양 예보 공공데이터를 수집하고, 대시보드/스팟/게시글/댓글/즐겨찾기/사용자 인증 API를 제공합니다.

## 기술 스택

- Java 21
- Spring Boot 4
- Spring Security
- MyBatis
- MySQL
- Gradle

## 실행 준비

MySQL 데이터베이스를 준비한 뒤 `docs/schema.sql`을 실행해 테이블을 생성합니다.

```bash
mysql -u {user} -p {database} < docs/schema.sql
```

로컬 기본값은 아래와 같습니다.

```properties
DB_URL=jdbc:mysql://127.0.0.1:3306/badamoyeo?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=ssafy
DB_PASSWORD=ssafy
```

## 환경변수

필요한 값은 실행 환경에서 직접 설정합니다. 비밀값은 Git에 올리지 않습니다.

```bash
DB_URL=...
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=...
OPENAPI_MARINE_SERVICE_KEY=...
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

AI 챗봇을 사용할 경우 아래 값도 설정합니다. 설정하지 않으면 서버는 정상 실행되고 챗봇 API만 `503 Service Unavailable`을 반환합니다.

```bash
AI_CHAT_PROVIDER=openai
OPENAI_API_KEY=...
OPENAI_CHAT_MODEL=gpt-5.4-mini
AI_ANALYSIS_MODEL=gpt-5-mini
```

OAuth를 사용할 경우 아래 값도 설정합니다.

```bash
GOOGLE_OAUTH_CLIENT_ID=...
GOOGLE_OAUTH_CLIENT_SECRET=...
KAKAO_OAUTH_CLIENT_ID=...
KAKAO_OAUTH_CLIENT_SECRET=...
NAVER_OAUTH_CLIENT_ID=...
NAVER_OAUTH_CLIENT_SECRET=...
```

운영 HTTPS 환경에서는 refresh token cookie 보안을 위해 아래 값을 권장합니다.

```bash
REFRESH_TOKEN_COOKIE_SECURE=true
```

## 실행

```bash
./gradlew bootRun
```

서버 기본 주소는 아래와 같습니다.

```text
http://localhost:8080/api
```

## AI 챗봇

로그인 후 access token을 포함해 챗봇 응답 리소스를 생성합니다.

```http
POST /api/ai/chat-completions
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "message": "이번 주말에 서핑하기 좋은 조건을 알려줘"
}
```

응답:

```json
{
  "content": "서핑 장소를 고를 때는 파고, 풍속, 수온을 함께 확인하세요.",
  "createdAt": "2026-06-22T12:00:00Z"
}
```

장소 추천·비추천 질문에는 Spring AI Tool Calling으로 `spots`와 `marine_forecasts`를 먼저 조회합니다.
AI는 DB에 저장된 장소와 가장 가까운 날짜의 예보만 근거로 답하며, 응답에 데이터 기준일을 포함하도록 설정되어 있습니다.

## 메인 AI 장소 추천

해양 예보 스케줄 수집이 성공하면 체험별 후보 장소를 AI가 평가해 최대 6곳을 저장합니다.
AI 호출이나 결과 검증이 실패하면 기존 추천은 유지됩니다.
서버 시작 시에는 동일 체험·예보일 추천이 이미 있으면 AI를 다시 호출하지 않으며,
정기 스케줄 수집 때는 갱신된 데이터로 추천을 다시 생성합니다.

기존 DB에는 먼저 아래 마이그레이션을 적용합니다.

```bash
mysql -u {user} -p {database} < docs/ai-recommendations.sql
```

최신 추천 조회:

```http
GET /api/ai/spot-recommendations?experience=surfing
```

응답의 `items`에는 카드 정보와 함께 AI가 생성한 `rank`, `reason`, `generatedAt`이 포함됩니다.

## 상세페이지 AI 분석

상세페이지에서 처음 요청된 장소·예보만 `gpt-5-mini`로 분석하고 DB에 저장합니다.
같은 예보가 다시 요청되면 저장된 결과를 반환하며, 해당 예보 행의 `updated_at`이 변경된 경우에만 다시 생성합니다.
AI 입력에는 정제된 `metrics`, 종합지수, 날씨, 물때만 사용하고 `rawData`는 제외합니다.

```http
GET /api/spots/{spotId}/ai-analysis
GET /api/spots/{spotId}/ai-analysis?targetDate=2026-06-23
```

응답에는 `summary`, `advantages`, `disadvantages`, `recommended`,
`recommendationReason`, `safetyNote`, `generatedAt`이 포함됩니다.

## 테스트

```bash
./gradlew test
```

## 인증 방식

로그인과 회원가입 응답 body에는 `accessToken`이 내려갑니다. 프론트엔드는 이후 요청에 아래 헤더를 보냅니다.

```http
Authorization: Bearer {accessToken}
```

`refreshToken`은 JSON body에 노출하지 않고 `HttpOnly` cookie로 전달합니다. refresh/logout 요청은 cookie를 사용하므로 프론트엔드에서 credentials 설정이 필요합니다.

```js
fetch("/api/auth/refresh", {
  method: "POST",
  credentials: "include"
});
```

## 해양 예보 수집

아래 6개 경험치 데이터를 공공 API에서 수집합니다.

- `seaTravel`
- `swimming`
- `mudflat`
- `scuba`
- `fishing`
- `surfing`

서버 시작 시 수집이 실행되도록 설정되어 있습니다.

```properties
openapi.marine.ingestion.enabled=true
openapi.marine.ingestion.run-on-startup=true
```

공공 API가 간헐적으로 `UNKNOWN_ERROR`를 반환할 수 있어 수집 로직은 큰 페이지부터 빠르게 요청하고, 실패한 구간만 작은 페이지로 나누어 재시도합니다.

```text
300 -> 100 -> 50 -> 10
```

각 단계는 최대 3번 재시도합니다. 한 경험치 수집이 끝까지 실패해도 다음 경험치 수집은 계속 진행됩니다.

관리자 수동 수집 API:

```http
POST /api/admin/ingest/marine-forecasts
```

## DB 문서

- `docs/schema.sql`: 신규 DB 생성용 스키마
- `docs/ingestion-optimization.sql`: 기존 DB 보정용 SQL

## Postman

Postman 컬렉션을 공유할 경우 `docs/postman/` 아래에 두는 것을 권장합니다.

권장 환경 변수:

```text
baseUrl=http://localhost:8080/api
accessToken=
spotId=76
postId=1
commentId=1
provider=google
```

컬렉션 또는 환경 파일에는 access token, refresh token, API key 같은 비밀값을 저장하지 않습니다.

## Git에 올리지 않는 파일

아래 파일은 로컬 실행 산출물이거나 비밀값을 포함할 수 있어 Git에 올리지 않습니다.

```text
build/
.gradle/
.idea/
uploads/
.env
.env.*
```
