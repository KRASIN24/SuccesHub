import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AchievementService } from '../../core/services/achievement.service';
import { Achievement } from '../../core/models/achievement.model';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { TranslateService } from '../../core/i18n/translate.service';
import {
  AchievementLabelPipe,
  AchievementDescPipe,
} from '../../core/i18n/achievement-label.pipe';

@Component({
  selector: 'app-achievements',
  standalone: true,
  imports: [
    CommonModule,
    LoadingSkeletonComponent,
    ErrorStateComponent,
    TranslatePipe,
    AchievementLabelPipe,
    AchievementDescPipe,
  ],
  template: `
    @if (loading()) {
      <app-loading-skeleton height="200px"></app-loading-skeleton>
    } @else if (error()) {
      <app-error-state [message]="error() || ''" (retry)="load()"></app-error-state>
    } @else {
      <div class="achievements-page">
        <h1>{{ 'achievementsPage.title' | translate }}</h1>
        <div class="merits-grid">
          @for (a of achievements(); track a.id) {
            <article class="merit-card" [class.locked]="a.locked">
              <span class="material-icons" aria-hidden="true">{{ a.icon }}</span>
              <h3>{{ a | achievementLabel }}</h3>
              <p>{{ a | achievementDesc }}</p>
            </article>
          }
        </div>
      </div>
    }
  `,
  styles: `
    .achievements-page {
      padding: 24px;
    }
    .achievements-page h1 {
      margin: 0;
      color: var(--color-text);
    }
    .merits-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(min(100%, 180px), 1fr));
      gap: 16px;
      margin-top: 24px;
    }
    .merit-card {
      min-width: 0;
      padding: 20px;
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: 12px;
      text-align: center;
      box-shadow: none;
      color: var(--color-text-muted);
      overflow: hidden;
    }
    .merit-card h3 {
      margin: 0.5rem 0 0.35rem;
      font-size: 1rem;
      line-height: 1.25;
      color: inherit;
      overflow-wrap: break-word;
      word-break: normal;
      hyphens: auto;
    }
    .merit-card p {
      margin: 0;
      color: var(--color-text-faint, var(--color-text-muted));
      font-size: 0.9rem;
      line-height: 1.35;
      overflow-wrap: break-word;
      word-break: normal;
    }
    .merit-card .material-icons {
      font-size: 40px;
      color: var(--color-text-faint, #9a948c);
    }
    /* Unlocked — gold plate so light theme clearly differs from locked */
    .merit-card:not(.locked) {
      color: var(--color-text);
      background: color-mix(in srgb, var(--color-gold, #d29f22) 12%, var(--color-surface, #fff));
      border-color: color-mix(in srgb, var(--color-gold, #d29f22) 55%, var(--color-border, #ddd));
      box-shadow:
        0 0 0 1px color-mix(in srgb, var(--color-gold, #d29f22) 28%, transparent),
        var(--shadow-card, 0 1px 2px rgba(0, 0, 0, 0.06));
    }
    .merit-card:not(.locked) .material-icons {
      color: var(--color-gold, #d29f22);
    }
    .merit-card:not(.locked) p {
      color: var(--color-text-muted);
    }
    .merit-card.locked {
      opacity: 1;
      background: color-mix(in srgb, var(--color-text) 3%, var(--color-surface, #fff));
    }
  `,
})
export class AchievementsComponent implements OnInit {
  private readonly achievementService = inject(AchievementService);
  private readonly i18n = inject(TranslateService);

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
        this.error.set(this.i18n.t('achievementsPage.loadFailed'));
        this.loading.set(false);
      },
    });
  }
}
