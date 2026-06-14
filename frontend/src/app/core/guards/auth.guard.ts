import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Allows navigation only when there is an active session. The session state is
 * resolved once at startup (see APP_INITIALIZER in app.config.ts), so this is a
 * synchronous check. When unauthenticated, it kicks off the OIDC login redirect.
 */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);

  if (auth.isLoggedIn()) {
    return true;
  }

  auth.login();
  return false;
};
