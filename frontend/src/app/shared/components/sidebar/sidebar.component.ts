import { Component, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
  host: {
    '[class.collapsed]': 'collapsed()',
  },
})
export class SidebarComponent {
  readonly collapsed = input(false);
  readonly closeRequested = output<void>();

  readonly user = {
    name: 'The Sovereign',
    rank: 'Level 42 Productivity Explorer',
  };

  readonly navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'grid_view', route: '/dashboard' },
    { label: 'Tasks', icon: 'check_circle', route: '/tasks' },
    { label: 'Goals', icon: 'my_location', route: '/goals' },
    { label: 'Achievements', icon: 'military_tech', route: '/achievements' },
    { label: 'Profile', icon: 'person', route: '/profile' },
  ];

  readonly bottomItems: NavItem[] = [
    { label: 'Settings', icon: 'settings', route: '/settings' },
  ];
}
