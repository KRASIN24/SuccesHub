import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { TaskService } from '../../core/services/task.service';
import { Task, TaskCategory } from '../../core/models/task.model';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';

interface QuestColumn {
  id: string;
  name: string;
  tag: string;
  grantXp: boolean;
  muted?: boolean;
  tasks: Task[];
}

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [CommonModule, FormsModule, LoadingSkeletonComponent, ErrorStateComponent],
  templateUrl: './tasks.component.html',
  styleUrl: './tasks.component.scss',
})
export class TasksComponent implements OnInit {
  private readonly taskService = inject(TaskService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly columns = signal<QuestColumn[]>([]);

  // Task form state
  readonly activeAddTaskId = signal<string | null>(null);
  newTaskTitle = '';
  newTaskDesc = '';
  newTaskXp = 100;

  // Category form state
  readonly showNewCategoryForm = signal(false);
  newCategoryName = '';
  newCategoryTag = '';
  newCategoryGrantXp = true;

  ngOnInit(): void {
    this.loadTasksData();
  }

  loadTasksData(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      categories: this.taskService.getCategories(),
      tasks: this.taskService.getTasks()
    }).subscribe({
      next: (data) => {
        const mappedColumns: QuestColumn[] = data.categories.map((cat) => {
          return {
            id: cat.id,
            name: cat.name,
            tag: cat.tag,
            grantXp: cat.grantXp,
            muted: cat.muted,
            tasks: data.tasks.filter((task) => task.categoryId === cat.id)
          };
        });
        this.columns.set(mappedColumns);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load tasks data', err);
        this.error.set('Could not synchronize task strategy canvas.');
        this.loading.set(false);
      }
    });
  }

  toggleGrantXp(column: QuestColumn): void {
    const updatedGrantXp = !column.grantXp;
    
    this.taskService.updateCategory(column.id, {
      name: column.name,
      tag: column.tag,
      grantXp: updatedGrantXp,
      muted: column.muted,
      sortOrder: 0
    }).subscribe({
      next: () => {
        column.grantXp = updatedGrantXp;
      },
      error: (err) => {
        console.error('Failed to toggle category grantXp', err);
      }
    });
  }

  toggleTask(task: Task): void {
    const newStatus = task.status === 'DONE' ? 'TODO' : 'DONE';
    if (newStatus === 'DONE') {
      this.taskService.completeTask(task.id).subscribe({
        next: () => {
          this.loadTasksData();
        },
        error: (err) => {
          console.error('Failed to complete task', err);
        }
      });
    } else {
      this.taskService.updateTask(task.id, {
        title: task.title,
        description: task.description,
        xpReward: task.xpReward,
        status: 'TODO',
        categoryId: task.categoryId
      }).subscribe({
        next: () => {
          this.loadTasksData();
        },
        error: (err) => {
          console.error('Failed to reset task', err);
        }
      });
    }
  }

  openAddTaskForm(categoryId: string): void {
    this.activeAddTaskId.set(categoryId);
    this.newTaskTitle = '';
    this.newTaskDesc = '';
    this.newTaskXp = 100;
  }

  cancelAddTask(): void {
    this.activeAddTaskId.set(null);
  }

  saveTask(categoryId: string): void {
    if (!this.newTaskTitle || !this.newTaskTitle.trim()) return;

    this.taskService.createTask({
      title: this.newTaskTitle.trim(),
      description: this.newTaskDesc.trim(),
      xpReward: this.newTaskXp,
      categoryId: categoryId,
      status: 'TODO'
    }).subscribe({
      next: () => {
        this.activeAddTaskId.set(null);
        this.loadTasksData();
      },
      error: (err) => {
        console.error('Failed to create task', err);
      }
    });
  }

  saveCategory(): void {
    if (!this.newCategoryName || !this.newCategoryName.trim()) return;

    this.taskService.createCategory({
      name: this.newCategoryName.trim(),
      tag: this.newCategoryTag.trim(),
      grantXp: this.newCategoryGrantXp,
      muted: false,
      sortOrder: this.columns().length
    }).subscribe({
      next: () => {
        this.showNewCategoryForm.set(false);
        this.newCategoryName = '';
        this.newCategoryTag = '';
        this.newCategoryGrantXp = true;
        this.loadTasksData();
      },
      error: (err) => {
        console.error('Failed to create category', err);
      }
    });
  }
}
