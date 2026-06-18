import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { GoalService } from '../../core/services/goal.service';
import { ProfileService } from '../../core/services/profile.service';
import { Goal, GoalSummary } from '../../core/models/goal.model';
import { UserProfile } from '../../core/models/profile.model';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';

@Component({
  selector: 'app-goals',
  standalone: true,
  imports: [CommonModule, LoadingSkeletonComponent, ErrorStateComponent],
  templateUrl: './goals.component.html',
  styleUrl: './goals.component.scss',
})
export class GoalsComponent implements OnInit {
  private readonly goalService = inject(GoalService);
  private readonly profileService = inject(ProfileService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly activeGoals = signal<Goal[]>([]);
  readonly conqueredGoals = signal<Goal[]>([]);
  readonly summary = signal<GoalSummary | null>(null);
  readonly profile = signal<UserProfile | null>(null);

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
      profile: this.profileService.getProfile()
    }).subscribe({
      next: (data) => {
        this.activeGoals.set(data.active);
        this.conqueredGoals.set(data.completed);
        this.summary.set(data.summary);
        this.profile.set(data.profile);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load goals data', err);
        this.error.set('Could not synchronize campaign status with the server.');
        this.loading.set(false);
      }
    });
  }

  progress(goal: Goal): number {
    if (goal.targetValue === 0) return 0;
    return Math.round((goal.currentProgress / goal.targetValue) * 100);
  }

  healthRemaining(goal: Goal): number {
    return Math.max(0, 100 - this.progress(goal));
  }

  attackGoal(goal: Goal): void {
    // Attack increment logic
    const increment = Math.max(1, goal.targetValue / 10);
    const newProgress = Math.min(goal.targetValue, goal.currentProgress + increment);
    const completed = newProgress >= goal.targetValue;

    const payload: Partial<Goal> = {
      name: goal.name,
      tier: goal.tier,
      targetDescription: goal.targetDescription,
      targetValue: goal.targetValue,
      currentProgress: newProgress,
      xpReward: goal.xpReward,
      icon: goal.icon,
      featured: goal.featured,
      status: completed ? 'COMPLETED' : 'ACTIVE'
    };

    this.goalService.updateGoal(goal.id, payload).subscribe({
      next: () => {
        this.loadGoalsData();
      },
      error: (err) => {
        console.error('Failed to update goal', err);
      }
    });
  }
}
