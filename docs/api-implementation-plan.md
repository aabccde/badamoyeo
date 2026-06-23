# Badamoyeo API Implementation Notes

## Current foundation

- Spring Boot 4 + MyBatis project configuration
- MySQL datasource properties using `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- JWT access token authentication with persisted refresh tokens
- Refresh token delivery through `HttpOnly` cookies
- CORS credentials support for the frontend origin
- Public Open API ingestion for six marine experiences
- Dashboard, spot, post, comment, favorite, and user profile APIs

## Base URL

The server is configured with:

The server exposes endpoints from the root path without a global context path.

All endpoint paths below are served under `/api`.

## Auth convention

`accessToken` is returned in the JSON response body and should be sent by the frontend as:

```http
Authorization: Bearer {accessToken}
```

`refreshToken` is not exposed in JSON responses. It is stored in an `HttpOnly` cookie:

```http
Set-Cookie: refreshToken=...; HttpOnly; SameSite=Lax; Path=/auth
```

Frontend requests that rely on the refresh token cookie must include credentials:

```js
fetch("/auth/refresh", {
  method: "POST",
  credentials: "include"
});
```

For axios:

```js
axios.post("/auth/refresh", {}, { withCredentials: true });
```

## Auth endpoints

- `POST /auth/signup`
  - Response body: `accessToken`, `user`
  - Response cookie: `refreshToken`
- `POST /auth/login`
  - Response body: `accessToken`, `user`
  - Response cookie: `refreshToken`
- `POST /auth/refresh`
  - Reads `refreshToken` from cookie, with body fallback for compatibility
  - Response body: `accessToken`, `user`
- `POST /auth/logout`
  - Reads `refreshToken` from cookie, with body fallback for compatibility
  - Revokes the refresh token and expires the cookie
  - Does not require a valid access token
- `GET /auth/oauth/{provider}`
  - Supported providers: `google`, `kakao`, `naver`
- `GET /auth/oauth/{provider}/callback`
  - Issues service tokens
  - Redirects to the configured frontend OAuth success URL
  - Sends `refreshToken` through an `HttpOnly` cookie, not a URL query parameter

## Security and deployment settings

Required or recommended environment variables:

```bash
OPENAPI_MARINE_SERVICE_KEY=...
JWT_SECRET=...
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
REFRESH_TOKEN_COOKIE_SECURE=true
```

`REFRESH_TOKEN_COOKIE_SECURE=true` should be used in HTTPS production environments.

Admin ingestion endpoints are protected with `ROLE_ADMIN`:

```http
POST /admin/ingest/marine-forecasts
```

Scheduled ingestion and startup ingestion call the ingestion service internally and do not depend on the admin HTTP endpoint.

## Marine experience APIs

Implemented dashboard APIs:

- `GET /dashboard/markers`
- `GET /dashboard`

Implemented spot APIs:

- `GET /spots`
- `GET /spots/{spotId}`
- `POST /spots/{spotId}/favorite`
- `DELETE /spots/{spotId}/favorite`

Experience values:

- `seaTravel`
- `swimming`
- `mudflat`
- `scuba`
- `fishing`
- `surfing`

Sort values:

- `index`
- `community`
- `nearby`

## Community APIs

Implemented post APIs:

- `GET /spots/{spotId}/posts`
- `POST /spots/{spotId}/posts`
- `GET /posts/{postId}`
- `PATCH /posts/{postId}`
- `DELETE /posts/{postId}`
- `POST /posts/images`
- `POST /posts/{postId}/likes`
- `DELETE /posts/{postId}/likes`

Post image policy:

- File extensions: `jpg`, `jpeg`, `png`, `gif`, `webp`
- File content types: JPEG, PNG, GIF, WebP
- Max file size: `app.upload.max-file-size-bytes`, default 5 MB
- Max request size: `spring.servlet.multipart.max-request-size`, default 20 MB
- Max image count per post: `app.post.max-image-count`, default 5

Implemented comment APIs:

- `GET /posts/{postId}/comments`
- `POST /posts/{postId}/comments`
- `PATCH /comments/{commentId}`
- `DELETE /comments/{commentId}`

Comment deletion is soft-delete based. Deleted comments remain in the tree with `status = DELETED` and placeholder content, so child replies can remain visible.

## User APIs

Implemented user APIs:

- `GET /users/me`
- `PATCH /users/me`
- `POST /users/me/image`
- `PATCH /users/me/password`
- `GET /users/me/posts`
- `GET /users/me/favorite-spots`
- `DELETE /users/me`

`GET /users/me/favorite-spots` supports:

- `page`
- `pageSize`
- `targetDate`

If `targetDate` is omitted, the API uses the current server date.

## Recommended next steps

1. Update the shared Notion API spec to match the cookie-based auth contract.
2. Add focused integration tests for auth cookie flow and CORS credentials.
3. Add deployment docs for required environment variables.
4. Review production OAuth redirect URLs and frontend callback handling.
