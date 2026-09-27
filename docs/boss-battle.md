# Boss Battle goals

Boss Battle turns long-running goals into user-owned bosses. A boss gains progress when the user completes linked, XP-enabled tasks; reaching its target moves it from **Active Encounters** to **Conquered Foes** and awards the configured goal XP.

## User workflow

1. Open `/goals` and choose **Summon Boss**.
2. Set its name, tier, icon, target description/value, XP reward, and whether it is featured.
3. On `/tasks`, create a task and select the active boss as its goal.
4. Complete the task through `PATCH /api/tasks/{taskId}/complete`.
5. The reward engine applies `task difficulty × boss damage factor` progress. The default factor is `2`.
6. When progress reaches the target, the backend marks the boss `COMPLETED`, labels it `Slain`, records `completedAt`, and adds its XP reward.

The Boss Battle page reloads active and completed goals, campaign summary, profile, and weekly insight together. It currently supports creating bosses, but not editing, deleting, abandoning, or manually attacking them in the UI.

## Data and reward flow

```text
Strategy Canvas task (goalId)
  -> PATCH /api/tasks/{id}/complete
  -> TaskServiceImpl checks first, XP-eligible completion
  -> GamificationEngineImpl
  -> BossDamageServiceImpl
  -> goal.currentProgress += task.difficulty * bossDamageFactor
  -> optional goal completion and goal XP
  -> task completion response includes reward.bossDamage
```

Only the authenticated user's goal can be linked to a task or mutated. Deleting a goal leaves its tasks in place and clears their `goal_id` through the database foreign key.

Key implementation paths:

- REST and DTOs: [`GoalController`](../src/main/java/com/succeshub/appdomain/controller/GoalController.java), [`GoalDto`](../src/main/java/com/succeshub/appdomain/dto/GoalDto.java)
- Goal lifecycle: [`GoalServiceImpl`](../src/main/java/com/succeshub/appdomain/service/impl/GoalServiceImpl.java)
- Task-driven damage: [`BossDamageServiceImpl`](../src/main/java/com/succeshub/appdomain/service/impl/BossDamageServiceImpl.java)
- Reward orchestration: [`GamificationEngineImpl`](../src/main/java/com/succeshub/appdomain/service/impl/GamificationEngineImpl.java)
- Angular page and API client: [`goals.component.ts`](../frontend/src/app/features/goals/goals.component.ts), [`goal.service.ts`](../frontend/src/app/core/services/goal.service.ts)

## Goal API

All endpoints require an authenticated OIDC session and derive ownership from the token subject. Browser mutations also require the `JSESSIONID` cookie and the readable `XSRF-TOKEN` cookie echoed as the `X-XSRF-TOKEN` header. Angular supplies both automatically; raw HTTP clients must preserve the session and CSRF cookies and add the header.

| Method | Path | Behavior |
|--------|------|----------|
| `GET` | `/api/goals?status=ACTIVE` | Active goals, newest first |
| `GET` | `/api/goals?status=COMPLETED` | Completed goals, newest first |
| `GET` | `/api/goals` | Active goals followed by completed goals; abandoned goals are omitted |
| `GET` | `/api/goals/summary` | Total, completed, and rounded completion percentage |
| `POST` | `/api/goals` | Create an active goal |
| `PUT` | `/api/goals/{id}` | Replace mutable fields and optionally change status |
| `DELETE` | `/api/goals/{id}` | Delete an owned goal and unlink associated tasks |

### Create a boss

```http
POST /api/goals
Content-Type: application/json

{
  "name": "Read 10 books",
  "tier": "Elite Threat",
  "targetDescription": "10 books",
  "targetValue": 10,
  "currentProgress": 0,
  "xpReward": 500,
  "icon": "menu_book",
  "featured": true
}
```

`name` must be nonblank and at most 255 characters. `targetValue` must be at least `1`; `xpReward` cannot be negative. The UI always creates with `currentProgress: 0`.

### Link and complete a task

```http
POST /api/tasks
Content-Type: application/json

{
  "title": "Read chapter 1",
  "goalId": "<goal UUID>",
  "difficulty": 4,
  "durationMinutes": 30,
  "priority": 2,
  "xpReward": 0
}
```

```http
PATCH /api/tasks/<task UUID>/complete
Content-Type: application/json

{}
```

With the default damage factor, difficulty `4` deals `8` progress. The completion response's `reward.bossDamage` value can be:

```json
{
  "goalId": "<goal UUID>",
  "goalName": "Read 10 books",
  "progressDelta": 8.0,
  "healthRemaining": 20,
  "goalCompleted": false,
  "goalXpReward": 0
}
```

`reward.bossDamage` is `null` when a rewarded task has no active linked goal. For an already completed/rewarded task or a task in a non-XP category, the enclosing `reward` is `null`.

## Completion and summary semantics

- Task damage is clamped at `targetValue`; health is rounded to an integer percentage and never returned below zero.
- `progressDelta` reports the calculated damage, not necessarily the stored increase. For example, `8` damage against `2` remaining progress reports `8` while stored progress increases by `2`.
- A linked task only deals damage when its category grants XP. Category-less tasks grant XP by default and can deal damage.
- A task's first rewarded completion is the only one that deals damage. Repeating the completion endpoint returns no reward.
- Goal XP from task-driven completion is added after task XP calculation, so it is not reduced by the daily XP cap and can push the daily counter above that cap.
- Goal XP is not included in `reward.xp.totalXp` or the persisted task XP event. It is included in the updated profile, level-up detection, daily XP, and subsequent achievement evaluation. The current Angular celebration emits an XP tick only for `reward.xp.totalXp`, not a separate goal-XP tick.
- `overallPercent` is `completedGoals / totalGoals`, rounded to the nearest integer. The denominator includes abandoned goals.
- Updating a goal directly to `COMPLETED` also awards its XP on the transition, independently of task progress.

## Constraints and pitfalls

- Use the completion endpoint. Setting a task's status to `DONE` with `PUT /api/tasks/{id}` bypasses XP, boss damage, achievements, and loot.
- A category with **Grant XP** disabled also disables boss damage for its tasks.
- `PUT /api/goals/{id}` is a full update, not a patch. Send all mutable fields; omitted primitive values deserialize as zero and can fail validation.
- Status values on goal updates are case-sensitive: `ACTIVE`, `COMPLETED`, or `ABANDONED`.
- The Angular `Goal` type only models `ACTIVE | COMPLETED`, so abandoning a goal is not type-safe through the current client even though the backend supports it.
- Direct completion through the goal update endpoint adds profile XP, but does not update the daily XP counter, run achievement evaluation, or grant loot.
- Reopening a completed goal and setting it to `COMPLETED` again can award its goal XP again. The update path does not clear the old `completedAt` or `slainLabel`.
- API callers can set negative or above-target `currentProgress`; only task-driven damage clamps progress. Prefer `0 <= currentProgress <= targetValue`.
- Backend task linking checks goal ownership but not lifecycle status. A client can link a completed or abandoned goal, but completing that task produces no boss damage.
- The Angular `GoalService` exposes update and delete methods, but the current Boss Battle page does not call them.
- The Strategy Canvas only selects a boss while creating a task. Its existing-task status updates preserve `goalId`; relinking an existing task requires a direct backend `PUT`.
- Page loading uses `forkJoin`; failure of any goal, summary, profile, or weekly-insight request displays the page-level error state.
- There are no dedicated backend or Angular tests for goals or boss damage. Keep the manual completion check below when changing the workflow.

## Verification

When changing this subsystem:

```powershell
.\mvnw.cmd test
cd frontend
npm run build
npm run test:ci
```

Manually verify that summoning a boss refreshes Active Encounters, an XP-enabled linked task reduces its health, the killing task moves it to Conquered Foes, and the Grand Campaign counts update.

## Related docs

- [Local development](local-dev.md)
