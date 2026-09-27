# Account Deactivation Grace Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace immediate account hard-delete with a 30-day cancelable deactivation, confirm-first reactivate screen, and scheduled permanent purge.

**Architecture:** App-side `user_profile.deactivated_at` (Keycloak stays enabled until purge). Delete sets the timestamp and logs out. Login works; SPA/API gate blocks the app until `POST /api/account/reactivate`. A daily job purges Keycloak + DB rows when `deactivated_at` is older than 30 days.

**Tech Stack:** Spring Boot 3.5, Liquibase, JPA, Spring `@Scheduled`, Angular 18, existing Account API + AuthService.

**Spec:** `docs/superpowers/specs/2026-09-26-account-deactivation-grace-design.md`

## Global Constraints

- Liquibase new changesets: `author="krasin"` only
- Commit subjects: plain imperative English (no Conventional Commits prefixes)
- OpenAPI + JavaDoc on every new/changed controller endpoint and service method
- Deactivated users: all `/api/**` except account status, reactivate, and logout → `403` / `ACCOUNT_DEACTIVATED`
- Grace period: exactly 30 days from `deactivated_at`
- Do **not** call `UserDataCleanupService` or Keycloak delete on user-initiated delete (only on purge job)

---

## File map

| File | Role |
|------|------|
| `db/changelog/2026-09-26-02-user-profile-deactivated-at.xml` | Add column |
| `db.changelog-master.xml` | Include changelog |
| `UserProfile.java` | `deactivatedAt` field |
| `UserProfileRepository.java` | Find due for purge |
| `AccountStatusResponse.java` | `deactivated`, `purgeAt` |
| `AccountService` / `AccountServiceImpl` | Deactivate, reactivate, status |
| `AccountController.java` | `POST /reactivate` |
| `AccountDeactivatedException` (or AppException) | 403 code |
| `DeactivatedAccountFilter` or interceptor/aspect | Block APIs |
| `AccountPurgeScheduler.java` | Daily purge |
| `SuccesHubApplication.java` | `@EnableScheduling` if missing |
| `AccountServiceImplTest.java` | Unit tests |
| `account.service.ts` | Types + reactivate |
| `settings.component.*` | Delete copy |
| Reactivate screen + guard | Block app until reactivated |

---

### Task 1: Schema + entity

**Files:**
- Create: `src/main/resources/db/changelog/2026-09-26-02-user-profile-deactivated-at.xml`
- Modify: `src/main/resources/db/changelog/db.changelog-master.xml`
- Modify: `src/main/java/com/succeshub/appdomain/model/UserProfile.java`
- Modify: `src/main/java/com/succeshub/appdomain/repository/UserProfileRepository.java`

- [ ] **Step 1: Add Liquibase changeset**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="2026-09-26-02-user-profile-deactivated-at" author="krasin">
        <addColumn tableName="user_profile" schemaName="succeshub">
            <column name="deactivated_at" type="TIMESTAMP WITH TIME ZONE">
                <constraints nullable="true"/>
            </column>
        </addColumn>
    </changeSet>
</databaseChangeLog>
```

Include in `db.changelog-master.xml` after the notification changelog.

- [ ] **Step 2: Entity + repository**

Add to `UserProfile`:

```java
@Column(name = "deactivated_at")
private Instant deactivatedAt;
```

Add to `UserProfileRepository`:

```java
/**
 * Profiles whose deactivation grace period has ended (ready for permanent purge).
 */
List<UserProfile> findByDeactivatedAtBefore(Instant cutoff);
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/changelog/2026-09-26-02-user-profile-deactivated-at.xml \
  src/main/resources/db/changelog/db.changelog-master.xml \
  src/main/java/com/succeshub/appdomain/model/UserProfile.java \
  src/main/java/com/succeshub/appdomain/repository/UserProfileRepository.java
git commit -m "Add user_profile.deactivated_at for 30-day account grace"
```

---

### Task 2: Account service — deactivate / reactivate / status

**Files:**
- Modify: `AccountStatusResponse.java`, `AccountService.java`, `AccountServiceImpl.java`, `AccountController.java`
- Modify: `AccountServiceImplTest.java`
- Create: `AccountDeactivatedException.java` (extends `AppException`, status 403, code `ACCOUNT_DEACTIVATED`)

**Interfaces:**
- Produces: `AccountStatusResponse(email, mfaEnabled, deactivated, purgeAt)`
- Produces: `void reactivate(OidcUser)`
- Produces: `deleteAccount` only sets `deactivatedAt` (no cleanup/Keycloak delete)
- Grace: `Duration.ofDays(30)`; `purgeAt = deactivatedAt.plus(30, ChronoUnit.DAYS)`

- [ ] **Step 1: Update failing tests first**

In `AccountServiceImplTest`:

- Inject `@Mock UserProfileRepository` (or a small `UserProfileService` helper — prefer repository + getOrCreate pattern already used elsewhere).
- Change `deleteAccount_cascadesLocalThenKeycloak` → `deleteAccount_setsDeactivatedAtWithoutPurge`:
  - verify `profile.setDeactivatedAt(...)` saved
  - `verifyNoInteractions(userDataCleanupService)`
  - `verify(adminClient, never()).deleteUser(any())`
- Add `reactivate_clearsDeactivatedAt`
- Add `getStatus_includesPurgeAtWhenDeactivated`

Constructor of `AccountServiceImpl` gains `UserProfileRepository` (and keep cleanup for purge scheduler only — cleanup stays injected for now unused in delete, or remove from AccountServiceImpl and use only in scheduler).

- [ ] **Step 2: Implement service methods**

```java
// deleteAccount: validate DELETE → load/create profile → setDeactivatedAt(Instant.now()) → save
// reactivate: load profile → if deactivatedAt != null → set null → save
// getStatus: include deactivated + purgeAt (null when active)
```

Controller:

```java
@PostMapping("/reactivate")
@Operation(...)
@ApiResponse(responseCode = "204", ...)
public ResponseEntity<Void> reactivate(@AuthenticationPrincipal OidcUser principal) { ... }
```

`deleteAccount` endpoint still logs out after service call (existing controller behavior).

- [ ] **Step 3: Run tests**

```bash
.\mvnw.cmd -Dtest=AccountServiceImplTest test
```

Expected: PASS

- [ ] **Step 4: Commit**

```bash
git commit -m "Deactivate accounts for 30 days instead of deleting immediately"
```

---

### Task 3: API gate for deactivated sessions

**Files:**
- Create: `src/main/java/com/succeshub/coreinfra/auth/DeactivatedAccountInterceptor.java` (or `OncePerRequestFilter`)
- Register via `WebMvcConfigurer` in `config` package
- Create: `AccountDeactivatedException` if not in Task 2
- Ensure global exception handler maps `AppException` → JSON with `errorCode`

**Allowed paths when deactivated:**

- `GET /api/account`
- `POST /api/account/reactivate`
- `POST /logout` (not under `/api`)
- Swagger / public / oauth unchanged

All other `/api/**` → throw `AccountDeactivatedException`.

- [ ] **Step 1: Implement interceptor**

Resolve `OidcUser` from `SecurityContext`; if anonymous, skip. Load profile by `sub`; if `deactivatedAt != null` and request is not allow-listed → 403.

- [ ] **Step 2: Manual smoke or unit test** with `MockMvc` if project already has security MockMvc tests; otherwise verify with a focused unit test on the allow-list logic.

- [ ] **Step 3: Commit**

```bash
git commit -m "Block API access for deactivated accounts until reactivate"
```

---

### Task 4: Purge scheduler

**Files:**
- Create: `src/main/java/com/succeshub/appdomain/service/AccountPurgeScheduler.java` (or under `coreinfra.auth`)
- Modify: `SuccesHubApplication.java` — add `@EnableScheduling`

- [ ] **Step 1: Scheduler**

```java
@Scheduled(cron = "0 15 4 * * *") // 04:15 daily
@Transactional
public void purgeExpiredDeactivations() {
    Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
    for (UserProfile p : profileRepository.findByDeactivatedAtBefore(cutoff)) {
        String id = p.getKeycloakId();
        userDataCleanupService.deleteAllForUser(id); // deletes profile row too
        adminClient.deleteUser(id);
    }
}
```

Note: `deleteAllForUser` already deletes the profile — do not double-delete.

- [ ] **Step 2: Unit test** with mocked repo/cleanup/admin — one profile due, one not due; verify only due is purged.

- [ ] **Step 3: Commit**

```bash
git commit -m "Purge deactivated accounts after 30 days"
```

---

### Task 5: Frontend — status, delete copy, reactivate screen

**Files:**
- Modify: `frontend/src/app/core/services/account.service.ts`
- Modify: `frontend/src/app/features/settings/settings.component.html` (+ scss/ts if needed)
- Create: `frontend/src/app/features/account/reactivate-account.component.ts` (html/scss inline or separate)
- Modify: `frontend/src/app/app.routes.ts` — public-to-auth route `/account/reactivate` with authGuard but **without** other app chrome requirements
- Modify: `auth.guard.ts` or add `activeAccountGuard` that redirects to `/account/reactivate` when `deactivated`
- Modify: `app.component` / layout to hide sidebar on reactivate route if needed

- [ ] **Step 1: Extend `AccountStatus`**

```typescript
export interface AccountStatus {
  email: string | null;
  mfaEnabled: boolean;
  deactivated: boolean;
  purgeAt: string | null; // ISO-8601
}

reactivate(): Observable<void> {
  return this.http.post<void>(`${this.base}/reactivate`, {});
}
```

- [ ] **Step 2: Settings delete dialog copy**

Replace hint with: account will be **deactivated** for **30 days**, then permanently deleted; sign in again within 30 days to cancel. Button can stay “Delete forever” or become “Deactivate account” — prefer **Deactivate account** + confirm still `DELETE`.

- [ ] **Step 3: Reactivate page**

Show purge date from `purgeAt`. Buttons: Reactivate → `account.reactivate()` then `router.navigate(['/dashboard'])`. Logout → `auth.logout()`.

- [ ] **Step 4: Guard**

After `loadCurrentUser`, if logged in, call `getStatus()` once (or fold into auth initializer). If `deactivated`, redirect to `/account/reactivate` for any other route.

- [ ] **Step 5: Commit**

```bash
git commit -m "Add reactivate screen and 30-day deactivation copy in Settings"
```

---

### Task 6: Docs / Notion / verification

- [ ] Update Notion under Cursour Work (Account deactivation grace) — purpose, behavior, verification
- [ ] Run `.\mvnw.cmd test` (at least account + purge tests)
- [ ] Manual: deactivate → login → reactivate screen → reactivate → app works

---

## Spec coverage checklist

| Spec item | Task |
|-----------|------|
| `deactivated_at` column | 1 |
| Delete sets timestamp, no immediate purge | 2 |
| Status includes deactivated + purgeAt | 2 |
| Reactivate endpoint | 2 |
| API 403 when deactivated | 3 |
| Settings copy | 5 |
| Reactivate confirm UI | 5 |
| 30-day scheduled hard delete | 4 |
| Username stays taken until purge | implicit (no Keycloak delete until Task 4) |

## Placeholder / consistency self-review

- No TBDs
- `AccountStatusResponse` field names aligned FE/BE: `deactivated`, `purgeAt`
- Cleanup only in purge scheduler after Task 2 removes it from delete path
