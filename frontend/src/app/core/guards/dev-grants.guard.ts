import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { DevToolsService } from '../services/dev-tools.service';
import { GamificationService } from '../services/gamification.service';

/**
 * Allows navigation only when {@code succeshub.loot.enable-dev-grants} is on
 * (same gate as the floating Dev bubble).
 */
export const devGrantsGuard: CanActivateFn = () => {
  const tools = inject(DevToolsService);
  const gamification = inject(GamificationService);
  const router = inject(Router);

  if (tools.enabled()) {
    return true;
  }

  return gamification.getClientConfig().pipe(
    map((config) => {
      tools.applyConfig(config);
      return config.enableLootDevGrants
        ? true
        : router.createUrlTree(['/dashboard']);
    }),
    catchError(() => of(router.createUrlTree(['/dashboard'])))
  );
};
