/**
 * Supported UI locales for SuccessHub.
 * Add entries here as translation packs land in MVP.
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
 * Translation files / ngx-translate packs should match these codes.
 */
export const SUPPORTED_LOCALES: readonly AppLocale[] = [
  { code: 'en-US', label: 'English (US)', nativeLabel: 'English' },
  { code: 'pl-PL', label: 'Polish', nativeLabel: 'Polski' },
  { code: 'de-DE', label: 'German', nativeLabel: 'Deutsch' },
] as const;

export function isSupportedLocale(code: string): boolean {
  return SUPPORTED_LOCALES.some((l) => l.code === code);
}

export function localeLabel(code: string): string {
  return SUPPORTED_LOCALES.find((l) => l.code === code)?.label ?? code;
}
