import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/home/home.component').then((m) => m.HomeComponent),
    title: 'Dashboard — SuccesHub',
  },
  {
    path: 'tasks',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/tasks/tasks.component').then((m) => m.TasksComponent),
    title: 'Quest Log — SuccesHub',
  },
  {
    path: 'goals',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/goals/goals.component').then((m) => m.GoalsComponent),
    title: 'Boss Battle — SuccesHub',
  },
  {
    path: 'achievements',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/placeholder/placeholder.component').then(
        (m) => m.PlaceholderComponent
      ),
    title: 'Achievements — SuccesHub',
    data: { title: 'Achievements', icon: 'military_tech' },
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/placeholder/placeholder.component').then(
        (m) => m.PlaceholderComponent
      ),
    title: 'Profile — SuccesHub',
    data: { title: 'Profile', icon: 'person' },
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/placeholder/placeholder.component').then(
        (m) => m.PlaceholderComponent
      ),
    title: 'Settings — SuccesHub',
    data: { title: 'Settings', icon: 'settings' },
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
