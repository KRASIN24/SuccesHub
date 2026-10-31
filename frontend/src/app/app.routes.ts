import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { activeAccountGuard } from './core/guards/active-account.guard';
import { devGrantsGuard } from './core/guards/dev-grants.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'account/reactivate',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/account/reactivate-account.component').then(
        (m) => m.ReactivateAccountComponent
      ),
    title: 'Reactivate account — SuccessHub',
  },
  {
    path: 'dashboard',
    canActivate: [authGuard, activeAccountGuard],
    loadComponent: () =>
      import('./features/home/home.component').then((m) => m.HomeComponent),
    title: 'Dashboard — SuccessHub',
  },
  {
    path: 'tasks',
    canActivate: [authGuard, activeAccountGuard],
    loadComponent: () =>
      import('./features/tasks/tasks.component').then((m) => m.TasksComponent),
    title: 'Tasks — SuccessHub',
  },
  {
    path: 'goals',
    canActivate: [authGuard, activeAccountGuard],
    loadComponent: () =>
      import('./features/goals/goals.component').then((m) => m.GoalsComponent),
    title: 'Campaigns — SuccessHub',
  },
  {
    path: 'goals/:id',
    canActivate: [authGuard, activeAccountGuard],
    loadComponent: () =>
      import('./features/goals/goal-detail.component').then((m) => m.GoalDetailComponent),
    title: 'Campaign — SuccessHub',
  },
  {
    path: 'achievements',
    canActivate: [authGuard, activeAccountGuard],
    loadComponent: () =>
      import('./features/achievements/achievements.component').then(
        (m) => m.AchievementsComponent
      ),
    title: 'Achievements — SuccessHub',
  },
  {
    path: 'loot-boxes',
    canActivate: [authGuard, activeAccountGuard],
    loadComponent: () =>
      import('./features/loot-boxes/loot-boxes.component').then((m) => m.LootBoxesComponent),
    title: 'The Vault — SuccessHub',
  },
  {
    path: 'cache',
    redirectTo: 'loot-boxes',
    pathMatch: 'full',
  },
  {
    path: 'profile',
    canActivate: [authGuard, activeAccountGuard],
    loadComponent: () =>
      import('./features/profile/profile.component').then((m) => m.ProfileComponent),
    title: 'Profile — SuccessHub',
    data: { title: 'Profile', icon: 'person' },
  },
  {
    path: 'settings',
    canActivate: [authGuard, activeAccountGuard],
    loadComponent: () =>
      import('./features/settings/settings.component').then(
        (m) => m.SettingsComponent
      ),
    title: 'Settings — SuccessHub',
  },
  {
    path: 'dev/visual-lab',
    canActivate: [authGuard, activeAccountGuard, devGrantsGuard],
    loadComponent: () =>
      import('./features/dev/visual-lab/visual-lab.component').then(
        (m) => m.VisualLabComponent
      ),
    title: 'Visual Lab — SuccessHub',
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
