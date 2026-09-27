import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ThemeService } from './theme.service';
import { AppPreferencesService } from './app-preferences.service';

export interface User {
  id: string;
  name: string;
  email: string;
  roles: string[];
}

/**
 * Backend-For-Frontend auth client.
 *
 * No tokens are stored in the browser. Login is a full-page redirect to the
 * backend's OAuth2 entry point (which forwards to Keycloak); the resulting
 * session lives in an HttpOnly cookie. Logout is a CSRF-protected form POST so
 * the backend can also end the Keycloak SSO session.
 */
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly theme = inject(ThemeService);
  private readonly prefs = inject(AppPreferencesService);

  private readonly _currentUser = signal<User | null>(null);

  readonly currentUser = this._currentUser.asReadonly();
  readonly isLoggedIn = computed(() => this._currentUser() !== null);
  readonly userInitials = computed(() => {
    const user = this._currentUser();
    if (!user?.name) return '';
    return user.name
      .split(/[\s._-]+/)
      .filter(Boolean)
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  });

  /**
   * Fetches the current session's user. Resolves to null (and clears state)
   * when there is no active session. Safe to call on app startup.
   */
  loadCurrentUser(): Observable<User | null> {
    return this.http.get<User>(`${environment.apiUrl}/user`).pipe(
      tap((user) => {
        this._currentUser.set(user);
        try {
          sessionStorage.removeItem('sh-reg-draft-v1');
        } catch {
          /* ignore */
        }
      }),
      catchError(() => {
        this._currentUser.set(null);
        return of(null);
      })
    );
  }

  /** Starts the OIDC login by redirecting the whole browser to the backend. */
  login(): void {
    try {
      sessionStorage.removeItem('sh-reg-draft-v1');
    } catch {
      /* ignore */
    }
    window.location.href = this.authorizationUrl();
  }

  /**
   * Starts Keycloak self-registration via the BFF so the OAuth state cookie is
   * set before the browser leaves for :8080.
   */
  register(): void {
    window.location.href = this.authorizationUrl({ register: true });
  }

  /**
   * Logs out via a top-level form POST so Spring Security can invalidate the
   * session and trigger Keycloak RP-initiated logout. The CSRF token is read
   * from the XSRF-TOKEN cookie and submitted as the _csrf parameter.
   */
  logout(): void {
    this.theme.syncBridgeCookies();

    const submitLogout = () => {
      const form = document.createElement('form');
      form.method = 'post';
      form.action = '/logout';

      const csrfToken = this.readCookie('XSRF-TOKEN');
      if (!csrfToken) {
        window.location.href =
          'http://localhost:8080/realms/succeshub-realm/protocol/openid-connect/logout'
          + '?client_id=succeshub-backend'
          + '&post_logout_redirect_uri='
          + encodeURIComponent('http://localhost:4200/');
        return;
      }

      const input = document.createElement('input');
      input.type = 'hidden';
      input.name = '_csrf';
      input.value = csrfToken;
      form.appendChild(input);

      this._currentUser.set(null);
      document.body.appendChild(form);
      form.submit();
    };

    if (!this.readCookie('XSRF-TOKEN')) {
      this.http.get(`${environment.apiUrl}/user`, { observe: 'response' }).pipe(
        catchError(() => of(null))
      ).subscribe(() => submitLogout());
      return;
    }
    submitLogout();
  }

  private authorizationUrl(opts?: { register?: boolean }): string {
    this.theme.syncBridgeCookies();
    const dark = this.theme.isDark();
    const loc = this.prefs.preferences().locale.startsWith('pl') ? 'pl' : 'en';
    const params = new URLSearchParams({
      ui_locales: loc,
      sh_theme: dark ? 'dark' : 'light',
    });
    if (opts?.register) {
      params.set('register', '1');
    }
    return `/oauth2/authorization/keycloak?${params.toString()}`;
  }

  private readCookie(name: string): string | null {
    const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
    return match ? decodeURIComponent(match[1]) : null;
  }
}
