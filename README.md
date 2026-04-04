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

Deployment-конфигурация хранится прямо в этом репозитории, в каталоге [deploy](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/deploy).

Что там есть:

- [Dockerfile](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/Dockerfile) для сборки образа приложения
- [deploy/base](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/deploy/base) с базовыми Kubernetes manifests
- [deploy/overlays/dev](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/deploy/overlays/dev) с примером overlay для dev
- [deploy/README.md](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/deploy/README.md) с краткой инструкцией

Минимальный сценарий:

```bash
docker build -t poprog-knowledge-base-back:local .
kubectl apply -k deploy/overlays/dev
kubectl apply -n poprog-dev -f deploy/base/secret.example.yaml
```

Важно:

- манифесты предполагают, что PostgreSQL и Elasticsearch уже существуют в кластере
- `deploy/overlays/dev` сам создаёт namespace `poprog-dev`
- секреты intentionally не хранятся в git в “боевом” виде
- образ в manifests нужно привязывать к реальному тегу через CI/CD или overlay

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

### Поиск

- `GET /api/search?q=<query>&limit=20`

Поиск возвращает смешанный список результатов из публикаций и студенческих работ.  
В ответе есть тип источника, идентификатор сущности, контекст группы и данные для отображения карточки результата.
Поиск начинает работать от 3 символов и поддерживает частичные совпадения внутри индексируемых полей.

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
