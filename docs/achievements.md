# Achievements and merit unlocks

Achievements are persistent, per-user milestones. The backend owns the catalog and unlock state; the Angular client displays that state on the Dashboard and the dedicated `/achievements` route.

## User workflow

1. The Dashboard and Achievements page call `GET /api/achievements`.
2. Locked and unlocked merits use the same catalog metadata. Locked cards are dimmed; descriptions remain visible.
3. Task completion and pending-day processing evaluate achievement rules.
4. Task-completion responses include newly unlocked achievements and trigger the global achievement overlay.
5. Future catalog reads return `locked: false` and the persisted `unlockedAt` timestamp.

The current UI does not render `unlockedAt`, and reading the catalog does not evaluate or unlock achievements. A lazy unlock during `GET /api/gamification/daily` is persisted silently because the daily-status response does not include achievement results.

## Unlock rules

| Key | Requirement | Rule source |
|-----|-------------|-------------|
| `SPEEDSTER` | Complete at least `speedsterTaskThreshold` tasks in the current server day (default: 10) | `GamificationProperties` |
| `PIONEER` | Have at least `goals_required` completed goals (seeded as 1) | Achievement definition |
| `ARCHIVIST` | Reach the definition's `xp_required` lifetime XP (seeded as 10,000) | Achievement definition |
| `CONSISTENT` | Reach at least the greater of `streak_required` and 7 days | Achievement definition plus a seven-day floor |
| `SOVEREIGN` | Reach level 50 | Hard-coded evaluator rule |

Day boundaries use the backend server timezone. `SPEEDSTER` does not use the catalog's `tasks_required` column; its threshold is `succeshub.gamification.speedster-task-threshold`.

## Evaluation triggers

### Task completion

`PATCH /api/tasks/{id}/complete` evaluates achievements after XP, goal damage, and any goal-completion XP have been applied. This means one completion can unlock multiple merits.

For every achievement unlocked on this path, the backend creates one pending loot box with source `ACHIEVEMENT`. The response contains the unlocked achievement list but only one `lootBoxEarned` UUID: when several merits unlock together, it is the last granted box ID. The pending-box list remains authoritative.

### Pending-day close

`GET /api/gamification/daily` runs pending-day close before returning Dashboard state. `POST /api/gamification/close-day` invokes the same close operation explicitly. Achievement evaluation occurs only when at least one pending day is processed; an `alreadyClosed` result returns before evaluation.

This path can unlock streak-dependent achievements, but it does **not** grant an achievement-sourced loot box:

- Lazy close through `GET /daily` discards the close result, so the unlock is persisted without an overlay.
- Explicit `POST /close-day` returns unlocks in `achievementsUnlocked` when it actually processes a pending day.
- `lootBoxesEarned` contains only boxes created by streak-milestone processing.

### Catalog reads

`GET /api/achievements` only joins global definitions with the authenticated user's saved unlock rows. It never evaluates rules.

## API contract

The endpoint requires an authenticated OIDC session.

```http
GET /api/achievements
```

```json
[
  {
    "id": "a1000000-0000-0000-0000-000000000001",
    "key": "SPEEDSTER",
    "label": "Speedster",
    "icon": "bolt",
    "description": "Complete 10 tasks in a single day.",
    "locked": false,
    "unlockedAt": "2026-06-20T18:42:00Z"
  },
  {
    "id": "a1000000-0000-0000-0000-000000000003",
    "key": "ARCHIVIST",
    "label": "Archivist",
    "icon": "diamond",
    "description": "Accumulate 10,000 total XP.",
    "locked": true,
    "unlockedAt": null
  }
]
```

Clients must not rely on response order: definitions are loaded without an explicit sort.

## Architecture

```text
Task completion ──> GamificationEngine ─┐
                                       ├─> AchievementEvaluator
Pending-day close ─> DailyRitualService ┘       │
                                                └─> user_achievement

GET /api/achievements ─> AchievementService ─> definitions + user unlocks
```

- `achievement_definition` is the global catalog.
- `user_achievement` stores a user's unlock timestamp and definition reference.
- `AchievementEvaluatorImpl` maps supported definition keys to executable rules.
- `AchievementServiceImpl` builds the read model used by both Angular screens.
- `GamificationCelebrationService` turns mutation response entries into global overlays.

## Adding or changing an achievement

1. Add a new Liquibase changeset with a stable UUID, unique key, display metadata, and any threshold columns the rule uses.
2. Add the key's rule to `AchievementEvaluatorImpl`. An unknown key remains locked forever.
3. Confirm that an existing evaluator trigger observes the required state. Listing the catalog is not a trigger.
4. Decide explicitly whether task-completion and day-close unlocks should have the same loot behavior; they currently do not.
5. Add evaluator tests for the threshold, already-unlocked behavior, and neighboring boundary values.
6. If order matters in the UI, add an explicit ordering field and repository sort instead of relying on database order.

The schema indexes `user_id` but has no unique constraint on `(user_id, achievement_id)`. Parallel evaluations are therefore not protected from duplicate unlock rows at the database level.

## Troubleshooting

### A seeded merit never unlocks

- Confirm its key has a case in `AchievementEvaluatorImpl`.
- Confirm the correct threshold column is populated.
- Trigger evaluation through canonical task completion or a pending-day close; a catalog read is insufficient.
- For `SPEEDSTER`, inspect `speedster-task-threshold`, task completion timestamps, and the server timezone.

### The unlock appears but no cache was earned

- Unlocks found during pending-day close do not grant achievement caches.
- For task-completion unlocks, refresh `GET /api/gamification/loot-boxes`; the completion response exposes only one box ID even if several were granted.

### The Dashboard fails while the dedicated page can retry

The Dashboard loads achievements inside a `forkJoin` with its other widgets, so one failed request fails the whole Dashboard load. The dedicated route shows its own error state and retry action. A `401` from `/api/achievements` indicates that the OIDC session is missing or expired.

## Source map

- API: [`AchievementController.java`](../src/main/java/com/succeshub/appdomain/controller/AchievementController.java)
- Read model: [`AchievementServiceImpl.java`](../src/main/java/com/succeshub/appdomain/service/impl/AchievementServiceImpl.java)
- Rule engine: [`AchievementEvaluatorImpl.java`](../src/main/java/com/succeshub/appdomain/service/impl/AchievementEvaluatorImpl.java)
- Task trigger: [`GamificationEngineImpl.java`](../src/main/java/com/succeshub/appdomain/service/impl/GamificationEngineImpl.java)
- Day-close trigger: [`DailyRitualServiceImpl.java`](../src/main/java/com/succeshub/appdomain/service/impl/DailyRitualServiceImpl.java)
- Catalog migration: [`2026-06-14-02-seed-achivements.xml`](../src/main/resources/db/changelog/2026-06-14-02-seed-achivements.xml)
- Threshold migration: [`2026-06-18-05-seed-rewards.xml`](../src/main/resources/db/changelog/2026-06-18-05-seed-rewards.xml)
- Angular page: [`achievements.component.ts`](../frontend/src/app/features/achievements/achievements.component.ts)
- Client orchestration: [`gamification-celebration.service.ts`](../frontend/src/app/core/services/gamification-celebration.service.ts)

## Verification

Run the focused backend test:

```bash
./mvnw -Dtest=AchievementEvaluatorImplTest test
```

The current focused test verifies that `SPEEDSTER` uses `speedsterTaskThreshold`, independently of the qualifying-day threshold. There are no dedicated Angular tests for the Achievements page or overlay yet.
