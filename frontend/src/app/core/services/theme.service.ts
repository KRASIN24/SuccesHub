import { Injectable, inject } from '@angular/core';
import { AppPreferencesService } from './app-preferences.service';

/**
 * Applies dark/light appearance via the {@code app-dark} class and bridge cookies
 * for same-origin Keycloak / OAuth query fallbacks.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly prefs = inject(AppPreferencesService);

  /** Call once at app startup so the DOM matches stored preference. */
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

  /** Cookies readable by Keycloak (same host / localhost) + query bridge fallback. */
  syncBridgeCookies(): void {
    const dark = this.isDark();
    const locale = this.prefs.preferences().locale;
    const kcLocale = locale.toLowerCase().startsWith('pl') ? 'pl' : 'en';
    document.cookie = `successhub_theme=${dark ? 'dark' : 'light'}; path=/; SameSite=Lax; max-age=31536000`;
    document.cookie = `successhub_locale=${encodeURIComponent(locale)}; path=/; SameSite=Lax; max-age=31536000`;
    // Keycloak reads this for FreeMarker message bundles when ui_locales is dropped.
    document.cookie = `KEYCLOAK_LOCALE=${kcLocale}; path=/; SameSite=Lax; max-age=31536000`;
  }

  applyFromPreference(): void {
    this.apply(this.prefs.preferences().darkTheme);
  }

  private apply(dark: boolean): void {
    if (typeof document === 'undefined') {
      return;
    }
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
