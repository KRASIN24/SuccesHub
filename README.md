# SuccessHub

A full-stack **portfolio MVP** that turns everyday productivity into a daily game loop — without Snapchat-style FOMO.

You schedule real work, complete it, and the system responds the way games do: XP, streaks, boss HP, achievements, and earn-only loot. Goals are fought as **bosses**; cosmetics and shields come from loot you earned, never from a store.

> **Scope:** runnable local demo (Docker + Keycloak). This is intentionally **not** a production-hosted SaaS. Auth URLs, secrets, and Keycloak are local-dev posture.

---

## Why this project

Most “gamified todo” demos stop at a points counter. SuccessHub is built to show a **closed ritual**:

1. **Arrive** — dashboard shows level, streak, today’s missions  
2. **Commit** — schedule tasks; live XP preview builds anticipation  
3. **Execute** — complete work → XP, optional boss damage, celebrations  
4. **Collect** — achievements / loot stay pending until you open them  
5. **Seal** — optional “Seal the day” celebration; streak integrity does **not** depend on clicking it (lazy day-close on next visit)

Design bias: *anticipation toward closure* (rings, boss bars), not *fear of missing a streak button*.

---

## Demo

Large, standalone clips — same README pattern as [ClipboardManager.ahk](https://github.com/KRASIN24/ClipboardManager.ahk) and [AssetCraft](https://github.com/KRASIN24/AssetCraft): one heading, one short script, one full-width GIF.

Recording checklist & filenames: [README demo GIFs](https://app.notion.com/p/3e3b2614a9c081a39f96ee993b076a59). Prefer ~960px wide, muted, 8–20s. Broken images until the files exist mean “not recorded yet.”

### Daily hit

Complete a scheduled task on the dashboard → XP tick / daily progress moves.

![Daily hit](docs/images/loop-daily-hit.gif)

### Boss damage

Complete a task linked to a goal → boss damage toast → Goals HP bar drops.

![Boss damage](docs/images/loop-boss-damage.gif)

### Loot → equip

Open a pending Celestial Cache → reveal → equip a TITLE or FRAME on Profile → navbar / hero updates.

![Loot equip](docs/images/loop-loot-equip.gif)

### Seal the day

After a qualifying day → click **Seal the day** → UI flips to **Day sealed**.

![Seal the day](docs/images/loop-seal-day.gif)

---

## Architecture

| Layer | Choice | Why it matters for a portfolio |
|-------|--------|--------------------------------|
| API | Java 21 · Spring Boot 3 · JPA · **Liquibase** | Versioned schema, not `ddl-auto` spaghetti |
| Auth | **Keycloak** OIDC + Spring Security **BFF** | Tokens stay server-side; browser gets `HttpOnly` session + CSRF |
| UI | Angular 18 (standalone) · Tailwind | Lazy routes, shared profile signals, celebration overlays |
| DB | PostgreSQL 16 | Real persistence for streak days, loot, XP events |
| Infra | Docker Compose | Postgres + Keycloak + Adminer with one command |
| CI | GitHub Actions on PRs to `master` | Backend tests + frontend build/test — no CD yet |

```text
┌─────────────────────┐
│  Angular (:4200)    │  cookie session + XSRF header
└──────────┬──────────┘
           │ /api , /oauth2 , /login , /logout  (dev proxy)
           ▼
┌─────────────────────┐         ┌──────────────────┐
│ Spring Boot (:8082) │──OIDC──►│ Keycloak (:8080) │
│ BFF + domain API    │         └──────────────────┘
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ PostgreSQL (:5432)  │  Liquibase on startup
└─────────────────────┘
```

**Auth model (important):** the SPA never holds access tokens in `localStorage`. Spring is the OAuth2 client; after login the browser only sees the session cookie. That is a deliberate BFF choice, not a missing JWT tutorial.

---

## Product surface (MVP)

| Area | Route / entry | Behavior |
|------|---------------|----------|
| Dashboard | `/dashboard` | Daily ritual, missions, forecast, Seal the day |
| Quest log | `/tasks` | Categories, XP preview, boss link, complete for rewards |
| Boss battle | `/goals` | Goals as bosses; progress from linked task damage |
| Achievements | `/achievements` | Merit unlocks from engine rules |
| Celestial Cache | `/loot-boxes` | Pending boxes → open → inventory |
| Profile vault | `/profile` | Equip cosmetics; streak calendar; shields / utility cards |
| Login | Keycloak | Seed user for local demo |

### How rewards work (short)

- Completing an XP-enabled task goes through `GamificationEngine` (formula, daily cap, streak bonus, optional variable bonus, weekly-challenge bonus).  
- Linked tasks deal boss damage (`difficulty × bossDamageFactor`).  
- Streaks settle via **lazy close** on `GET /api/gamification/daily` — forgetting “Seal the day” does not break integrity.  
- Loot is **earn-only** (milestones, achievements, weekly reset). Opening rolls rewards into inventory (shields, boosts, titles, frames).

Swagger (dev profile): http://localhost:8082/swagger-ui/index.html

---

## Quick start

**Prerequisites:** Docker Desktop · JDK 21 · Node.js 20+

```powershell
# 1) Infrastructure (Postgres + Keycloak + Adminer)
cd docker
docker compose up -d --wait
cd ..

# 2) API — profile `dev`, http://localhost:8082
.\mvnw.cmd spring-boot:run

# 3) UI — http://localhost:4200
cd frontend
npm install
npm start
```

1. Open http://localhost:4200  
2. Sign in with Keycloak seed credentials: **`testuser` / `testpass`**  
3. You land on the dashboard after OAuth redirect  

| Service | Port / URL |
|---------|------------|
| Angular | http://localhost:4200 |
| Spring Boot | http://localhost:8082 |
| Swagger (`dev` only) | http://localhost:8082/swagger-ui/index.html |
| Keycloak | http://localhost:8080 |
| Adminer | http://localhost:8081 |
| Postgres | `localhost:5432` |

### IntelliJ (preferred day-to-day)

Shared Run/Debug configs live in [`.run/`](.run/) (`Full Stack`, infra start/stop, Spring `dev`/`prod`, Angular). Full guide: [docs/local-dev.md](docs/local-dev.md).

### Useful commands

```powershell
# Wipe Docker Postgres volume and re-apply Liquibase on next boot
cd docker
docker compose down -v
docker compose up -d --wait

# Same checks as CI
.\mvnw.cmd test
cd frontend
npm ci
npm run build
npm run test:ci
```

Spring profiles: **`dev`** (default — Swagger on, loot dev grants on) · **`prod`** (quieter, no grants, Swagger off). Details in [docs/local-dev.md](docs/local-dev.md).

---

## What’s intentionally out of scope

- Hosted public demo / CD pipeline ([docs/ci.md](docs/ci.md))  
- Boss V2 (crusades, henchmen), email/push notifications, real-money store  
- User timezone (day boundaries use **server TZ** in V1)

Those live on the project’s Post-MVP backlog in Notion — not half-implemented here.

---

## Repo map

| Path | Contents |
|------|----------|
| `src/main/java/...` | Spring Boot API, gamification domain, BFF security |
| `src/main/resources/db/changelog/` | Liquibase migrations |
| `frontend/` | Angular SPA |
| `docker/` | Compose for Postgres / Keycloak / Adminer |
| `docs/` | Local-dev, CI, README media |
| `.run/` | IntelliJ shared run configurations |
| `.github/workflows/` | PR CI |

---

## License

Private portfolio project — all rights reserved unless otherwise noted.
