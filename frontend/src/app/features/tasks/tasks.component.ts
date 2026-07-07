import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { TaskService } from '../../core/services/task.service';
import { GoalService } from '../../core/services/goal.service';
import { GamificationService } from '../../core/services/gamification.service';
import { GamificationCelebrationService } from '../../core/services/gamification-celebration.service';
import { Task, TaskCategory } from '../../core/models/task.model';
import { Goal } from '../../core/models/goal.model';
import { XpPreview } from '../../core/models/gamification.model';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';

interface QuestColumn {
  id: string;
  name: string;
  tag: string;
  grantXp: boolean;
  sortOrder: number;
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
  private readonly goalService = inject(GoalService);
  private readonly gamificationService = inject(GamificationService);
  private readonly celebrations = inject(GamificationCelebrationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly saveError = signal<string | null>(null);
  readonly columns = signal<QuestColumn[]>([]);
  readonly goals = signal<Goal[]>([]);
  readonly xpPreview = signal<XpPreview | null>(null);

  readonly activeAddTaskId = signal<string | null>(null);
  newTaskTitle = '';
  newTaskDesc = '';
  newTaskDifficulty = 3;
  newTaskDuration = 30;
  newTaskPriority = 2;
  newTaskGoalId: string | null = null;

  readonly showNewCategoryForm = signal(false);
  readonly editingCategoryId = signal<string | null>(null);
  readonly categorySaveError = signal<string | null>(null);
  newCategoryName = '';
  newCategoryTag = '';
  newCategoryGrantXp = true;

  editCategoryName = '';
  editCategoryTag = '';
  editCategoryGrantXp = true;
  private editCategorySortOrder = 0;

  ngOnInit(): void {
    this.loadTasksData();
  }

  loadTasksData(silent = false): void {
    if (!silent) {
      this.loading.set(true);
    }
    this.error.set(null);

    forkJoin({
      categories: this.taskService.getCategories(),
      tasks: this.taskService.getTasks(),
      goals: this.goalService.getGoals('ACTIVE'),
    }).subscribe({
      next: (data) => {
        this.goals.set(data.goals);
        this.columns.set(
          data.categories.map((cat) => ({
            id: cat.id,
            name: cat.name,
            tag: cat.tag,
            grantXp: cat.grantXp,
            sortOrder: cat.sortOrder,
            tasks: data.tasks.filter((task) => task.categoryId === cat.id),
          }))
        );
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load tasks data', err);
        this.error.set('Could not synchronize task strategy canvas.');
        this.loading.set(false);
      },
    });
  }

  refreshXpPreview(): void {
    this.gamificationService
      .previewXp(this.newTaskDifficulty, this.newTaskDuration, this.newTaskPriority, false)
      .subscribe({
        next: (preview) => this.xpPreview.set(preview),
        error: () => this.xpPreview.set(null),
      });
  }

  toggleGrantXp(column: QuestColumn): void {
    const updatedGrantXp = !column.grantXp;
    this.taskService
      .updateCategory(column.id, this.categoryPayload(column, { grantXp: updatedGrantXp }))
      .subscribe({
        next: () => {
          column.grantXp = updatedGrantXp;
        },
        error: (err) => console.error('Failed to toggle category grantXp', err),
      });
  }

  openEditCategoryForm(column: QuestColumn): void {
    this.editingCategoryId.set(column.id);
    this.activeAddTaskId.set(null);
    this.categorySaveError.set(null);
    this.editCategoryName = column.name;
    this.editCategoryTag = column.tag;
    this.editCategoryGrantXp = column.grantXp;
    this.editCategorySortOrder = column.sortOrder;
  }

  cancelEditCategory(): void {
    this.editingCategoryId.set(null);
    this.categorySaveError.set(null);
  }

  saveCategoryEdit(columnId: string): void {
    if (!this.editCategoryName?.trim()) return;

    this.categorySaveError.set(null);
    this.taskService
      .updateCategory(columnId, {
        name: this.editCategoryName.trim(),
        tag: this.editCategoryTag.trim(),
        grantXp: this.editCategoryGrantXp,
        sortOrder: this.editCategorySortOrder,
      })
      .subscribe({
        next: () => {
          this.editingCategoryId.set(null);
          this.loadTasksData(true);
        },
        error: (err) => {
          console.error('Failed to update category', err);
          this.categorySaveError.set(
            err?.error?.message ?? 'Could not update category. Check you are logged in and try again.'
          );
        },
      });
  }

  private categoryPayload(
    column: QuestColumn,
    overrides: Partial<Pick<TaskCategory, 'name' | 'tag' | 'grantXp' | 'sortOrder'>> = {}
  ): Partial<TaskCategory> {
    return {
      name: column.name,
      tag: column.tag,
      grantXp: column.grantXp,
      sortOrder: column.sortOrder,
      ...overrides,
    };
  }

  toggleTask(task: Task, column: QuestColumn): void {
    if (task.status === 'DONE') {
      this.reopenTask(task);
      return;
    }
    if (!column.grantXp) {
      this.taskService
        .updateTask(task.id, {
          ...this.taskUpdatePayload(task),
          status: 'DONE',
        })
        .subscribe({
          next: () => this.loadTasksData(true),
          error: (err) => console.error('Failed to complete task', err),
        });
      return;
    }

    this.taskService.completeTask(task.id).subscribe({
      next: (completion) => {
        this.celebrations.handleTaskCompletion(completion);
        this.loadTasksData(true);
      },
      error: (err) => console.error('Failed to complete task', err),
    });
  }

  private reopenTask(task: Task): void {
    this.taskService
      .updateTask(task.id, {
        ...this.taskUpdatePayload(task),
        status: 'TODO',
      })
      .subscribe({
        next: () => this.loadTasksData(true),
        error: (err) => console.error('Failed to reopen task', err),
      });
  }

  private taskUpdatePayload(task: Task): Partial<Task> {
    return {
      title: task.title,
      description: task.description,
      xpReward: task.xpReward,
      categoryId: task.categoryId,
      difficulty: task.difficulty,
      durationMinutes: task.durationMinutes,
      priority: task.priority,
      goalId: task.goalId,
    };
  }

  scheduleTask(task: Task): void {
    this.taskService.scheduleTasks([task.id]).subscribe({
      next: () => this.loadTasksData(true),
      error: (err) => console.error('Failed to schedule task', err),
    });
  }

  openAddTaskForm(categoryId: string): void {
    this.activeAddTaskId.set(categoryId);
    this.editingCategoryId.set(null);
    this.newTaskTitle = '';
    this.newTaskDesc = '';
    this.newTaskDifficulty = 3;
    this.newTaskDuration = 30;
    this.newTaskPriority = 2;
    this.newTaskGoalId = null;
    this.saveError.set(null);
    this.refreshXpPreview();
  }

  cancelAddTask(): void {
    this.activeAddTaskId.set(null);
    this.xpPreview.set(null);
  }

  saveTask(categoryId: string): void {
    if (!this.newTaskTitle?.trim()) return;

    this.saveError.set(null);
    this.taskService
      .createTask({
        categoryId,
        title: this.newTaskTitle.trim(),
        description: this.newTaskDesc.trim(),
        xpReward: this.xpPreview()?.estimatedTotalXp ?? 0,
        difficulty: Number(this.newTaskDifficulty) || 3,
        durationMinutes: Number(this.newTaskDuration) || 30,
        priority: Number(this.newTaskPriority) || 2,
        goalId: this.newTaskGoalId,
      })
      .subscribe({
        next: () => {
          this.activeAddTaskId.set(null);
          this.xpPreview.set(null);
          this.loadTasksData(true);
        },
        error: (err) => {
          console.error('Failed to create task', err);
          const message =
            err?.error?.message ??
            err?.error?.fieldErrors?.title ??
            'Could not create task. Check you are logged in and try again.';
          this.saveError.set(message);
        },
      });
  }

  saveCategory(): void {
    if (!this.newCategoryName?.trim()) return;

    this.taskService
      .createCategory({
        name: this.newCategoryName.trim(),
        tag: this.newCategoryTag.trim(),
        grantXp: this.newCategoryGrantXp,
        sortOrder: this.columns().length,
      })
      .subscribe({
        next: () => {
          this.showNewCategoryForm.set(false);
          this.newCategoryName = '';
          this.newCategoryTag = '';
          this.newCategoryGrantXp = true;
          this.loadTasksData();
        },
        error: (err) => console.error('Failed to create category', err),
      });
  }
}
