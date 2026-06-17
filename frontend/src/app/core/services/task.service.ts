import { Injectable, inject } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import { Task, TaskCategory } from '../models/task.model';

@Injectable({
  providedIn: 'root',
})
export class TaskService {
  private readonly api = inject(ApiService);

  getTasks(status?: string): Observable<Task[]> {
    const params = status ? { status } : undefined;
    return this.api.get<Task[]>('/tasks', params);
  }

  createTask(task: Partial<Task>): Observable<Task> {
    return this.api.post<Task>('/tasks', task);
  }

  updateTask(id: string, task: Partial<Task>): Observable<Task> {
    return this.api.put<Task>(`/tasks/${id}`, task);
  }

  completeTask(id: string): Observable<Task> {
    return this.api.patch<Task>(`/tasks/${id}/complete`, {});
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
}
