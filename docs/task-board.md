# Task board hierarchy, ordering, and deletion

## Purpose

The authenticated `/tasks` route is SuccessHub's task-planning surface. It combines a category board for editing work with a campaign view for reviewing open tasks by active goal.

The feature spans:

- Angular board and editor state;
- user-owned task and category APIs;
- one-level task hierarchy and sibling ordering;
- reward-aware completion, reminders, and dashboard scheduling.

## User-visible workflow

### Category view

- Categories are horizontal columns. Each category controls whether completion grants XP.
- Tasks can be starred, edited, completed, deleted, reordered, moved between categories, or nested one level deep.
- Each category has its own `Manual`, `Starred`, `Title`, or `Due` sort mode.
- The editor manages difficulty, duration, priority, goal, parent, due date, and reminder.

### Campaign view

Open tasks linked to active goals are grouped by goal, with an additional **Unlinked** section. Tasks linked to inactive goals are omitted. If an active goal has category subscriptions and a task is outside them, the UI can move it into a valid category.

This view is derived from the same task data. Editing still happens in the task editor sheet.

## Loading and ownership

The page initially loads categories, tasks, and active goals with `forkJoin`. It then loads category subscriptions for each active goal before rendering. Any failed request puts the whole page into its retry state.

Primary task, parent, goal, and reorder-item lookups are scoped to the authenticated Keycloak subject. Category lookups also verify the category owner; child queries start from an already owned parent. A valid session is therefore not enough to mutate another user's task or category.

## Hierarchy invariants

Tasks form a maximum two-level tree:

```text
category
├── root task
│   ├── subtask
│   └── subtask
└── root task
```

The backend enforces these rules:

1. A subtask cannot become a parent.
2. A task that already has children cannot become a subtask.
3. A task cannot be its own parent.
4. Parent and child must share a category.
5. Creating a child realigns its category to the parent's category.
6. Moving a root to another category also moves its direct children.

The Angular drag predicate prevents common invalid drops, but the service remains authoritative. Invalid hierarchy changes return `400 BAD_REQUEST` with a concrete message such as `Task with subtasks cannot become a subtask`.

## Sorting and drag persistence

Sort modes are browser-local and stored by category:

```text
successhub.tasks.sortModeByCategory
```

`Starred`, `Title`, and `Due` only change presentation. Due sorting checks `dueDate`, then `scheduledDate`, then `reminderAt`. Reordering within the same list is persisted only in `Manual` mode; cross-list moves persist placement and sibling order in every sort mode.

Task reordering is scoped to one sibling group: either all roots in a category or all children of one parent. `PATCH /api/tasks/reorder` must receive every task ID in that group exactly once. Partial lists, duplicates, foreign tasks, a nested parent, or a category mismatch are rejected.

Moving a task between lists uses several requests:

1. `PUT /api/tasks/{id}` changes category and parent.
2. `PATCH /api/tasks/reorder` writes the destination order.
3. A second reorder call writes the previous list when it still contains tasks.

These requests are not one database transaction. On failure, the UI reloads server state; the move can persist even if one sibling order does not.

Category drag uses `PATCH /api/task-categories/reorder`. Its `orderedIds` must contain every category owned by the user exactly once.

## Deletion behavior

Deleting a root task also deletes its subtasks because the `parent_task_id` foreign key uses `ON DELETE CASCADE`.

Category deletion from the UI deliberately:

1. deletes only root tasks in that category, in parallel;
2. waits for all root deletions;
3. deletes the category;
4. removes the category's local sort preference.

Deleting every task, including children, in parallel would race explicit child deletes against the database cascade. The focused Angular test protects the root-only behavior.

The sequence is destructive, has no undo, and spans multiple HTTP requests. If one request fails, some roots may already be gone while the category remains. Reload before retrying so the client uses current server state.

Direct `DELETE /api/task-categories/{id}` also deletes associated tasks through the JPA category relation. This differs from the database's `category_id ON DELETE SET NULL` rule because the service loads and removes the entity graph.

## Due dates, scheduling, and reminders

`dueDate`, `scheduledDate`, and `reminderAt` are separate:

- `dueDate` is planning metadata and drives the Due sort.
- `scheduledDate` determines whether a `TODO` task appears as a dashboard mission when it equals the backend's effective current date.
- `reminderAt` is an instant used by the notification scheduler.

On an existing task, selecting **Today** immediately calls `PATCH /api/tasks/schedule` and sets an unsaved, browser-local due date in the editor. The schedule request uses the backend's effective current date and remains applied even if the editor is cancelled. Selecting **Today** while creating a task sets only `dueDate`; it does not schedule the new task. Clearing or changing the due date does not clear `scheduledDate` because there is no unschedule endpoint.

The browser converts the local reminder field to UTC before saving. `TaskReminderScheduler` polls every 60 seconds by default and emits one in-app `TASK_REMINDER` for each incomplete, due, unnotified task. The notification links to `/tasks?focus={taskId}`, where the board scrolls to the task after loading.

## Completion paths

- XP-enabled categories use `PATCH /api/tasks/{id}/complete`, which invokes the gamification engine and updates shared profile state.
- Non-XP categories use `PUT /api/tasks/{id}` with `status: "DONE"` and intentionally bypass rewards.
- Reopening uses `PUT` with `status: "TODO"`. Previously awarded XP is not awarded again.

## API examples

Create a subtask:

```http
POST /api/tasks
Content-Type: application/json

{
  "categoryId": "11111111-1111-1111-1111-111111111111",
  "parentTaskId": "22222222-2222-2222-2222-222222222222",
  "title": "Write integration tests",
  "xpReward": 20,
  "difficulty": 3,
  "durationMinutes": 45,
  "priority": 2
}
```

Persist the complete root order for a category:

```http
PATCH /api/tasks/reorder
Content-Type: application/json

{
  "categoryId": "11111111-1111-1111-1111-111111111111",
  "parentTaskId": null,
  "orderedIds": [
    "33333333-3333-3333-3333-333333333333",
    "22222222-2222-2222-2222-222222222222"
  ]
}
```

Browser writes require the BFF session and CSRF header. Angular supplies these for same-origin proxied requests.

## Troubleshooting

| Symptom | Likely cause | Resolution |
|---|---|---|
| Drag order snaps back | The category is not in `Manual` mode, or an update/reorder request failed | Switch to `Manual`; inspect the failed request, then reload |
| A task cannot be nested | The target is already a subtask, the dragged task has children, or an API update explicitly supplied a category different from the parent | Keep one hierarchy level; choose a root parent and let creation or drag align the category |
| Category remains after deletion | A root deletion or the final category deletion failed | Reload, inspect the inline error, and retry with current data |
| A task due today is absent from Dashboard | `dueDate` was set without `scheduledDate`, the task is not `TODO`, or its schedule does not match the backend's effective today | Edit the saved task and select **Today**; confirm its status |
| Reminder did not appear immediately | Polling can take about 60 seconds, the task is complete, the reminder already fired, or the notification panel has stale data | Confirm task status and reminder time, allow one poll interval, then reopen the panel or refresh |
| Campaign view shows a category mismatch | The task's goal has subscriptions that exclude its current category | Use **Move to campaign category** and select an allowed category |

## Code map

- Board orchestration: `frontend/src/app/features/tasks/tasks.component.ts`
- Editor sheet: `frontend/src/app/features/tasks/task-editor-sheet/`
- Client API: `frontend/src/app/core/services/task.service.ts`
- REST APIs: `TaskController` and `TaskCategoryController`
- Task invariants and moves: `TaskServiceImpl`
- Category ordering and deletion: `TaskCategoryServiceImpl`
- Reminder polling: `TaskReminderScheduler`
- Schema rules: `2026-06-14-03-create-task.xml` and `2026-10-04-01-task-google-ux-columns.xml`
- Focused tests: `TasksComponent` spec and `TaskServiceImplGoogleUxTest`

## Verification

For a focused check:

```powershell
.\mvnw.cmd -Dtest=TaskServiceImplGoogleUxTest test
cd frontend
npm run test:ci -- --include src/app/features/tasks/tasks.component.spec.ts
```

Before merging, also run the repository's full backend tests and frontend build/test commands from the root README.
