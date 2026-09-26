import { Injectable, inject, signal } from '@angular/core';
import { Achievement } from '../models/achievement.model';
import { BossDamage, CloseDayResult, RewardEvent, TaskCompletion } from '../models/gamification.model';
import { ProfileService } from './profile.service';
import { LootPendingService } from './loot-pending.service';
import { NotificationService } from './notification.service';
import { AppPreferencesService } from './app-preferences.service';

/**
 * Global celebration overlays (XP tick, level-up, achievements, boss damage).
 * Loot boxes are NOT opened here — earned caches stay pending until the user
 * opens them on the Celestial Cache page.
 *
 * Achievement / loot / boss-slain overlays respect Settings → Notifications.
 */
@Injectable({
  providedIn: 'root',
})
export class GamificationCelebrationService {
  private readonly profileService = inject(ProfileService);
  private readonly lootPending = inject(LootPendingService);
  private readonly notifications = inject(NotificationService);
  private readonly prefs = inject(AppPreferencesService);
  private sequence = 0;

  readonly xpTrigger = signal<{ xp: number; tick: number } | null>(null);
  readonly levelUpTrigger = signal<{ level: number; tick: number } | null>(null);
  readonly achievementTrigger = signal<{ achievement: Achievement; tick: number } | null>(null);
  readonly cacheEarnedTrigger = signal<{ count: number; tick: number } | null>(null);
  readonly bossDamageTrigger = signal<{ damage: BossDamage; tick: number } | null>(null);
  /** Bumped when goals should soft-refresh after boss damage. */
  readonly goalsRefreshTick = signal(0);

  handleTaskCompletion(completion: TaskCompletion): void {
    if (completion.updatedProfile) {
      this.profileService.applyProfile(completion.updatedProfile);
    }

    const reward = completion.reward;
    if (!reward) {
      return;
    }
    this.playReward(reward);
  }

  handleCloseDay(result: CloseDayResult): void {
    this.profileService.refreshProfile().subscribe({
      error: (err) => console.error('Failed to refresh profile after close day', err),
    });

    const showAchievements = this.prefs.preferences().notifyAchievementUnlocked;
    if (showAchievements) {
      result.achievementsUnlocked.forEach((achievement, index) => {
        setTimeout(() => {
          this.achievementTrigger.set({ achievement, tick: ++this.sequence });
        }, index * 1200);
      });
    }

    if (result.lootBoxesEarned.length > 0) {
      const count = result.lootBoxesEarned.length;
      this.lootPending.notifyEarned(count);
      if (this.shouldShowCacheToast()) {
        this.cacheEarnedTrigger.set({ count, tick: ++this.sequence });
      }
    }

    if (
      result.achievementsUnlocked.length > 0 ||
      result.lootBoxesEarned.length > 0
    ) {
      this.notifications.refresh();
    }
  }

  private playReward(reward: RewardEvent): void {
    if (reward.xp.totalXp > 0) {
      this.xpTrigger.set({ xp: reward.xp.totalXp, tick: ++this.sequence });
    }

    if (reward.leveledUp) {
      setTimeout(() => {
        this.levelUpTrigger.set({ level: reward.newLevel, tick: ++this.sequence });
      }, 400);
    }

    if (reward.bossDamage) {
      const damage = reward.bossDamage;
      setTimeout(() => {
        if (this.shouldShowBossToast(damage)) {
          this.bossDamageTrigger.set({ damage, tick: ++this.sequence });
        }
        this.goalsRefreshTick.update((n) => n + 1);
      }, 550);
    }

    if (this.prefs.preferences().notifyAchievementUnlocked) {
      reward.achievementsUnlocked.forEach((achievement, index) => {
        setTimeout(() => {
          this.achievementTrigger.set({ achievement, tick: ++this.sequence });
        }, 800 + index * 1200);
      });
    }

    if (reward.lootBoxEarned) {
      this.lootPending.notifyEarned(1);
      if (this.shouldShowCacheToast()) {
        this.cacheEarnedTrigger.set({ count: 1, tick: ++this.sequence });
      }
    }

    if (
      reward.achievementsUnlocked.length > 0 ||
      reward.lootBoxEarned ||
      reward.bossDamage?.goalCompleted
    ) {
      this.notifications.refresh();
    }
  }

  /** Cache toast covers loot + streak-milestone grants (same UI). */
  private shouldShowCacheToast(): boolean {
    const p = this.prefs.preferences();
    return p.notifyLootEarned || p.notifyStreakMilestone;
  }

  /**
   * Damage ticks always show; slain toast respects Boss defeated preference.
   */
  private shouldShowBossToast(damage: BossDamage): boolean {
    if (!damage.goalCompleted) {
      return true;
    }
    return this.prefs.preferences().notifyBossDefeated;
  }
}
