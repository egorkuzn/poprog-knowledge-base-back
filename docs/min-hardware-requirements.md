# Minimal Hardware Requirements for Deployment

## Goal

Define minimum hardware baselines for deploying the POPROG project stack:
- Backend API (`Spring Boot`).
- Frontend (`Vite` static build served by web server).
- PostgreSQL.
- Elasticsearch.
- File storage for uploaded documents.

Date: `2026-04-22`.

## Deployment profiles

### 1) Local/dev baseline (single host)

Use case:
- developer machine, demo, local QA.

Minimum:
- CPU: `4 vCPU`
- RAM: `8 GB`
- Storage:
  - system/app: `20 GB`
  - DB + search indices + files: `30 GB` (start)
- Network: `100 Mbps`

Recommended:
- CPU: `6 vCPU`
- RAM: `12 GB`
- Storage total: `80+ GB SSD`

Notes:
- Elasticsearch and PostgreSQL together are the main memory consumers.
- Keep JVM heap for backend moderate to avoid swap pressure.

### 2) Staging baseline (single host)

Use case:
- integration testing, release candidate validation.

Minimum:
- CPU: `6 vCPU`
- RAM: `16 GB`
- Storage: `120 GB SSD`
- Network: `200 Mbps`

Recommended:
- CPU: `8 vCPU`
- RAM: `24 GB`
- Storage: `200 GB SSD`

Notes:
- Staging should mirror production topology as closely as possible.
- Enable retention limits for logs, indices, and uploaded files.

### 3) Production baseline (split roles)

Use case:
- first public rollout with moderate load.

Minimum split:
1. App node (frontend + backend)
   - CPU: `4 vCPU`
   - RAM: `8 GB`
   - Storage: `40 GB SSD`
2. Data node (PostgreSQL + Elasticsearch)
   - CPU: `8 vCPU`
   - RAM: `24 GB`
   - Storage: `250 GB SSD` (NVMe preferred)

Recommended split:
1. App node: `6 vCPU`, `12 GB RAM`, `60 GB SSD`
2. PostgreSQL node: `6 vCPU`, `16 GB RAM`, `200 GB SSD`
3. Elasticsearch node: `6 vCPU`, `16 GB RAM`, `250 GB SSD`

## Resource model by component

### Backend API
- Sensitive to:
  - assistant requests,
  - search request concurrency,
  - file streaming.
- Minimum heap guideline:
  - dev/stage: `1-2 GB`
  - prod start: `2-4 GB`

### PostgreSQL
- Sensitive to:
  - chat history growth,
  - donation/payment event history,
  - metrics event ingestion.
- Reserve disk growth:
  - `+2-5 GB/month` at moderate traffic (depends on retention).

### Elasticsearch
- Sensitive to:
  - indexed PDF text volume,
  - search chunk count.
- Memory:
  - avoid running below `4 GB` container/JVM budget in non-trivial environments.

### File storage
- PDF-first model.
- Plan separate volume with quotas.
- Start with `100 GB` in production unless known lower dataset size.

## Capacity assumptions and first SLO baseline

Initial planning assumptions:
- concurrent active users: `50-100`
- search requests: up to `5-10 RPS`
- assistant requests: up to `1-3 RPS` burst
- file downloads: bursty, low average RPS

Initial service targets:
- `/api/search` p95: `< 600 ms`
- `/api/assistant/chat` p95:
  - widget route: `< 400 ms`
  - LLM route: depends on provider, target `< 6 s`
- error rate (5xx): `< 1%`

## Monitoring requirements

Track saturation continuously:
- CPU utilization per node and per container.
- RAM + swap.
- disk I/O and free space.
- DB connection pool usage.
- Elasticsearch cluster health, index size, query latency.
- endpoint latency and 4xx/5xx rates.

Alert thresholds (initial):
- CPU > `80%` for 10 min.
- RAM > `85%` for 10 min.
- disk free < `20%`.
- error rate > `2%` for 5 min.

## Scaling guidance

Scale-out triggers:
1. Search latency grows while CPU/RAM on data node stay high.
2. Assistant latency spikes due to concurrent LLM calls.
3. File storage growth exceeds forecast.

First scale actions:
1. Separate DB and Elasticsearch if still co-located.
2. Increase data-node RAM first, then CPU.
3. Add app replica behind reverse proxy.

## Task linkage

- Created for: `#52` `[DEPLOY] Минимальные требования к железу для развертывания`.
- This document is a baseline; tune after real traffic telemetry.
