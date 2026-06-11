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
    path: 'tasks',
    loadComponent: () =>
      import('./features/tasks/tasks.component').then((m) => m.TasksComponent),
    title: 'Quest Log — SuccesHub',
  },
  {
    path: 'goals',
    loadComponent: () =>
      import('./features/goals/goals.component').then((m) => m.GoalsComponent),
    title: 'Boss Battle — SuccesHub',
  },
  {
    path: 'achievements',
    loadComponent: () =>
      import('./features/placeholder/placeholder.component').then(
        (m) => m.PlaceholderComponent
      ),
    title: 'Achievements — SuccesHub',
    data: { title: 'Achievements', icon: 'military_tech' },
  },
  {
    path: 'profile',
    loadComponent: () =>
      import('./features/placeholder/placeholder.component').then(
        (m) => m.PlaceholderComponent
      ),
    title: 'Profile — SuccesHub',
    data: { title: 'Profile', icon: 'person' },
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
