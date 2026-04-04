# Настройка truststore для GigaChat

Этот файл описывает, как заполнить:

- `GIGACHAT_TRUST_STORE_PATH`
- `GIGACHAT_TRUST_STORE_PASSWORD`
- `GIGACHAT_TRUST_STORE_TYPE`

в локальном `.env` для работы с GigaChat через HTTPS.

## Зачем это нужно

Если при запросе к GigaChat видишь ошибку:

```text
PKIX path building failed
unable to find valid certification path to requested target
```

значит JVM не доверяет сертификату `ngw.devices.sberbank.ru`.

В этом проекте можно не трогать глобальный truststore Java, а подключить отдельный truststore только для GigaChat.

## Что нужно подготовить

Нужен файл truststore в формате:

- `PKCS12` (`.p12`) или
- `JKS` (`.jks`)

Внутри должен лежать сертификат или цепочка сертификатов, которым должен доверять клиент при обращении к GigaChat.

## Вариант 1. Если у тебя уже есть готовый truststore

Тогда просто укажи его в `.env`.

Пример:

```env
GIGACHAT_TRUST_STORE_PATH=/Users/egorkuznecov/certs/gigachat-truststore.p12
GIGACHAT_TRUST_STORE_PASSWORD=myStrongPassword
GIGACHAT_TRUST_STORE_TYPE=PKCS12
```

Что сюда писать:

- `GIGACHAT_TRUST_STORE_PATH`
  - абсолютный путь до файла truststore
  - пример: `/Users/egorkuznecov/certs/gigachat-truststore.p12`

- `GIGACHAT_TRUST_STORE_PASSWORD`
  - пароль, с которым был создан truststore
  - его задаёшь ты сам при создании файла

- `GIGACHAT_TRUST_STORE_TYPE`
  - `PKCS12`, если файл `.p12`
  - `JKS`, если файл `.jks`

## Вариант 2. Создать truststore самостоятельно из сертификата

Если у тебя есть сертификат, например `gigachat.crt`, можно собрать truststore локально.

### Для PKCS12

```bash
keytool -importcert \
  -alias gigachat \
  -file /absolute/path/to/gigachat.crt \
  -keystore /absolute/path/to/gigachat-truststore.p12 \
  -storetype PKCS12 \
  -storepass myStrongPassword \
  -noprompt
```

После этого в `.env`:

```env
GIGACHAT_TRUST_STORE_PATH=/absolute/path/to/gigachat-truststore.p12
GIGACHAT_TRUST_STORE_PASSWORD=myStrongPassword
GIGACHAT_TRUST_STORE_TYPE=PKCS12
```

### Для JKS

```bash
keytool -importcert \
  -alias gigachat \
  -file /absolute/path/to/gigachat.crt \
  -keystore /absolute/path/to/gigachat-truststore.jks \
  -storetype JKS \
  -storepass myStrongPassword \
  -noprompt
```

После этого в `.env`:

```env
GIGACHAT_TRUST_STORE_PATH=/absolute/path/to/gigachat-truststore.jks
GIGACHAT_TRUST_STORE_PASSWORD=myStrongPassword
GIGACHAT_TRUST_STORE_TYPE=JKS
```

## Как проверить содержимое truststore

### Для PKCS12

```bash
keytool -list -v \
  -keystore /absolute/path/to/gigachat-truststore.p12 \
  -storetype PKCS12 \
  -storepass myStrongPassword
```

### Для JKS

```bash
keytool -list -v \
  -keystore /absolute/path/to/gigachat-truststore.jks \
  -storetype JKS \
  -storepass myStrongPassword
```

Если команда показывает сертификаты без ошибок, truststore собран корректно.

## Что именно вписывать в `.env`

Пример для этого проекта:

```env
GIGACHAT_ENABLED=true
GIGACHAT_AUTH_URL=https://ngw.devices.sberbank.ru:9443
GIGACHAT_API_URL=https://gigachat.devices.sberbank.ru
GIGACHAT_AUTHORIZATION_KEY=<your_authorization_key>
GIGACHAT_SCOPE=GIGACHAT_API_PERS
GIGACHAT_MODEL=GigaChat
GIGACHAT_TRUST_STORE_PATH=/Users/egorkuznecov/certs/gigachat-truststore.p12
GIGACHAT_TRUST_STORE_PASSWORD=myStrongPassword
GIGACHAT_TRUST_STORE_TYPE=PKCS12
```

## После изменения `.env`

Перезапусти backend так, чтобы переменные из `.env` были подхвачены:

```bash
set -a
source .env
GRADLE_USER_HOME=.gradle ./gradlew bootRun --no-daemon
```

## Как понять, что truststore помог

Если всё настроено верно:

- backend стартует как обычно
- запрос к `POST /api/assistant/chat` больше не падает с `PKIX path building failed`

Если ошибка осталась, проверь:

- правильный ли сертификат импортирован
- не перепутан ли `storepass`
- соответствует ли `GIGACHAT_TRUST_STORE_TYPE` реальному формату файла
- указан ли абсолютный путь в `GIGACHAT_TRUST_STORE_PATH`
