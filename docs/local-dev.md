# Local development (SuccessHub)

Use IntelliJ **Run/Debug** configurations from the [`.run/`](../.run/) folder instead of manual terminal commands.

## One-time setup

1. **Docker Desktop** — running before any infra config.
2. **JDK 21** — required by the Maven project (`pom.xml`).
3. **Node.js** — for the Angular frontend.
4. Open the **repo root** in IntelliJ and **Load Maven Project** so the `succes-hub` module resolves.
5. In `frontend/`, run `npm install` once (IntelliJ does not do this automatically).
6. Set the **Node interpreter** when prompted (Angular Dev Server config), or under **Settings → Languages & Frameworks → Node.js**.

Bundled IntelliJ plugins used: **Shell Script**, **Java**, **JavaScript/TypeScript**. **Docker** is optional (view container logs in the IDE).

## Run configurations

| Config | What it does |
|--------|----------------|
| **Start Infrastructure** | `docker compose up -d --wait` — Postgres (healthy), Keycloak, Adminer |
| **Stop Infrastructure** | `docker compose down` |
| **Reset Dev DB (wipes volumes)** | `docker compose down -v` then `up -d --wait` — destroys the containerized Postgres volume |
| **Spring Boot (SuccesHub)** | Starts backend on http://localhost:8082 with profile **`dev`** (runs **Start Infrastructure** first) |
| **Spring Boot (SuccesHub) - Prod** | Same as above with profile **`prod`** (no loot dev grants, quieter logs, Swagger off) |
| **Angular Dev Server** | `npm start` in `frontend/` → http://localhost:4200 |
| **Full Stack** | Spring Boot + Angular in parallel (infra starts via Spring Boot’s before-launch hook) |

### Day-to-day

| Scenario | Run config |
|----------|------------|
| Fresh morning / new clone | **Full Stack** |
| Backend only | **Spring Boot (SuccesHub)** |
| Frontend only (backend already up) | **Angular Dev Server** |
| End of day | **Stop Infrastructure** |

### Clear the database and re-apply migrations

Postgres runs in Docker; schema changes are applied by **Liquibase** when Spring Boot starts.

1. Run **Reset Dev DB (wipes volumes)** — removes the Postgres data volume and recreates empty containers.
2. Run **Spring Boot (SuccesHub)** or **Full Stack** — Liquibase runs all migrations on startup.

Do **not** use Reset as a before-launch step on Spring Boot; it is destructive and intended only when you need a clean slate.

## Ports

| Service | Port |
|---------|------|
| Angular | 4200 |
| Keycloak | 8080 |
| Adminer | 8081 |
| Spring Boot API | 8082 |
| Postgres | 5432 |

Swagger UI (dev profile only): http://localhost:8082/swagger-ui/index.html

Test user (Keycloak seed): `testuser` / `testpass`

## Spring profiles

Runtime flags (e.g. loot dev grants) are controlled **only on the Spring side** via profiles. The Angular app reads `GET /api/gamification/config` at runtime — no frontend profile switch.

| Profile | When to use | Notable settings |
|---------|-------------|------------------|
| **`dev`** (default) | Local development | `succeshub.loot.enable-dev-grants: true`, SQL logging, Swagger on |
| **`prod`** | Production-like runs | Dev grants off, WARN logging, Swagger off |

### Activate a profile

**IntelliJ:** use **Spring Boot (SuccesHub)** (`dev`) or **Spring Boot (SuccesHub) - Prod** (`prod`).

**Maven:**

```bash
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=prod
```

**JAR:**

```bash
java -jar target/succes-hub-*.jar --spring.profiles.active=prod
```

**Environment variable:**

```bash
set SPRING_PROFILES_ACTIVE=prod
```

If you omit the profile, Spring uses **`dev`** (`spring.profiles.default` in `application.yaml`).

## Maintaining shared run configurations

All team run configs live in [`.run/`](../.run/) and are **committed to git**. Personal IDE state stays in `.idea/` (gitignored).

### Adding or changing a config

1. **Run → Edit Configurations…**
2. Create or edit the configuration.
3. Enable **Store as project file** and save under `.run/` (project root).
4. Commit the new or updated `*.run.xml` file.

### After pulling changes

IntelliJ picks up new `.run/*.xml` files automatically. If a config is missing:

1. **File → Invalidate Caches** (rarely needed), or
2. Close and reopen the project.

### Removing stale local configs

If you have old run configs only in `.idea/workspace.xml` (not shared):

1. **Run → Edit Configurations…**
2. Delete duplicates that are not stored as project files.
3. Rely on the `.run/` copies from git.

Do not commit `.idea/runConfigurations/` — use `.run/` at the repo root so everyone gets the same toolbar entries after `git pull`.

## Verification

1. **Start Infrastructure** — containers `succeshub-postgres`, `succeshub-keycloak`, `succeshub-adminer` are running.
2. **Spring Boot (SuccesHub)** — http://localhost:8082 responds.
3. **Angular Dev Server** — http://localhost:4200 loads.
4. **Full Stack** — all of the above without typing compose/mvn/npm commands.
5. **Reset Dev DB (wipes volumes)** then **Spring Boot** — empty DB, Liquibase migrations applied.

## Related docs

- [Gamification reward loop](gamification.md) — task completion, daily close, celebrations, and loot lifecycle
- [Continuous Integration](ci.md) — CI jobs and local reproduction
