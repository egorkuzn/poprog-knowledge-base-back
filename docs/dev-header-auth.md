# Dev Header Auth (local fallback)

This project supports a local development fallback authentication that reads user identity from HTTP headers.

## Purpose

- unblock backend/frontend integration before Keycloak is ready;
- keep the same identity model that will be used with Keycloak (`subject`, `email`, `name`, `roles`).

## Security model

- disabled by default (`app.auth.dev-headers.enabled=false`);
- enabled in local profile (`application-local.properties`);
- hard-blocked outside allowed profiles (`local`, `dev`) even if someone enables the flag.

## Headers

- `subject` (required)
- `email` (optional)
- `name` (optional)
- `roles` (optional, comma-separated, for example: `USER,ADMIN`)

## Controller usage

Use `@CurrentUserParam` to inject current user:

```kotlin
fun endpoint(@CurrentUserParam user: CurrentUser): SomeResponse { ... }
```

Optional user (public endpoint):

```kotlin
fun endpoint(@CurrentUserParam(required = false) user: CurrentUser?): SomeResponse { ... }
```

## Local example

```bash
curl -H "subject: dev-user-1" \
     -H "email: dev@example.com" \
     -H "name: Dev User" \
     -H "roles: USER,ADMIN" \
     http://localhost:8080/api/some-protected-endpoint
```
