import { Component, Input, inject, signal } from '@angular/core';
import { Achievement } from '../../../core/models/achievement.model';
import { TranslateService } from '../../../core/i18n/translate.service';

@Component({
  selector: 'app-achievement-slam',
  standalone: true,
  template: `
    @if (visible() && achievement(); as a) {
      <div class="achievement-slam">
        <span class="material-icons">{{ a.icon }}</span>
        <span>{{ unlockedLabel(a) }}</span>
      </div>
    }
  `,
  styles: [`
    .achievement-slam {
      position: fixed;
      bottom: 24px;
      right: 24px;
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px 24px;
      background: rgba(26,26,46,0.95);
      border: 1px solid var(--color-gold, #d4af37);
      border-radius: 8px;
      z-index: 1050;
      animation: slam 1s ease-out forwards;
    }
    @keyframes slam {
      0% { transform: scale(0.5); opacity: 0; }
      30% { transform: scale(1.1); opacity: 1; }
      100% { transform: scale(1); opacity: 0; }
    }
  `],
})
export class AchievementSlamComponent {
  private readonly i18n = inject(TranslateService);

  readonly visible = signal(false);
  readonly achievement = signal<Achievement | null>(null);

  unlockedLabel(a: Achievement): string {
    const name = this.i18n.catalogEntry(
      'achievements',
      a.key,
      'label',
      a.label
    );
    return this.i18n.t('gamification.achievementUnlocked', { name });
  }

  @Input() set trigger(value: { achievement: Achievement; tick: number } | null) {
    if (!value) return;
    this.achievement.set(value.achievement);
    this.visible.set(true);
    setTimeout(() => this.visible.set(false), 1000);
  }
}
