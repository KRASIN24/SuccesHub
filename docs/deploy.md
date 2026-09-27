# Deploy configuration (W9 + W10)

Local `dev` keeps the same defaults as before (`localhost:4200` / Keycloak `:8080`).  
For any non-local host, set environment variables — **no code edits**.

Backend profile: `--spring.profiles.active=prod` (or `SPRING_PROFILES_ACTIVE=prod`).

## Two ways to run (do not confuse them)

| Mode | Command | Use when |
|------|---------|----------|
| **Day-to-day local** | Infra Compose + host Spring + `ng serve` | Coding, debugging, hot reload — see [local-dev.md](local-dev.md) |
| **Full stack in Docker** | `docker compose --profile app up` | Package smoke test / portfolio demo without JDK/Node on the host |

Dockerizing the apps does **not** remove local run. Profile `app` keeps default Compose = infra only.

## Full stack Compose (W10)

**IntelliJ:** use **Start Full Stack (Docker)** / **Stop Full Stack (Docker)** / **Stop Full Stack Apps (keep infra)** from [`.run/`](../.run/) — see [local-dev.md](local-dev.md).

From `docker/` (or repo root with `-f docker/docker-compose.yml`):

```powershell
# Infra + Spring BFF + nginx SPA (first build is slow: Maven + npm)
docker compose --profile app up -d --build --wait

# Stop app containers but keep Postgres/Keycloak (optional)
docker compose --profile app stop backend frontend

# Tear everything down (keeps DB volume unless -v)
docker compose --profile app down
```

Then open:

| URL | What |
|-----|------|
| http://localhost:4200 | SPA (nginx) + proxied `/api` + OAuth |
| http://localhost:8080 | Keycloak |
| http://localhost:8081 | Adminer |
| http://localhost:8082 | Spring BFF directly (optional debug) |

Login: Keycloak seed user `testuser` / `testpass` (or register).

### What the images do

- **`Dockerfile.backend`** (repo root) — multi-stage Maven Temurin 21 → JRE jar on `:8082`
- **`frontend/Dockerfile`** — `ng build` → nginx; `frontend/nginx.conf` mirrors `proxy.conf.json`
- Compose wires W9 env vars: browser Keycloak URLs use `localhost:8080`; backend token/JWK/admin use service name `keycloak`

### Port conflict tip

Do **not** run `ng serve` and the Compose `frontend` service at the same time (both want host `:4200`). Same for host Spring vs Compose `backend` on `:8082`.

## Auth / SPA URLs

| Variable | Default (local) | Purpose |
|----------|-----------------|--------|
| `SUCCESSHUB_FRONTEND_URL` | `http://localhost:4200/` | Post-login + post-logout SPA redirect |
| `SUCCESSHUB_CORS_ORIGINS` | `http://localhost:4200,http://localhost:8080` | Allowed CORS origins (comma-separated; include Keycloak for theme → BFF) |
| `SUCCESSHUB_KEYCLOAK_LOGOUT_URI` | `http://localhost:8080/realms/succeshub-realm/protocol/openid-connect/logout` | RP-initiated logout endpoint |
| `SUCCESSHUB_OAUTH_REDIRECT_URI` | `http://localhost:4200/login/oauth2/code/keycloak` | OAuth2 authorization-code redirect (must match Keycloak client) |
| `SUCCESSHUB_OAUTH_CLIENT_ID` | `succeshub-backend` | Keycloak client id |
| `SUCCESSHUB_OAUTH_CLIENT_SECRET` | `changeme` | Keycloak client secret — **replace in real deploy** |
| `SUCCESSHUB_KEYCLOAK_AUTH_URI` | `…/protocol/openid-connect/auth` | Authorization endpoint (**browser**) |
| `SUCCESSHUB_KEYCLOAK_TOKEN_URI` | `…/protocol/openid-connect/token` | Token endpoint (**server**; Compose uses `http://keycloak:8080/...`) |
| `SUCCESSHUB_KEYCLOAK_JWK_URI` | `…/protocol/openid-connect/certs` | JWKS (**server**) |
| `SUCCESSHUB_KEYCLOAK_USERINFO_URI` | `…/protocol/openid-connect/userinfo` | UserInfo (**server**) |
| `SUCCESSHUB_KEYCLOAK_SERVER_URL` | `http://localhost:8080` | Admin/API base (**server**; Compose uses `http://keycloak:8080`) |

Bound in code as `succeshub.auth.*` (`AuthProperties`) and `spring.security.oauth2.client.*`.

### Example (hosted SPA + API + Keycloak)

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:SUCCESSHUB_FRONTEND_URL = "https://app.example.com/"
$env:SUCCESSHUB_CORS_ORIGINS = "https://app.example.com"
$env:SUCCESSHUB_OAUTH_REDIRECT_URI = "https://app.example.com/login/oauth2/code/keycloak"
$env:SUCCESSHUB_OAUTH_CLIENT_SECRET = "<secret-from-keycloak>"
$env:SUCCESSHUB_KEYCLOAK_LOGOUT_URI = "https://auth.example.com/realms/succeshub-realm/protocol/openid-connect/logout"
$env:SUCCESSHUB_KEYCLOAK_AUTH_URI = "https://auth.example.com/realms/succeshub-realm/protocol/openid-connect/auth"
$env:SUCCESSHUB_KEYCLOAK_TOKEN_URI = "https://auth.example.com/realms/succeshub-realm/protocol/openid-connect/token"
$env:SUCCESSHUB_KEYCLOAK_JWK_URI = "https://auth.example.com/realms/succeshub-realm/protocol/openid-connect/certs"
$env:SUCCESSHUB_KEYCLOAK_USERINFO_URI = "https://auth.example.com/realms/succeshub-realm/protocol/openid-connect/userinfo"
.\mvnw.cmd spring-boot:run
```

## Database

| Variable | Default |
|----------|---------|
| `SUCCESSHUB_DB_URL` | `jdbc:postgresql://localhost:5432/succeshub` |
| `SUCCESSHUB_DB_USER` | `postgres` |
| `SUCCESSHUB_DB_PASSWORD` | `postgres` |

In Compose profile `app`, DB URL is `jdbc:postgresql://postgres:5432/succeshub` (service DNS name).

## Keycloak client checklist

When the SPA origin changes, update the Keycloak client `succeshub-backend`:

1. **Valid redirect URIs** — must include `SUCCESSHUB_OAUTH_REDIRECT_URI` (exact).
2. **Web origins** — SPA origin (CORS at IdP if used).
3. **Post logout redirect URIs** — e.g. `https://app.example.com/*` (must cover `SUCCESSHUB_FRONTEND_URL`).

Realm export for local Docker: `docker/keycloak-export/succeshub-realm-realm.json` (localhost only).

## Same-origin reverse proxy

SuccessHub’s SPA image uses nginx this way locally (`frontend/nginx.conf`):

- Browser origin is a single host (`localhost:4200`) → `SUCCESSHUB_FRONTEND_URL` / CORS / OAuth redirect match that host.
- Proxied paths: `/api`, `/public`, `/oauth2`, `/login`, `/logout`.
- Prefer forwarding `X-Forwarded-*` (already set in nginx). For HTTPS termination in front of nginx, enable Spring forwarded headers if redirects break (W11 follow-up).

## Related docs

- Local run: [local-dev.md](local-dev.md)
- CI (no CD yet): [ci.md](ci.md)
- Product README: [../README.md](../README.md)
- Design: [superpowers/specs/2026-09-27-app-docker-design.md](superpowers/specs/2026-09-27-app-docker-design.md)
