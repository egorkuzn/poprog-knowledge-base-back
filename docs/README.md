# POPROG Back Docs

Документация backend-сервиса по состоянию на 2026-04-12.

## Локальный запуск

```bash
./gradlew bootRun --args='--spring.profiles.active=local --server.port=18080'
```

## Аутентификация в dev/local

Для защищенных account-endpoints используется заголовочная авторизация:

- `subject`
- `email`
- `name`
- `roles`

## Ключевые API (актуально)

- `/api/account/profile`
- `/api/account/chats`
- `/api/account/favorites`
- `/api/account/donations`
- `/api/account/donations/export.csv`
- `/api/account/donations/export.pdf`
- `/api/donations` (публичный донат без авторизации)
- `/api/admin/donations/kpi` (`[ADMIN]`)
- `/api/admin/donations/events` (`[ADMIN]`, фильтры + пагинация)
- `/api/admin/donations/export.csv` (`[ADMIN]`)
- `/api/admin/donations/export.pdf` (`[ADMIN]`)
- `/api/metrics/events`
- `/api/metrics/reports/dau-wau`
- `/api/metrics/reports/search-success`
- `/api/metrics/reports/ctr`
- `/api/market/apps`
- `/api/market/categories`
- `/api/search`
- `/api/files/{path}`

## CORS

Для локальной разработки включены origins:

- `http://localhost:5173`
- `http://localhost:5174`

(mapping для `/api/**`)

## Связанные документы

- `docs/openapi.yaml`
- `docs/dev-header-auth.md`
- `docs/gigachat-setup.md`
- `docs/gigachat-truststore-setup.md`
- `docs/architecture/README.md`
- `docs/security-audit-2026-04-22.md`
- `docs/min-hardware-requirements.md`
