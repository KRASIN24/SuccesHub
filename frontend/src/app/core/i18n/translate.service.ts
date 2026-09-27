import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { DEFAULT_LOCALE, isSupportedLocale } from './locales.config';

type Dict = Record<string, unknown>;

/**
 * Lightweight runtime i18n for shell + Settings strings.
 * Loads JSON packs from {@code /assets/i18n/{locale}.json}.
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

  t(key: string): string {
    // Touch revision so template bindings depending on this service refresh.
    this.revision();
    const fromActive = this.resolve(this.dict(), key);
    if (fromActive != null) {
      return fromActive;
    }
    const fromEn = this.resolve(this.fallback, key);
    return fromEn ?? key;
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
