import { Injectable, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { GamificationService } from './gamification.service';
import { BoxType } from '../models/gamification.model';

/** Tool ids for the hybrid messenger bubble system. */
export type DevToolId = 'streak' | 'cache' | 'time' | 'ritual';

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
      },
      error: () => {
        this.enabled.set(false);
        this.openTool.set(null);
      },
    });
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

  /** Notify pages that gamification state changed (streak / clock / ritual / loot). */
  notifyDataChanged(): void {
    this.dataRevision.update((n) => n + 1);
  }

  applyStreakResult(currentStreak: number): void {
    this.liveStreak.set(currentStreak);
    this.streakSetValue.set(currentStreak);
  }
}
