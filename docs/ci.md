# Continuous Integration (GitHub Actions)

SuccessHub uses [GitHub Actions](https://github.com/KRASIN24/SuccesHub/actions) for CI when opening or updating a pull request to `master`. There is no deployment pipeline yet.

## Workflow

**File:** [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)

**Triggers:**

- `pull_request` targeting `master` only (not on direct pushes to branches)

**Concurrency:** in-progress runs for the same PR are cancelled when a new commit is pushed to the PR branch.

## Jobs

Two jobs run **in parallel**:

| Job | What it runs | Requirements |
|-----|----------------|--------------|
| **Backend** | `./mvnw test` | JDK 21, Maven cache |
| **Frontend** | `npm ci`, `npm run build`, `npm run test:ci` | Node 20, npm cache |

### Backend

- Tests do **not** need Docker or Postgres — [`SuccesHubApplicationTests`](../src/test/java/com/succeshub/succes_hub/SuccesHubApplicationTests.java) excludes JPA/Liquibase autoconfig.
- Verifies context load, Swagger UI, and OpenAPI docs.

### Frontend

- **Build:** production Angular build (`ng build`).
- **Test:** headless Karma via `npm run test:ci` (`ChromeHeadless`, single run).
- Karma config: [`frontend/karma.conf.js`](../frontend/karma.conf.js).

## Reproduce locally

Same commands as CI:

```powershell
# Backend
.\mvnw.cmd test

# Frontend
cd frontend
npm ci
npm run build
npm run test:ci
```

## What CI does not cover (yet)

- **CD / deploy** — no Dockerfiles or cloud target configured.
- **Integration tests** with Postgres, Keycloak, or Testcontainers.
- **Path filters** — both jobs run on every change (acceptable for current repo size).

## After the first green run

Optionally enable **branch protection** on `master` in GitHub:

1. **Settings → Branches → Add rule** for `master`
2. Require status checks: `Backend` and `Frontend`

## Related docs

- Local dev run configs: [`docs/local-dev.md`](local-dev.md)
