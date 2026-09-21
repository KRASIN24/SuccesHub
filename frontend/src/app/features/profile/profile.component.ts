import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { GamificationService } from '../../core/services/gamification.service';
import { ProfileService } from '../../core/services/profile.service';
import { AuthService } from '../../core/services/auth.service';
import { EquippedCosmeticsService } from '../../core/services/equipped-cosmetics.service';
import { InventoryItem, RewardItem } from '../../core/models/gamification.model';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';
import { StreakPanelComponent } from './streak-panel/streak-panel.component';

type Category = 'UTILITY' | 'TITLE' | 'AVATAR_FRAME';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    LoadingSkeletonComponent,
    ErrorStateComponent,
    StreakPanelComponent,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent implements OnInit {
  private readonly gamification = inject(GamificationService);
  private readonly profileService = inject(ProfileService);
  private readonly authService = inject(AuthService);
  private readonly cosmetics = inject(EquippedCosmeticsService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);
  readonly inventory = signal<InventoryItem[]>([]);

  readonly profile = this.profileService.profile;
  readonly user = this.authService.currentUser;
  readonly userInitials = this.authService.userInitials;

  readonly totalItems = computed(() => this.inventory().reduce((sum, i) => sum + i.quantity, 0));
  readonly utilityStacks = computed(() => this.byCategory('UTILITY'));
  readonly titleStacks = computed(() => this.byCategory('TITLE'));
  readonly frameStacks = computed(() => this.byCategory('AVATAR_FRAME'));

  /** Total count of legendary-rarity items owned (summed across stacks). */
  readonly legendaryCount = computed(() =>
    this.inventory()
      .filter((i) => i.reward.rarity === 'LEGENDARY')
      .reduce((sum, i) => sum + i.quantity, 0)
  );

  /** Distinct rewards collected, regardless of quantity. */
  readonly uniqueCount = computed(
    () => new Set(this.inventory().map((i) => i.reward.key)).size
  );

  readonly stats = computed(() => [
    { label: 'Items Held', value: this.totalItems(), icon: 'inventory_2' },
    { label: 'Legendaries', value: this.legendaryCount(), icon: 'trophy' },
    { label: 'Unique Rewards', value: this.uniqueCount(), icon: 'auto_awesome' },
  ]);

  readonly xpPercent = computed(() => {
    const p = this.profile();
    if (!p || p.nextLevelXp === 0) return 0;
    return Math.round((p.currentXp / p.nextLevelXp) * 100);
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
    }).subscribe({
      next: ({ inventory }) => {
        this.inventory.set(inventory);
        this.cosmetics.applyInventory(inventory);
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
    this.gamification.getInventory().subscribe((inv) => this.inventory.set(inv));
    this.profileService.getProfile().subscribe();
  }

  equip(item: InventoryItem): void {
    if (this.categoryOf(item.reward) === 'UTILITY' || this.busy()) return;
    this.busy.set(true);
    this.gamification.equipInventoryItem(item.id).subscribe({
      next: () => {
        this.gamification.getInventory().subscribe({
          next: (inv) => {
            this.inventory.set(inv);
            this.cosmetics.applyInventory(inv);
            this.busy.set(false);
          },
          error: () => this.busy.set(false),
        });
      },
      error: () => this.busy.set(false),
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

  revealCardClass(rarity: string): string {
    switch (rarity) {
      case 'LEGENDARY':
        return 'reveal-card--legendary';
      case 'RARE':
        return 'reveal-card--rare';
      default:
        return 'reveal-card--common';
    }
  }

  revealRarityTextClass(rarity: string): string {
    switch (rarity) {
      case 'LEGENDARY':
        return 'reveal-card__rarity--legendary';
      case 'RARE':
        return 'reveal-card__rarity--rare';
      default:
        return 'reveal-card__rarity--common';
    }
  }

  private byCategory(category: Category): InventoryItem[] {
    return this.inventory().filter((i) => this.categoryOf(i.reward) === category);
  }
}
