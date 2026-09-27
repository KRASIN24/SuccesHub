import { Injectable, computed, inject } from '@angular/core';
import {
  AppLocale,
  DEFAULT_LOCALE,
  SUPPORTED_LOCALES,
  isSupportedLocale,
} from '../i18n/locales.config';
import { TranslateService } from '../i18n/translate.service';
import { AppPreferencesService } from './app-preferences.service';

/**
 * Locale / language selection for MVP i18n.
 * Persists via {@link AppPreferencesService}, sets {@code <html lang>},
 * and drives {@link TranslateService} packs.
 */
@Injectable({ providedIn: 'root' })
export class LocaleService {
  private readonly prefs = inject(AppPreferencesService);
  private readonly translate = inject(TranslateService);

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

  /** Call once at app startup so html lang + packs match stored preference. */
  async init(): Promise<void> {
    const code = this.locale();
    this.applyToDocument(code);
    await this.translate.init(code);
  }

  /**
   * Applies locale locally without writing prefs (server hydration).
   */
  async hydrate(code: string): Promise<void> {
    const next = isSupportedLocale(code) ? code : DEFAULT_LOCALE;
    this.prefs.update({ locale: next });
    this.applyToDocument(next);
    await this.translate.use(next);
  }

  setLocale(code: string): void {
    const next = isSupportedLocale(code) ? code : DEFAULT_LOCALE;
    this.prefs.update({ locale: next });
    this.applyToDocument(next);
    void this.translate.use(next);
  }

  private applyToDocument(code: string): void {
    if (typeof document === 'undefined') return;
    document.documentElement.lang = code;
  }
}
