import { Injectable, inject, signal } from '@angular/core';
import { Achievement } from '../models/achievement.model';
import { CloseDayResult, RewardEvent, RewardItem, TaskCompletion } from '../models/gamification.model';
import { GamificationService } from './gamification.service';
import { ProfileService } from './profile.service';

@Injectable({
  providedIn: 'root',
})
export class GamificationCelebrationService {
  private readonly gamification = inject(GamificationService);
  private readonly profileService = inject(ProfileService);
  private sequence = 0;

  readonly xpTrigger = signal<{ xp: number; tick: number } | null>(null);
  readonly levelUpTrigger = signal<{ level: number; tick: number } | null>(null);
  readonly achievementTrigger = signal<{ achievement: Achievement; tick: number } | null>(null);
  readonly lootTrigger = signal<{ items: RewardItem[]; tick: number } | null>(null);

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

    result.lootBoxesEarned.forEach((lootBoxId, index) => {
      setTimeout(() => this.openLootBox(lootBoxId), (result.achievementsUnlocked.length + index) * 1200);
    });
  }

  checkPendingLootBoxes(): void {
    this.gamification.getPendingLootBoxes().subscribe({
      next: (boxes) => {
        const pending = boxes.find((b) => b.status === 'PENDING');
        if (pending) {
          this.openLootBox(pending.id);
        }
      },
      error: (err) => console.error('Failed to load loot boxes', err),
    });
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
      setTimeout(
        () => this.openLootBox(reward.lootBoxEarned!),
        800 + reward.achievementsUnlocked.length * 1200
      );
    }
  }

  private openLootBox(id: string): void {
    this.gamification.openLootBox(id).subscribe({
      next: (box) => {
        if (box.contents.length > 0) {
          this.lootTrigger.set({ items: box.contents, tick: ++this.sequence });
        }
      },
      error: (err) => console.error('Failed to open loot box', err),
    });
  }
}
