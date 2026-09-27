# W13 Settings Theme + Language Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Settings Visual Theme and Language real: dark/light across the SPA (including feature pages), en/pl for shell + Settings, persist both on `user_profile`, and mirror appearance + locale on the Keycloak login theme.

**Architecture:** `ThemeService` toggles `app-dark` and syncs cookies; light CSS vars on `:root`, dark under `.app-dark`. Custom `TranslateService` + JSON packs (`en-US`, `pl-PL`). Profile columns + `PATCH /api/profile/preferences` are source of truth when logged in; localStorage is cache + pre-login bridge. OAuth authorization customizer forwards `ui_locales` + `sh_theme`; Keycloak theme JS applies light/dark.

**Tech Stack:** Spring Boot 3.5, Liquibase, JPA, Angular 18 standalone, Tailwind v4 `@theme` tokens, Keycloak custom login theme (`docker/themes/successhub`).

**Spec:** `docs/superpowers/specs/2026-09-27-settings-theme-locale-design.md`

## Global Constraints

- Liquibase new changesets: `author="krasin"` only
- Commit subjects: plain imperative English (no Conventional Commits prefixes)
- OpenAPI + JavaDoc on every new/changed controller endpoint and service method
- Locales allow-list: `en-US`, `pl-PL` only (drop `de-DE` from UI registry)
- Default theme: dark (`darkTheme: true`); default locale: `en-US`
- Sound / animations / notification toggles stay localStorage-only
- W15 design-system unify is out of scope (light must be readable, not one shared button system)
- Document under Cursour Work when API/UI behavior ships

---

## File map

| File | Role |
|------|------|
| `db/changelog/2026-09-27-01-user-profile-theme-locale.xml` | `dark_theme`, `locale` columns |
| `db.changelog-master.xml` | Include changelog |
| `UserProfile.java` | Entity fields |
| `ProfileDto.java` | Add `darkTheme`, `locale` |
| `UpdatePreferencesRequest.java` | PATCH body |
| `UserProfileService` / `Impl` | `updatePreferences` |
| `ProfileController.java` | `PATCH /preferences` |
| `UserProfileServiceImplPreferencesTest.java` | Unit tests |
| `SecurityConfig.java` | Forward `ui_locales` + `sh_theme` on OAuth |
| `frontend/.../theme.service.ts` | Apply `app-dark` + cookies |
| `frontend/.../translate.service.ts` + pipe | i18n |
| `frontend/src/assets/i18n/en-US.json`, `pl-PL.json` | Packs |
| `locales.config.ts` | Drop `de-DE` |
| `locale.service.ts` | Drive TranslateService |
| `app-preferences.service.ts` | Unchanged shape; hydrate from server |
| `profile.model.ts` / `profile.service.ts` | prefs fields + PATCH |
| `app.config.ts` / `index.html` | Init Theme + Translate; remove hard dark inline |
| `global.css` | Light `:root` / `.app-dark` tokens |
| Feature + shell `*.scss` | Replace hardcoded dark where needed |
| `settings.component.*` | Persist via API + i18n strings |
| `sidebar` / `navbar` | i18n labels |
| `auth.service.ts` | Write theme cookie; pass query on login URL |
| `docker/themes/successhub/login/...` | Light tokens + theme.js |

---

### Task 1: Schema + entity + DTO fields

**Files:**
- Create: `src/main/resources/db/changelog/2026-09-27-01-user-profile-theme-locale.xml`
- Modify: `src/main/resources/db/changelog/db.changelog-master.xml`
- Modify: `src/main/java/com/succeshub/appdomain/model/UserProfile.java`
- Modify: `src/main/java/com/succeshub/appdomain/dto/ProfileDto.java`
- Modify: `src/main/java/com/succeshub/appdomain/service/impl/UserProfileServiceImpl.java` (`toDto`)

**Interfaces:**
- Produces: `UserProfile.darkTheme: boolean` (default `true`), `UserProfile.locale: String` (default `"en-US"`); `ProfileDto(..., boolean darkTheme, String locale)`

- [ ] **Step 1: Liquibase changeset**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="2026-09-27-01-user-profile-theme-locale" author="krasin">
        <addColumn tableName="user_profile" schemaName="succeshub">
            <column name="dark_theme" type="BOOLEAN" defaultValueBoolean="true">
                <constraints nullable="false"/>
            </column>
            <column name="locale" type="VARCHAR(16)" defaultValue="en-US">
                <constraints nullable="false"/>
            </column>
        </addColumn>
    </changeSet>
</databaseChangeLog>
```

Include after `2026-09-26-02-user-profile-deactivated-at.xml` in master.

- [ ] **Step 2: Entity + DTO + mapper**

Add to `UserProfile`:

```java
@Column(name = "dark_theme", nullable = false)
private boolean darkTheme = true;

@Column(name = "locale", nullable = false, length = 16)
private String locale = "en-US";
```

Extend `ProfileDto` record with `boolean darkTheme, String locale` as the last two components. Update `toDto` to pass `p.isDarkTheme()`, `p.getLocale()`.

Fix every other `new ProfileDto(...)` / test constructor call site the compiler flags.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/changelog/2026-09-27-01-user-profile-theme-locale.xml \
  src/main/resources/db/changelog/db.changelog-master.xml \
  src/main/java/com/succeshub/appdomain/model/UserProfile.java \
  src/main/java/com/succeshub/appdomain/dto/ProfileDto.java \
  src/main/java/com/succeshub/appdomain/service/impl/UserProfileServiceImpl.java
git commit -m "$(cat <<'EOF'
Add profile dark_theme and locale columns

EOF
)"
```

---

### Task 2: PATCH preferences API (TDD)

**Files:**
- Create: `src/main/java/com/succeshub/appdomain/dto/UpdatePreferencesRequest.java`
- Modify: `src/main/java/com/succeshub/appdomain/service/UserProfileService.java`
- Modify: `src/main/java/com/succeshub/appdomain/service/impl/UserProfileServiceImpl.java`
- Modify: `src/main/java/com/succeshub/appdomain/controller/ProfileController.java`
- Create: `src/test/java/com/succeshub/appdomain/service/impl/UserProfileServiceImplPreferencesTest.java`

**Interfaces:**
- Consumes: Task 1 entity/DTO
- Produces: `ProfileDto updatePreferences(String keycloakId, Boolean darkTheme, String locale)` — null fields mean “leave unchanged”; invalid locale throws `IllegalArgumentException` (or existing validation pattern → 400)

- [ ] **Step 1: Write failing tests**

```java
@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplPreferencesTest {

    @Mock UserProfileRepository repository;
    @InjectMocks UserProfileServiceImpl service;

    @Test
    void updatePreferences_setsDarkThemeAndLocale() {
        UserProfile profile = new UserProfile();
        profile.setKeycloakId("kc-1");
        profile.setDarkTheme(true);
        profile.setLocale("en-US");
        when(repository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));
        when(repository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        var dto = service.updatePreferences("kc-1", false, "pl-PL");

        assertFalse(dto.darkTheme());
        assertEquals("pl-PL", dto.locale());
    }

    @Test
    void updatePreferences_rejectsUnsupportedLocale() {
        UserProfile profile = new UserProfile();
        profile.setKeycloakId("kc-1");
        when(repository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));

        assertThrows(IllegalArgumentException.class,
                () -> service.updatePreferences("kc-1", null, "de-DE"));
    }

    @Test
    void updatePreferences_nullFieldsLeaveExistingValues() {
        UserProfile profile = new UserProfile();
        profile.setKeycloakId("kc-1");
        profile.setDarkTheme(false);
        profile.setLocale("pl-PL");
        when(repository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));
        when(repository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        var dto = service.updatePreferences("kc-1", null, null);

        assertFalse(dto.darkTheme());
        assertEquals("pl-PL", dto.locale());
    }
}
```

- [ ] **Step 2: Run tests — expect fail**

Run: `.\mvnw.cmd test -Dtest=UserProfileServiceImplPreferencesTest`

Expected: FAIL (method missing) or compile error.

- [ ] **Step 3: Implement request + service + controller**

`UpdatePreferencesRequest.java`:

```java
package com.succeshub.appdomain.dto;

/**
 * Partial update for UI theme and locale preferences stored on the profile.
 */
public record UpdatePreferencesRequest(
        Boolean darkTheme,
        String locale
) {}
```

Allow-list in service (private static final `Set.of("en-US", "pl-PL")`). If `locale` non-null and not allow-listed → `IllegalArgumentException`. Map to 400 in existing exception handler if present; otherwise add `@ExceptionHandler` or validate with a custom constraint — prefer throwing and mapping like other profile errors.

Controller:

```java
@Operation(summary = "Update UI preferences",
        description = "Persists dark/light theme and interface locale on the user profile.")
@ApiResponse(responseCode = "200", description = "Preferences updated")
@ApiResponse(responseCode = "400", description = "Invalid locale or body")
@ApiResponse(responseCode = "401", description = "Not authenticated")
@PatchMapping("/preferences")
public ResponseEntity<ProfileDto> updatePreferences(
        @AuthenticationPrincipal OidcUser principal,
        @RequestBody UpdatePreferencesRequest request) {
    if (principal == null) {
        return ResponseEntity.status(401).build();
    }
    return ResponseEntity.ok(userProfileService.updatePreferences(
            principal.getSubject(), request.darkTheme(), request.locale()));
}
```

Add JavaDoc on interface method with `@param` / `@return` / `@throws`.

- [ ] **Step 4: Run tests — expect pass**

Run: `.\mvnw.cmd test -Dtest=UserProfileServiceImplPreferencesTest,UserProfileServiceImplDisplayNameTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/succeshub/appdomain/dto/UpdatePreferencesRequest.java \
  src/main/java/com/succeshub/appdomain/service/UserProfileService.java \
  src/main/java/com/succeshub/appdomain/service/impl/UserProfileServiceImpl.java \
  src/main/java/com/succeshub/appdomain/controller/ProfileController.java \
  src/test/java/com/succeshub/appdomain/service/impl/UserProfileServiceImplPreferencesTest.java
git commit -m "$(cat <<'EOF'
Wire PATCH profile preferences for theme and locale

EOF
)"
```

---

### Task 3: ThemeService + boot wiring

**Files:**
- Create: `frontend/src/app/core/services/theme.service.ts`
- Modify: `frontend/src/app/app.config.ts`
- Modify: `frontend/src/index.html`
- Modify: `frontend/src/app/features/settings/settings.component.ts`
- Modify: `frontend/src/app/core/services/auth.service.ts` (cookie write on login/logout — partial; finish Keycloak bridge in Task 8)

**Interfaces:**
- Produces: `ThemeService.init(): void`, `setDark(dark: boolean): void`, `isDark(): boolean`, `syncBridgeCookies(): void`
- Cookie names: `successhub_theme=light|dark`, `successhub_locale=<code>` (path `/`; `SameSite=Lax`)

- [ ] **Step 1: Implement ThemeService**

```typescript
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly prefs = inject(AppPreferencesService);

  init(): void {
    this.apply(this.prefs.preferences().darkTheme);
  }

  setDark(dark: boolean): void {
    this.prefs.setDarkTheme(dark);
    this.apply(dark);
  }

  isDark(): boolean {
    return this.prefs.preferences().darkTheme;
  }

  /** Cookies readable by same-origin Keycloak (Docker nginx) + query bridge fallback. */
  syncBridgeCookies(): void {
    const dark = this.isDark();
    const locale = this.prefs.preferences().locale;
    document.cookie = `successhub_theme=${dark ? 'dark' : 'light'}; path=/; SameSite=Lax; max-age=31536000`;
    document.cookie = `successhub_locale=${encodeURIComponent(locale)}; path=/; SameSite=Lax; max-age=31536000`;
  }

  private apply(dark: boolean): void {
    const root = document.documentElement;
    const body = document.body;
    root.classList.toggle('app-dark', dark);
    body?.classList.toggle('app-dark', dark);
    root.style.colorScheme = dark ? 'dark' : 'light';
    const meta = document.querySelector('meta[name="color-scheme"]');
    if (meta) {
      meta.setAttribute('content', dark ? 'dark' : 'light');
    }
    this.syncBridgeCookies();
  }
}
```

- [ ] **Step 2: APP_INITIALIZER + index.html**

In `app.config.ts` add initializer deps `[ThemeService]` → `theme.init()`.

`index.html`: remove inline `style="background-color: #0a0a0c; color: #ededee;"` from `<body>`. Keep initial `class="app-dark"` as FOUC-safe default matching product default; ThemeService corrects after prefs read.

- [ ] **Step 3: Settings uses ThemeService**

```typescript
setDarkTheme(dark: boolean): void {
  this.themeService.setDark(dark);
  // persistence API wired in Task 7
  this.flashSuccess(/* i18n later */);
}
```

- [ ] **Step 4: Verify build**

Run: `cd frontend && npx ng build --configuration development`

Expected: success

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/core/services/theme.service.ts \
  frontend/src/app/app.config.ts frontend/src/index.html \
  frontend/src/app/features/settings/settings.component.ts
git commit -m "$(cat <<'EOF'
Apply dark theme class from Settings preferences

EOF
)"
```

---

### Task 4: Light / dark CSS tokens

**Files:**
- Modify: `frontend/src/global.css`

**Interfaces:**
- Produces: light values as default `@theme` / `:root`; full dark palette overrides under `html.app-dark` / `.app-dark`

- [ ] **Step 1: Restructure tokens**

Move current dark hex values from bare `@theme` into:

```css
@theme {
  /* light defaults — usable shell */
  --color-app-bg: #f4f2f0;
  --color-app-bg-2: #ebe7e2;
  --color-surface: #ffffff;
  --color-surface-2: #f7f5f2;
  --color-surface-3: #efece8;
  --color-border: rgba(20, 16, 12, 0.1);
  --color-border-strong: rgba(20, 16, 12, 0.18);
  --color-text: #1a1816;
  --color-text-muted: #5c574f;
  --color-text-faint: #8a847a;
  --color-on-surface: #1a1816;
  --color-on-surface-variant: #5c574f;
  --color-surface-container-lowest: #ffffff;
  --color-surface-container-low: #f7f5f2;
  --color-surface-container: #efece8;
  --color-surface-container-high: #e6e1db;
  --color-surface-container-highest: #ddd7cf;
  /* keep gold / danger / success / fonts / radii as today */
}

.app-dark {
  --color-app-bg: #0a0a0c;
  --color-app-bg-2: #060608;
  --color-surface: #141417;
  --color-surface-2: #1a1a1e;
  --color-surface-3: #202026;
  --color-border: rgba(255, 255, 255, 0.07);
  --color-border-strong: rgba(255, 255, 255, 0.13);
  --color-text: #ededee;
  --color-text-muted: #9a9aa2;
  --color-text-faint: #62626a;
  --color-on-surface: #e7e1e6;
  --color-on-surface-variant: #d3c5af;
  --color-surface-container-lowest: #0f0d11;
  --color-surface-container-low: #1d1b1f;
  --color-surface-container: #211f23;
  --color-surface-container-high: #2b292d;
  --color-surface-container-highest: #363438;
  /* mirror any other dark Material cousins currently in @theme */
}
```

Also theme scrollbar thumb under light vs `.app-dark` (light: `rgba(0,0,0,0.2)`).

- [ ] **Step 2: Manual spot-check**

Toggle Settings theme: shell bg/text must flip. Dark should match today's look.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/global.css
git commit -m "$(cat <<'EOF'
Add light theme token set beside app-dark palette

EOF
)"
```

---

### Task 5: Feature-page light pass

**Files (audit + fix hardcoded dark):**
- `frontend/src/app/features/loot-boxes/loot-boxes.component.scss`
- `frontend/src/app/features/loot-boxes/reveal-card/reveal-card.component.scss`
- `frontend/src/app/features/settings/settings.component.scss` (`#1a1714`, etc.)
- `frontend/src/app/features/profile/streak-panel/streak-panel.component.scss` (PrimeNG `--p-surface-*` hardcodes)
- `frontend/src/app/features/home/home.component.scss` (if any)
- `frontend/src/app/features/tasks/tasks.component.scss`
- `frontend/src/app/features/goals/goals.component.scss`
- `frontend/src/app/shared/components/navbar/navbar.component.scss`
- `frontend/src/app/shared/components/sidebar/sidebar.component.scss`
- Remove forced `app-dark` class from templates that pin dark (e.g. `streak-panel.component.html` `class="... app-dark"`)

**Pattern:** replace `#211f23` / `#1a1a1e` / `#363438` panel fills with `var(--color-surface-container)` / `var(--color-surface-2)` / `var(--color-surface-container-highest)`. Keep intentional accent hex (gold, crimson `#5d0018`) unless contrast breaks in light — then use `var(--color-danger)` or a darker crimson on light.

- [ ] **Step 1: Grep and replace**

Run: `rg -n "#[0-9a-fA-F]{3,8}" frontend/src/app/features frontend/src/app/shared --glob "*.scss"`

For each dark panel/background hit, switch to tokens. For streak-panel PrimeNG overrides, bind to `var(--color-*)` instead of fixed dark surfaces.

- [ ] **Step 2: Manual verify light mode**

Routes: `/dashboard`, `/tasks`, `/goals`, `/profile`, `/loot-boxes`, `/settings` — text readable, cards not black-on-black.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/features frontend/src/app/shared
git commit -m "$(cat <<'EOF'
Restyle feature pages for light theme tokens

EOF
)"
```

---

### Task 6: TranslateService + packs + drop German

**Files:**
- Create: `frontend/src/assets/i18n/en-US.json`
- Create: `frontend/src/assets/i18n/pl-PL.json`
- Create: `frontend/src/app/core/i18n/translate.service.ts`
- Create: `frontend/src/app/core/i18n/translate.pipe.ts`
- Modify: `frontend/src/app/core/i18n/locales.config.ts`
- Modify: `frontend/src/app/core/services/locale.service.ts`
- Modify: `frontend/src/app/app.config.ts`
- Confirm `angular.json` assets include `src/assets` (already default)

**Interfaces:**
- Produces: `TranslateService.init(): Promise<void>`, `t(key: string): string`, `use(locale: string): Promise<void>`; `TranslatePipe`

- [ ] **Step 1: Packs (minimum keys)**

`en-US.json` / `pl-PL.json` nested:

```json
{
  "nav": {
    "dashboard": "Dashboard",
    "tasks": "Tasks",
    "goals": "Goals",
    "achievements": "Achievements",
    "cache": "Cache",
    "profile": "Profile",
    "settings": "Settings"
  },
  "settings": {
    "appearance": "Appearance",
    "visualTheme": "Visual Theme",
    "visualThemeDesc": "Dark or light appearance",
    "language": "Language",
    "languageDesc": "Interface language",
    "prefsSaved": "Preferences saved.",
    "languageSaved": "Language preference saved."
  },
  "common": {
    "signOut": "Sign out",
    "save": "Save",
    "cancel": "Cancel"
  }
}
```

Polish pack: natural PL equivalents (e.g. `Ustawienia`, `Motyw`, `Język`, `Zapisano preferencje.`).

- [ ] **Step 2: TranslateService + pipe**

Load via `HttpClient.get(`/assets/i18n/${code}.json`)`. Keep `en-US` dictionary as fallback. Nested key resolve: `nav.dashboard` → split by `.`.

`LocaleService.setLocale` must call `translate.use(next)` after prefs update. `init` loads current locale pack.

APP_INITIALIZER: deps `[LocaleService, TranslateService]` — `async () => { locale.init(); await translate.init(); }` (merge with existing locale init carefully so Theme + Translate + Auth order stays sane).

- [ ] **Step 3: Drop `de-DE` from `SUPPORTED_LOCALES`**

- [ ] **Step 4: Build**

Run: `cd frontend && npx ng build --configuration development`

Expected: success; assets copied

- [ ] **Step 5: Commit**

```bash
git add frontend/src/assets/i18n frontend/src/app/core/i18n \
  frontend/src/app/core/services/locale.service.ts \
  frontend/src/app/app.config.ts
git commit -m "$(cat <<'EOF'
Add en and pl translation packs for shell strings

EOF
)"
```

---

### Task 7: Wire shell + Settings i18n and profile persistence

**Files:**
- Modify: `frontend/src/app/core/models/profile.model.ts`
- Modify: `frontend/src/app/core/services/profile.service.ts`
- Modify: `frontend/src/app/shared/components/sidebar/sidebar.component.ts` (+ html if needed)
- Modify: `frontend/src/app/shared/components/navbar/navbar.component.*` (aria-labels / visible strings)
- Modify: `frontend/src/app/features/settings/settings.component.ts` + `.html`
- Hydrate: where profile is first loaded (settings + app shell / home) call `applyServerPreferences(profile)`

**Interfaces:**
- Produces: `ProfileService.updatePreferences(partial: { darkTheme?: boolean; locale?: string }): Observable<UserProfile>`
- `UserProfile.darkTheme: boolean; locale: string`

- [ ] **Step 1: Profile model + API client**

```typescript
updatePreferences(body: { darkTheme?: boolean; locale?: string }): Observable<UserProfile> {
  return this.api
    .patch<UserProfile>('/profile/preferences', body)
    .pipe(tap((profile) => this.profileState.set(profile)));
}
```

- [ ] **Step 2: Hydrate helper** (e.g. on `ProfileService` or small `UiPreferencesFacade`)

When profile arrives:

```typescript
prefs.update({ darkTheme: profile.darkTheme, locale: profile.locale });
theme.apply from prefs; locale.setLocale(profile.locale); // avoid PATCH loop
```

Use a flag or `setLocaleLocalOnly` / `hydrateFromServer` so hydration does not re-PATCH.

- [ ] **Step 3: Settings save path**

```typescript
setDarkTheme(dark: boolean): void {
  this.theme.setDark(dark);
  this.profileService.updatePreferences({ darkTheme: dark }).subscribe({
    next: () => this.flashSuccess(this.i18n.t('settings.prefsSaved')),
    error: () => { /* toast error; refreshProfile + re-hydrate */ },
  });
}

onLocaleChange(code: string): void {
  this.localeService.setLocale(code);
  this.profileService.updatePreferences({ locale: code }).subscribe({ ... });
}
```

- [ ] **Step 4: Sidebar labels via translate pipe**

Change `navItems` to use keys (`labelKey: 'nav.dashboard'`) and template `{{ item.labelKey | translate }}`.

- [ ] **Step 5: Build + manual**

en↔pl flips sidebar + Settings. Reload keeps language. Second browser/session after login restores server prefs.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/core/models/profile.model.ts \
  frontend/src/app/core/services/profile.service.ts \
  frontend/src/app/shared/components/sidebar \
  frontend/src/app/shared/components/navbar \
  frontend/src/app/features/settings
git commit -m "$(cat <<'EOF'
Persist theme and locale on profile and translate shell

EOF
)"
```

---

### Task 8: Keycloak login theme + OAuth bridge

**Files:**
- Modify: `src/main/java/com/succeshub/config/SecurityConfig.java` (`kcActionAuthorizationRequestResolver`)
- Modify: `frontend/src/app/core/services/auth.service.ts`
- Modify: `docker/themes/successhub/login/resources/css/successhub.css`
- Create: `docker/themes/successhub/login/resources/js/theme-bridge.js`
- Modify: `docker/themes/successhub/login/theme.properties` (scripts=…)
- Modify: `docker/themes/successhub/login/login.ftl` (and register.ftl if needed) to ensure script loads — or rely on theme.properties `scripts=`

**Bridge contract:**
- Query: `sh_theme=light|dark`, OIDC `ui_locales=en` or `pl`
- Cookie fallback: `successhub_theme`, `successhub_locale` (same-origin Docker)
- SPA `login()` / `register()` / `logout()` call `theme.syncBridgeCookies()` then redirect with query params:

```typescript
login(): void {
  this.theme.syncBridgeCookies();
  const dark = this.theme.isDark();
  const loc = this.prefs.preferences().locale.startsWith('pl') ? 'pl' : 'en';
  window.location.href =
    `/oauth2/authorization/keycloak?ui_locales=${loc}&sh_theme=${dark ? 'dark' : 'light'}`;
}
```

- [ ] **Step 1: SecurityConfig forwards params**

In `customize(...)`, copy request params into additionalParameters:

```java
String uiLocales = request.getParameter("ui_locales");
String shTheme = request.getParameter("sh_theme");
Map<String, Object> extra = new HashMap<>(authorizationRequest.getAdditionalParameters());
if (uiLocales != null && !uiLocales.isBlank()) {
    extra.put("ui_locales", uiLocales);
}
if (shTheme != null && !shTheme.isBlank()) {
    extra.put("sh_theme", shTheme);
}
builder.additionalParameters(extra);
```

(Keep existing `kc_action` / register logic.)

- [ ] **Step 2: theme-bridge.js**

```javascript
(function () {
  function readCookie(name) {
    const m = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
    return m ? decodeURIComponent(m[1]) : null;
  }
  const params = new URLSearchParams(window.location.search);
  const theme = params.get('sh_theme') || readCookie('successhub_theme') || 'dark';
  const dark = theme !== 'light';
  document.documentElement.classList.toggle('app-dark', dark);
  document.body.classList.toggle('app-dark', dark);
  document.documentElement.style.colorScheme = dark ? 'dark' : 'light';
})();
```

Add to `theme.properties`: `scripts=js/theme-bridge.js ...` (preserve existing scripts if any).

- [ ] **Step 3: Light tokens in successhub.css**

Mirror SPA approach: light defaults on `:root`; move current dark `--sh-*` values under `html.app-dark` / `.app-dark`. Ensure background/card/text use vars (already do).

- [ ] **Step 4: Manual verify**

Set light + pl in Settings → logout → Keycloak login should be light and Polish UI messages where Keycloak locale packs exist (`pl`). If realm lacks `pl`, theme still light; document limitation in Cursour Work.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/succeshub/config/SecurityConfig.java \
  frontend/src/app/core/services/auth.service.ts \
  docker/themes/successhub
git commit -m "$(cat <<'EOF'
Mirror SPA theme and locale on Keycloak login

EOF
)"
```

---

### Task 9: Docs + full verification

**Files:**
- Update Cursour Work Settings page (Notion) with theme/locale/API notes
- Optional: short note on Keycloak Auth BFF page for `ui_locales` / `sh_theme`

- [ ] **Step 1: Run backend tests**

Run: `.\mvnw.cmd test`

Expected: PASS

- [ ] **Step 2: Run frontend build**

Run: `cd frontend && npx ng build --configuration development`

Expected: PASS

- [ ] **Step 3: Manual acceptance checklist**

- [ ] Dark ↔ light flips shell + Home/Tasks/Goals/Profile/Loot/Settings
- [ ] en ↔ pl updates sidebar + Settings; persists reload
- [ ] Prefs survive logout/login (DB)
- [ ] `de-DE` gone from dropdown
- [ ] Keycloak login respects light/dark (+ locale when available)
- [ ] Swagger shows `PATCH /api/profile/preferences`

- [ ] **Step 4: Notion + final commit if doc files changed in repo**

Update Notion only for product notes; no need to commit Notion.

If any repo markdown touched, commit separately.

---

## Spec coverage (self-review)

| Spec requirement | Task |
|------------------|------|
| ThemeService + `app-dark` | 3 |
| Light tokens + feature readability | 4, 5 |
| Translate en/pl shell+Settings | 6, 7 |
| Drop de-DE | 6 |
| Backend profile persistence | 1, 2, 7 |
| Keycloak theme + locale bridge | 8 |
| Verification / OpenAPI | 2, 9 |

No intentional placeholders left. Cookie alone is insufficient across `:4200`/`:8080`; Task 8 uses OAuth query params as the reliable bridge.
