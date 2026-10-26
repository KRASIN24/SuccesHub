import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { ProfileService } from './profile.service';
import { Observable, tap } from 'rxjs';
import { TaskCompletion } from '../models/gamification.model';
import {
  Task,
  TaskCategory,
  TaskCreateRequest,
  TaskReorderRequest,
} from '../models/task.model';

@Injectable({
  providedIn: 'root',
})
export class TaskService {
  private readonly api = inject(ApiService);
  private readonly profileService = inject(ProfileService);

  getTasks(status?: string): Observable<Task[]> {
    const params = status ? { status } : undefined;
    return this.api.get<Task[]>('/tasks', params);
  }

  createTask(payload: TaskCreateRequest): Observable<Task> {
    const body: Record<string, unknown> = {
      categoryId: payload.categoryId,
      title: payload.title,
      description: payload.description ?? '',
      xpReward: payload.xpReward ?? 0,
      difficulty: payload.difficulty ?? 3,
      durationMinutes: payload.durationMinutes ?? 30,
      priority: payload.priority ?? 2,
      starred: payload.starred ?? false,
    };
    const goalId = payload.goalId?.trim();
    if (goalId) {
      body['goalId'] = goalId;
    }
    if (payload.parentTaskId) {
      body['parentTaskId'] = payload.parentTaskId;
    }
    if (payload.sortOrder != null) {
      body['sortOrder'] = payload.sortOrder;
    }
    if (payload.dueDate) {
      body['dueDate'] = payload.dueDate;
    }
    if (payload.reminderAt) {
      body['reminderAt'] = payload.reminderAt;
    }
    return this.api.post<Task>('/tasks', body);
  }

  updateTask(
    id: string,
    task: Partial<Task> & {
      clearParent?: boolean;
      clearReminder?: boolean;
      dueDate?: string | null;
      reminderAt?: string | null;
      parentTaskId?: string | null;
    }
  ): Observable<Task> {
    const body: Record<string, unknown> = {
      categoryId: task.categoryId,
      title: task.title,
      description: task.description ?? '',
      xpReward: task.xpReward ?? 0,
      difficulty: task.difficulty ?? 3,
      durationMinutes: task.durationMinutes ?? 30,
      priority: task.priority ?? 2,
      status: task.status,
      starred: task.starred ?? false,
      sortOrder: task.sortOrder ?? 0,
      dueDate: task.dueDate ?? null,
    };
    const goalId = task.goalId?.trim();
    if (goalId) {
      body['goalId'] = goalId;
    }
    if (task.clearParent) {
      body['clearParent'] = true;
    } else if (task.parentTaskId) {
      body['parentTaskId'] = task.parentTaskId;
    }
    if (task.clearReminder) {
      body['clearReminder'] = true;
    } else if (task.reminderAt) {
      body['reminderAt'] = task.reminderAt;
    } else if (task.reminderAt === null) {
      body['clearReminder'] = true;
    }
    return this.api.put<Task>(`/tasks/${id}`, body);
  }

  completeTask(id: string): Observable<TaskCompletion> {
    return this.api.patch<TaskCompletion>(`/tasks/${id}/complete`, {}).pipe(
      tap((completion) => this.syncProfileFromCompletion(completion))
    );
  }

  scheduleTasks(taskIds: string[]): Observable<void> {
    return this.api.patch<void>('/tasks/schedule', { taskIds });
  }

  starTask(id: string, starred: boolean): Observable<Task> {
    return this.api.patch<Task>(`/tasks/${id}/star`, { starred });
  }

  reorderTasks(payload: TaskReorderRequest): Observable<void> {
    return this.api.patch<void>('/tasks/reorder', payload);
  }

  deleteTask(id: string): Observable<void> {
    return this.api.delete<void>(`/tasks/${id}`);
  }

  getCategories(): Observable<TaskCategory[]> {
    return this.api.get<TaskCategory[]>('/task-categories');
  }

  createCategory(category: Partial<TaskCategory>): Observable<TaskCategory> {
    return this.api.post<TaskCategory>('/task-categories', category);
  }

  updateCategory(id: string, category: Partial<TaskCategory>): Observable<TaskCategory> {
    return this.api.put<TaskCategory>(`/task-categories/${id}`, category);
  }

  deleteCategory(id: string): Observable<void> {
    return this.api.delete<void>(`/task-categories/${id}`);
  }

  reorderCategories(orderedIds: string[]): Observable<void> {
    return this.api.patch<void>('/task-categories/reorder', { orderedIds });
  }

  private syncProfileFromCompletion(completion: TaskCompletion | null | undefined): void {
    if (completion?.updatedProfile) {
      this.profileService.applyProfile(completion.updatedProfile);
    }
  }
}
