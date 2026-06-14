import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

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
      tap((user) => this._currentUser.set(user)),
      catchError(() => {
        this._currentUser.set(null);
        return of(null);
      })
    );
  }

  /** Starts the OIDC login by redirecting the whole browser to the backend. */
  login(): void {
    window.location.href = '/oauth2/authorization/keycloak';
  }

  /**
   * Logs out via a top-level form POST so Spring Security can invalidate the
   * session and trigger Keycloak RP-initiated logout. The CSRF token is read
   * from the XSRF-TOKEN cookie and submitted as the _csrf parameter.
   */
  logout(): void {
    const form = document.createElement('form');
    form.method = 'post';
    form.action = '/logout';

    const csrfToken = this.readCookie('XSRF-TOKEN');
    if (csrfToken) {
      const input = document.createElement('input');
      input.type = 'hidden';
      input.name = '_csrf';
      input.value = csrfToken;
      form.appendChild(input);
    }

    document.body.appendChild(form);
    form.submit();
  }

  private readCookie(name: string): string | null {
    const match = document.cookie.match(
      new RegExp('(?:^|; )' + name.replace(/([.$?*|{}()[\]\\/+^])/g, '\\$1') + '=([^;]*)')
    );
    return match ? decodeURIComponent(match[1]) : null;
  }
}
