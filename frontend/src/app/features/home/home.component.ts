import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ProfileService } from '../../core/services/profile.service';
import { TaskService } from '../../core/services/task.service';
import { AchievementService } from '../../core/services/achievement.service';
import { GamificationService } from '../../core/services/gamification.service';
import { GamificationCelebrationService } from '../../core/services/gamification-celebration.service';
import { QuoteService } from '../../core/services/quote.service';
import { MarketService } from '../../core/services/market.service';
import { AstronomyService } from '../../core/services/astronomy.service';
import { Task } from '../../core/models/task.model';
import { Achievement } from '../../core/models/achievement.model';
import { DailyStatus, Forecast } from '../../core/models/gamification.model';
import { MarketData } from '../../core/models/market.model';
import { LunarData } from '../../core/models/lunar.model';
import { DailyQuote } from '../../core/models/lunar.model';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [DecimalPipe, RouterLink, LoadingSkeletonComponent, ErrorStateComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  protected readonly profileService = inject(ProfileService);
  private readonly taskService = inject(TaskService);
  private readonly achievementService = inject(AchievementService);
  private readonly gamificationService = inject(GamificationService);
  private readonly celebrations = inject(GamificationCelebrationService);
  private readonly quoteService = inject(QuoteService);
  private readonly marketService = inject(MarketService);
  private readonly astronomyService = inject(AstronomyService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly celebrating = signal(false);

  readonly daily = signal<DailyStatus | null>(null);
  readonly forecast = signal<Forecast | null>(null);
  readonly unscheduled = signal<Task[]>([]);
  readonly merits = signal<Achievement[]>([]);
  readonly marketData = signal<MarketData | null>(null);
  readonly lunarData = signal<LunarData | null>(null);
  readonly quote = signal<DailyQuote | null>(null);

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(silent = false): void {
    if (!silent) {
      this.loading.set(true);
    }
    this.error.set(null);

    forkJoin({
      profile: this.profileService.getProfile(),
      daily: this.gamificationService.getDailyStatus(),
      forecast: this.gamificationService.getForecast(),
      allTasks: this.taskService.getTasks('TODO'),
      merits: this.achievementService.getAchievements(),
      market: this.marketService.getPrices(),
      lunar: this.astronomyService.getLunarPhase(),
      quote: this.quoteService.getDailyQuote(),
    }).subscribe({
      next: (data) => {
        this.daily.set(data.daily);
        this.forecast.set(data.forecast);
        const scheduledIds = new Set(data.daily.scheduledToday.map((t) => t.id));
        this.unscheduled.set(data.allTasks.filter((t) => !scheduledIds.has(t.id) && !t.scheduledDate));
        this.merits.set(data.merits);
        this.marketData.set(data.market);
        this.lunarData.set(data.lunar);
        this.quote.set(data.quote);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load dashboard data', err);
        this.error.set('Could not synchronize dashboard state with the server.');
        this.loading.set(false);
      },
    });
  }

  get xpPercent(): number {
    const p = this.profileService.profile();
    if (!p || p.nextLevelXp === 0) return 0;
    return Math.round((p.currentXp / p.nextLevelXp) * 100);
  }

  get dailyXpPercent(): number {
    const d = this.daily();
    if (!d) return 0;
    const total = d.xpEarnedToday + d.dailyXpRemaining;
    if (total === 0) return 0;
    return Math.round((d.xpEarnedToday / total) * 100);
  }

  get tasksRemaining(): number {
    return this.daily()?.scheduledToday.length ?? 0;
  }

  estimatedXp(task: Task): number {
    return task.xpReward;
  }

  scheduleTask(task: Task): void {
    this.taskService.scheduleTasks([task.id]).subscribe({
      next: () => this.loadDashboardData(true),
      error: (err) => console.error('Failed to schedule task', err),
    });
  }

  toggleTask(task: Task): void {
    this.taskService.completeTask(task.id).subscribe({
      next: (completion) => {
        this.celebrations.handleTaskCompletion(completion);
        this.loadDashboardData(true);
      },
      error: (err) => console.error('Failed to complete task', err),
    });
  }

  celebrateDay(): void {
    if (this.celebrating()) return;
    this.celebrating.set(true);
    this.gamificationService.closeDay().subscribe({
      next: (result) => {
        this.celebrations.handleCloseDay(result);
        this.loadDashboardData(true);
        this.celebrating.set(false);
      },
      error: (err) => {
        console.error('Failed to close day', err);
        this.celebrating.set(false);
      },
    });
  }
}
