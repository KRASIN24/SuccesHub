import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, catchError, of } from 'rxjs';
import { AccountService } from '../services/account.service';
import { AuthService } from '../services/auth.service';

/**
 * When the session is logged in but the account is in the deletion grace period,
 * send the user to the confirm-first reactivate screen instead of the app.
 */
export const activeAccountGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const account = inject(AccountService);
  const router = inject(Router);

  if (!auth.isLoggedIn()) {
    return true;
  }

  return account.getStatus().pipe(
    map((status) => {
      if (status.deactivated) {
        return router.createUrlTree(['/account/reactivate']);
      }
      return true;
    }),
    catchError(() => of(true))
  );
};
