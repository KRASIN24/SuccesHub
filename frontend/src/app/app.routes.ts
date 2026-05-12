import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/home/home.component').then((m) => m.HomeComponent),
    title: 'Dashboard — SuccesHub',
  },
  {
    path: 'goals',
    loadComponent: () =>
      import('./features/placeholder/placeholder.component').then(
        (m) => m.PlaceholderComponent
      ),
    title: 'Goals — SuccesHub',
    data: { title: 'Goals', icon: 'flag' },
  },
  {
    path: 'tasks',
    loadComponent: () =>
      import('./features/placeholder/placeholder.component').then(
        (m) => m.PlaceholderComponent
      ),
    title: 'Tasks — SuccesHub',
    data: { title: 'Tasks', icon: 'task_alt' },
  },
  {
    path: 'achievements',
    loadComponent: () =>
      import('./features/placeholder/placeholder.component').then(
        (m) => m.PlaceholderComponent
      ),
    title: 'Achievements — SuccesHub',
    data: { title: 'Achievements', icon: 'emoji_events' },
  },
  {
    path: 'settings',
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
