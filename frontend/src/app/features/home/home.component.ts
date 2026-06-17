import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { ProfileService } from '../../core/services/profile.service';
import { TaskService } from '../../core/services/task.service';
import { AchievementService } from '../../core/services/achievement.service';
import { QuoteService } from '../../core/services/quote.service';
import { MarketService } from '../../core/services/market.service';
import { AstronomyService } from '../../core/services/astronomy.service';
import { UserProfile } from '../../core/models/profile.model';
import { Task } from '../../core/models/task.model';
import { Achievement } from '../../core/models/achievement.model';
import { MarketData } from '../../core/models/market.model';
import { LunarData } from '../../core/models/lunar.model';
import { DailyQuote } from '../../core/models/lunar.model';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [DecimalPipe, LoadingSkeletonComponent, ErrorStateComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  private readonly profileService = inject(ProfileService);
  private readonly taskService = inject(TaskService);
  private readonly achievementService = inject(AchievementService);
  private readonly quoteService = inject(QuoteService);
  private readonly marketService = inject(MarketService);
  private readonly astronomyService = inject(AstronomyService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly profile = signal<UserProfile | null>(null);
  readonly tasks = signal<Task[]>([]);
  readonly merits = signal<Achievement[]>([]);
  readonly marketData = signal<MarketData | null>(null);
  readonly lunarData = signal<LunarData | null>(null);
  readonly quote = signal<DailyQuote | null>(null);

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      profile: this.profileService.getProfile(),
      tasks: this.taskService.getTasks('TODO'),
      merits: this.achievementService.getAchievements(),
      market: this.marketService.getPrices(),
      lunar: this.astronomyService.getLunarPhase(),
      quote: this.quoteService.getDailyQuote()
    }).subscribe({
      next: (data) => {
        this.profile.set(data.profile);
        this.tasks.set(data.tasks);
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
      }
    });
  }

  get xpPercent(): number {
    const p = this.profile();
    if (!p || p.nextLevelXp === 0) return 0;
    return Math.round((p.currentXp / p.nextLevelXp) * 100);
  }

  get tasksRemaining(): number {
    return this.tasks().length;
  }

  toggleTask(task: Task): void {
    this.taskService.completeTask(task.id).subscribe({
      next: () => {
        this.tasks.update((list) => list.filter((t) => t.id !== task.id));
        this.profileService.getProfile().subscribe((prof) => this.profile.set(prof));
      },
      error: (err) => {
        console.error('Failed to complete task', err);
      }
    });
  }
}
