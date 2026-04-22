# Security Audit Report (2026-04-22)

## Scope

Audit target:
- Backend API (`Spring Boot`, `PostgreSQL`, `Elasticsearch`).
- Auth flows (local dev headers, transition path to Keycloak).
- File access and upload surface.
- Assistant endpoints and user-owned data.
- Admin-only donation/reporting endpoints.

Repository:
- `egorkuzn/poprog-knowledge-base-back`

## Summary

Current security baseline is acceptable for local/dev and pilot usage, but production readiness requires several hardening steps.

Key positives:
- Role checks for admin donation reporting APIs are in place.
- Owner-bound account data model is implemented for chats/favorites/donations.
- File API and upload constraints exist (including PDF-only flow).
- Dev-header auth is profile-gated and configurable.

Main gaps to close before production:
- Complete Keycloak OIDC integration and disable header auth in production.
- Formalize security headers and CORS allowlist per environment.
- Add stricter request throttling/rate-limits on search/assistant/public donation endpoints.
- Introduce a repeatable dependency and secret scanning routine in CI.

## Findings

### High

1. Authentication model not finalized for production
- Status: open.
- Risk: if dev-header mode is mistakenly enabled outside local/dev, attacker can spoof identity via headers.
- Required action:
  - complete Keycloak OIDC resource server flow (`#44`);
  - set hard production guardrail: `AUTH_DEV_HEADERS_ENABLED=false` + profile deny list.

2. Public payment flow not finalized (webhook trust and idempotency hardening pending)
- Status: open.
- Risk: weak webhook validation/idempotency may lead to payment state desynchronization or abuse.
- Required action:
  - complete YooKassa webhook verification and idempotent processing (`#41`);
  - add signature validation tests and replay protection.

### Medium

1. External-facing endpoints need explicit rate limiting
- Endpoints: `/api/search`, `/api/assistant/chat`, `/api/donations`.
- Risk: brute-force/abuse, model-cost amplification, noisy load spikes.
- Required action:
  - per-IP and per-user quotas;
  - burst limits + 429 policy;
  - endpoint-level monitoring.

2. Security headers policy should be explicit
- Risk: inconsistent browser-side protections in reverse-proxy variations.
- Required action:
  - enforce `Content-Security-Policy`, `X-Frame-Options`, `Referrer-Policy`, `X-Content-Type-Options` centrally.

3. Secret hygiene process needs formalization
- Risk: accidental credential leakage in `.env`-style local operations.
- Required action:
  - secret scanning in CI;
  - documented rotation policy for AI/payment credentials.

### Low

1. Dependency vulnerability routine can be strengthened
- Required action:
  - scheduled SCA report (weekly) and update cadence policy.

2. Security runbook can be expanded
- Required action:
  - add incident response mini-playbook (token leak, webhook abuse, file abuse).

## Verified controls already present

- Account data ownership isolation (chat/favorites/donation flows).
- Admin endpoint segregation for donation reporting exports and KPI.
- PDF-centric upload validation path.
- Structured assistant routing and chat history persistence with owner context.

## Remediation Plan

Priority 1 (must-do before production):
1. Complete Keycloak OIDC integration (`#44`).
2. Complete secure payment webhook flow (`#41`).
3. Enforce production-off switch for dev header auth.

Priority 2 (short-term hardening):
1. Add rate limiting for search/assistant/donation endpoints.
2. Apply explicit security header policy.
3. Add CI secret scanning + dependency scan workflow.

Priority 3 (operational maturity):
1. Security runbook and incident response checklist.
2. Quarterly permission and endpoint review.

## Task linkage

- Created for: `#53` `[SECURITY] Аудит безопасности приложения`.
- Follow-up dependencies:
  - `#44` Keycloak OIDC integration.
  - `#41` YooKassa production payment flow.
