import { Component, inject, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { LootPendingService } from '../../../core/services/loot-pending.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  showPendingBadge?: boolean;
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
  protected readonly lootPending = inject(LootPendingService);

  readonly collapsed = input(false);
  readonly closeRequested = output<void>();

  logout(): void {
    this.auth.logout();
  }

  readonly navItems: NavItem[] = [
    { label: 'Dashboard', icon: 'grid_view', route: '/dashboard' },
    { label: 'Tasks', icon: 'check_circle', route: '/tasks' },
    { label: 'Goals', icon: 'my_location', route: '/goals' },
    { label: 'Achievements', icon: 'military_tech', route: '/achievements' },
    { label: 'Cache', icon: 'inventory_2', route: '/loot-boxes', showPendingBadge: true },
    { label: 'Profile', icon: 'person', route: '/profile' },
  ];

  /** Settings stays a PlaceholderComponent — hide until a real page ships. */
  readonly bottomItems: NavItem[] = [];
}
