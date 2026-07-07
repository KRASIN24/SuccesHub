import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { GoalService } from '../../core/services/goal.service';
import { ProfileService } from '../../core/services/profile.service';
import { GamificationService } from '../../core/services/gamification.service';
import { Goal, GoalSummary } from '../../core/models/goal.model';
import { UserProfile } from '../../core/models/profile.model';
import { WeeklyInsight } from '../../core/models/gamification.model';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';

@Component({
  selector: 'app-goals',
  standalone: true,
  imports: [CommonModule, FormsModule, LoadingSkeletonComponent, ErrorStateComponent],
  templateUrl: './goals.component.html',
  styleUrl: './goals.component.scss',
})
export class GoalsComponent implements OnInit {
  private readonly goalService = inject(GoalService);
  private readonly profileService = inject(ProfileService);
  private readonly gamificationService = inject(GamificationService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly activeGoals = signal<Goal[]>([]);
  readonly conqueredGoals = signal<Goal[]>([]);
  readonly summary = signal<GoalSummary | null>(null);
  readonly profile = signal<UserProfile | null>(null);
  readonly weeklyInsight = signal<WeeklyInsight | null>(null);
  readonly showAddForm = signal(false);
  readonly saveError = signal<string | null>(null);
  readonly saving = signal(false);

  newGoalName = '';
  newGoalTier = 'Standard Beast';
  newGoalTargetDescription = '';
  newGoalTargetValue = 100;
  newGoalXpReward = 500;
  newGoalIcon = 'savings';
  newGoalFeatured = false;

  ngOnInit(): void {
    this.loadGoalsData();
  }

  loadGoalsData(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      active: this.goalService.getGoals('ACTIVE'),
      completed: this.goalService.getGoals('COMPLETED'),
      summary: this.goalService.getSummary(),
      profile: this.profileService.getProfile(),
      insight: this.gamificationService.getWeeklyInsights(),
    }).subscribe({
      next: (data) => {
        this.activeGoals.set(data.active);
        this.conqueredGoals.set(data.completed);
        this.summary.set(data.summary);
        this.profile.set(data.profile);
        this.weeklyInsight.set(data.insight);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load goals data', err);
        this.error.set('Could not synchronize campaign status with the server.');
        this.loading.set(false);
      },
    });
  }

  progress(goal: Goal): number {
    if (goal.targetValue === 0) return 0;
    return Math.round((goal.currentProgress / goal.targetValue) * 100);
  }

  healthRemaining(goal: Goal): number {
    return Math.max(0, 100 - this.progress(goal));
  }

  openAddForm(): void {
    this.showAddForm.set(true);
    this.saveError.set(null);
    this.newGoalName = '';
    this.newGoalTier = 'Standard Beast';
    this.newGoalTargetDescription = '';
    this.newGoalTargetValue = 100;
    this.newGoalXpReward = 500;
    this.newGoalIcon = 'savings';
    this.newGoalFeatured = false;
  }

  cancelAddForm(): void {
    this.showAddForm.set(false);
    this.saveError.set(null);
  }

  saveGoal(): void {
    const name = this.newGoalName.trim();
    if (!name) return;

    const targetValue = Number(this.newGoalTargetValue);
    if (!Number.isFinite(targetValue) || targetValue < 1) {
      this.saveError.set('Target value must be at least 1.');
      return;
    }

    this.saving.set(true);
    this.saveError.set(null);

    this.goalService
      .createGoal({
        name,
        tier: this.newGoalTier.trim() || undefined,
        targetDescription: this.newGoalTargetDescription.trim() || undefined,
        targetValue,
        currentProgress: 0,
        xpReward: Math.max(0, Number(this.newGoalXpReward) || 0),
        icon: this.newGoalIcon || undefined,
        featured: this.newGoalFeatured,
      })
      .subscribe({
        next: () => {
          this.showAddForm.set(false);
          this.saving.set(false);
          this.loadGoalsData();
        },
        error: (err) => {
          console.error('Failed to create goal', err);
          this.saving.set(false);
          const message =
            err?.error?.message ??
            err?.error?.fieldErrors?.name ??
            'Could not summon boss. Check you are logged in and try again.';
          this.saveError.set(message);
        },
      });
  }
}
