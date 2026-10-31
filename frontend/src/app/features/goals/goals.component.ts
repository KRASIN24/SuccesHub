import { Component, OnInit, effect, inject, signal, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { GoalService } from '../../core/services/goal.service';
import { ProfileService } from '../../core/services/profile.service';
import { GamificationService } from '../../core/services/gamification.service';
import { GamificationCelebrationService } from '../../core/services/gamification-celebration.service';
import { DevToolsService } from '../../core/services/dev-tools.service';
import { Goal, GoalSummary } from '../../core/models/goal.model';
import { UserProfile } from '../../core/models/profile.model';
import { WeeklyInsight } from '../../core/models/gamification.model';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';
import { GlitchButtonComponent } from '../../shared/components/glitch-button/glitch-button.component';
import { GlitchCheckboxComponent } from '../../shared/components/glitch-checkbox/glitch-checkbox.component';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { TranslateService } from '../../core/i18n/translate.service';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-goals',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    LoadingSkeletonComponent,
    ErrorStateComponent,
    GlitchButtonComponent,
    GlitchCheckboxComponent,
    TranslatePipe,
    PageHeaderComponent,
  ],
  templateUrl: './goals.component.html',
  styleUrl: './goals.component.scss',
})
export class GoalsComponent implements OnInit {
  protected readonly i18n = inject(TranslateService);
  private readonly goalService = inject(GoalService);
  private readonly profileService = inject(ProfileService);
  private readonly gamificationService = inject(GamificationService);
  private readonly celebrations = inject(GamificationCelebrationService);
  private readonly devTools = inject(DevToolsService);
  private readonly router = inject(Router);

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
  readonly strikingId = signal<string | null>(null);

  newGoalName = '';
  newGoalTier = 'Standard Beast';
  newGoalTargetDescription = '';
  newGoalTargetValue = 100;
  newGoalXpReward = 500;
  newGoalIcon = 'savings';
  newGoalFeatured = false;

  /** Campaign type choices — stored values stay stable; labels/descriptions come from i18n. */
  readonly tierOptions = [
    { value: 'Standard Beast', labelKey: 'goals.tierStandard', descKey: 'goals.tierStandardDesc' },
    { value: 'Elite Threat', labelKey: 'goals.tierElite', descKey: 'goals.tierEliteDesc' },
    { value: 'Legendary Leviathan', labelKey: 'goals.tierLegendary', descKey: 'goals.tierLegendaryDesc' },
  ] as const;

  constructor() {
    effect(() => {
      const tick = this.celebrations.goalsRefreshTick();
      if (tick > 0 && !this.loading()) {
        this.softRefreshGoals();
      }
    });
    effect(() => {
      const rev = this.devTools.dataRevision();
      untracked(() => {
        if (rev === 0 || this.loading()) {
          return;
        }
        this.softRefreshGoals();
        this.profileService.getProfile().subscribe({
          next: (p) => this.profile.set(p),
          error: () => undefined,
        });
      });
    });
  }

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

  /** Quiet refresh after boss damage from task completion elsewhere in the app. */
  private softRefreshGoals(): void {
    forkJoin({
      active: this.goalService.getGoals('ACTIVE'),
      completed: this.goalService.getGoals('COMPLETED'),
      summary: this.goalService.getSummary(),
    }).subscribe({
      next: (data) => {
        this.activeGoals.set(data.active);
        this.conqueredGoals.set(data.completed);
        this.summary.set(data.summary);
      },
      error: (err) => console.error('Failed to soft-refresh goals', err),
    });
  }

  progress(goal: Goal): number {
    if (goal.targetValue === 0) return 0;
    return Math.round((goal.currentProgress / goal.targetValue) * 100);
  }

  healthRemaining(goal: Goal): number {
    return Math.max(0, 100 - this.progress(goal));
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
    if (goal.targetValue <= 0) return false;
    return this.healthRemaining(goal) <= 15;
  }

  canStrike(goal: Goal): boolean {
    return (goal.pendingDamage ?? 0) > 0 && this.strikingId() !== goal.id;
  }

  openDetail(goal: Goal, event?: Event): void {
    event?.stopPropagation();
    void this.router.navigate(['/goals', goal.id]);
  }

  strike(goal: Goal, event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    if (!this.canStrike(goal)) return;
    this.strikingId.set(goal.id);
    this.goalService.strike(goal.id).subscribe({
      next: (damage) => {
        this.strikingId.set(null);
        this.celebrations.handleStrike(damage);
        this.softRefreshGoals();
      },
      error: (err) => {
        console.error('Strike failed', err);
        this.strikingId.set(null);
      },
    });
  }

  /** Maps stored tier values (stable) to display labels from i18n. */
  tierLabel(tier: string | null | undefined): string {
    switch (tier) {
      case 'Elite Threat':
        return this.i18n.t('goals.tierElite');
      case 'Legendary Leviathan':
        return this.i18n.t('goals.tierLegendary');
      case 'Standard Beast':
      default:
        return this.i18n.t('goals.tierStandard');
    }
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
            'Could not create campaign. Check you are logged in and try again.';
          this.saveError.set(message);
        },
      });
  }
}
