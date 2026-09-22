# SuccessHub

Gamified productivity for a **portfolio MVP**: complete real tasks → earn XP, damage goals-as-bosses, protect streaks, open earn-only loot — never pay-to-win.

> Local demo only for now (Docker + Keycloak). Not a production SaaS deploy.

## Demo loops

Short clips of the core ritual (record → convert to GIF → drop into `docs/images/`). Recording scripts: [README demo GIFs](https://app.notion.com/p/3e3b2614a9c081a39f96ee993b076a59).

| Loop | Clip |
|------|------|
| Daily hit — complete a task, see XP | ![Daily hit](docs/images/loop-daily-hit.gif) |
| Boss damage — linked task hits a goal | ![Boss damage](docs/images/loop-boss-damage.gif) |
| Loot → equip — open Cache, wear cosmetics | ![Loot equip](docs/images/loop-loot-equip.gif) |
| Seal the day — close the daily ritual | ![Seal the day](docs/images/loop-seal-day.gif) |

*(Images appear after you add the four GIFs; placeholders are intentional until then.)*

## Stack

| Layer | Tech |
|-------|------|
| API | Java 21, Spring Boot 3, JPA, Liquibase |
| Auth | Keycloak OIDC, Spring Security **BFF** (session cookie + CSRF) |
| UI | Angular 18, Tailwind |
| Data | PostgreSQL 16 |
| Local infra | Docker Compose (Postgres, Keycloak, Adminer) |
| CI | GitHub Actions — `mvnw test` + Angular build/tests on PRs to `master` |

```text
Browser (Angular :4200)
    │  cookie session + XSRF
    ▼
Spring Boot BFF (:8082)  ──OIDC──►  Keycloak (:8080)
    │
    ▼
PostgreSQL (:5432)
```

## Quick start

**Prereqs:** Docker Desktop, JDK 21, Node.js 20+.

```powershell
# 1) Infra
cd docker
docker compose up -d --wait

# 2) Backend (repo root) — profile `dev`, port 8082
.\mvnw.cmd spring-boot:run

# 3) Frontend
cd frontend
npm install
npm start
```

Open http://localhost:4200 → login with Keycloak seed user **`testuser` / `testpass`**.

| Service | URL |
|---------|-----|
| App | http://localhost:4200 |
| API / Swagger (`dev`) | http://localhost:8082/swagger-ui/index.html |
| Keycloak | http://localhost:8080 |
| Adminer | http://localhost:8081 |

Prefer IntelliJ? Use shared run configs in [`.run/`](.run/) — see [docs/local-dev.md](docs/local-dev.md).

## What works in the MVP

- Keycloak login (BFF session)
- Tasks / Strategy Canvas with XP preview
- Goals as bosses (task-linked damage)
- Daily ritual: streak, Seal the day, weekly challenges
- Achievements + earn-only loot boxes + inventory cosmetics
- Profile streak calendar (shields / utility cards)

Deferred (not in this MVP): Boss V2, email/push, hosted demo — see project Notion **Post-MVP Backlog**.

## Docs

| Doc | Purpose |
|-----|---------|
| [docs/local-dev.md](docs/local-dev.md) | IntelliJ configs, ports, profiles, reset DB |
| [docs/ci.md](docs/ci.md) | GitHub Actions (no CD yet) |
| [docs/images/](docs/images/) | README GIFs |

## License

Private portfolio project — all rights reserved unless otherwise noted.
