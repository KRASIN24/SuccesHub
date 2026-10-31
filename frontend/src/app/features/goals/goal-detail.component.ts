import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { GoalService } from '../../core/services/goal.service';
import { TaskService } from '../../core/services/task.service';
import { GamificationCelebrationService } from '../../core/services/gamification-celebration.service';
import { Goal, GoalSubscription } from '../../core/models/goal.model';
import { Task, TaskCategory } from '../../core/models/task.model';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';
import { GlitchButtonComponent } from '../../shared/components/glitch-button/glitch-button.component';
import { GlitchCheckboxComponent } from '../../shared/components/glitch-checkbox/glitch-checkbox.component';
import {
  SearchPickerComponent,
  SearchPickerOption,
} from '../../shared/components/search-picker/search-picker.component';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { TranslateService } from '../../core/i18n/translate.service';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-goal-detail',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    LoadingSkeletonComponent,
    ErrorStateComponent,
    GlitchButtonComponent,
    GlitchCheckboxComponent,
    SearchPickerComponent,
    TranslatePipe,
    PageHeaderComponent,
  ],
  templateUrl: './goal-detail.component.html',
  styleUrl: './goal-detail.component.scss',
})
export class GoalDetailComponent implements OnInit {
  protected readonly i18n = inject(TranslateService);
  protected readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly goalService = inject(GoalService);
  private readonly taskService = inject(TaskService);
  private readonly celebrations = inject(GamificationCelebrationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly goal = signal<Goal | null>(null);
  readonly linkedTasks = signal<Task[]>([]);
  readonly subscriptions = signal<GoalSubscription[]>([]);
  readonly categories = signal<TaskCategory[]>([]);
  readonly allTasks = signal<Task[]>([]);
  readonly saving = signal(false);
  readonly striking = signal(false);
  readonly saveError = signal<string | null>(null);
  readonly editing = signal(false);

  editName = '';
  editTier = 'Standard Beast';
  editTargetDescription = '';
  editTargetValue = 100;
  editXpReward = 500;
  editIcon = 'savings';
  editFeatured = false;

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      void this.router.navigate(['/goals']);
      return;
    }
    this.load(id);
  }

  load(id: string): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      goal: this.goalService.getGoal(id),
      tasks: this.goalService.getLinkedTasks(id),
      subs: this.goalService.listSubscriptions(id),
      categories: this.taskService.getCategories(),
      allTasks: this.taskService.getTasks(),
    }).subscribe({
      next: (data) => {
        this.goal.set(data.goal);
        this.linkedTasks.set(data.tasks);
        this.subscriptions.set(data.subs);
        this.categories.set(data.categories);
        this.allTasks.set(data.allTasks);
        this.syncEditForm(data.goal);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load campaign', err);
        this.error.set('Could not load campaign.');
        this.loading.set(false);
      },
    });
  }

  private syncEditForm(g: Goal): void {
    this.editName = g.name;
    this.editTier = g.tier || 'Standard Beast';
    this.editTargetDescription = g.targetDescription || '';
    this.editTargetValue = g.targetValue;
    this.editXpReward = g.xpReward;
    this.editIcon = g.icon || 'savings';
    this.editFeatured = g.featured;
  }

  healthRemaining(goal: Goal): number {
    if (goal.targetValue === 0) return 0;
    return Math.max(0, 100 - Math.round((goal.currentProgress / goal.targetValue) * 100));
  }

  pendingDamage(goal: Goal): number {
    return Math.round(goal.pendingDamage ?? 0);
  }

  absoluteRemaining(goal: Goal): number {
    return Math.max(0, goal.targetValue - goal.currentProgress);
  }

  isFinalStrike(goal: Goal): boolean {
    const pending = goal.pendingDamage ?? 0;
    return pending > 0 && pending >= this.absoluteRemaining(goal);
  }

  isLowHp(goal: Goal): boolean {
    return this.healthRemaining(goal) <= 15;
  }

  canStrike(goal: Goal): boolean {
    return (goal.pendingDamage ?? 0) > 0 && !this.striking();
  }

  tierLabel(tier: string | null | undefined): string {
    switch (tier) {
      case 'Elite Threat':
        return this.i18n.t('goals.tierElite');
      case 'Legendary Leviathan':
        return this.i18n.t('goals.tierLegendary');
      default:
        return this.i18n.t('goals.tierStandard');
    }
  }

  availableTasksToAttach(): Task[] {
    const linked = new Set(this.linkedTasks().map((t) => t.id));
    return this.allTasks().filter((t) => t.status !== 'DONE' && !linked.has(t.id));
  }

  availableCategoriesToSubscribe(): TaskCategory[] {
    const taken = new Set(this.subscriptions().map((s) => s.categoryId));
    return this.categories().filter((c) => !taken.has(c.id));
  }

  taskPickerOptions(): SearchPickerOption[] {
    return this.availableTasksToAttach().map((t) => ({ id: t.id, label: t.title }));
  }

  categoryPickerOptions(): SearchPickerOption[] {
    return this.availableCategoriesToSubscribe().map((c) => ({ id: c.id, label: c.name }));
  }

  strike(): void {
    const g = this.goal();
    if (!g || !this.canStrike(g)) return;
    this.striking.set(true);
    this.goalService.strike(g.id).subscribe({
      next: (damage) => {
        this.striking.set(false);
        this.celebrations.handleStrike(damage);
        if (damage.goalCompleted) {
          void this.router.navigate(['/goals']);
        } else {
          this.load(g.id);
        }
      },
      error: (err) => {
        console.error('Strike failed', err);
        this.striking.set(false);
      },
    });
  }

  startEdit(): void {
    const g = this.goal();
    if (!g) return;
    this.syncEditForm(g);
    this.saveError.set(null);
    this.editing.set(true);
  }

  cancelEdit(): void {
    this.editing.set(false);
    this.saveError.set(null);
  }

  saveEdit(): void {
    const g = this.goal();
    if (!g) return;
    const name = this.editName.trim();
    if (!name) return;
    this.saving.set(true);
    this.saveError.set(null);
    this.goalService
      .updateGoal(g.id, {
        name,
        tier: this.editTier,
        targetDescription: this.editTargetDescription.trim() || undefined,
        targetValue: Number(this.editTargetValue),
        currentProgress: g.currentProgress,
        xpReward: Math.max(0, Number(this.editXpReward) || 0),
        icon: this.editIcon,
        featured: this.editFeatured,
        status: g.status,
      })
      .subscribe({
        next: (updated) => {
          this.goal.set(updated);
          this.editing.set(false);
          this.saving.set(false);
        },
        error: (err) => {
          console.error('Update failed', err);
          this.saving.set(false);
          this.saveError.set(err?.error?.message ?? 'Could not save campaign.');
        },
      });
  }

  deleteGoal(): void {
    const g = this.goal();
    if (!g) return;
    if (!confirm(this.i18n.t('goals.deleteConfirm'))) return;
    this.goalService.deleteGoal(g.id).subscribe({
      next: () => void this.router.navigate(['/goals']),
      error: (err) => console.error('Delete failed', err),
    });
  }

  attachTask(taskId: string): void {
    const g = this.goal();
    if (!g || !taskId) return;
    this.goalService.attachTask(g.id, taskId).subscribe({
      next: () => this.load(g.id),
      error: (err) => console.error('Attach failed', err),
    });
  }

  detachTask(taskId: string): void {
    const g = this.goal();
    if (!g) return;
    this.goalService.detachTask(g.id, taskId).subscribe({
      next: () => this.load(g.id),
      error: (err) => console.error('Detach failed', err),
    });
  }

  subscribeCategory(categoryId: string): void {
    this.subscribeCategories([categoryId]);
  }

  subscribeCategories(categoryIds: string[]): void {
    const g = this.goal();
    if (!g || categoryIds.length === 0) return;
    forkJoin(categoryIds.map((id) => this.goalService.subscribeCategory(g.id, id))).subscribe({
      next: () => {
        this.saveError.set(null);
        this.load(g.id);
      },
      error: (err) => {
        console.error('Subscribe failed', err);
        this.saveError.set(err?.error?.message ?? this.i18n.t('goals.subscribeError'));
        this.load(g.id);
      },
    });
  }

  unsubscribeCategory(categoryId: string): void {
    const g = this.goal();
    if (!g) return;
    this.goalService.unsubscribeCategory(g.id, categoryId).subscribe({
      next: () => this.load(g.id),
      error: (err) => console.error('Unsubscribe failed', err),
    });
  }
}
