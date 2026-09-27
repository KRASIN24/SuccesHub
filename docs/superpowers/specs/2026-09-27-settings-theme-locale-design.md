# W13 — Settings: Visual Theme + Language

**Date:** 2026-09-27  
**Status:** Approved — implementation plan next  
**Branch:** `feature/settings-theme-locale`  
**Notion:** Next Steps W13

## Goal

Make Settings **Visual Theme** and **Language** actually change the product: dark/light across the SPA (including feature pages), en/pl for shell + Settings, and matching Keycloak login appearance + locale.

## Decisions (locked)

| Topic | Choice |
| --- | --- |
| Approach | ThemeService + light CSS vars + small custom i18n (no ngx-translate yet) |
| Locales | **en-US** + **pl-PL** only; drop **de-DE** from dropdown until packs exist |
| Light quality | Usable / intentional app-wide — fix hardcoded dark in feature pages |
| Keycloak | Login theme follows SPA light/dark + en/pl |
| Prefs storage | **Backend profile is source of truth** when logged in; `localStorage` is cache + pre-login / Keycloak bridge |
| Still out | German pack · W15 one-button/brand design-system unify |

## Architecture

### Theme

- New **`ThemeService`** (`providedIn: 'root'`):
  - `init()` via `APP_INITIALIZER` (alongside `LocaleService.init`)
  - Reads `AppPreferencesService.darkTheme`
  - Adds/removes `app-dark` on `<html>` and `<body>`
  - Syncs `color-scheme` / meta so browser UI matches
- Settings `app-theme-toggle` → prefs + `ThemeService.setDark(dark)` (today prefs-only)
- PrimeNG already uses `darkModeSelector: '.app-dark'` — keep that contract
- **`index.html`**: stop hardcoding dark-only body inline colors that fight light mode; ThemeService owns class before paint

### Tokens (`global.css`)

- Light defaults on `:root` (bg, surface, text, border, Material surface/on-surface cousins)
- Current dark `@theme` values live under `.app-dark`
- Feature SCSS: replace hardcoded dark hex/rgba with `var(--color-*)` or `:root` / `.app-dark` pairs so Tasks, Goals, Home, Profile/streak, Loot, etc. are readable in light mode
- W15 still owns unifying competing button/brand systems; W13 owns “light is not broken”

### Language

- **`TranslateService`**: load `assets/i18n/en-US.json` and `pl-PL.json`
- Template `translate` pipe + `t(key)` for TS strings
- Missing key → English string → raw key
- Pack load failure → last good pack / English; never blank the shell
- Wire: sidebar, navbar, Settings (labels, toasts, dialogs on that page)
- Feature page copy stays English until a later pass (except where theme SCSS changes touch templates)
- `LocaleService.setLocale` updates prefs, `html[lang]`, and active pack
- Remove `de-DE` from `SUPPORTED_LOCALES`

### Backend persistence (profile)

Theme and locale travel with the user account, not only the browser.

- **Columns** on `succeshub.user_profile` (Liquibase):
  - `dark_theme` `BOOLEAN NOT NULL DEFAULT TRUE`
  - `locale` `VARCHAR(16) NOT NULL DEFAULT 'en-US'`
- **DTO:** extend `ProfileDto` with `darkTheme` + `locale`
- **API:** `PATCH /api/profile/preferences` body `{ "darkTheme"?: boolean, "locale"?: string }` — validate locale against allow-list (`en-US`, `pl-PL`); return full `ProfileDto`
- JavaDoc + OpenAPI on controller; service method + unit test for update + invalid locale

### Sync rules (logged-in)

1. **Boot / after profile load:** server values win → write into `AppPreferencesService` → ThemeService + LocaleService/TranslateService apply
2. **Settings change:** optimistic local apply → `PATCH /preferences` → on success keep; on failure toast error and re-fetch profile (or revert)
3. **localStorage** remains a fast cache and the bridge for Keycloak before/during login (cookie still written from local prefs)
4. **First login** with only local prefs and default server columns: optional one-time push of local → server if user already chose light/pl before profile existed (migrate on first GET if server still defaults and local differs — keep simple: push on first Settings change or on first authenticated boot if local ≠ server defaults)

### Keycloak login theme

- Extend `docker/themes/successhub` with the same light/dark CSS variable split as the SPA; login JS toggles `app-dark` from `sh_theme` query param (forwarded by BFF OAuth customizer) with cookie fallback `successhub_theme` for same-origin Docker
- SPA calls `ThemeService.syncBridgeCookies()` then redirects to `/oauth2/authorization/keycloak?ui_locales=…&sh_theme=…`
- Language: OIDC `ui_locales` mapped from `en-US` → `en`, `pl-PL` → `pl`
- If Keycloak ignores locale, login still works in realm default; document the contract under Cursour Work

## Data flow

```
Settings toggle/select
  → apply locally (prefs + Theme/Locale/Translate)
  → PATCH /api/profile/preferences
  → ProfileDto updates client cache
  → on logout/login redirect: cookie + ui_locales for Keycloak

App boot (authenticated)
  → GET /api/profile
  → hydrate darkTheme + locale from server into prefs + DOM
```

## Error handling

- Corrupt local prefs → default **dark** + **en-US** until profile loads
- Invalid locale on PATCH → `400`; UI keeps previous value
- PATCH failure → toast; re-sync from GET profile
- Theme apply is idempotent
- Translate missing keys / pack errors degrade to English
- Keycloak sync is entry-time only (cookie/query at redirect), not live mid-flow

## Acceptance

- [ ] Dark ↔ light from Settings flips shell + major feature routes with intentional light tokens
- [ ] en ↔ pl updates sidebar, navbar, Settings strings; persists across reload
- [ ] Theme + locale saved on `user_profile` and restored after login on another browser/session
- [ ] `de-DE` removed from language dropdown
- [ ] Keycloak login reflects light/dark + en/pl set in the SPA
- [ ] `ng build` + `mvnw test` green; OpenAPI/JavaDoc updated for preferences endpoint

## Verification

- Manual: Settings theme + language, spot-check Home / Tasks / Goals / Profile / Loot in light
- Manual: change prefs → logout → login (or other browser) → same theme/locale
- Manual: logout → Keycloak login with light + pl
- Build/tests as above

## Out of scope (explicit)

- German (or other) translation packs
- Syncing sound/animations/notification toggles to the backend (stay localStorage unless added later)
- W15 design-system unify (single button/brand everywhere)
- Translating all feature-page English copy in this bite
