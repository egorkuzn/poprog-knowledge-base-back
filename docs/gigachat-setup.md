# Подключение GigaChat API

Этот файл описывает, что нужно для подключения GigaChat API в `poprog-knowledge-base-back`.

## Что уже реализовано в backend

В проекте уже есть:

- OAuth-получение access token для GigaChat
- кэширование токена до истечения срока действия
- клиент для `chat/completions`
- публичная ручка:
  - `POST /api/assistant/chat`

Код интеграции:

- [GigaChatConfig.kt](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/config/GigaChatConfig.kt)
- [GigaChatProperties.kt](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/config/GigaChatProperties.kt)
- [GigaChatTokenProvider.kt](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/outbound/assistant/gigachat/GigaChatTokenProvider.kt)
- [GigaChatAiAssistantAdapter.kt](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/outbound/assistant/gigachat/GigaChatAiAssistantAdapter.kt)
- [AiAssistantController.kt](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/src/main/kotlin/com/example/poprogknowledgebaseback/adapters/inbound/web/assistant/AiAssistantController.kt)

Отдельная инструкция по truststore:

- [gigachat-truststore-setup.md](/Users/egorkuznecov/IdeaProjects/poprog-knowledge-base-back/docs/gigachat-truststore-setup.md)

## Что нужно получить снаружи

Для реального подключения нужны:

1. Доступ к GigaChat API в кабинете Sber Developers
2. `authorization key` для OAuth
3. Разрешённый `scope`
4. Выбранная модель, например `GigaChat` или `GigaChat-Pro`

## Какие переменные окружения нужны

Backend использует такие параметры:

```env
GIGACHAT_ENABLED=true
GIGACHAT_AUTH_URL=https://ngw.devices.sberbank.ru:9443
GIGACHAT_API_URL=https://gigachat.devices.sberbank.ru
GIGACHAT_AUTHORIZATION_KEY=<your_basic_authorization_key>
GIGACHAT_SCOPE=GIGACHAT_API_PERS
GIGACHAT_MODEL=GigaChat
GIGACHAT_TRUST_STORE_PATH=/absolute/path/to/gigachat-truststore.p12
GIGACHAT_TRUST_STORE_PASSWORD=<truststore_password>
GIGACHAT_TRUST_STORE_TYPE=PKCS12
```

### Пояснения

- `GIGACHAT_ENABLED`
  - включает интеграцию
  - если `false`, ручка `/api/assistant/chat` существует, но отвечает `503`

- `GIGACHAT_AUTH_URL`
  - базовый URL для получения OAuth token
  - по умолчанию: `https://ngw.devices.sberbank.ru:9443`

- `GIGACHAT_API_URL`
  - базовый URL для запросов к модели
  - по умолчанию: `https://gigachat.devices.sberbank.ru`

- `GIGACHAT_AUTHORIZATION_KEY`
  - ключ для заголовка `Authorization: Basic ...`
  - можно передавать либо уже с префиксом `Basic `, либо только значение ключа

- `GIGACHAT_SCOPE`
  - scope для OAuth-запроса
  - по умолчанию: `GIGACHAT_API_PERS`

- `GIGACHAT_MODEL`
  - модель, в которую backend будет отправлять `chat/completions`

- `GIGACHAT_TRUST_STORE_PATH`
  - путь к truststore с сертификатом или цепочкой сертификатов для `ngw.devices.sberbank.ru`
  - если не задан, используется стандартный truststore JVM

- `GIGACHAT_TRUST_STORE_PASSWORD`
  - пароль от truststore
  - обязателен, если задан `GIGACHAT_TRUST_STORE_PATH`

- `GIGACHAT_TRUST_STORE_TYPE`
  - тип truststore
  - по умолчанию: `PKCS12`

## Как backend работает с GigaChat

### 1. Получение токена

Backend вызывает:

```text
POST {GIGACHAT_AUTH_URL}/api/v2/oauth
```

С заголовками:

```text
Authorization: Basic <key>
RqUID: <uuid>
Content-Type: application/x-www-form-urlencoded
```

И телом:

```text
scope=GIGACHAT_API_PERS
```

### 2. Запрос к модели

После получения токена backend вызывает:

```text
POST {GIGACHAT_API_URL}/api/v1/chat/completions
```

С заголовком:

```text
Authorization: Bearer <access_token>
```

## Как запустить локально

### Вариант 1. Через переменные окружения в shell

```bash
export GIGACHAT_ENABLED=true
export GIGACHAT_AUTHORIZATION_KEY='<your_basic_authorization_key>'
export GIGACHAT_SCOPE='GIGACHAT_API_PERS'
export GIGACHAT_MODEL='GigaChat'
export GIGACHAT_TRUST_STORE_PATH='/absolute/path/to/gigachat-truststore.p12'
export GIGACHAT_TRUST_STORE_PASSWORD='<truststore_password>'
export GIGACHAT_TRUST_STORE_TYPE='PKCS12'
GRADLE_USER_HOME=.gradle ./gradlew bootRun --no-daemon
```

### Вариант 2. Через IDE run configuration

Добавь те же переменные в конфигурацию запуска Spring Boot приложения.

## Как проверить ручку

Swagger:

- [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

Пример запроса:

```bash
curl -X POST http://localhost:8080/api/assistant/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "messages": [
      {
        "role": "system",
        "content": "Отвечай кратко и по делу"
      },
      {
        "role": "user",
        "content": "Привет! Кто ты?"
      }
    ]
  }'
```

Пример ответа:

```json
{
  "content": "Привет! Я ИИ-ассистент на базе GigaChat.",
  "model": "GigaChat",
  "finishReason": "stop",
  "promptTokens": 18,
  "completionTokens": 12,
  "totalTokens": 30
}
```

## Что проверить, если не работает

### 503 от backend

Проверь:

- `GIGACHAT_ENABLED=true`
- задан `GIGACHAT_AUTHORIZATION_KEY`

### 401 / 403 от GigaChat

Проверь:

- корректность authorization key
- правильный scope
- активирован ли доступ к GigaChat API в кабинете

### Таймауты / сетевые ошибки

Проверь:

- доступность `ngw.devices.sberbank.ru:9443`
- доступность `gigachat.devices.sberbank.ru`
- прокси, firewall, VPN и корпоративные ограничения

### SSL / сертификат не доверен

Если видишь ошибку вида `PKIX path building failed`, значит JVM не доверяет сертификату GigaChat OAuth endpoint.

В этом проекте это решается отдельным truststore для GigaChat:

1. Собери truststore с нужным сертификатом или цепочкой сертификатов
2. Заполни:
   - `GIGACHAT_TRUST_STORE_PATH`
   - `GIGACHAT_TRUST_STORE_PASSWORD`
   - `GIGACHAT_TRUST_STORE_TYPE`
3. Перезапусти backend

Это влияет только на GigaChat-клиенты и не меняет глобальные SSL-настройки JVM.

## Полезные ссылки

- [GigaChat: работа с историей чата](https://developers.sber.ru/docs/ru/gigachat/guides/keeping-context)
- [GigaChat: использование SDK и chat completions](https://developers.sber.ru/docs/ru/gigachat/guides/using-sdks)
