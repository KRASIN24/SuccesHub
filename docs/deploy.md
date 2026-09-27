# Deploy configuration (W9)

Local `dev` keeps the same defaults as before (`localhost:4200` / Keycloak `:8080`).  
For any non-local host, set environment variables — **no code edits**.

Backend profile: `--spring.profiles.active=prod` (or `SPRING_PROFILES_ACTIVE=prod`).

## Auth / SPA URLs

| Variable | Default (local) | Purpose |
|----------|-----------------|--------|
| `SUCCESSHUB_FRONTEND_URL` | `http://localhost:4200/` | Post-login + post-logout SPA redirect |
| `SUCCESSHUB_CORS_ORIGINS` | `http://localhost:4200,http://localhost:8080` | Allowed CORS origins (comma-separated; include Keycloak for theme → BFF) |
| `SUCCESSHUB_KEYCLOAK_LOGOUT_URI` | `http://localhost:8080/realms/succeshub-realm/protocol/openid-connect/logout` | RP-initiated logout endpoint |
| `SUCCESSHUB_OAUTH_REDIRECT_URI` | `http://localhost:4200/login/oauth2/code/keycloak` | OAuth2 authorization-code redirect (must match Keycloak client) |
| `SUCCESSHUB_OAUTH_CLIENT_ID` | `succeshub-backend` | Keycloak client id |
| `SUCCESSHUB_OAUTH_CLIENT_SECRET` | `changeme` | Keycloak client secret — **replace in real deploy** |
| `SUCCESSHUB_KEYCLOAK_AUTH_URI` | `…/protocol/openid-connect/auth` | Authorization endpoint |
| `SUCCESSHUB_KEYCLOAK_TOKEN_URI` | `…/protocol/openid-connect/token` | Token endpoint |
| `SUCCESSHUB_KEYCLOAK_JWK_URI` | `…/protocol/openid-connect/certs` | JWKS |
| `SUCCESSHUB_KEYCLOAK_USERINFO_URI` | `…/protocol/openid-connect/userinfo` | UserInfo |

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

## Keycloak client checklist

When the SPA origin changes, update the Keycloak client `succeshub-backend`:

1. **Valid redirect URIs** — must include `SUCCESSHUB_OAUTH_REDIRECT_URI` (exact).
2. **Web origins** — SPA origin (CORS at IdP if used).
3. **Post logout redirect URIs** — e.g. `https://app.example.com/*` (must cover `SUCCESSHUB_FRONTEND_URL`).

Realm export for local Docker: `docker/keycloak-export/succeshub-realm-realm.json` (localhost only).

## Same-origin reverse proxy

If nginx (or similar) serves the SPA and proxies `/api`, `/oauth2`, `/login`, `/logout` to Spring:

- Browser origin is a single HTTPS host → set `SUCCESSHUB_FRONTEND_URL` / CORS / OAuth redirect to that host.
- Prefer forwarding `X-Forwarded-*` and enabling Spring forwarded headers so redirects stay correct behind TLS termination (follow-up if needed for your host).

## Related docs

- Local run: [local-dev.md](local-dev.md)
- CI (no CD yet): [ci.md](ci.md)
- Product README: [../README.md](../README.md)
