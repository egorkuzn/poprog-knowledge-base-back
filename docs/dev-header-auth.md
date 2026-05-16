# Dev Header Auth (local fallback)

This project supports Keycloak JWT authentication and a local development fallback authentication that reads user identity from HTTP headers.

## Purpose

- unblock backend/frontend integration before Keycloak is ready;
- keep the same identity model that will be used with Keycloak (`subject`, `email`, `name`, `roles`).

## Security model

- Keycloak JWT auth is enabled with `app.auth.keycloak.enabled=true`;
- Keycloak tokens are read from `Authorization: Bearer <access_token>`;
- disabled by default (`app.auth.dev-headers.enabled=false`);
- enabled in local profile (`application-local.properties`);
- hard-blocked outside allowed profiles (`local`, `dev`) even if someone enables the flag.

## Keycloak JWT mode

Configure the backend with:

- `KEYCLOAK_AUTH_ENABLED=true`
- `KEYCLOAK_ISSUER_URI=http://95.174.95.251/realms/reflex-ide`
- `KEYCLOAK_JWK_SET_URI=http://95.174.95.251/realms/reflex-ide/protocol/openid-connect/certs`
- `KEYCLOAK_REALM=reflex-ide`
- `KEYCLOAK_CLIENT_ID=reflex-web-client`
- `KEYCLOAK_REQUIRED_AUDIENCE=reflex-web-client`
- `KEYCLOAK_ADMIN_REALM=master`
- `KEYCLOAK_ADMIN_CLIENT_ID=admin-cli`
- `KEYCLOAK_ADMIN_USERNAME=<admin>`
- `KEYCLOAK_ADMIN_PASSWORD=<password>`

The backend maps:

- `sub` -> `CurrentUser.subject`
- `email` -> `CurrentUser.email`
- `name` or `preferred_username` -> `CurrentUser.name`
- `realm_access.roles` and `resource_access[client-id].roles` -> `CurrentUser.roles`

Every authenticated Keycloak user receives the local `USER` role. Admin endpoints require `ADMIN`.

`POST /api/account/register` uses Keycloak Admin API to create a user in the portal realm. The backend stores only the created Keycloak user id, name and email; it does not store the password locally.

## Dev headers

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
