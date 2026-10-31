import { Component, Input, inject, signal } from '@angular/core';
import { BossDamage } from '../../../core/models/gamification.model';
import { TranslateService } from '../../../core/i18n/translate.service';

@Component({
  selector: 'app-boss-damage-toast',
  standalone: true,
  template: `
    @if (visible()) {
      <div class="boss-damage-toast">
        {{ message() }}
      </div>
    }
  `,
  styles: `
    .boss-damage-toast {
      position: fixed;
      top: 28%;
      left: calc(50% + var(--overlay-inline-offset, 0px) / 2);
      transform: translateX(-50%);
      max-width: min(90vw, 440px);
      padding: 0.85rem 1.25rem;
      border-radius: 12px;
      border: 1px solid rgba(255, 112, 67, 0.45);
      background: rgba(28, 12, 8, 0.94);
      color: #ffab91;
      font-size: 0.95rem;
      font-weight: 600;
      text-align: center;
      box-shadow: 0 12px 40px rgba(0, 0, 0, 0.45);
      animation: bossDamageToast 2.6s ease-out forwards;
      pointer-events: none;
      z-index: 1000;
    }

    @keyframes bossDamageToast {
      0% { opacity: 0; transform: translateX(-50%) translateY(12px); }
      12% { opacity: 1; transform: translateX(-50%) translateY(0); }
      78% { opacity: 1; }
      100% { opacity: 0; transform: translateX(-50%) translateY(-10px); }
    }
  `,
})
export class BossDamageToastComponent {
  private readonly i18n = inject(TranslateService);

  readonly visible = signal(false);
  readonly message = signal('');

  @Input() set trigger(value: { damage: BossDamage; tick: number } | null) {
    if (!value) return;
    const d = value.damage;
    const delta = Math.round(d.progressDelta);
    if (d.goalCompleted) {
      let msg = this.i18n.t('gamification.bossDefeated', {
        name: d.goalName,
        xp: d.goalXpReward,
      });
      if (delta > 0) {
        msg += this.i18n.t('gamification.bossDamageExtra', { delta });
      }
      this.message.set(msg);
    } else {
      // Bank toast from task complete uses bossHit; Strike release can use bossStrike when pct dropped.
      this.message.set(
        this.i18n.t('gamification.bossHit', {
          name: d.goalName,
          delta: delta || Math.round(d.progressDelta),
          pct: d.healthRemaining,
        })
      );
    }
    this.visible.set(true);
    setTimeout(() => this.visible.set(false), 2600);
  }
}
