import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import {
  BoxType,
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

  getBoxTypes(): Observable<BoxType[]> {
    return this.api.get<BoxType[]>('/gamification/loot-boxes/types');
  }

  getPendingLootBoxes(): Observable<LootBox[]> {
    return this.api.get<LootBox[]>('/gamification/loot-boxes');
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
}
