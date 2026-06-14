import { Component, computed, inject, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

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
  private readonly auth = inject(AuthService);

  readonly collapsed = input(false);
  readonly closeRequested = output<void>();

  readonly displayName = computed(
    () => this.auth.currentUser()?.name ?? 'The Sovereign'
  );

  readonly user = {
    rank: 'Level 42 Productivity Explorer',
  };

  logout(): void {
    this.auth.logout();
  }

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
