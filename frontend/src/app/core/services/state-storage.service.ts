import { Injectable } from '@angular/core';

/**
 * Stores the SPA URL the user intended before a full-page OIDC login so we can
 * restore it after Keycloak returns to the default frontend root.
 */
@Injectable({
  providedIn: 'root',
})
export class StateStorageService {
  private static readonly KEY = 'successhub_previous_url';

  storeUrl(url: string | null | undefined): void {
    const safe = this.sanitize(url);
    if (!safe) {
      return;
    }
    try {
      sessionStorage.setItem(StateStorageService.KEY, safe);
    } catch {
      /* ignore quota / private mode */
    }
  }

  getUrl(): string | null {
    try {
      return this.sanitize(sessionStorage.getItem(StateStorageService.KEY));
    } catch {
      return null;
    }
  }

  clearUrl(): void {
    try {
      sessionStorage.removeItem(StateStorageService.KEY);
    } catch {
      /* ignore */
    }
  }

  /**
   * Allows only same-app relative paths. Rejects protocol-relative, absolute,
   * and auth entry/exit paths that would loop the OIDC dance.
   */
  sanitize(url: string | null | undefined): string | null {
    if (!url || typeof url !== 'string') {
      return null;
    }
    const trimmed = url.trim();
    if (!trimmed.startsWith('/') || trimmed.startsWith('//')) {
      return null;
    }
    if (/^[a-z][a-z0-9+.-]*:/i.test(trimmed)) {
      return null;
    }
    const pathOnly = trimmed.split(/[?#]/, 1)[0]!.toLowerCase();
    if (
      pathOnly === '/oauth2' ||
      pathOnly.startsWith('/oauth2/') ||
      pathOnly === '/login' ||
      pathOnly.startsWith('/login/') ||
      pathOnly === '/logout' ||
      pathOnly.startsWith('/logout/')
    ) {
      return null;
    }
    return trimmed;
  }
}
