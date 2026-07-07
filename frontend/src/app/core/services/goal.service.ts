import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import { Goal, GoalCreateRequest, GoalSummary } from '../models/goal.model';

@Injectable({
  providedIn: 'root',
})
export class GoalService {
  private readonly api = inject(ApiService);

  getGoals(status?: 'ACTIVE' | 'COMPLETED'): Observable<Goal[]> {
    const params = status ? { status } : undefined;
    return this.api.get<Goal[]>('/goals', params);
  }

  getSummary(): Observable<GoalSummary> {
    return this.api.get<GoalSummary>('/goals/summary');
  }

  createGoal(goal: GoalCreateRequest): Observable<Goal> {
    return this.api.post<Goal>('/goals', goal);
  }

  updateGoal(id: string, goal: Partial<Goal>): Observable<Goal> {
    return this.api.put<Goal>(`/goals/${id}`, goal);
  }

  deleteGoal(id: string): Observable<void> {
    return this.api.delete<void>(`/goals/${id}`);
  }
}
