# Theme and localization

SuccessHub supports dark/light appearance and English/Polish runtime
translations in both the Angular app and the Keycloak login flow. Theme and
locale are account preferences; the browser cache only supplies the first paint
and the pre-login bridge.

## User-visible behavior

- **Theme:** Settings switches the SPA between light and dark immediately.
  PrimeNG and feature styles follow the `app-dark` class.
- **Language:** Settings switches between `en-US` and `pl-PL` without a reload.
  Normal missing keys fall back to English, then to the key itself. Catalog
  labels use the API-provided English label as their final fallback.
- **Persistence:** authenticated profile values restore on later sessions and
  override stale browser values after the profile loads.
- **Keycloak:** login, registration, and post-logout pages preserve the selected
  theme and use Keycloak's `en` or `pl` message bundle.

German is not supported. Sound, animation, and notification preferences remain
local-only; `PATCH /api/profile/preferences` persists only theme and locale.
Localization covers the shell, Settings, and the main feature pages, but it is
not yet exhaustive: some transient overlays, toasts, and account-reactivation
copy remain English-only.

## Source-of-truth flow

```text
App startup
  localStorage successhub_prefs
    -> ThemeService + LocaleService (fast first paint)

Authenticated profile load
  GET /api/profile
    -> ProfileService
    -> user_profile.dark_theme + user_profile.locale
    -> localStorage + DOM + active translation pack

Settings change
  apply locally
    -> PATCH /api/profile/preferences
    -> keep returned profile, or refresh profile to revert after an error
```

The backend defaults are dark theme and `en-US`. The Liquibase migration adds
`user_profile.dark_theme BOOLEAN NOT NULL DEFAULT TRUE` and
`user_profile.locale VARCHAR(16) NOT NULL DEFAULT 'en-US'`.

## Preferences API

`PATCH /api/profile/preferences` requires the normal BFF session and CSRF
protection. Both fields are optional; an omitted or `null` field is unchanged.

```json
{
  "darkTheme": false,
  "locale": "pl-PL"
}
```

The response is the complete `ProfileDto`. Supported locale values are
case-sensitive:

| Value | Result |
| --- | --- |
| `en-US` | English pack |
| `pl-PL` | Polish pack |
| Any other non-null value | `400 BAD_REQUEST` |

The public controller contract is in `ProfileController`; validation and
persistence live in `UserProfileServiceImpl`.

## SPA architecture

| Area | Responsibility |
| --- | --- |
| `AppPreferencesService` | Reads and writes `successhub_prefs`; corrupt or unavailable storage falls back to dark + `en-US` |
| `ThemeService` | Toggles `app-dark` on `<html>` and `<body>`, sets `color-scheme`, and writes bridge cookies |
| `LocaleService` | Validates the locale, sets `<html lang>`, and activates a translation pack |
| `TranslateService` | Loads `/assets/i18n/{locale}.json`; resolves nested keys and English fallback |
| `ProfileService` | Hydrates browser state from the server profile and keeps PATCH responses synchronized |
| `global.css` | Defines light defaults and `.app-dark` token overrides |

`ThemeService`, `LocaleService`, and session loading are registered as Angular
`APP_INITIALIZER`s. PrimeNG uses `darkModeSelector: '.app-dark'`, so removing or
renaming that class breaks third-party widget theming.

The English pack is the fallback pack and is required at startup. A missing
Polish pack falls back to English; a missing or invalid English pack can reject
the locale initializer and must be treated as a build/deployment error.

## Keycloak bridge

The SPA never sends a token or account data to the login theme. It sends only UI
preferences:

| Channel | Values | Purpose |
| --- | --- | --- |
| OAuth query | `sh_theme=light\|dark` | Custom Keycloak theme appearance |
| OAuth query | `ui_locales=en\|pl`, `kc_locale=en\|pl` | OIDC hint and FreeMarker message locale |
| Cookies | `successhub_theme`, `successhub_locale`, `KEYCLOAK_LOCALE` | Same-host fallback across Keycloak redirects |

The flow is:

1. `AuthService` writes bridge cookies and starts the BFF OAuth redirect.
2. `BridgePrefsCookieFilter` validates and repeats the UI-only cookies on the
   OAuth entry response.
3. `SecurityConfig` forwards the locale/theme parameters to Keycloak.
4. `theme-bridge.js` resolves query, referrer, then cookie values; it applies
   `app-dark` and seeds `KEYCLOAK_LOCALE`.
5. Logout posts the current preferences to Spring, which includes them in the
   post-logout login URL.

The cookies are intentionally readable by JavaScript and must never contain
secrets. Cookie sharing is a same-host fallback (for example, localhost on
different ports); query/referrer propagation remains necessary when origins
differ.

## Adding a locale

Adding only a JSON file is insufficient. Update every allow-list and bridge:

1. Add the BCP 47 locale to `frontend/src/app/core/i18n/locales.config.ts`.
2. Copy `en-US.json` to `frontend/src/assets/i18n/{locale}.json` and preserve the
   complete key tree, especially `achievements.*` and `rewards.*`.
3. Add the exact BCP 47 value to `UserProfileServiceImpl.SUPPORTED_LOCALES`.
4. Expand the short Keycloak-locale mapping in `ThemeService`,
   `AuthService`, `BridgePrefsCookieFilter`, `SecurityConfig`, and
   `theme-bridge.js`.
5. Add the Keycloak `messages_{language}.properties` bundle and enable the
   locale in the realm configuration.
6. Add backend validation tests, compare translation-pack keys, then run the
   backend tests and frontend build.

Keycloak `.properties` bundles use Java properties encoding; follow the existing
escaped Unicode style for non-ASCII text.

## Troubleshooting

### Selection changes, then changes back

The Settings UI applies optimistically. A failed PATCH triggers
`GET /api/profile`, and the stored server value replaces the local selection.
Inspect the PATCH response and CSRF/session state instead of changing
`localStorage` manually.

### SPA is translated but Keycloak is not

- Confirm the authorization URL contains both `ui_locales` and `kc_locale`.
- Inspect `KEYCLOAK_LOCALE` and `successhub_locale`.
- Confirm internationalization and the locale are enabled in the active realm.
- Realm export changes apply automatically only on first import. Update an
  existing realm or recreate the local volume if losing local data is acceptable.

### Keycloak keeps old CSS or JavaScript

Compose mounts `docker/themes/successhub` from the checkout running Docker.
Make sure that is the checkout containing the changes. Development Compose
disables theme caches, but the browser may still need a hard refresh.

### Raw translation keys appear

The key is absent from both the active and English packs. Keep both JSON key
trees aligned. API-backed catalog labels also require stable backend keys to
match `achievements.*` or `rewards.*`.

### Light mode has isolated dark surfaces

Feature styles should consume `var(--color-*)` tokens. Hardcoded dark
backgrounds and `rgba(255,255,255,...)` borders do not adapt. The Keycloak theme
has a separate token set in `docker/themes/successhub/login/resources/css/`.

## Verification

```bash
./mvnw test
cd frontend
npm run build
npm run test:ci
```

Manual smoke:

1. Switch theme and locale in `/settings`; reload and confirm the values remain.
2. Visit the main feature routes in light mode and check text/card contrast.
3. Log out, register, and log in; confirm Keycloak preserves appearance and
   locale.
4. Sign in with the same user in another browser profile; confirm server-backed
   preferences hydrate after the profile loads.
