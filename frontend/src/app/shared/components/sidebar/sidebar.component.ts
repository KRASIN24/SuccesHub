import { Component, OnInit, computed, inject, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ProfileService } from '../../../core/services/profile.service';
import { LootPendingService } from '../../../core/services/loot-pending.service';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';

interface NavItem {
  labelKey: string;
  icon: string;
  route: string;
  showPendingBadge?: boolean;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, TranslatePipe],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
  host: {
    '[class.collapsed]': 'collapsed()',
  },
})
export class SidebarComponent implements OnInit {
  private readonly auth = inject(AuthService);
  protected readonly profileService = inject(ProfileService);
  protected readonly lootPending = inject(LootPendingService);

  readonly collapsed = input(false);
  readonly closeRequested = output<void>();

  readonly displayName = computed(
    () =>
      this.profileService.profile()?.displayName ??
      this.auth.currentUser()?.name ??
      'The Sovereign'
  );

  ngOnInit(): void {
    if (!this.profileService.profile()) {
      this.profileService.getProfile().subscribe({
        error: (err) => console.error('Failed to load sidebar profile', err),
      });
    }
  }

  logout(): void {
    this.auth.logout();
  }

  readonly navItems: NavItem[] = [
    { labelKey: 'nav.dashboard', icon: 'grid_view', route: '/dashboard' },
    { labelKey: 'nav.tasks', icon: 'check_circle', route: '/tasks' },
    { labelKey: 'nav.goals', icon: 'my_location', route: '/goals' },
    { labelKey: 'nav.achievements', icon: 'military_tech', route: '/achievements' },
    { labelKey: 'nav.cache', icon: 'inventory_2', route: '/loot-boxes', showPendingBadge: true },
    { labelKey: 'nav.profile', icon: 'person', route: '/profile' },
  ];

  readonly bottomItems: NavItem[] = [
    { labelKey: 'nav.settings', icon: 'settings', route: '/settings' },
  ];
}
