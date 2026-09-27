/**
 * Supported UI locales for SuccessHub.
 * Add entries here as translation packs land in MVP.
 *
 * Adding a language:
 * 1. Append an {@link AppLocale} here.
 * 2. Copy {@code assets/i18n/en-US.json} → {@code assets/i18n/{code}.json} and translate.
 * 3. Keep the same key tree — especially {@code rewards.*} and {@code achievements.*} —
 *    so catalog items localize without backend changes.
 */
export interface AppLocale {
  /** BCP 47 code, e.g. en-US */
  code: string;
  /** Label shown in the language dropdown (English name). */
  label: string;
  /** Native endonym, e.g. Polski */
  nativeLabel: string;
}

export const DEFAULT_LOCALE = 'en-US';

/**
 * Registry of languages the app can switch to.
 * Packs live at {@code /assets/i18n/{code}.json}.
 */
export const SUPPORTED_LOCALES: readonly AppLocale[] = [
  { code: 'en-US', label: 'English (US)', nativeLabel: 'English' },
  { code: 'pl-PL', label: 'Polish', nativeLabel: 'Polski' },
] as const;

export function isSupportedLocale(code: string): boolean {
  return SUPPORTED_LOCALES.some((l) => l.code === code);
}

export function localeLabel(code: string): string {
  return SUPPORTED_LOCALES.find((l) => l.code === code)?.label ?? code;
}
