# Account deactivation grace lived elsewhere; this file is the W10 Docker design.

# Dockerize SuccessHub apps (W10) — design

**Status:** Approved (Approach A — same-origin nginx gateway)  
**Date:** 2026-09-27  
**Branch:** `feature/mvp-app-docker`

## Goal

Package Spring Boot API + Angular SPA as Docker images and add Compose services so infra + app + UI can start together, without replacing day-to-day local `mvnw` / `ng serve`.

## Approach

Same-origin nginx serves the SPA and proxies `/api`, `/public`, `/oauth2`, `/login`, `/logout` to the BFF (mirrors `frontend/proxy.conf.json`).

Compose **profile `app`**: default `docker compose up` stays infra-only; `--profile app` adds backend + frontend.

## Key decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Gateway | nginx in frontend image | Matches local proxy; one browser origin for cookies/CSRF |
| Local vs Docker | Both remain valid | Profile keeps IntelliJ infra configs working |
| Keycloak URLs | Split browser vs backchannel | Auth/logout use `localhost:8080`; token/JWK/userinfo/admin use `http://keycloak:8080` |
| Env | W9 `SUCCESSHUB_*` | No code edits for URLs |
| Backend profile | `prod` in Compose | Portfolio demo shape; Swagger off |

## Out of scope (W11)

Public hosting, TLS, managed secrets, CI image publish.
