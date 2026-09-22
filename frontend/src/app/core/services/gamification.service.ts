import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import {
  BoxType,
  CloseDayResult,
  DailyStatus,
  DevUnsealResult,
  Forecast,
  GamificationClientConfig,
  InventoryItem,
  LootBox,
  StreakActionResult,
  StreakCalendar,
  UseItemResult,
  WeeklyInsight,
  XpPreview,
} from '../models/gamification.model';

@Injectable({
  providedIn: 'root',
})
export class GamificationService {
  private readonly api = inject(ApiService);

  previewXp(
    difficulty: number,
    durationMinutes: number,
    priority: number,
    weeklyChallenge = false
  ): Observable<XpPreview> {
    return this.api.get<XpPreview>('/gamification/xp-preview', {
      difficulty,
      durationMinutes,
      priority,
      weeklyChallenge,
    });
  }

  getDailyStatus(): Observable<DailyStatus> {
    return this.api.get<DailyStatus>('/gamification/daily');
  }

  closeDay(): Observable<CloseDayResult> {
    return this.api.post<CloseDayResult>('/gamification/close-day', {});
  }

  getForecast(): Observable<Forecast> {
    return this.api.get<Forecast>('/gamification/forecast');
  }

  getWeeklyInsights(): Observable<WeeklyInsight> {
    return this.api.get<WeeklyInsight>('/gamification/insights/weekly');
  }

  getClientConfig(): Observable<GamificationClientConfig> {
    return this.api.get<GamificationClientConfig>('/gamification/config');
  }

  getBoxTypes(): Observable<BoxType[]> {
    return this.api.get<BoxType[]>('/gamification/loot-boxes/types');
  }

  getPendingLootBoxes(): Observable<LootBox[]> {
    return this.api.get<LootBox[]>('/gamification/loot-boxes');
  }

  getLootBoxHistory(limit = 3): Observable<LootBox[]> {
    return this.api.get<LootBox[]>('/gamification/loot-boxes/history', { limit });
  }

  grantLootBox(boxType: string): Observable<LootBox> {
    return this.api.post<LootBox>('/gamification/loot-boxes/grant', { boxType });
  }

  openLootBox(id: string): Observable<LootBox> {
    return this.api.post<LootBox>(`/gamification/loot-boxes/${id}/open`, {});
  }

  getInventory(): Observable<InventoryItem[]> {
    return this.api.get<InventoryItem[]>('/gamification/inventory');
  }

  equipInventoryItem(id: string): Observable<InventoryItem> {
    return this.api.post<InventoryItem>(`/gamification/inventory/${id}/equip`, {});
  }

  /** Consumes a utility card (banks a shield or grants XP from a boost). */
  useInventoryItem(id: string): Observable<UseItemResult> {
    return this.api.post<UseItemResult>(`/gamification/inventory/${id}/use`, {});
  }

  /** Fetches the streak activity calendar for a month (yyyy-MM); defaults to current month. */
  getStreakCalendar(month?: string): Observable<StreakCalendar> {
    return this.api.get<StreakCalendar>(
      '/gamification/streak/calendar',
      month ? { month } : undefined
    );
  }

  adjustStreak(delta: number): Observable<StreakActionResult> {
    return this.api.post<StreakActionResult>('/gamification/streak/adjust', { delta });
  }

  setStreak(value: number): Observable<StreakActionResult> {
    return this.api.post<StreakActionResult>('/gamification/streak/set', { value });
  }

  resetStreak(): Observable<StreakActionResult> {
    return this.api.post<StreakActionResult>('/gamification/streak/reset', {});
  }

  /** Spends a streak shield to protect a specific missed day (yyyy-MM-dd). */
  shieldDay(date: string): Observable<StreakActionResult> {
    return this.api.post<StreakActionResult>('/gamification/streak/shield', { date });
  }

  /** Sets or clears the virtual day offset (dev tools). */
  setDevClock(body: { dayOffset?: number; clear?: boolean }): Observable<GamificationClientConfig> {
    return this.api.post<GamificationClientConfig>('/gamification/dev/clock', body);
  }

  /** Forces lazy close of pending days through yesterday (dev tools). */
  forceClosePending(): Observable<CloseDayResult> {
    return this.api.post<CloseDayResult>('/gamification/dev/close-pending', {});
  }

  /** Clears today's celebrate seal (dev tools). */
  unsealToday(): Observable<DevUnsealResult> {
    return this.api.post<DevUnsealResult>('/gamification/dev/unseal', {});
  }
}
