# Gamification reward loop

SuccessHub's gamification flow connects task completion to XP, streaks, achievements, boss damage, and loot. This document describes the system-level contract shared by the Spring backend and Angular client. For endpoint schemas, use Swagger UI in the `dev` profile at <http://localhost:8082/swagger-ui/index.html>.

## Architecture

The backend is the source of truth for rewards and inventory. The frontend renders the returned state and coordinates cross-page feedback.

```text
Dashboard or Strategy Canvas
  -> PATCH /api/tasks/{id}/complete
     -> TaskServiceImpl
        -> GamificationEngineImpl
           -> XpCalculator and xp_event ledger
           -> UserProfile XP and daily counters
           -> BossDamageService for linked goals
           -> AchievementEvaluator
           -> pending loot for unlocked achievements
  <- TaskCompletionDto { task, reward, updatedProfile }
     -> ProfileService updates shared navbar state
     -> GamificationCelebrationService triggers overlays
     -> LootPendingService updates the sidebar badge
```

Daily streak processing is separate. `GET /api/gamification/daily` first closes unprocessed days, then returns the current dashboard state. `POST /api/gamification/close-day` invokes the same close logic, but the dashboard has already called the daily endpoint by the time the user can press **Celebrate**. It therefore normally returns `alreadyClosed: true` with no new rewards. Streak correctness does not depend on that button, and the current API does not replay rewards that were produced by the earlier lazy close.

## Canonical user workflow

1. **Schedule tasks:** `PATCH /api/tasks/schedule` with `{"taskIds":["<uuid>"]}` assigns the selected tasks to the server's current day.
2. **Complete a task:** call `PATCH /api/tasks/{id}/complete`. This is the only task endpoint that runs the reward engine.
3. **Apply the response:** use `updatedProfile` for shared XP/level state. Treat `reward: null` as a successful completion with no new reward.
4. **Refresh daily state:** `GET /api/gamification/daily` returns scheduled missions, daily XP, streak state, weekly challenges, and whether a celebration is available.
5. **Keep loot pending:** earned caches remain unopened in the database and appear in the global cache count.
6. **Open loot deliberately:** the user opens a pending cache on `/loot-boxes`; the backend performs three rolls, persists them in inventory, and returns the reveal contents.

The Strategy Canvas uses the category's `grantXp` flag. XP-enabled categories use the completion endpoint and celebrations. Other categories use the normal task update endpoint to mark the task `DONE` without awarding rewards.

## Public contracts

All routes require an authenticated BFF session unless noted otherwise.

| Method | Path | Contract and side effects |
|--------|------|---------------------------|
| `PATCH` | `/api/tasks/{id}/complete` | Marks the task done and, when eligible, runs XP, boss, achievement, and loot processing. Returns `TaskCompletionDto`. |
| `PATCH` | `/api/tasks/schedule` | Sets `scheduled_date` to today for the supplied task IDs. Returns `204`. |
| `GET` | `/api/gamification/xp-preview` | Estimates XP from `difficulty`, `durationMinutes`, `priority`, and `weeklyChallenge`; does not persist or roll the variable bonus. |
| `GET` | `/api/gamification/daily` | Lazy-closes pending days, then returns daily missions, XP remaining, streak, challenges, and scheduled tasks. |
| `POST` | `/api/gamification/close-day` | Runs the same idempotent close logic. After the dashboard's daily-status load, this normally returns `alreadyClosed: true` and empty reward lists. |
| `GET` | `/api/gamification/forecast` | Returns the configured milestone teaser and weekly challenge settings. At streak 100 or above, the milestone remains 100. |
| `GET` | `/api/gamification/loot-boxes` | Lists unopened boxes, newest first. |
| `GET` | `/api/gamification/loot-boxes/history?limit=3` | Lists opened boxes and contents; `limit` is clamped to 1–10. |
| `POST` | `/api/gamification/loot-boxes/{id}/open` | Performs exactly three independent rolls and adds the results to inventory. Reopening returns the stored contents without rerolling. |
| `GET` | `/api/gamification/inventory` | Lists stacked reward items. |
| `POST` | `/api/gamification/inventory/{id}/equip` | Toggles a title or frame and unequips the previous item of the same type. Functional items are returned unchanged. |
| `GET` | `/api/gamification/config` | Returns client-safe flags such as `enableLootDevGrants`. |
| `POST` | `/api/gamification/loot-boxes/grant` | Development-only manual grant. Returns `403` when dev grants are disabled. |

### Completion example

```json
{
  "task": {
    "id": "7dc9b76e-f137-4fd2-b3c1-4f9d082c42c9",
    "status": "DONE"
  },
  "reward": {
    "xp": {
      "baseXp": 120,
      "streakBonus": 12,
      "firstTaskBonus": 10,
      "variableBonus": 0,
      "challengeBonus": 0,
      "totalXp": 142,
      "dailyXpRemaining": 158
    },
    "leveledUp": false,
    "achievementsUnlocked": [],
    "lootBoxEarned": null
  },
  "updatedProfile": {
    "level": 4,
    "currentXp": 210
  }
}
```

Fields not relevant to the example are omitted. Use the generated OpenAPI schema for the complete DTO.

## Frontend orchestration

| Component or service | Responsibility |
|----------------------|----------------|
| `TaskService` | Calls task APIs and immediately applies `updatedProfile` from completion responses. |
| `ProfileService` | Owns the shared profile signal consumed by the navbar and gamification screens. |
| `GamificationCelebrationService` | Converts reward responses into XP, level-up, achievement, and cache-earned triggers. It never opens loot. |
| `LootPendingService` | Shares pending boxes with the sidebar and Cache page. It increments counts optimistically, then reconciles with the server after 400 ms. |
| `GamificationService` | Typed client for `/api/gamification/**`. |
| `AppComponent` | Mounts global overlays and loads the initial pending-cache count for an authenticated user. |

Task completion is currently initiated from `/dashboard` and `/tasks`. Pending loot is opened on `/loot-boxes`. Inventory client methods exist, but no current component consumes them; adding inventory UI requires wiring `getInventory()` and `equipInventoryItem()`.

## Constraints and common pitfalls

- **Do not complete reward-bearing tasks with `PUT /api/tasks/{id}` and `status: "DONE"`.** That updates task state but bypasses the gamification engine.
- **Completion is idempotent.** A task already done or represented in `xp_event` returns `reward: null`; clients must not treat that as a failed request.
- **Category settings are authoritative.** A category with `grantXp: false` intentionally produces no XP or celebration.
- **Earn is not open.** Task-completion achievement unlocks and streak milestones can create pending caches. Achievements unlocked during daily close currently do not grant a cache. Never call the open endpoint automatically from a completion handler.
- **Day boundaries use the server timezone.** Streaks, grace-period checks, daily counters, and the Monday challenge scheduler do not use a per-user timezone in V1.
- **Daily close is lazy and idempotent.** Loading daily status can update streak state and consumes the close result internally. The later Celebrate request normally has no rewards to animate; it is not a required write and currently cannot replay the lazy-close result.
- **Preview is not a promise.** The completion total can differ because the preview excludes the random variable bonus and available daily XP can change.
- **Manual grants are profile-gated.** The Angular app reads the backend config; do not add a separate frontend environment flag.
- **Some seeded utility effects are descriptive only.** `DOUBLE_STRIKE` and `SURGE_TOKEN` have no consumption logic. `weeklyLootMinQualifyingDays` and the `WEEKLY_RESET` source also have no active caller.

## Authentication and errors

The browser uses a BFF session (`JSESSIONID`); OIDC tokens remain on the backend. Unauthenticated `/api/**` requests return `401` rather than redirecting. Angular's XSRF integration sends the `XSRF-TOKEN` cookie value as `X-XSRF-TOKEN` on mutating requests.

Handled application errors use:

```json
{
  "status": 404,
  "errorCode": "NOT_FOUND",
  "message": "Task not found: <uuid>"
}
```

Validation errors can also include `fieldErrors`. A bare `400` or `403` is expected for invalid or disabled manual loot grants because that controller handles those cases directly.

## Extending the loop

When adding a reward-bearing action:

1. Keep reward calculation and persistence inside a backend transaction.
2. Return the updated profile and an explicit reward payload.
3. Apply profile state before launching overlays.
4. Notify pending loot without opening it.
5. Make retries idempotent with a persisted event or equivalent guard.
6. Add backend tests for duplicate requests and frontend tests for `reward: null`.

## Verification

Run the same checks as CI:

```bash
./mvnw test
cd frontend
npm ci
npm run build
npm run test:ci
```

For a manual workflow, start the full stack using [local development](local-dev.md), log in as the seeded test user, and verify:

1. Completing an XP-enabled task updates the navbar and shows an XP overlay.
2. Completing a task in a non-XP category marks it done without a reward.
3. An earned cache raises the sidebar count but does not open.
4. Opening and collecting a cache removes it from pending state and updates history.
5. Repeating the completion request does not award a second `xp_event`.

## Source map

- Backend entry points: `TaskController`, `GamificationController`
- Reward orchestration: `TaskServiceImpl.complete`, `GamificationEngineImpl`
- Daily processing: `DailyRitualServiceImpl`, `StreakServiceImpl`
- Loot lifecycle: `LootBoxServiceImpl`
- XP formula and defaults: `XpCalculator`, `GamificationProperties`
- Frontend clients: `task.service.ts`, `gamification.service.ts`
- Frontend state and feedback: `profile.service.ts`, `gamification-celebration.service.ts`, `loot-pending.service.ts`
