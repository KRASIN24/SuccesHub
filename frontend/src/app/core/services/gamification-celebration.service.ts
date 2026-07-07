import { Injectable, inject, signal } from '@angular/core';
import { Achievement } from '../models/achievement.model';
import { CloseDayResult, RewardEvent, TaskCompletion } from '../models/gamification.model';
import { ProfileService } from './profile.service';
import { LootPendingService } from './loot-pending.service';

/**
 * Global celebration overlays (XP tick, level-up, achievements).
 * Loot boxes are NOT opened here — earned caches stay pending until the user
 * opens them on the Celestial Cache page.
 */
@Injectable({
  providedIn: 'root',
})
export class GamificationCelebrationService {
  private readonly profileService = inject(ProfileService);
  private readonly lootPending = inject(LootPendingService);
  private sequence = 0;

  readonly xpTrigger = signal<{ xp: number; tick: number } | null>(null);
  readonly levelUpTrigger = signal<{ level: number; tick: number } | null>(null);
  readonly achievementTrigger = signal<{ achievement: Achievement; tick: number } | null>(null);
  readonly cacheEarnedTrigger = signal<{ count: number; tick: number } | null>(null);

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
    result.achievementsUnlocked.forEach((achievement, index) => {
      setTimeout(() => {
        this.achievementTrigger.set({ achievement, tick: ++this.sequence });
      }, index * 1200);
    });
    if (result.lootBoxesEarned.length > 0) {
      const count = result.lootBoxesEarned.length;
      this.cacheEarnedTrigger.set({ count, tick: ++this.sequence });
      this.lootPending.notifyEarned(count);
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

    reward.achievementsUnlocked.forEach((achievement, index) => {
      setTimeout(() => {
        this.achievementTrigger.set({ achievement, tick: ++this.sequence });
      }, 800 + index * 1200);
    });
    if (reward.lootBoxEarned) {
      this.cacheEarnedTrigger.set({ count: 1, tick: ++this.sequence });
      this.lootPending.notifyEarned(1);
    }
  }
}
