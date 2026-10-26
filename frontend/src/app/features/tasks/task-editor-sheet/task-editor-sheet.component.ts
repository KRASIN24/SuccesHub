import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TaskService } from '../../../core/services/task.service';
import { GamificationService } from '../../../core/services/gamification.service';
import { Goal } from '../../../core/models/goal.model';
import { TASK_DURATION_PRESETS, Task } from '../../../core/models/task.model';
import { XpPreview } from '../../../core/models/gamification.model';
import { GlitchButtonComponent } from '../../../shared/components/glitch-button/glitch-button.component';
import { GlitchCheckboxComponent } from '../../../shared/components/glitch-checkbox/glitch-checkbox.component';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { TranslateService } from '../../../core/i18n/translate.service';

export type TaskEditorMode = 'create' | 'edit';

export interface EditorCategoryOption {
  id: string;
  name: string;
}

@Component({
  selector: 'app-task-editor-sheet',
  standalone: true,
  imports: [CommonModule, FormsModule, GlitchButtonComponent, GlitchCheckboxComponent, TranslatePipe],
  templateUrl: './task-editor-sheet.component.html',
  styleUrl: './task-editor-sheet.component.scss',
})
export class TaskEditorSheetComponent implements OnChanges {
  protected readonly i18n = inject(TranslateService);
  private readonly taskService = inject(TaskService);
  private readonly gamificationService = inject(GamificationService);

  readonly durationPresets = TASK_DURATION_PRESETS;

  @Input({ required: true }) open = false;
  @Input({ required: true }) mode: TaskEditorMode = 'create';
  @Input() task: Task | null = null;
  @Input() categoryId: string | null = null;
  @Input() categories: EditorCategoryOption[] = [];
  @Input() parentCandidates: Task[] = [];
  @Input() goals: Goal[] = [];
  @Input() subtasks: Task[] = [];

  @Output() readonly closed = new EventEmitter<void>();
  @Output() readonly saved = new EventEmitter<void>();
  @Output() readonly deleted = new EventEmitter<void>();
  @Output() readonly subtaskAdded = new EventEmitter<void>();
  @Output() readonly openSubtask = new EventEmitter<Task>();
  @Output() readonly toggleComplete = new EventEmitter<Task>();
  @Output() readonly toggleStar = new EventEmitter<Task>();

  title = '';
  description = '';
  difficulty = 3;
  durationMinutes = 30;
  durationCustom = false;
  priority = 2;
  goalId: string | null = null;
  selectedCategoryId: string | null = null;
  selectedParentId: string | null = null;
  starred = false;
  dueDate = '';
  reminderLocal = '';
  readonly saving = signal(false);
  readonly saveError = signal<string | null>(null);
  readonly xpPreview = signal<XpPreview | null>(null);
  readonly newSubtaskTitle = signal('');
  readonly addingSubtask = signal(false);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] || changes['task'] || changes['mode'] || changes['categoryId']) {
      this.hydrate();
    }
  }

  private hydrate(): void {
    this.saveError.set(null);
    this.newSubtaskTitle.set('');
    this.selectedCategoryId = this.categoryId;
    if (this.mode === 'edit' && this.task) {
      this.title = this.task.title;
      this.description = this.task.description ?? '';
      this.difficulty = this.task.difficulty;
      this.durationMinutes = this.task.durationMinutes;
      this.durationCustom = !TASK_DURATION_PRESETS.includes(
        this.task.durationMinutes as (typeof TASK_DURATION_PRESETS)[number]
      );
      this.priority = this.task.priority;
      this.goalId = this.task.goalId ?? null;
      this.starred = !!this.task.starred;
      this.selectedParentId = this.task.parentTaskId ?? null;
      this.dueDate = this.task.dueDate ?? '';
      this.reminderLocal = this.toLocalInput(this.task.reminderAt);
    } else {
      this.title = '';
      this.description = '';
      this.difficulty = 3;
      this.durationMinutes = 30;
      this.durationCustom = false;
      this.priority = 2;
      this.goalId = null;
      this.starred = false;
      this.selectedParentId = null;
      this.dueDate = '';
      this.reminderLocal = '';
    }
    this.refreshXpPreview();
  }

  private toLocalInput(iso: string | null | undefined): string {
    if (!iso) return '';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '';
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  private fromLocalInput(local: string): string | null {
    if (!local?.trim()) return null;
    const d = new Date(local);
    if (Number.isNaN(d.getTime())) return null;
    return d.toISOString();
  }

  setDurationPreset(minutes: number): void {
    this.durationCustom = false;
    this.durationMinutes = minutes;
    this.refreshXpPreview();
  }

  enableCustomDuration(): void {
    this.durationCustom = true;
  }

  filteredParentCandidates(): Task[] {
    const catId = this.selectedCategoryId;
    return this.parentCandidates.filter((t) => {
      if (catId && t.categoryId !== catId) return false;
      if (this.task && t.id === this.task.id) return false;
      return true;
    });
  }

  onCategoryChange(): void {
    // Parent must live in the selected category
    if (this.selectedParentId) {
      const parent = this.parentCandidates.find((p) => p.id === this.selectedParentId);
      if (parent && parent.categoryId !== this.selectedCategoryId) {
        this.selectedParentId = null;
      }
    }
  }

  refreshXpPreview(): void {
    this.gamificationService
      .previewXp(this.difficulty, this.durationMinutes, this.priority, false)
      .subscribe({
        next: (preview) => this.xpPreview.set(preview),
        error: () => this.xpPreview.set(null),
      });
  }

  onBackdrop(): void {
    this.closed.emit();
  }

  onCancel(): void {
    this.closed.emit();
  }

  onStarClick(): void {
    this.starred = !this.starred;
    if (this.mode === 'edit' && this.task) {
      this.toggleStar.emit({ ...this.task, starred: this.starred });
    }
  }

  onSave(): void {
    if (!this.title.trim() || !this.selectedCategoryId) return;
    this.saving.set(true);
    this.saveError.set(null);

    const reminderAt = this.fromLocalInput(this.reminderLocal);
    const payloadBase = {
      categoryId: this.selectedCategoryId,
      title: this.title.trim(),
      description: this.description.trim(),
      difficulty: Number(this.difficulty) || 3,
      durationMinutes: Number(this.durationMinutes) || 30,
      priority: Number(this.priority) || 2,
      goalId: this.goalId,
      starred: this.starred,
      dueDate: this.dueDate || null,
      reminderAt,
    };

    if (this.mode === 'create') {
      this.taskService
        .createTask({
          ...payloadBase,
          xpReward: this.xpPreview()?.estimatedTotalXp ?? 0,
          parentTaskId: this.selectedParentId,
        })
        .subscribe({
          next: () => {
            this.saving.set(false);
            this.saved.emit();
          },
          error: (err) => {
            this.saving.set(false);
            this.saveError.set(err?.error?.message ?? this.i18n.t('tasks.saveFailed'));
          },
        });
      return;
    }

    if (!this.task) return;
    const clearParent = !this.selectedParentId && !!this.task.parentTaskId;
    const clearReminder = !reminderAt && !!this.task.reminderAt;

    this.taskService
      .updateTask(this.task.id, {
        title: payloadBase.title,
        description: payloadBase.description,
        categoryId: payloadBase.categoryId,
        difficulty: payloadBase.difficulty,
        durationMinutes: payloadBase.durationMinutes,
        priority: payloadBase.priority,
        starred: payloadBase.starred,
        status: this.task.status,
        sortOrder: this.task.sortOrder,
        xpReward: this.xpPreview()?.estimatedTotalXp ?? this.task.xpReward,
        goalId: this.goalId ?? undefined,
        dueDate: payloadBase.dueDate,
        reminderAt: reminderAt,
        parentTaskId: this.selectedParentId,
        clearParent,
        clearReminder,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.saved.emit();
        },
        error: (err) => {
          this.saving.set(false);
          this.saveError.set(err?.error?.message ?? this.i18n.t('tasks.saveFailed'));
        },
      });
  }

  onDelete(): void {
    if (!this.task) return;
    this.taskService.deleteTask(this.task.id).subscribe({
      next: () => this.deleted.emit(),
      error: (err) => {
        console.error('Failed to delete task', err);
        this.saveError.set(this.i18n.t('tasks.deleteFailed'));
      },
    });
  }

  dueDateLabel(): string {
    if (!this.dueDate) return this.i18n.t('tasks.pickDate');
    return this.formatDateDisplay(this.dueDate);
  }

  reminderLabel(): string {
    if (!this.reminderLocal) return this.i18n.t('tasks.pickDateTime');
    return this.formatDateTimeDisplay(this.reminderLocal);
  }

  isDueToday(): boolean {
    return this.dueDate === this.isoDateOffset(0);
  }

  isDueTomorrow(): boolean {
    return this.dueDate === this.isoDateOffset(1);
  }

  setDueToday(): void {
    this.dueDate = this.isoDateOffset(0);
    if (this.mode === 'edit' && this.task) {
      this.taskService.scheduleTasks([this.task.id]).subscribe({
        error: (err) => console.error('Failed to schedule', err),
      });
    }
  }

  setDueTomorrow(): void {
    this.dueDate = this.isoDateOffset(1);
  }

  clearDueDate(): void {
    this.dueDate = '';
  }

  clearReminder(): void {
    this.reminderLocal = '';
  }

  private isoDateOffset(days: number): string {
    const d = new Date();
    d.setHours(12, 0, 0, 0);
    d.setDate(d.getDate() + days);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  }

  private formatDateDisplay(isoDate: string): string {
    const d = new Date(`${isoDate}T12:00:00`);
    if (Number.isNaN(d.getTime())) return isoDate;
    return d.toLocaleDateString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  }

  private formatDateTimeDisplay(local: string): string {
    const d = new Date(local);
    if (Number.isNaN(d.getTime())) return local;
    return d.toLocaleString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  addSubtask(): void {
    const title = this.newSubtaskTitle().trim();
    if (!title || !this.task || !this.selectedCategoryId) return;
    this.addingSubtask.set(true);
    this.taskService
      .createTask({
        categoryId: this.selectedCategoryId,
        title,
        parentTaskId: this.task.id,
        difficulty: 2,
        durationMinutes: 15,
        priority: 2,
        xpReward: 0,
      })
      .subscribe({
        next: () => {
          this.addingSubtask.set(false);
          this.newSubtaskTitle.set('');
          this.subtaskAdded.emit();
        },
        error: (err) => {
          this.addingSubtask.set(false);
          console.error('Failed to add subtask', err);
        },
      });
  }
}
