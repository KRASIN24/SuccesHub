# Tasks — Google-style UX

## Purpose

Quest Log click model aligned with Google Tasks: gold circle complete, flat list rows, dedicated editor sheet, plus star, sort modes, one-level subtasks, and manual drag reorder.

## Backend

- Columns on `succeshub.task`: `parent_task_id`, `starred`, `sort_order` (Liquibase `2026-10-04-01`, author krasin)
- `PATCH /api/tasks/{id}/star`, `PATCH /api/tasks/reorder`
- Create rejects nested parents (one level only)

## Frontend

- Circle complete control (≥44px hit area); Grant XP stays glitch-checkbox
- Flat `.task-row` list; row body opens editor sheet
- Sort modes in `localStorage` key `successhub.tasks.sortMode`
- CDK drag when sort = Manual

## Verification

- Unit: `TaskServiceImplCompleteTest`, `TaskServiceImplGoogleUxTest`
- Manual: complete/reopen, sheet CRUD, star, subtask, reorder, sort modes, light/dark
