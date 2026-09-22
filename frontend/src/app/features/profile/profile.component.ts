import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { GamificationService } from '../../core/services/gamification.service';
import { ProfileService } from '../../core/services/profile.service';
import { AuthService } from '../../core/services/auth.service';
import { EquippedCosmeticsService } from '../../core/services/equipped-cosmetics.service';
import { AchievementService } from '../../core/services/achievement.service';
import { TaskService } from '../../core/services/task.service';
import { InventoryItem, RewardItem } from '../../core/models/gamification.model';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';
import { FrameOrnamentsComponent } from '../../shared/components/frame-ornaments/frame-ornaments.component';
import { StreakPanelComponent } from './streak-panel/streak-panel.component';

type Category = 'UTILITY' | 'TITLE' | 'AVATAR_FRAME';

interface OverviewStat {
  category: string;
  label: string;
  icon: string;
  value: string | number;
  suffix?: string;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    LoadingSkeletonComponent,
    ErrorStateComponent,
    StreakPanelComponent,
    FrameOrnamentsComponent,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent implements OnInit {
  private readonly gamification = inject(GamificationService);
  private readonly profileService = inject(ProfileService);
  private readonly authService = inject(AuthService);
  private readonly achievementsApi = inject(AchievementService);
  private readonly tasksApi = inject(TaskService);
  /** Shared equipped TITLE / FRAME — also drives navbar + dashboard chrome. */
  protected readonly cosmetics = inject(EquippedCosmeticsService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);
  readonly inventory = signal<InventoryItem[]>([]);
  readonly tasksCompleted = signal(0);
  readonly achievementsUnlocked = signal(0);
  readonly achievementsTotal = signal(0);

  readonly profile = this.profileService.profile;
  readonly user = this.authService.currentUser;
  readonly userInitials = this.authService.userInitials;

  readonly utilityStacks = computed(() => this.byCategory('UTILITY'));
  readonly titleStacks = computed(() => this.byCategory('TITLE'));
  readonly frameStacks = computed(() => this.byCategory('AVATAR_FRAME'));

  readonly rankLabel = computed(() => {
    const title = this.cosmetics.equippedTitle()?.label;
    if (title) {
      return title;
    }
    return 'UNRANKED';
  });

  readonly systemUid = computed(() => {
    const id = this.profile()?.keycloakId || this.user()?.id || '';
    if (!id) {
      return 'SH-————';
    }
    const compact = id.replace(/[^a-zA-Z0-9]/g, '').slice(-5).toUpperCase();
    return `SH-${compact || '00000'}`;
  });

  readonly xpPercent = computed(() => {
    const p = this.profile();
    if (!p || p.nextLevelXp === 0) return 0;
    return Math.round((p.currentXp / p.nextLevelXp) * 100);
  });

  readonly overviewStats = computed<OverviewStat[]>(() => {
    const p = this.profile();
    const unlocked = this.achievementsUnlocked();
    const total = this.achievementsTotal();
    return [
      {
        category: 'Consistency',
        label: 'Day streak',
        icon: 'local_fire_department',
        value: p?.currentStreak ?? 0,
      },
      {
        category: 'Potential',
        label: 'Level XP',
        icon: 'database',
        value: (p?.currentXp ?? 0).toLocaleString(),
      },
      {
        category: 'Execution',
        label: 'Tasks completed',
        icon: 'check_circle',
        value: this.tasksCompleted(),
      },
      {
        category: 'Legacy',
        label: 'Achievements',
        icon: 'trophy',
        value: unlocked,
        suffix: total > 0 ? `/${total}` : undefined,
      },
    ];
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      inventory: this.gamification.getInventory(),
      profile: this.profile() ? of(this.profile()!) : this.profileService.getProfile(),
      achievements: this.achievementsApi.getAchievements(),
      doneTasks: this.tasksApi.getTasks('DONE'),
    }).subscribe({
      next: ({ inventory, achievements, doneTasks }) => {
        this.replaceInventory(inventory);
        this.achievementsTotal.set(achievements.length);
        this.achievementsUnlocked.set(achievements.filter((a) => !a.locked).length);
        this.tasksCompleted.set(doneTasks.length);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load profile or collection.');
        this.loading.set(false);
      },
    });
  }

  /** Refetches inventory and profile after a streak action without flashing the full-page skeleton. */
  onStreakChanged(): void {
    this.gamification.getInventory().subscribe((inv) => this.mergeInventory(inv));
    this.profileService.getProfile().subscribe();
  }

  /**
   * Equips a title/frame like a radio: the chosen item becomes Equipped and stays in place.
   * Clicking the already-equipped item is a no-op (does not unequip).
   */
  equip(item: InventoryItem): void {
    if (this.categoryOf(item.reward) === 'UTILITY' || this.busy() || item.equipped) {
      return;
    }

    const type = item.reward.type;

    // Optimistic: only flip equipped flags — never reorder the list.
    this.inventory.update((list) =>
      list.map((entry) => {
        if (entry.id === item.id) {
          return { ...entry, equipped: true };
        }
        if (entry.reward.type === type && entry.equipped) {
          return { ...entry, equipped: false };
        }
        return entry;
      })
    );
    this.cosmetics.applyInventory(this.inventory());

    this.busy.set(true);
    this.gamification.equipInventoryItem(item.id).subscribe({
      next: () => {
        this.gamification.getInventory().subscribe({
          next: (inv) => {
            this.mergeInventory(inv);
            this.busy.set(false);
          },
          error: () => this.busy.set(false),
        });
      },
      error: () => {
        this.gamification.getInventory().subscribe({
          next: (inv) => {
            this.mergeInventory(inv);
            this.busy.set(false);
          },
          error: () => this.busy.set(false),
        });
      },
    });
  }

  categoryOf(reward: RewardItem): Category {
    switch (reward.type) {
      case 'TITLE':
        return 'TITLE';
      case 'FRAME':
        return 'AVATAR_FRAME';
      default:
        return 'UTILITY';
    }
  }

  /** Full replace with a stable sort (initial load). */
  private replaceInventory(inv: InventoryItem[]): void {
    const sorted = this.stableSort(inv);
    this.inventory.set(sorted);
    this.cosmetics.applyInventory(sorted);
  }

  /**
   * Patch equipped/qty from the server while keeping the current visual order.
   * Prevents Collection chips/frames from jumping when the API returns a shuffled list.
   */
  private mergeInventory(inv: InventoryItem[]): void {
    const byId = new Map(inv.map((i) => [i.id, i]));
    const current = this.inventory();

    if (current.length === 0) {
      this.replaceInventory(inv);
      return;
    }

    const merged = current
      .filter((c) => byId.has(c.id))
      .map((c) => {
        const fresh = byId.get(c.id)!;
        return {
          ...c,
          equipped: fresh.equipped,
          quantity: fresh.quantity,
          reward: fresh.reward,
        };
      });

    const known = new Set(merged.map((i) => i.id));
    for (const item of this.stableSort(inv)) {
      if (!known.has(item.id)) {
        merged.push(item);
      }
    }

    this.inventory.set(merged);
    this.cosmetics.applyInventory(merged);
  }

  private byCategory(category: Category): InventoryItem[] {
    return this.inventory().filter((i) => this.categoryOf(i.reward) === category);
  }

  private stableSort(items: InventoryItem[]): InventoryItem[] {
    return [...items].sort((a, b) => {
      const keyCmp = (a.reward.key || '').localeCompare(b.reward.key || '');
      if (keyCmp !== 0) return keyCmp;
      return a.id.localeCompare(b.id);
    });
  }
}
