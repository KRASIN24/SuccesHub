import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import {
  CloseDayResult,
  DailyStatus,
  Forecast,
  InventoryItem,
  LootBox,
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

  getPendingLootBoxes(): Observable<LootBox[]> {
    return this.api.get<LootBox[]>('/gamification/loot-boxes');
  }

  openLootBox(id: string): Observable<LootBox> {
    return this.api.post<LootBox>(`/gamification/loot-boxes/${id}/open`, {});
  }

  getInventory(): Observable<InventoryItem[]> {
    return this.api.get<InventoryItem[]>('/gamification/inventory');
  }
}
