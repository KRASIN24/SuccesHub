# Account deactivation (30-day grace) then purge

**Date:** 2026-09-26  
**Status:** Approved — implementing  
**Branch context:** Settings / W8 auth self-service

## Goal

Replace immediate hard-delete with a **30-day cancelable deactivation**. Users are told clearly. After 30 days, Keycloak user + SuccessHub data are permanently removed. During the window, login works and shows a **confirm-first Reactivate** screen before app access.

## Why not Keycloak `enabled=false`

Disabling the Keycloak user blocks login entirely, so a Reactivate confirmation UI cannot appear. Deactivation is therefore **app-side** (`deactivatedAt`), with Keycloak remaining enabled until the purge job.

## User-visible behavior

### Delete account (Settings)

1. User types `DELETE` and confirms.
2. Backend sets `user_profile.deactivated_at = now()` (progress retained).
3. Session is logged out (BFF + Keycloak SSO).
4. Copy explains:
   - Account is **deactivated**, not erased yet.
   - Progress is kept for **30 days**.
   - After that it is **permanently deleted**.
   - Sign in again within 30 days to **cancel** deletion.

### Login while deactivated

1. Normal Keycloak + BFF login succeeds.
2. SPA shows only an **Account scheduled for deletion** screen (purge date shown).
3. Actions:
   - **Reactivate account** → clears `deactivated_at`, enter app.
   - **Logout** → leave.
4. No dashboard / feature routes until reactivated.

### After 30 days

Scheduled job permanently deletes:

- All SuccessHub rows for that user (existing cleanup service).
- Keycloak user via Admin API.

## Technical design

### Data

- Liquibase (author `krasin`): nullable `user_profile.deactivated_at TIMESTAMPTZ`.
- Entity: `UserProfile.deactivatedAt` (`Instant`).

### API

| Endpoint | Auth | Behavior |
|----------|------|----------|
| `GET /api/account` (extend status) | session | Include `deactivated` + `purgeAt` (`deactivatedAt + 30d`) when set |
| `POST /api/account/delete` | session | Validate `DELETE`; set `deactivatedAt`; do **not** call cleanup/Keycloak delete; then logout |
| `POST /api/account/reactivate` | session | Clear `deactivatedAt` if set; 204 |
| Existing APIs | session | If profile has `deactivatedAt`, return **403** with code e.g. `ACCOUNT_DEACTIVATED` (except account status, reactivate, logout) |

### Frontend

- Settings delete dialog: updated copy (30-day grace + cancel by signing in).
- On load / route guard: if status says deactivated → force **reactivate** screen (block other routes).
- Reactivate screen: show purge date; Reactivate / Logout buttons.

### Scheduler

- Daily (or hourly) job: find profiles where `deactivated_at <= now() - 30 days`.
- For each: `UserDataCleanupService.deleteAllForUser` then `KeycloakAdminClient.deleteUser`.
- Idempotent on missing Keycloak user (404 OK).

### Availability / registration

- Username/email of a deactivated (not yet purged) user remain **taken** until purge.

## Out of scope

- Email reminders before purge.
- Partial data anonymization.
- Admin UI to force-purge early.

## Verification

1. Delete account → logged out; copy mentions 30 days.
2. Login → only reactivate screen; APIs return 403 if forced.
3. Reactivate → app works; `deactivated_at` null.
4. With `deactivated_at` forced to 31 days ago, job removes Keycloak user + DB rows.
5. `.\mvnw.cmd test` for account/cleanup coverage.
