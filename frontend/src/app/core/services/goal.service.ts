import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import {
  Goal,
  GoalCreateRequest,
  GoalSubscription,
  GoalSummary,
  GoalUpdateRequest,
} from '../models/goal.model';
import { BossDamage } from '../models/gamification.model';
import { Task } from '../models/task.model';

@Injectable({
  providedIn: 'root',
})
export class GoalService {
  private readonly api = inject(ApiService);

  getGoals(status?: 'ACTIVE' | 'COMPLETED'): Observable<Goal[]> {
    const params = status ? { status } : undefined;
    return this.api.get<Goal[]>('/campaigns', params);
  }

  getGoal(id: string): Observable<Goal> {
    return this.api.get<Goal>(`/campaigns/${id}`);
  }

  getSummary(): Observable<GoalSummary> {
    return this.api.get<GoalSummary>('/campaigns/summary');
  }

  createGoal(goal: GoalCreateRequest): Observable<Goal> {
    return this.api.post<Goal>('/campaigns', goal);
  }

  updateGoal(id: string, goal: GoalUpdateRequest): Observable<Goal> {
    return this.api.put<Goal>(`/campaigns/${id}`, goal);
  }

  deleteGoal(id: string): Observable<void> {
    return this.api.delete<void>(`/campaigns/${id}`);
  }

  strike(id: string): Observable<BossDamage> {
    return this.api.post<BossDamage>(`/campaigns/${id}/strike`, {});
  }

  getLinkedTasks(goalId: string): Observable<Task[]> {
    return this.api.get<Task[]>(`/campaigns/${goalId}/tasks`);
  }

  attachTask(goalId: string, taskId: string): Observable<Task> {
    return this.api.post<Task>(`/campaigns/${goalId}/tasks/${taskId}`, {});
  }

  detachTask(goalId: string, taskId: string): Observable<Task> {
    return this.api.delete<Task>(`/campaigns/${goalId}/tasks/${taskId}`);
  }

  listSubscriptions(goalId: string): Observable<GoalSubscription[]> {
    return this.api.get<GoalSubscription[]>(`/campaigns/${goalId}/subscriptions`);
  }

  subscribeCategory(goalId: string, categoryId: string): Observable<GoalSubscription> {
    return this.api.post<GoalSubscription>(`/campaigns/${goalId}/subscriptions`, { categoryId });
  }

  unsubscribeCategory(goalId: string, categoryId: string): Observable<void> {
    return this.api.delete<void>(`/campaigns/${goalId}/subscriptions/${categoryId}`);
  }
}
