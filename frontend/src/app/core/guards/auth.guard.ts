import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Allows navigation only when there is an active session. The session state is
 * resolved once at startup (see APP_INITIALIZER in app.config.ts), so this is a
 * synchronous check. When unauthenticated, it kicks off the OIDC login redirect
 * (or registration when {@code ?register=1} is present).
 */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);

  if (auth.isLoggedIn()) {
    return true;
  }

  // Prefer registration when Create account bounced through the SPA with ?register=1
  // so we never race into login() and send the user back to the Keycloak login page.
  if (
    typeof window !== 'undefined' &&
    new URLSearchParams(window.location.search).get('register') === '1'
  ) {
    auth.register();
    return false;
  }

  auth.login();
  return false;
};
