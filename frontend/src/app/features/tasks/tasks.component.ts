import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import {
  CdkDrag,
  CdkDragDrop,
  CdkDragMove,
  CdkDropList,
  DragDropModule,
  moveItemInArray,
} from '@angular/cdk/drag-drop';
import { forkJoin, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { TaskService } from '../../core/services/task.service';
import { GoalService } from '../../core/services/goal.service';
import { GamificationCelebrationService } from '../../core/services/gamification-celebration.service';
import { Task, TaskCategory, TaskSortMode } from '../../core/models/task.model';
import { Goal, GoalSubscription } from '../../core/models/goal.model';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';
import { GlitchButtonComponent } from '../../shared/components/glitch-button/glitch-button.component';
import { GlitchCheckboxComponent } from '../../shared/components/glitch-checkbox/glitch-checkbox.component';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { TranslateService } from '../../core/i18n/translate.service';
import {
  TaskEditorMode,
  TaskEditorSheetComponent,
} from './task-editor-sheet/task-editor-sheet.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

const SORT_BY_CATEGORY_KEY = 'successhub.tasks.sortModeByCategory';

interface QuestColumn {
  id: string;
  name: string;
  tag: string;
  grantXp: boolean;
  sortOrder: number;
  /** All tasks in this category (roots + subtasks). */
  taskCount: number;
  roots: Task[];
  childrenByParentId: Record<string, Task[]>;
}

type BoardMode = 'category' | 'campaign';

interface CampaignSection {
  id: string;
  name: string;
  tasks: Task[];
}

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DragDropModule,
    LoadingSkeletonComponent,
    ErrorStateComponent,
    GlitchButtonComponent,
    GlitchCheckboxComponent,
    TranslatePipe,
    TaskEditorSheetComponent,
    PageHeaderComponent,
  ],
  templateUrl: './tasks.component.html',
  styleUrl: './tasks.component.scss',
})
export class TasksComponent implements OnInit {
  protected readonly i18n = inject(TranslateService);
  private readonly taskService = inject(TaskService);
  private readonly goalService = inject(GoalService);
  private readonly celebrations = inject(GamificationCelebrationService);
  private readonly route = inject(ActivatedRoute);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly columns = signal<QuestColumn[]>([]);
  readonly goals = signal<Goal[]>([]);
  readonly focusTaskId = signal<string | null>(null);
  readonly allTasks = signal<Task[]>([]);
  readonly boardMode = signal<BoardMode>('category');
  readonly campaignSections = signal<CampaignSection[]>([]);
  /** goalId → subscribed category IDs */
  readonly subscriptionsByGoal = signal<Record<string, string[]>>({});
  readonly mismatchPickerTaskId = signal<string | null>(null);
  mismatchPickCategoryId = '';

  /** categoryId → sort mode */
  readonly sortModes = signal<Record<string, TaskSortMode>>(this.readSortModes());
  readonly sortSheetCategoryId = signal<string | null>(null);
  /** Stable CDK connected-to ids (rebuilt when columns change). */
  readonly taskDropListIds = signal<string[]>([]);
  /** True while a task row is being dragged (arms nest drop tips). */
  readonly taskDragActive = signal(false);
  private dragPointer = { x: 0, y: 0 };

  readonly editorOpen = signal(false);
  readonly editorMode = signal<TaskEditorMode>('create');
  readonly editorTask = signal<Task | null>(null);
  readonly editorCategoryId = signal<string | null>(null);

  readonly showNewCategoryForm = signal(false);
  readonly editingCategoryId = signal<string | null>(null);
  readonly categorySaveError = signal<string | null>(null);
  newCategoryName = '';
  newCategoryGrantXp = true;

  editCategoryName = '';
  editCategoryGrantXp = true;
  private editCategorySortOrder = 0;
  private editCategoryTag = '';

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      this.focusTaskId.set(params.get('focus'));
    });
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
    })
      .pipe(
        switchMap((data) => {
          if (data.goals.length === 0) {
            return of({ ...data, subs: [] as GoalSubscription[][] });
          }
          return forkJoin(data.goals.map((g) => this.goalService.listSubscriptions(g.id))).pipe(
            map((subs) => ({ ...data, subs }))
          );
        })
      )
      .subscribe({
        next: (data) => {
          this.goals.set(data.goals);
          this.allTasks.set(data.tasks);
          this.columns.set(
            data.categories.map((cat) => this.buildColumn(cat, data.tasks))
          );
          const byGoal: Record<string, string[]> = {};
          data.goals.forEach((g, i) => {
            byGoal[g.id] = (data.subs[i] ?? []).map((s) => s.categoryId);
          });
          this.subscriptionsByGoal.set(byGoal);
          this.campaignSections.set(this.buildCampaignSections(data.goals, data.tasks));
          this.refreshTaskDropListIds();
          this.loading.set(false);

          const openId = this.editorTask()?.id;
          if (openId && this.editorOpen()) {
            const refreshed = data.tasks.find((t) => t.id === openId) ?? null;
            this.editorTask.set(refreshed);
            if (refreshed?.categoryId) {
              this.editorCategoryId.set(refreshed.categoryId);
            }
          }

          const focusId = this.focusTaskId();
          if (focusId) {
            setTimeout(() => {
              document.getElementById('task-' + focusId)?.scrollIntoView({
                behavior: 'smooth',
                block: 'center',
              });
            }, 50);
          }
        },
        error: (err) => {
          console.error('Failed to load tasks data', err);
          this.error.set('Could not synchronize task strategy canvas.');
          this.loading.set(false);
        },
      });
  }

  setBoardMode(mode: BoardMode): void {
    this.boardMode.set(mode);
    this.mismatchPickerTaskId.set(null);
  }

  private buildCampaignSections(goals: Goal[], tasks: Task[]): CampaignSection[] {
    const sections: CampaignSection[] = goals.map((g) => ({
      id: g.id,
      name: g.name,
      tasks: tasks.filter((t) => t.goalId === g.id && t.status !== 'DONE'),
    }));
    sections.push({
      id: 'unlinked',
      name: this.i18n.t('tasks.unlinkedCampaign'),
      tasks: tasks.filter((t) => !t.goalId && t.status !== 'DONE'),
    });
    return sections;
  }

  isMismatch(task: Task): boolean {
    if (!task.goalId) return false;
    const subCats = this.subscriptionsByGoal()[task.goalId] ?? [];
    if (subCats.length === 0) return false;
    return !task.categoryId || !subCats.includes(task.categoryId);
  }

  subscribedCategoriesFor(task: Task): TaskCategory[] {
    if (!task.goalId) return [];
    const ids = new Set(this.subscriptionsByGoal()[task.goalId] ?? []);
    return this.columns()
      .filter((c) => ids.has(c.id))
      .map((c) => ({
        id: c.id,
        name: c.name,
        tag: c.tag,
        grantXp: c.grantXp,
        sortOrder: c.sortOrder,
      }));
  }

  moveToCampaignCategory(task: Task): void {
    if (!task.goalId) return;
    const cats = this.subscriptionsByGoal()[task.goalId] ?? [];
    if (cats.length === 1) {
      this.applyCategoryMove(task, cats[0]);
      return;
    }
    if (cats.length > 1) {
      this.mismatchPickCategoryId = '';
      this.mismatchPickerTaskId.set(task.id);
    }
  }

  confirmMismatchMove(task: Task): void {
    if (!this.mismatchPickCategoryId) return;
    this.applyCategoryMove(task, this.mismatchPickCategoryId);
    this.mismatchPickerTaskId.set(null);
  }

  cancelMismatchMove(): void {
    this.mismatchPickerTaskId.set(null);
  }

  private applyCategoryMove(task: Task, categoryId: string): void {
    this.taskService
      .updateTask(task.id, {
        ...this.taskUpdatePayload(task),
        categoryId,
      })
      .subscribe({
        next: () => this.loadTasksData(true),
        error: (err) => console.error('Failed to move task to campaign category', err),
      });
  }

  private buildColumn(cat: TaskCategory, tasks: Task[]): QuestColumn {
    const inCat = tasks.filter((t) => t.categoryId === cat.id);
    const childrenByParentId: Record<string, Task[]> = {};
    for (const t of inCat) {
      if (t.parentTaskId) {
        const list = childrenByParentId[t.parentTaskId] ?? [];
        list.push(t);
        childrenByParentId[t.parentTaskId] = list;
      }
    }
    for (const parentId of Object.keys(childrenByParentId)) {
      childrenByParentId[parentId] = this.sortTasks(childrenByParentId[parentId], cat.id);
    }
    const roots = this.sortTasks(
      inCat.filter((t) => !t.parentTaskId),
      cat.id
    );
    return {
      id: cat.id,
      name: cat.name,
      tag: cat.tag,
      grantXp: cat.grantXp,
      sortOrder: cat.sortOrder,
      taskCount: inCat.length,
      roots,
      childrenByParentId,
    };
  }

  sortModeFor(categoryId: string): TaskSortMode {
    return this.sortModes()[categoryId] ?? 'manual';
  }

  private sortTasks(tasks: Task[], categoryId: string): Task[] {
    const mode = this.sortModeFor(categoryId);
    const copy = [...tasks];
    switch (mode) {
      case 'starred':
        return copy.sort((a, b) => Number(b.starred) - Number(a.starred) || a.sortOrder - b.sortOrder);
      case 'title':
        return copy.sort((a, b) => a.title.localeCompare(b.title) || a.sortOrder - b.sortOrder);
      case 'due':
        return copy.sort((a, b) => {
          const ad = a.dueDate || a.scheduledDate || a.reminderAt || '';
          const bd = b.dueDate || b.scheduledDate || b.reminderAt || '';
          if (!ad && !bd) return a.sortOrder - b.sortOrder;
          if (!ad) return 1;
          if (!bd) return -1;
          return ad.localeCompare(bd) || a.sortOrder - b.sortOrder;
        });
      case 'manual':
      default:
        return copy.sort((a, b) => a.sortOrder - b.sortOrder);
    }
  }

  private readSortModes(): Record<string, TaskSortMode> {
    try {
      const raw = localStorage.getItem(SORT_BY_CATEGORY_KEY);
      if (!raw) {
        // migrate legacy global key once
        const legacy = localStorage.getItem('successhub.tasks.sortMode');
        if (legacy === 'manual' || legacy === 'starred' || legacy === 'title' || legacy === 'due') {
          return {};
        }
        return {};
      }
      const parsed = JSON.parse(raw) as Record<string, string>;
      const out: Record<string, TaskSortMode> = {};
      for (const [k, v] of Object.entries(parsed)) {
        if (v === 'manual' || v === 'starred' || v === 'title' || v === 'due') {
          out[k] = v;
        }
      }
      return out;
    } catch {
      return {};
    }
  }

  private persistSortModes(modes: Record<string, TaskSortMode>): void {
    try {
      localStorage.setItem(SORT_BY_CATEGORY_KEY, JSON.stringify(modes));
    } catch {
      /* ignore */
    }
  }

  openSortSheet(categoryId: string): void {
    this.sortSheetCategoryId.set(categoryId);
  }

  closeSortSheet(): void {
    this.sortSheetCategoryId.set(null);
  }

  setSortMode(mode: TaskSortMode): void {
    const categoryId = this.sortSheetCategoryId();
    if (!categoryId) return;
    const next = { ...this.sortModes(), [categoryId]: mode };
    this.sortModes.set(next);
    this.persistSortModes(next);
    this.sortSheetCategoryId.set(null);
    this.rebuildColumns();
  }

  private rebuildColumns(): void {
    this.columns.set(
      this.columns().map((col) => {
        const cat: TaskCategory = {
          id: col.id,
          name: col.name,
          tag: col.tag,
          grantXp: col.grantXp,
          sortOrder: col.sortOrder,
        };
        return this.buildColumn(
          cat,
          this.allTasks().filter((t) => t.categoryId === col.id)
        );
      })
    );
    this.refreshTaskDropListIds();
  }

  private refreshTaskDropListIds(): void {
    this.taskDropListIds.set(this.connectedTaskListIds());
  }

  childrenOf(column: QuestColumn, parentId: string): Task[] {
    // Stable array reference required for CDK transferArrayItem (including empty nests).
    if (!column.childrenByParentId[parentId]) {
      column.childrenByParentId[parentId] = [];
    }
    return column.childrenByParentId[parentId];
  }

  /** Root tasks usable as parents when nesting (exclude self / own descendants). */
  parentCandidatesForEditor(): Task[] {
    const editing = this.editorTask();
    return this.allTasks().filter((t) => {
      if (t.parentTaskId) return false;
      if (editing && t.id === editing.id) return false;
      return true;
    });
  }

  setGrantXp(column: QuestColumn, grantXp: boolean): void {
    if (column.grantXp === grantXp) return;
    this.taskService
      .updateCategory(column.id, this.categoryPayload(column, { grantXp }))
      .subscribe({
        next: () => {
          column.grantXp = grantXp;
        },
        error: (err) => console.error('Failed to update category grantXp', err),
      });
  }

  openEditCategoryForm(column: QuestColumn): void {
    this.editingCategoryId.set(column.id);
    this.categorySaveError.set(null);
    this.editCategoryName = column.name;
    this.editCategoryGrantXp = column.grantXp;
    this.editCategorySortOrder = column.sortOrder;
    this.editCategoryTag = column.tag ?? '';
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
        tag: this.editCategoryTag,
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

  deleteCategory(columnId: string): void {
    this.categorySaveError.set(null);
    const rootTasksInCategory = this.allTasks().filter(
      (t) => t.categoryId === columnId && !t.parentTaskId
    );
    const finish = (): void => {
      this.taskService.deleteCategory(columnId).subscribe({
        next: () => {
          const nextModes = { ...this.sortModes() };
          delete nextModes[columnId];
          this.sortModes.set(nextModes);
          try {
            localStorage.setItem(SORT_BY_CATEGORY_KEY, JSON.stringify(nextModes));
          } catch {
            /* ignore */
          }
          this.editingCategoryId.set(null);
          this.loadTasksData(true);
        },
        error: (err: { error?: { message?: string } }) => {
          console.error('Failed to delete category', err);
          this.categorySaveError.set(
            err?.error?.message ?? this.i18n.t('tasks.deleteCategoryFailed')
          );
        },
      });
    };

    if (rootTasksInCategory.length === 0) {
      finish();
      return;
    }

    forkJoin(rootTasksInCategory.map((t) => this.taskService.deleteTask(t.id))).subscribe({
      next: () => finish(),
      error: (err: { error?: { message?: string } }) => {
        console.error('Failed to delete category tasks', err);
        this.categorySaveError.set(
          err?.error?.message ?? this.i18n.t('tasks.deleteCategoryFailed')
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
      tag: column.tag ?? '',
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
      starred: task.starred,
      sortOrder: task.sortOrder,
      parentTaskId: task.parentTaskId,
      dueDate: task.dueDate,
      reminderAt: task.reminderAt,
    };
  }

  toggleStar(task: Task, event?: Event): void {
    this.setStar(task, !task.starred, event);
  }

  setStar(task: Task, starred: boolean, event?: Event): void {
    event?.stopPropagation();
    this.taskService.starTask(task.id, starred).subscribe({
      next: () => this.loadTasksData(true),
      error: (err) => console.error('Failed to star task', err),
    });
  }

  toggleTaskFromSheet(task: Task): void {
    const column = this.columns().find((c) => c.id === task.categoryId);
    if (!column) return;
    this.toggleTask(task, column);
  }

  openCreateSheet(categoryId: string): void {
    this.editorMode.set('create');
    this.editorTask.set(null);
    this.editorCategoryId.set(categoryId);
    this.editorOpen.set(true);
    this.editingCategoryId.set(null);
  }

  openEditSheet(task: Task): void {
    this.editorMode.set('edit');
    this.editorTask.set(task);
    this.editorCategoryId.set(task.categoryId ?? null);
    this.editorOpen.set(true);
  }

  closeEditor(): void {
    this.editorOpen.set(false);
    this.editorTask.set(null);
  }

  onEditorSaved(): void {
    this.closeEditor();
    this.loadTasksData(true);
  }

  onEditorDeleted(): void {
    this.closeEditor();
    this.loadTasksData(true);
  }

  onSubtaskAdded(): void {
    this.loadTasksData(true);
  }

  editorSubtasks(): Task[] {
    const task = this.editorTask();
    if (!task) return [];
    const col = this.columns().find((c) => c.id === task.categoryId);
    return col ? this.childrenOf(col, task.id) : [];
  }

  onCategoryDrop(event: CdkDragDrop<QuestColumn[]>): void {
    if (event.previousIndex === event.currentIndex) return;
    const cols = [...this.columns()];
    moveItemInArray(cols, event.previousIndex, event.currentIndex);
    cols.forEach((c, i) => {
      c.sortOrder = i;
    });
    this.columns.set(cols);
    this.taskService.reorderCategories(cols.map((c) => c.id)).subscribe({
      next: () => this.loadTasksData(true),
      error: (err) => {
        console.error('Failed to reorder categories', err);
        this.loadTasksData(true);
      },
    });
  }

  rootListId(categoryId: string): string {
    return `roots-${categoryId}`;
  }

  subListId(parentId: string): string {
    return `subs-${parentId}`;
  }

  /** All task drop-list ids so roots/subtasks can move across categories and nest. */
  connectedTaskListIds(): string[] {
    const ids: string[] = [];
    for (const col of this.columns()) {
      ids.push(this.rootListId(col.id));
      for (const root of col.roots) {
        ids.push(this.subListId(root.id));
      }
    }
    return ids;
  }

  /** Reject nesting a task that already has children (one level only). */
  canEnterSubtaskList = (drag: CdkDrag, drop: CdkDropList): boolean => {
    const task = drag.data as Task | undefined;
    if (!task?.id) return false;
    const parentId = String(drop.id ?? '').replace(/^subs-/, '');
    if (task.id === parentId) return false;
    return !this.hasChildren(task.id);
  };

  onTaskDragStarted(): void {
    this.taskDragActive.set(true);
  }

  onTaskDragMoved(event: CdkDragMove): void {
    this.dragPointer = { x: event.pointerPosition.x, y: event.pointerPosition.y };
  }

  onTaskDragEnded(): void {
    this.taskDragActive.set(false);
  }

  /**
   * Resolve nest target from pointer position (works across categories).
   * Uses tip bounding boxes so the first empty yellow tip is easy to hit.
   */
  private findNestTargetAtPointer(
    draggedId: string
  ): { column: QuestColumn; parentId: string } | null {
    if (this.hasChildren(draggedId)) return null;

    const { x, y } = this.dragPointer;
    const tips = document.querySelectorAll<HTMLElement>('[data-nest-for]');
    for (const tip of Array.from(tips)) {
      if (tip.classList.contains('is-empty') && !tip.classList.contains('is-armed')) {
        continue;
      }
      const rect = tip.getBoundingClientRect();
      // Generous vertical hit band for empty first-nest tips
      const padY = tip.classList.contains('is-empty') ? 10 : 4;
      if (x < rect.left || x > rect.right || y < rect.top - padY || y > rect.bottom + padY) {
        continue;
      }
      const parentId = tip.dataset['nestFor'];
      if (!parentId || parentId === draggedId) continue;
      const column = this.columns().find((c) => c.roots.some((r) => r.id === parentId));
      if (!column) continue;
      return { column, parentId };
    }
    return null;
  }

  private draggedTask(event: CdkDragDrop<Task[]>): Task | null {
    return (
      (event.item.data as Task | undefined) ??
      event.previousContainer.data[event.previousIndex] ??
      null
    );
  }

  onRootDrop(column: QuestColumn, event: CdkDragDrop<Task[]>): void {
    const dragged = this.draggedTask(event);
    if (!dragged) return;

    const nest = this.findNestTargetAtPointer(dragged.id);
    if (nest) {
      this.moveIntoNest(nest.column, nest.parentId, event);
      return;
    }

    if (event.previousContainer === event.container) {
      if (event.previousIndex === event.currentIndex) return;
      if (this.sortModeFor(column.id) !== 'manual') return;
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      column.roots = [...event.container.data];
      this.persistSiblingOrder(column.id, null, column.roots.map((t) => t.id));
      return;
    }

    const previousMeta = this.listMetaFromId(event.previousContainer.id);
    const previousOrdered = event.previousContainer.data
      .filter((t) => t.id !== dragged.id)
      .map((t) => t.id);

    // Detach from previous list data, then insert as root in this column
    this.detachTaskFromAllLocalLists(dragged.id);
    const roots = [...column.roots.filter((t) => t.id !== dragged.id)];
    const insertAt = Math.min(event.currentIndex, roots.length);
    roots.splice(insertAt, 0, dragged);
    column.roots = roots;
    this.syncColumnRef(column);

    this.persistTaskPlacement(
      dragged,
      column.id,
      null,
      roots.map((t) => t.id),
      previousMeta,
      previousOrdered
    );
  }

  onSubtaskDrop(column: QuestColumn, parentId: string, event: CdkDragDrop<Task[]>): void {
    const dragged = this.draggedTask(event);
    if (!dragged) return;

    // Pointer may be over a different parent's tip (incl. another category).
    const nest = this.findNestTargetAtPointer(dragged.id);
    if (nest && nest.parentId !== parentId) {
      this.moveIntoNest(nest.column, nest.parentId, event);
      return;
    }

    if (event.previousContainer === event.container) {
      if (event.previousIndex === event.currentIndex) return;
      if (this.sortModeFor(column.id) !== 'manual') return;
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
      column.childrenByParentId = {
        ...column.childrenByParentId,
        [parentId]: [...event.container.data],
      };
      this.persistSiblingOrder(column.id, parentId, event.container.data.map((t) => t.id));
      return;
    }

    this.moveIntoNest(column, parentId, event);
  }

  /** Move dragged task into a parent's subtask list (any category). */
  private moveIntoNest(
    column: QuestColumn,
    parentId: string,
    event: CdkDragDrop<Task[]>
  ): void {
    const task = this.draggedTask(event);
    if (!task) return;
    if (task.id === parentId || this.hasChildren(task.id)) {
      this.loadTasksData(true);
      return;
    }

    const previousMeta = this.listMetaFromId(event.previousContainer.id);
    const previousOrdered = event.previousContainer.data
      .filter((t) => t.id !== task.id)
      .map((t) => t.id);

    this.detachTaskFromAllLocalLists(task.id);
    const kids = this.childrenOf(column, parentId).filter((t) => t.id !== task.id);
    const insertAt =
      event.container.id === this.subListId(parentId)
        ? Math.min(Math.max(event.currentIndex, 0), kids.length)
        : kids.length;
    kids.splice(insertAt, 0, task);
    column.childrenByParentId = { ...column.childrenByParentId, [parentId]: kids };
    this.syncColumnRef(column);

    this.persistTaskPlacement(
      task,
      column.id,
      parentId,
      kids.map((t) => t.id),
      previousMeta,
      previousOrdered
    );
  }

  private detachTaskFromAllLocalLists(taskId: string): void {
    for (const col of this.columns()) {
      col.roots = col.roots.filter((t) => t.id !== taskId);
      const nextKids: Record<string, Task[]> = { ...col.childrenByParentId };
      for (const pid of Object.keys(nextKids)) {
        nextKids[pid] = nextKids[pid].filter((t) => t.id !== taskId);
      }
      col.childrenByParentId = nextKids;
    }
  }

  private syncColumnRef(column: QuestColumn): void {
    this.columns.set(this.columns().map((c) => (c.id === column.id ? { ...column } : c)));
  }

  private listMetaFromId(
    listId: string
  ): { categoryId: string; parentTaskId: string | null } | null {
    if (listId.startsWith('roots-')) {
      return { categoryId: listId.slice('roots-'.length), parentTaskId: null };
    }
    if (listId.startsWith('subs-')) {
      const parentId = listId.slice('subs-'.length);
      const parent = this.allTasks().find((t) => t.id === parentId);
      if (!parent?.categoryId) return null;
      return { categoryId: parent.categoryId, parentTaskId: parentId };
    }
    return null;
  }

  private hasChildren(taskId: string): boolean {
    return this.columns().some((c) => (c.childrenByParentId[taskId]?.length ?? 0) > 0);
  }

  private persistSiblingOrder(
    categoryId: string,
    parentTaskId: string | null,
    orderedIds: string[],
    thenReload = true
  ): void {
    if (orderedIds.length === 0) {
      if (thenReload) this.loadTasksData(true);
      return;
    }
    this.taskService.reorderTasks({ categoryId, parentTaskId, orderedIds }).subscribe({
      next: () => {
        if (thenReload) this.loadTasksData(true);
      },
      error: (err) => {
        console.error('Failed to reorder', err);
        this.loadTasksData(true);
      },
    });
  }

  private persistTaskPlacement(
    task: Task,
    categoryId: string,
    parentTaskId: string | null,
    orderedIds: string[],
    previousMeta: { categoryId: string; parentTaskId: string | null } | null,
    previousOrderedIds: string[]
  ): void {
    this.taskService
      .updateTask(task.id, {
        ...this.taskUpdatePayload(task),
        categoryId,
        parentTaskId,
        clearParent: !parentTaskId,
      })
      .subscribe({
        next: () => {
          this.taskService
            .reorderTasks({ categoryId, parentTaskId, orderedIds })
            .subscribe({
              next: () => {
                if (previousMeta && previousOrderedIds.length > 0) {
                  this.persistSiblingOrder(
                    previousMeta.categoryId,
                    previousMeta.parentTaskId,
                    previousOrderedIds,
                    true
                  );
                } else {
                  this.loadTasksData(true);
                }
              },
              error: () => this.loadTasksData(true),
            });
        },
        error: (err) => {
          console.error('Failed to move task', err);
          this.loadTasksData(true);
        },
      });
  }

  saveCategory(): void {
    if (!this.newCategoryName?.trim()) return;

    this.taskService
      .createCategory({
        name: this.newCategoryName.trim(),
        tag: '',
        grantXp: this.newCategoryGrantXp,
        sortOrder: this.columns().length,
      })
      .subscribe({
        next: () => {
          this.showNewCategoryForm.set(false);
          this.newCategoryName = '';
          this.newCategoryGrantXp = true;
          this.loadTasksData();
        },
        error: (err) => console.error('Failed to create category', err),
      });
  }
}
