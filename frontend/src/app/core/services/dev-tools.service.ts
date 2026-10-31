import { Injectable, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { EquippedCosmeticsService } from './equipped-cosmetics.service';
import { GamificationService } from './gamification.service';
import { LootPendingService } from './loot-pending.service';
import { ProfileService } from './profile.service';
import { BoxType, DevCampaign, DevDiagnostics, DevRewardCatalog } from '../models/gamification.model';
import { UserProfile } from '../models/profile.model';

/** Tool ids for the hybrid messenger bubble system. */
export type DevToolId =
  | 'streak'
  | 'cache'
  | 'time'
  | 'ritual'
  | 'progress'
  | 'inventory'
  | 'combat'
  | 'campaigns';

/**
 * Shared state for gated local/demo testing tools.
 * Invisible when {@code enableLootDevGrants} is false.
 */
@Injectable({
  providedIn: 'root',
})
export class DevToolsService {
  private readonly gamification = inject(GamificationService);
  private readonly auth = inject(AuthService);
  private readonly profileService = inject(ProfileService);
  private readonly lootPending = inject(LootPendingService);
  private readonly cosmetics = inject(EquippedCosmeticsService);

  readonly enabled = signal(false);
  readonly openTool = signal<DevToolId | null>(null);
  readonly feedback = signal<string | null>(null);
  readonly busy = signal(false);
  readonly effectiveToday = signal('');
  readonly dayOffset = signal(0);
  readonly boxTypes = signal<BoxType[]>([]);
  readonly streakSetValue = signal(0);
  /** Last known streak after a Dev streak action (shown in the panel). */
  readonly liveStreak = signal<number | null>(null);
  /** When set, the panel shows a confirm step before running a destructive action. */
  readonly pendingConfirm = signal<'reset-streak' | 'clear-clock' | null>(null);
  /**
   * Bumped after any mutation so Profile / Home / streak calendar can refetch.
   * Starts at 0; consumers should ignore the initial 0.
   */
  readonly dataRevision = signal(0);

  readonly diagnostics = signal<DevDiagnostics | null>(null);
  readonly rewards = signal<DevRewardCatalog[]>([]);
  readonly rewardFilter = signal('');
  readonly selectedRewardKey = signal('');
  readonly grantQuantity = signal(1);
  readonly xpAmount = signal(100);
  readonly levelValue = signal(1);
  readonly shieldsValue = signal(0);
  readonly doubleStrike = signal(false);
  readonly surgeToken = signal(false);
  readonly campaigns = signal<DevCampaign[]>([]);
  readonly selectedCampaignId = signal('');
  readonly campaignPending = signal(10);
  readonly campaignProgress = signal(0);

  /** Loads config (+ box types when enabled). Safe to call on every navigation. */
  refresh(): void {
    if (!this.auth.isLoggedIn()) {
      this.enabled.set(false);
      this.openTool.set(null);
      return;
    }
    this.gamification.getClientConfig().subscribe({
      next: (config) => {
        this.enabled.set(config.enableLootDevGrants);
        this.effectiveToday.set(config.effectiveToday);
        this.dayOffset.set(config.dayOffset);
        if (!config.enableLootDevGrants) {
          this.openTool.set(null);
          return;
        }
        this.gamification.getBoxTypes().subscribe({
          next: (types) => this.boxTypes.set(types),
          error: () => this.boxTypes.set([]),
        });
        this.refreshDiagnostics();
      },
      error: () => {
        this.enabled.set(false);
        this.openTool.set(null);
      },
    });
  }

  refreshDiagnostics(): void {
    if (!this.auth.isLoggedIn() || !this.enabled()) {
      return;
    }
    this.gamification.getDevDiagnostics().subscribe({
      next: (diag) => {
        this.diagnostics.set(diag);
        this.effectiveToday.set(diag.effectiveToday);
        this.dayOffset.set(diag.dayOffset);
        this.liveStreak.set(diag.currentStreak);
        this.levelValue.set(diag.level);
        this.shieldsValue.set(diag.streakShields);
        this.doubleStrike.set(diag.pendingDoubleStrike);
        this.surgeToken.set(diag.pendingSurgeToken);
      },
      error: () => this.diagnostics.set(null),
    });
  }

  loadRewards(): void {
    this.gamification.getDevRewards().subscribe({
      next: (list) => {
        this.rewards.set(list);
        if (!this.selectedRewardKey() && list.length) {
          this.selectedRewardKey.set(list[0].key);
        }
      },
      error: () => this.rewards.set([]),
    });
  }

  loadCampaigns(): void {
    this.gamification.getDevCampaigns().subscribe({
      next: (list) => {
        this.campaigns.set(list);
        const selected = this.selectedCampaignId();
        if (!selected || !list.some((c) => c.id === selected)) {
          this.selectedCampaignId.set(list[0]?.id ?? '');
        }
        const current = list.find((c) => c.id === this.selectedCampaignId());
        if (current) {
          this.campaignPending.set(Math.max(1, Math.round(current.pendingDamage) || 10));
          this.campaignProgress.set(current.currentProgress);
        }
      },
      error: () => this.campaigns.set([]),
    });
  }

  filteredRewards(): DevRewardCatalog[] {
    const q = this.rewardFilter().trim().toLowerCase();
    if (!q) {
      return this.rewards();
    }
    return this.rewards().filter(
      (r) =>
        r.key.toLowerCase().includes(q) ||
        r.label.toLowerCase().includes(q) ||
        r.type.toLowerCase().includes(q)
    );
  }

  selectedCampaign(): DevCampaign | undefined {
    return this.campaigns().find((c) => c.id === this.selectedCampaignId());
  }

  toggle(tool: DevToolId): void {
    if (!this.enabled()) {
      return;
    }
    this.pendingConfirm.set(null);
    this.openTool.update((current) => (current === tool ? null : tool));
  }

  open(tool: DevToolId): void {
    if (!this.enabled()) {
      return;
    }
    this.pendingConfirm.set(null);
    this.openTool.set(tool);
    if (tool === 'inventory') {
      this.loadRewards();
    }
    if (tool === 'campaigns') {
      this.loadCampaigns();
    }
    this.refreshDiagnostics();
  }

  close(): void {
    this.openTool.set(null);
    this.pendingConfirm.set(null);
  }

  setFeedback(message: string | null): void {
    this.feedback.set(message);
  }

  applyConfig(config: { effectiveToday: string; dayOffset: number; enableLootDevGrants: boolean }): void {
    this.enabled.set(config.enableLootDevGrants);
    this.effectiveToday.set(config.effectiveToday);
    this.dayOffset.set(config.dayOffset);
  }

  /**
   * Notify shell + feature pages that gamification state changed.
   * Refreshes shared profile / pending loot / cosmetics immediately so navbar and
   * other live surfaces update without a full page reload.
   */
  notifyDataChanged(): void {
    this.dataRevision.update((n) => n + 1);
    this.refreshDiagnostics();
    if (!this.auth.isLoggedIn()) {
      return;
    }
    this.profileService.refreshProfile().subscribe({ error: () => undefined });
    this.lootPending.refresh();
    this.cosmetics.refresh();
    if (this.openTool() === 'campaigns') {
      this.loadCampaigns();
    }
  }

  /** Apply a profile snapshot returned by a Dev grant API (instant XP/level UI). */
  applyProfile(profile: UserProfile): void {
    this.profileService.applyProfile(profile);
    this.levelValue.set(profile.level);
    this.liveStreak.set(profile.currentStreak);
  }

  applyStreakResult(currentStreak: number): void {
    this.liveStreak.set(currentStreak);
    this.streakSetValue.set(currentStreak);
  }
}
