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
    - [outbound/persistence/publication](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/outbound/persistence/publication)
    - [outbound/persistence/studentwork](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/outbound/persistence/studentwork)
    - [outbound/search/elasticsearch](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/outbound/search/elasticsearch)
  - [application/publication](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/application/publication)
  - [application/studentwork](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/application/studentwork)
  - [application/search](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/application/search)
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
