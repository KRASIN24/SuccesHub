import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, MatIconModule, MatDividerModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
export class SidebarComponent {
  readonly navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    { label: 'Goals', icon: 'flag', route: '/goals' },
    { label: 'Tasks', icon: 'task_alt', route: '/tasks' },
    { label: 'Achievements', icon: 'emoji_events', route: '/achievements' },
  ];

  readonly bottomItems: NavItem[] = [
    { label: 'Settings', icon: 'settings', route: '/settings' },
  ];
}
