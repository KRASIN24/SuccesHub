import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { DEFAULT_LOCALE, isSupportedLocale } from './locales.config';

type Dict = Record<string, unknown>;

/**
 * Lightweight runtime i18n for shell, Settings, and catalog labels.
 * Loads JSON packs from {@code /assets/i18n/{locale}.json}.
 *
 * <p>To add a language: register it in {@link SUPPORTED_LOCALES}, then add a pack
 * with the same key tree (including {@code rewards.*} for titles/frames).
 * Missing entries fall back to en-US, then to the API/default English string.
 */
@Injectable({ providedIn: 'root' })
export class TranslateService {
  private readonly http = inject(HttpClient);
  private readonly dict = signal<Dict>({});
  private fallback: Dict = {};
  private current = DEFAULT_LOCALE;

  /** Version bump so impure translate pipe views refresh on pack switch. */
  readonly revision = signal(0);

  async init(locale: string = DEFAULT_LOCALE): Promise<void> {
    this.fallback = await this.loadPack(DEFAULT_LOCALE);
    await this.use(locale);
  }

  async use(locale: string): Promise<void> {
    const code = isSupportedLocale(locale) ? locale : DEFAULT_LOCALE;
    if (code === DEFAULT_LOCALE) {
      this.dict.set(this.fallback);
      this.current = code;
      this.revision.update((n) => n + 1);
      return;
    }
    try {
      const pack = await this.loadPack(code);
      this.dict.set(pack);
      this.current = code;
      this.revision.update((n) => n + 1);
    } catch {
      this.dict.set(this.fallback);
      this.current = DEFAULT_LOCALE;
      this.revision.update((n) => n + 1);
    }
  }

  t(key: string, params?: Record<string, string | number>): string {
    // Touch revision so template bindings depending on this service refresh.
    this.revision();
    const fromActive = this.resolve(this.dict(), key);
    let raw = fromActive ?? this.resolve(this.fallback, key) ?? key;
    if (params) {
      for (const [k, v] of Object.entries(params)) {
        raw = raw.replaceAll(`{{${k}}}`, String(v));
      }
    }
    return raw;
  }

  /**
   * Localized catalog label for a stable entity key (e.g. reward {@code TITLE_APEX_OPERATOR}).
   * Looks up {@code namespace.entityKey} in the active pack, then en-US, then {@code fallback}.
   */
  catalogLabel(namespace: string, entityKey: string, fallback: string): string {
    this.revision();
    const key = `${namespace}.${entityKey}`;
    return this.resolve(this.dict(), key) ?? this.resolve(this.fallback, key) ?? fallback;
  }

  /**
   * Nested catalog field, e.g. {@code achievements.SPEEDSTER.label}.
   * Used when an entity has multiple localizable strings (label + description).
   */
  catalogEntry(
    namespace: string,
    entityKey: string,
    field: string,
    fallback: string
  ): string {
    this.revision();
    const key = `${namespace}.${entityKey}.${field}`;
    return this.resolve(this.dict(), key) ?? this.resolve(this.fallback, key) ?? fallback;
  }

  private async loadPack(code: string): Promise<Dict> {
    return firstValueFrom(this.http.get<Dict>(`/assets/i18n/${code}.json`));
  }

  private resolve(dict: Dict, key: string): string | null {
    const parts = key.split('.');
    let cur: unknown = dict;
    for (const part of parts) {
      if (cur == null || typeof cur !== 'object') {
        return null;
      }
      cur = (cur as Dict)[part];
    }
    return typeof cur === 'string' ? cur : null;
  }
}
