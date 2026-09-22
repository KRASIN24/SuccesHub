import { Injectable, computed, inject } from '@angular/core';
import {
  AppLocale,
  DEFAULT_LOCALE,
  SUPPORTED_LOCALES,
  isSupportedLocale,
} from '../i18n/locales.config';
import { AppPreferencesService } from './app-preferences.service';

/**
 * Locale / language selection for MVP i18n.
 * Persists via {@link AppPreferencesService} and sets {@code <html lang>}.
 * Wire ngx-translate (or Angular i18n) to {@link locale} when translation packs land.
 */
@Injectable({ providedIn: 'root' })
export class LocaleService {
  private readonly prefs = inject(AppPreferencesService);

  readonly supportedLocales: readonly AppLocale[] = SUPPORTED_LOCALES;

  readonly locale = computed(() => {
    const code = this.prefs.preferences().locale;
    return isSupportedLocale(code) ? code : DEFAULT_LOCALE;
  });

  readonly currentLocaleMeta = computed(
    () =>
      SUPPORTED_LOCALES.find((l) => l.code === this.locale()) ??
      SUPPORTED_LOCALES[0]
  );

  /** Call once at app startup so html lang matches stored preference. */
  init(): void {
    this.applyToDocument(this.locale());
  }

  setLocale(code: string): void {
    const next = isSupportedLocale(code) ? code : DEFAULT_LOCALE;
    this.prefs.update({ locale: next });
    this.applyToDocument(next);
  }

  private applyToDocument(code: string): void {
    if (typeof document === 'undefined') return;
    document.documentElement.lang = code;
  }
}
