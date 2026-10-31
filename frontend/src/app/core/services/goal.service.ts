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
    return this.api.get<Goal[]>('/goals', params);
  }

  getGoal(id: string): Observable<Goal> {
    return this.api.get<Goal>(`/goals/${id}`);
  }

  getSummary(): Observable<GoalSummary> {
    return this.api.get<GoalSummary>('/goals/summary');
  }

  createGoal(goal: GoalCreateRequest): Observable<Goal> {
    return this.api.post<Goal>('/goals', goal);
  }

  updateGoal(id: string, goal: GoalUpdateRequest): Observable<Goal> {
    return this.api.put<Goal>(`/goals/${id}`, goal);
  }

  deleteGoal(id: string): Observable<void> {
    return this.api.delete<void>(`/goals/${id}`);
  }

  strike(id: string): Observable<BossDamage> {
    return this.api.post<BossDamage>(`/goals/${id}/strike`, {});
  }

  getLinkedTasks(goalId: string): Observable<Task[]> {
    return this.api.get<Task[]>(`/goals/${goalId}/tasks`);
  }

  attachTask(goalId: string, taskId: string): Observable<Task> {
    return this.api.post<Task>(`/goals/${goalId}/tasks/${taskId}`, {});
  }

  detachTask(goalId: string, taskId: string): Observable<Task> {
    return this.api.delete<Task>(`/goals/${goalId}/tasks/${taskId}`);
  }

  listSubscriptions(goalId: string): Observable<GoalSubscription[]> {
    return this.api.get<GoalSubscription[]>(`/goals/${goalId}/subscriptions`);
  }

  subscribeCategory(goalId: string, categoryId: string): Observable<GoalSubscription> {
    return this.api.post<GoalSubscription>(`/goals/${goalId}/subscriptions`, { categoryId });
  }

  unsubscribeCategory(goalId: string, categoryId: string): Observable<void> {
    return this.api.delete<void>(`/goals/${goalId}/subscriptions/${categoryId}`);
  }
}
