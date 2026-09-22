import { Component, OnInit, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { SidebarComponent } from './shared/components/sidebar/sidebar.component';
import { XpTickComponent } from './shared/components/gamification/xp-tick.component';
import { LevelUpOverlayComponent } from './shared/components/gamification/level-up-overlay.component';
import { AchievementSlamComponent } from './shared/components/gamification/achievement-slam.component';
import { CacheEarnedToastComponent } from './shared/components/gamification/cache-earned-toast.component';
import { BossDamageToastComponent } from './shared/components/gamification/boss-damage-toast.component';
import { DevToolsHostComponent } from './shared/components/dev-tools/dev-tools-host.component';
import { GamificationCelebrationService } from './core/services/gamification-celebration.service';
import { LootPendingService } from './core/services/loot-pending.service';
import { EquippedCosmeticsService } from './core/services/equipped-cosmetics.service';
import { AuthService } from './core/services/auth.service';
import { DevToolsService } from './core/services/dev-tools.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    NavbarComponent,
    SidebarComponent,
    XpTickComponent,
    LevelUpOverlayComponent,
    AchievementSlamComponent,
    CacheEarnedToastComponent,
    BossDamageToastComponent,
    DevToolsHostComponent,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  private readonly mobileBreakpoint = 960;
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly lootPending = inject(LootPendingService);
  private readonly cosmetics = inject(EquippedCosmeticsService);
  private readonly devTools = inject(DevToolsService);
  readonly celebrations = inject(GamificationCelebrationService);

  readonly sidenavOpen = signal(this.isDesktop());

  constructor() {
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => {
        if (!this.isDesktop()) {
          this.sidenavOpen.set(false);
        }
      });
  }

  ngOnInit(): void {
    if (this.auth.isLoggedIn()) {
      this.lootPending.refresh();
      this.cosmetics.refresh();
      this.devTools.refresh();
    }
  }

  toggleSidenav(): void {
    this.sidenavOpen.update((v) => !v);
  }

  closeSidenav(): void {
    this.sidenavOpen.set(false);
  }

  private isDesktop(): boolean {
    return typeof window !== 'undefined' && window.innerWidth > this.mobileBreakpoint;
  }
}
