import { Injectable, computed, inject, signal } from '@angular/core';
import { GamificationService } from './gamification.service';
import { LootBox } from '../models/gamification.model';

/**
 * Shared pending-cache state for sidebar badge and loot page counts.
 */
@Injectable({
  providedIn: 'root',
})
export class LootPendingService {
  private readonly gamification = inject(GamificationService);
  private refreshTimer: ReturnType<typeof setTimeout> | null = null;

  readonly pendingBoxes = signal<LootBox[]>([]);
  private readonly optimisticExtra = signal(0);

  readonly totalPendingCount = computed(
    () => this.pendingBoxes().length + this.optimisticExtra()
  );

  pendingCountFor(boxTypeId: string): number {
    return this.pendingBoxes().filter((b) => b.boxType === boxTypeId).length;
  }

  refresh(): void {
    this.gamification.getPendingLootBoxes().subscribe({
      next: (pending) => {
        this.pendingBoxes.set(pending);
        this.optimisticExtra.set(0);
      },
      error: (err) => console.error('Failed to refresh pending loot boxes', err),
    });
  }

  /** Bump counts immediately after earn, then reconcile with the server. */
  notifyEarned(count: number): void {
    if (count <= 0) return;
    this.optimisticExtra.update((n) => n + count);
    this.scheduleRefresh();
  }

  /** Remove an opened box from local state before Collect refetches. */
  removeOpened(lootBoxId: string): void {
    this.pendingBoxes.update((list) => list.filter((b) => b.id !== lootBoxId));
  }

  private scheduleRefresh(): void {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
    }
    this.refreshTimer = setTimeout(() => {
      this.refreshTimer = null;
      this.refresh();
    }, 400);
  }
}
