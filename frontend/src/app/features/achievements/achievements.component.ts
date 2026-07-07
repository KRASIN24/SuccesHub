import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AchievementService } from '../../core/services/achievement.service';
import { Achievement } from '../../core/models/achievement.model';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';

@Component({
  selector: 'app-achievements',
  standalone: true,
  imports: [CommonModule, LoadingSkeletonComponent, ErrorStateComponent],
  template: `
    @if (loading()) {
      <app-loading-skeleton height="200px"></app-loading-skeleton>
    } @else if (error()) {
      <app-error-state [message]="error() || ''" (retry)="load()"></app-error-state>
    } @else {
      <div class="achievements-page">
        <h1>Achievements</h1>
        <div class="merits-grid">
          @for (a of achievements(); track a.id) {
            <article class="merit-card" [class.locked]="a.locked">
              <span class="material-icons">{{ a.icon }}</span>
              <h3>{{ a.label }}</h3>
              <p>{{ a.description }}</p>
            </article>
          }
        </div>
      </div>
    }
  `,
  styles: [`
    .achievements-page { padding: 24px; }
    .merits-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; margin-top: 24px; }
    .merit-card { padding: 20px; background: rgba(255,255,255,0.03); border-radius: 12px; text-align: center; }
    .merit-card.locked { opacity: 0.4; }
    .merit-card .material-icons { font-size: 40px; color: var(--color-gold, #d4af37); }
  `],
})
export class AchievementsComponent implements OnInit {
  private readonly achievementService = inject(AchievementService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly achievements = signal<Achievement[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.achievementService.getAchievements().subscribe({
      next: (data) => {
        this.achievements.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load achievements.');
        this.loading.set(false);
      },
    });
  }
}
