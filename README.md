# POPROG Knowledge Base Back

Backend-сервис на Kotlin + Spring Boot для базы знаний POPROG.  
Сервис хранит публикации и студенческие работы в PostgreSQL, управляет схемой через Liquibase, предоставляет Swagger/OpenAPI и поддерживает полнотекстовый поиск через Elasticsearch.

В репозитории также лежит app-specific deployment config для контейнерного запуска и Kubernetes-деплоя.

## Что умеет сервис

- Хранить и отдавать публикации, сгруппированные по годам.
- Хранить и отдавать студенческие работы, сгруппированные по типу проекта.
- Выполнять CRUD-операции для публикаций.
- Выполнять CRUD-операции для студенческих работ.
- Выполнять полнотекстовый поиск по публикациям и студенческим работам через `/api/search`.
- Автоматически накатывать миграции Liquibase при старте приложения.
- Публиковать Swagger UI и OpenAPI-спецификацию.

## Технологии

- `Kotlin 2.2`
- `Spring Boot 4`
- `Spring Web MVC`
- `Spring Data JPA`
- `Spring Data Elasticsearch`
- `PostgreSQL 18`
- `Liquibase`
- `Testcontainers`
- `Springdoc OpenAPI`
- `Docker`
- `Kustomize / Kubernetes manifests`
- `GigaChat API`

## Запуск

### 1. Поднять инфраструктуру

В проекте уже подготовлен `docker-compose.yml` с PostgreSQL и Elasticsearch:

```bash
docker compose up -d
```

Сервисы будут доступны по адресам:

- PostgreSQL: `localhost:5432`
- Elasticsearch: `localhost:9200`

### 2. Запустить backend

```bash
./gradlew bootRun
```

По умолчанию используются такие настройки:

- `DB_URL=jdbc:postgresql://localhost:5432/poprog_kb`
- `DB_USER=postgres`
- `DB_PASSWORD=postgres`
- `ELASTICSEARCH_URIS=http://localhost:9200`
- `SEARCH_ENABLED=true`

При необходимости их можно переопределить через переменные окружения.

### Интеграция с GigaChat

В проекте подготовлен backend-слой интеграции с GigaChat API для будущего чата с ИИ-агентом.

Подробная инструкция по подключению:

- [docs/gigachat-setup.md](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/docs/gigachat-setup.md)

Переменные окружения:

- `GIGACHAT_ENABLED=false`
- `GIGACHAT_AUTH_URL=https://ngw.devices.sberbank.ru:9443`
- `GIGACHAT_API_URL=https://gigachat.devices.sberbank.ru`
- `GIGACHAT_AUTHORIZATION_KEY=<base64 authorization key>`
- `GIGACHAT_SCOPE=GIGACHAT_API_PERS`
- `GIGACHAT_MODEL=GigaChat`

Что уже реализовано:

- получение OAuth-токена GigaChat
- кэширование токена до истечения срока действия
- отправка chat completion-запросов в GigaChat
- application service для будущего использования из chat endpoint

### 3. Проверить Swagger

- Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- OpenAPI YAML-контракт в репозитории: [docs/openapi.yaml](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/docs/openapi.yaml)

## Deployment

Deployment-конфигурация хранится в репозиториях приложения и описывает как локальный контейнерный запуск backend, так и production-поставку на виртуальную машину. Подробное описание deployment-контура, требований и текст для дипломного отчета вынесены в общий документ:

- [docs/deployment-production-report.md](docs/deployment-production-report.md)

### Production-схема

Production-развертывание выполнено на виртуальной машине `poprog-edge`, которая принимает публичный HTTP-трафик. На машине установлен `nginx`, работающий как reverse proxy. Внешний пользователь открывает портал по корневому адресу `http://95.174.95.251/`, а nginx перенаправляет запросы во внутренние контейнеры:

- frontend: `127.0.0.1:18083`;
- backend: `127.0.0.1:18082`;
- Keycloak/Auth routes: `127.0.0.1:8080`;
- backend API: `/api/` и `/portal-api/`;
- файлы: `/files/`;
- health checks: `/actuator/`;
- OpenAPI: `/swagger-ui/` и `/v3/`.

Маршруты `/portal` и `/portal/...` оставлены только как redirect на корневые URL, чтобы старые ссылки не ломались. Основной портал должен открываться без технического префикса `/portal`.

### Backend-контейнер

Backend собирается в Docker-образ из [Dockerfile](Dockerfile) и публикуется в GitHub Container Registry:

```text
ghcr.io/egorkuzn/poprog-knowledge-base-back:<commit-sha>
ghcr.io/egorkuzn/poprog-knowledge-base-back:latest
```

На сервере backend запускается через [deploy/compose/docker-compose.prod.yml](deploy/compose/docker-compose.prod.yml). Compose-файл:

- запускает сервис `portal-backend`;
- подключает контейнер к docker-сети `auth-ride_default`;
- пробрасывает порт контейнера `8080` только на локальный адрес `127.0.0.1:18082`;
- подключает volume `portal_backend_storage` для загруженных файлов;
- берет чувствительные параметры из `.env`, который хранится на сервере и не коммитится.

Ключевые runtime-переменные:

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://postgres:5432/poprog_kb
DB_USER=${POPROG_DB_USER}
DB_PASSWORD=${POPROG_DB_PASSWORD}
SEARCH_ENABLED=true
ELASTICSEARCH_URIS=http://elasticsearch:9200
FILES_STORAGE_DIR=/app/storage
FILES_BASE_URL=/files
GIGACHAT_ENABLED=false
KEYCLOAK_AUTH_ENABLED=true
KEYCLOAK_BASE_URL=http://95.174.95.251
KEYCLOAK_ISSUER_URI=http://95.174.95.251/realms/reflex-ide
KEYCLOAK_JWK_SET_URI=http://95.174.95.251/realms/reflex-ide/protocol/openid-connect/certs
KEYCLOAK_REALM=reflex-ide
KEYCLOAK_CLIENT_ID=reflex-web-client
KEYCLOAK_REQUIRED_AUDIENCE=reflex-web-client
KEYCLOAK_ADMIN_REALM=master
KEYCLOAK_ADMIN_CLIENT_ID=admin-cli
KEYCLOAK_ADMIN_USERNAME=${KEYCLOAK_ADMIN_USERNAME}
KEYCLOAK_ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD}
AUTH_DEV_HEADERS_ENABLED=false
```

PostgreSQL переиспользуется из существующей docker-сети. Elasticsearch считается опциональной подсистемой: если индекс недоступен, backend продолжает обслуживать основные API, а поиск использует fallback по PostgreSQL.

### CI/CD

Для backend используется workflow [.github/workflows/backend-ci.yml](.github/workflows/backend-ci.yml).

При pull request выполняется:

```bash
./gradlew test --no-daemon
./gradlew bootJar --no-daemon
```

При push в `main` выполняется production-деплой:

1. GitHub Actions собирает Docker-образ.
2. Образ публикуется в GHCR с тегами commit SHA и `latest`.
3. Workflow готовит SSH-доступ через `DEPLOY_SSH_PRIVATE_KEY`.
4. На сервер загружается актуальный `docker-compose.yml`.
5. Сервер выполняет `docker compose pull portal-backend`.
6. Старый контейнер `poprog-portal-backend` удаляется.
7. Новый контейнер запускается через `docker compose up -d portal-backend`.
8. Workflow ожидает успешный ответ `http://127.0.0.1:18082/actuator/health`.
9. Выполняется публичная smoke-проверка через `http://<host>/actuator/health` и `http://<host>/api/publications/grouped`.

Секреты, которые нужны workflow:

- `DEPLOY_HOST`;
- `DEPLOY_USER`;
- `DEPLOY_SSH_PRIVATE_KEY`;
- стандартный `GITHUB_TOKEN` для публикации образа в GHCR.

Пароли базы данных и runtime-секреты остаются в `.env` на сервере. Workflow проверяет наличие `.env`, но не выводит его содержимое.

### Требования к deployment-контуру

Функциональные требования высокого приоритета:

- портал должен открываться по корневому адресу без `/portal`;
- backend API должен быть доступен через nginx по `/api/` и `/portal-api/`;
- файлы PDF должны отдаваться через `/files/`;
- frontend и backend должны деплоиться независимо;
- после деплоя должен выполняться health check;
- основные API должны работать даже при недоступности Elasticsearch;
- backend должен автоматически применять Liquibase-миграции при старте.

Функциональные требования среднего приоритета:

- Docker-образы должны публиковаться в GHCR;
- каждый образ должен иметь тег commit SHA;
- старые `/portal/...` ссылки должны перенаправляться на новые маршруты;
- OpenAPI-документация должна быть доступна через nginx;
- загруженные файлы должны сохраняться между перезапусками контейнера.

Нефункциональные требования:

- секреты не должны храниться в git;
- внешние запросы должны проходить через nginx, а контейнеры должны слушать только локальные порты;
- деплой должен перезапускать только измененный сервис;
- CI/CD должен останавливать поставку при ошибке тестов или health check;
- при ошибке деплоя workflow должен выводить последние логи backend-контейнера;
- система должна быть пригодна для малоресурсных виртуальных машин.

### Контроль после деплоя

```bash
curl -fsS http://95.174.95.251/actuator/health
curl -fsS http://95.174.95.251/api/publications/grouped
curl -fsS http://95.174.95.251/api/student-works/grouped
curl -fsS "http://95.174.95.251/api/search?q=post&limit=8"
```

На сервере:

```bash
docker ps
docker logs --tail=160 poprog-portal-backend
sudo nginx -t
sudo systemctl reload nginx
```

### Kubernetes manifests

В каталоге [deploy](deploy) также сохранены Kubernetes-манифесты:

- [deploy/base](deploy/base) с базовыми manifests;
- [deploy/overlays/dev](deploy/overlays/dev) с dev overlay;
- [deploy/base/secret.example.yaml](deploy/base/secret.example.yaml) как шаблон секрета.

Минимальный сценарий для Kubernetes:

```bash
docker build -t poprog-knowledge-base-back:local .
kubectl apply -k deploy/overlays/dev
kubectl apply -n poprog-dev -f deploy/base/secret.example.yaml
```

Манифесты предполагают, что PostgreSQL и Elasticsearch уже существуют в кластере. Production на текущем сервере использует Docker Compose, а Kubernetes-конфигурация оставлена как воспроизводимая альтернатива для следующего этапа развития инфраструктуры.

## Основные ручки

### Публикации

- `GET /api/publications/grouped`
- `POST /api/publications`
- `PUT /api/publications/{id}`
- `DELETE /api/publications/{id}`

### Студенческие работы

- `GET /api/student-works/grouped`
- `POST /api/student-works`
- `PUT /api/student-works/{id}`
- `DELETE /api/student-works/{id}`

`GET`-ответы grouped для публикаций и студенческих работ теперь содержат `id` каждого элемента, чтобы фронтенд мог прокручивать к конкретному найденному результату поиска.

### Поиск

- `GET /api/search?q=<query>&limit=20`

Поиск возвращает смешанный список результатов из публикаций и студенческих работ.  
В ответе есть тип источника, идентификатор сущности, контекст группы и данные для отображения карточки результата.
Поиск начинает работать от 3 символов, поддерживает частичные совпадения и учитывает текст внутри PDF-документов.

### ИИ-ассистент

- `POST /api/assistant/chat`
- `GET /api/assistant/chats/{chatId}/messages`

`POST /api/assistant/chat` принимает новые сообщения для диалога. Если `chatId` не передан, backend создаёт новый диалог. Если `chatId` передан, backend подмешивает сохранённую историю, отправляет запрос в GigaChat и сохраняет новые сообщения вместе с ответом ассистента.

`GET /api/assistant/chats/{chatId}/messages` возвращает сохранённую историю сообщений в хронологическом порядке.

### Меню проектов

- `GET /api/projects/menu`
- `POST /api/projects/menu/sections`
- `PUT /api/projects/menu/sections/{id}`
- `DELETE /api/projects/menu/sections/{id}`
- `POST /api/projects/menu/items`
- `PUT /api/projects/menu/items/{id}`
- `DELETE /api/projects/menu/items/{id}`
- `POST /api/projects/menu/promos`
- `PUT /api/projects/menu/promos/{id}`
- `DELETE /api/projects/menu/promos/{id}`
- `POST /api/projects/menu/resources/upload`

### Файлы

- `GET /api/files/{path}`

`GET /api/files/{path}` — публичная ручка получения PDF-файлов и изображений меню проекта по относительному пути в PostgreSQL-хранилище (например, `publications/<file>.pdf`, `student-works/<file>.pdf` или `projects-menu/<file>.png`). При успехе файл отдается с `Content-Disposition: inline`, чтобы его можно было открыть в браузере.

### Обратная связь

- `POST /api/feedback/usefulness`

`POST /api/feedback/usefulness` — публичная ручка для сохранения реакции пользователя о полезности сайта. Обязательные поля: `helpful`, `userName`, `userEmail`. Опциональные поля: `source`, `comment`. На backend также сохраняются `ipAddress` и `userAgent` из запроса.

### Личный кабинет

- `GET /api/account/profile`
- `PUT /api/account/profile`

`/api/account/*` требует авторизацию. В production backend принимает `Authorization: Bearer <access_token>` от Keycloak и извлекает из JWT `sub`, `email`, `name`/`preferred_username`, роли realm/client. Для локальной разработки можно использовать dev-заголовки (только для local/dev при `app.auth.dev-headers.enabled=true`):

- `subject` (обязательный)
- `email` (опциональный)
- `name` (опциональный)
- `roles` (опциональный, пример: `USER,ADMIN`)

Для Keycloak-интеграции используются переменные:

- `KEYCLOAK_AUTH_ENABLED`
- `KEYCLOAK_ISSUER_URI`
- `KEYCLOAK_JWK_SET_URI`
- `KEYCLOAK_REALM`
- `KEYCLOAK_CLIENT_ID`
- `KEYCLOAK_REQUIRED_AUDIENCE`
- `KEYCLOAK_ADMIN_REALM`
- `KEYCLOAK_ADMIN_CLIENT_ID`
- `KEYCLOAK_ADMIN_USERNAME`
- `KEYCLOAK_ADMIN_PASSWORD`

`POST /api/account/register` создает пользователя в Keycloak через Admin API и сохраняет локальный профиль портала. Новые пользователи получают роль `USER`; административные роли назначаются отдельно в Keycloak.

`GET /api/projects/menu` возвращает полную структуру hover-меню раздела "Проекты": секции, CTA, карточки направлений и промо-блоки.

CRUD-ручки позволяют менять метаданные меню через backend, а `POST /api/projects/menu/resources/upload` сохраняет изображение или другой ресурс и возвращает публичный URL, который можно подставить в item или promo.

## Тесты

Запуск тестов:

```bash
./gradlew test
```

В тестовом профиле поиск отключён через `app.search.enabled=false`, поэтому интеграционные тесты не зависят от локально поднятого Elasticsearch.

## Структура проекта

Проект организован в стиле гексагональной архитектуры.

```text
com.example.poprogknowledgebaseback
├── adapters
│   ├── inbound
│   │   └── web
│   └── outbound
│       ├── persistence
│       └── search
├── application
├── config
└── domain
```

### Дерево ссылок

- [src/main/kotlin/com/example/poprogknowledgebaseback](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback)
  - [adapters](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters)
    - [inbound/web/publication](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/inbound/web/publication)
    - [inbound/web/studentwork](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/inbound/web/studentwork)
    - [inbound/web/search](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/inbound/web/search)
    - [inbound/web/assistant](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/inbound/web/assistant)
    - [inbound/web/projectmenu](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/inbound/web/projectmenu)
    - [outbound/persistence/publication](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/outbound/persistence/publication)
    - [outbound/persistence/studentwork](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/outbound/persistence/studentwork)
    - [outbound/persistence/assistant](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/outbound/persistence/assistant)
    - [outbound/persistence/projectmenu](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/outbound/persistence/projectmenu)
    - [outbound/search/elasticsearch](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/outbound/search/elasticsearch)
    - [outbound/assistant/gigachat](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/outbound/assistant/gigachat)
  - [application/assistant](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/application/assistant)
  - [application/projectmenu](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/application/projectmenu)
  - [application/publication](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/application/publication)
  - [application/studentwork](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/application/studentwork)
  - [application/search](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/application/search)
  - [domain/assistant](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/domain/assistant)
  - [domain/projectmenu](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/domain/projectmenu)
  - [domain/publication](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/domain/publication)
  - [domain/studentwork](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/domain/studentwork)
  - [domain/search](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/domain/search)
  - [config](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/config)
- [src/main/resources/db/changelog](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/resources/db/changelog)
- [src/test/kotlin/com/example/poprogknowledgebaseback](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/test/kotlin/com/example/poprogknowledgebaseback)
- [docker-compose.yml](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/docker-compose.yml)
- [docs/openapi.yaml](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/docs/openapi.yaml)
- [deploy](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/deploy)
- [build.gradle.kts](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/build.gradle.kts)
- [docs/db-schema.puml](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/docs/db-schema.puml)
- [docs/db-schema.png](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/docs/db-schema.png)

## Полезные команды

Запуск инфраструктуры:

```bash
docker compose up -d
```

Остановка инфраструктуры:

```bash
docker compose down
```

Запуск приложения:

```bash
./gradlew bootRun
```

Запуск тестов:

```bash
./gradlew test
```
