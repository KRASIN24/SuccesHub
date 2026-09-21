import { Injectable, computed, inject, signal } from '@angular/core';
import { GamificationService } from './gamification.service';
import { InventoryItem, RewardItem } from '../models/gamification.model';

/**
 * Shared equipped TITLE / FRAME cosmetics for dashboard hero and navbar chrome.
 */
@Injectable({
  providedIn: 'root',
})
export class EquippedCosmeticsService {
  private readonly gamification = inject(GamificationService);
  private readonly inventory = signal<InventoryItem[]>([]);

  readonly equippedTitle = computed(() => this.findEquipped('TITLE'));
  readonly equippedFrame = computed(() => this.findEquipped('FRAME'));

  refresh(): void {
    this.gamification.getInventory().subscribe({
      next: (items) => this.inventory.set(items),
      error: (err) => console.error('Failed to load equipped cosmetics', err),
    });
  }

  applyInventory(items: InventoryItem[]): void {
    this.inventory.set(items);
  }

  private findEquipped(type: string): RewardItem | null {
    const item = this.inventory().find(
      (i) => i.equipped && (i.reward.type === type || (type === 'FRAME' && i.reward.type === 'AVATAR_FRAME'))
    );
    return item?.reward ?? null;
  }
}
