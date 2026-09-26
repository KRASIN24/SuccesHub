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
    // #region agent log
    fetch('http://127.0.0.1:7452/ingest/37b8ef57-bd0f-4015-8754-90251ebe3ff8',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'0d3552'},body:JSON.stringify({sessionId:'0d3552',hypothesisId:'A',location:'auth.service.ts:login',message:'spa-login-redirect',data:{href:window.location.href,hasSessionCookie:document.cookie.includes('JSESSIONID'),hasOauthCookie:document.cookie.includes('SUCCHUB_OAUTH2')},timestamp:Date.now()})}).catch(()=>{});
    // #endregion
    try {
      sessionStorage.removeItem('sh-reg-draft-v1');
    } catch {
      /* ignore */
    }
    window.location.href = '/oauth2/authorization/keycloak';
  }

  /**
   * Starts Keycloak self-registration via the BFF so the OAuth state cookie is
   * set before the browser leaves for :8080.
   */
  register(): void {
    // #region agent log
    fetch('http://127.0.0.1:7452/ingest/37b8ef57-bd0f-4015-8754-90251ebe3ff8',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'0d3552'},body:JSON.stringify({sessionId:'0d3552',hypothesisId:'D',location:'auth.service.ts:register',message:'spa-register-redirect',data:{href:window.location.href},timestamp:Date.now()})}).catch(()=>{});
    // #endregion
    window.location.href = '/oauth2/authorization/keycloak?register=1';
  }

  /**
   * Logs out via a top-level form POST so Spring Security can invalidate the
   * session and trigger Keycloak RP-initiated logout. The CSRF token is read
   * from the XSRF-TOKEN cookie and submitted as the _csrf parameter.
   */
  logout(): void {
    // #region agent log
    fetch('http://127.0.0.1:7452/ingest/37b8ef57-bd0f-4015-8754-90251ebe3ff8',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'0d3552'},body:JSON.stringify({sessionId:'0d3552',hypothesisId:'L',location:'auth.service.ts:logout',message:'spa-logout-submit',data:{hasXsrf:!!this.readCookie('XSRF-TOKEN'),href:window.location.href},timestamp:Date.now()})}).catch(()=>{});
    // #endregion

    const submitLogout = () => {
      const form = document.createElement('form');
      form.method = 'post';
      form.action = '/logout';

      const csrfToken = this.readCookie('XSRF-TOKEN');
      if (!csrfToken) {
        // Without CSRF Spring returns 403 and the sidebar button looks dead.
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

    // Ensure the XSRF cookie exists (first paint can race the CsrfCookieFilter).
    if (!this.readCookie('XSRF-TOKEN')) {
      this.http.get(`${environment.apiUrl}/user`, { observe: 'response' }).pipe(
        catchError(() => of(null))
      ).subscribe(() => submitLogout());
      return;
    }

    submitLogout();
  }

  private readCookie(name: string): string | null {
    const match = document.cookie.match(
      new RegExp('(?:^|; )' + name.replace(/([.$?*|{}()[\]\\/+^])/g, '\\$1') + '=([^;]*)')
    );
    return match ? decodeURIComponent(match[1]) : null;
  }
}
