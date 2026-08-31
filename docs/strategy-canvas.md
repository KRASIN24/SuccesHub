# Strategy Canvas tasks

The Strategy Canvas at `/tasks` is the task-planning surface for authenticated users. It groups tasks into user-owned categories, previews completion XP, optionally links work to an active boss, and schedules tasks. Today's dashboard missions include scheduled `TODO` tasks only.

## Request flow

On load, the Angular component waits for three requests:

1. `GET /api/task-categories`
2. `GET /api/tasks`
3. `GET /api/goals?status=ACTIVE`

It then builds one column per category and assigns tasks by `categoryId`. Categories use `sortOrder`; tasks arrive newest first. If any request fails, the whole canvas shows its retry state because the requests are combined with `forkJoin`.

The main implementation paths are:

- UI orchestration: [`frontend/src/app/features/tasks/tasks.component.ts`](../frontend/src/app/features/tasks/tasks.component.ts)
- Client contracts: [`frontend/src/app/core/models/task.model.ts`](../frontend/src/app/core/models/task.model.ts)
- HTTP wrapper: [`frontend/src/app/core/services/task.service.ts`](../frontend/src/app/core/services/task.service.ts)
- Task API: [`src/main/java/com/succeshub/appdomain/controller/TaskController.java`](../src/main/java/com/succeshub/appdomain/controller/TaskController.java)
- Category API: [`src/main/java/com/succeshub/appdomain/controller/TaskCategoryController.java`](../src/main/java/com/succeshub/appdomain/controller/TaskCategoryController.java)
- Task behavior: [`src/main/java/com/succeshub/appdomain/service/impl/TaskServiceImpl.java`](../src/main/java/com/succeshub/appdomain/service/impl/TaskServiceImpl.java)

All endpoints require the BFF login session. Browser writes also require the `XSRF-TOKEN` cookie value in `X-XSRF-TOKEN`; Angular handles this automatically. See [local development](local-dev.md) for starting the authenticated stack and Swagger.

## User workflow

### Create a category

Use **Add Category**, then provide:

- `name` — required, at most 100 characters
- `tag` — optional at the API level, at most 50 characters; send an empty string instead of `null` because the current edit form calls `.trim()` on it
- `grantXp` — whether a first completion may enter the reward engine
- `sortOrder` — the UI uses the current column count for new categories

Category updates are full `PUT` requests. Preserve `name`, `tag`, `grantXp`, and `sortOrder` when changing one field.

Using the current column count can duplicate an existing `sortOrder` after categories are changed through the API or deleted. Ordering between equal values is unspecified.

### Create a task

Use a column's add button. The form accepts a title of at most 255 characters, description, difficulty, duration, priority, and optional active boss. Changing the three numeric inputs calls:

```http
GET /api/gamification/xp-preview?difficulty=3&durationMinutes=30&priority=2&weeklyChallenge=false
```

Saving sends the selected category and the latest preview value. If the preview is pending or failed, `xpReward` is saved as `0`; the preview requests are not cancelled, so an older response can overwrite a newer one.

```json
{
  "categoryId": "1f9ebaa8-5e3f-48df-bfa1-981897af52d5",
  "title": "Draft launch checklist",
  "description": "Resolve the remaining release blockers",
  "xpReward": 0,
  "difficulty": 3,
  "durationMinutes": 30,
  "priority": 2,
  "goalId": "273b1df7-20b7-4c9b-bccd-d2e923459ffd"
}
```

The boss selector includes active goals only. A task without a category is valid at the API level but is not rendered by the Strategy Canvas because the UI only builds category-backed columns.

### Complete or reopen a task

Completion has two paths:

| Category setting | Client request | Result |
|---|---|---|
| `grantXp: true` | `PATCH /api/tasks/{id}/complete` | Marks done and, on the first eligible completion, runs XP, boss damage, achievements, profile synchronization, and celebrations |
| `grantXp: false` | `PUT /api/tasks/{id}` with `status: "DONE"` | Marks done without running the reward engine |

The completion endpoint treats a category-less task as XP-enabled and is idempotent. A task that is already done or already has an XP event returns success with `reward: null`; it does not award XP twice.

Reopening uses `PUT /api/tasks/{id}` with `status: "TODO"` and clears `completedAt`. If the task has already earned XP, its persisted XP-awarded state is retained and later completions do not grant another reward. A task first completed through the non-XP `PUT` path has not been rewarded; after reopening it and enabling XP, its next canonical completion can award its first reward.

### Schedule for today

The **+ Today** action sends:

```http
PATCH /api/tasks/schedule
Content-Type: application/json

{"taskIds":["1af1292a-138d-49cf-ae55-b65cf19da792"]}
```

The backend sets `scheduledDate` from the server's current date. There is no unschedule endpoint, and the Strategy Canvas exposes scheduling for `TODO` and `IN_PROGRESS` unscheduled tasks. Only scheduled `TODO` tasks appear in today's dashboard missions.

## API summary

| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/tasks?status=TODO` | Optional status filter accepts `TODO`, `IN_PROGRESS`, or `DONE`, case-insensitively |
| `POST` | `/api/tasks` | Creates a task; category and goal must belong to the session user |
| `PUT` | `/api/tasks/{id}` | Mixed update semantics; send status values in uppercase and use the client behavior described below |
| `PATCH` | `/api/tasks/{id}/complete` | Canonical reward-aware completion |
| `PATCH` | `/api/tasks/schedule` | Schedules every supplied task ID for today |
| `DELETE` | `/api/tasks/{id}` | Deletes one owned task |
| `GET` | `/api/task-categories` | Returns owned categories by ascending `sortOrder` |
| `POST` | `/api/task-categories` | Creates a category |
| `PUT` | `/api/task-categories/{id}` | Full category update |
| `DELETE` | `/api/task-categories/{id}` | Deletes the category and its associated tasks through the ORM cascade; no delete action is exposed in the current UI |

## Constraints and pitfalls

- **The XP preview is not a promise.** Actual XP is recalculated at completion and can include bonuses or daily-cap truncation. The stored `xpReward` field is display data; task rewards use difficulty, duration, and priority.
- **A non-XP category still shows a preview.** The create form requests and displays estimated XP even when its column has `grantXp: false`; completion intentionally grants none.
- **The category setting is read at completion time.** Toggling `grantXp` affects future completions of tasks already in that category.
- **HTML input bounds do not enforce the contract.** The UI advertises difficulty 1–5, duration in 15-minute steps from 15, and priority 1–3, but it does not block out-of-range saves. Backend validation rejects values below 1 only; it accepts higher difficulty/priority, durations below 15, and durations not divisible by 15.
- **Task `PUT` semantics are mixed.** Null category, status, difficulty, duration, and priority preserve their existing values. Null goal, metadata, and due date clear those fields, while an omitted `xpReward` deserializes to `0`. The Angular wrapper omits `metaLabel`, `metaType`, and `dueDate`, so non-XP completion or reopening clears them.
- **Mutation errors are uneven.** Create-task and edit-category forms show inline errors; toggles, scheduling, category creation, and completion only log failures to the browser console.
- **Category deletion is destructive.** `DELETE /api/task-categories/{id}` cascades through the JPA category-to-task relation and deletes the associated tasks. The database foreign key's `ON DELETE SET NULL` does not prevent this service-path data loss.
- **No reordering or task editing UI exists.** Services expose update and delete methods, but the current page only creates tasks, changes completion state, schedules tasks, and edits category fields.

## Troubleshooting

| Symptom | Check |
|---|---|
| Entire canvas shows “Could not synchronize” | Inspect all three load requests; one failed response makes `forkJoin` fail |
| Task exists in the API but not on the board | Confirm it has a `categoryId` matching a returned category |
| Completion gives no XP | Check the category's `grantXp`, whether the task was rewarded before, and the daily XP cap |
| Linked boss does not take damage | Use the reward-aware completion path and confirm the task still has an active, owned `goalId` |
| Category toggle appears unchanged | Inspect the `PUT /api/task-categories/{id}` response and browser console; the UI changes local state only after success |
| Task does not appear on today's dashboard | Confirm `scheduledDate` equals the backend server's current date and status is `TODO`; scheduled `IN_PROGRESS` tasks are excluded |

## Verification

Backend completion behavior has focused coverage in [`TaskServiceImplCompleteTest`](../src/test/java/com/succeshub/appdomain/service/impl/TaskServiceImplCompleteTest.java). There are currently no Angular tests for `TasksComponent`, and no service test covers scheduling, category CRUD, or category deletion.
