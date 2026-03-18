# Deployment

В этом каталоге лежит app-specific deployment config для `poprog-knowledge-base-back`.

## Структура

- `base/` — базовые Kubernetes-манифесты приложения
- `overlays/dev/` — пример overlay для dev-окружения
- `base/secret.example.yaml` — шаблон секрета, который нужно скопировать и заполнить отдельно

## Что включено

- `Deployment`
- `Service`
- `PersistentVolumeClaim` для хранения загруженных файлов
- `ConfigMap` с переменными окружения приложения
- `Secret` как шаблон для `DB_USER` и `DB_PASSWORD`

## Пример применения

```bash
kubectl apply -k deploy/overlays/dev
kubectl apply -n poprog-dev -f deploy/base/secret.example.yaml
```

## Примечания

- Ожидается, что `PostgreSQL` и `Elasticsearch` уже доступны в кластере.
- `overlay` сам создаёт namespace `poprog-dev`, после чего в него можно положить секрет.
- Базовый образ приложения указан как `ghcr.io/egorkuzn/poprog-knowledge-base-back:latest` и должен быть заменён на реальный тег в overlay или CI/CD.
