import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import { Achievement } from '../models/achievement.model';

@Injectable({
  providedIn: 'root',
})
export class AchievementService {
  private readonly api = inject(ApiService);

  getAchievements(): Observable<Achievement[]> {
    return this.api.get<Achievement[]>('/achievements');
  }
}
