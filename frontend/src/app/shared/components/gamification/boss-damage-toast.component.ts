import { Component, Input, signal } from '@angular/core';
import { BossDamage } from '../../../core/models/gamification.model';

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
      left: 50%;
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
  readonly visible = signal(false);
  readonly message = signal('');

  @Input() set trigger(value: { damage: BossDamage; tick: number } | null) {
    if (!value) return;
    const d = value.damage;
    const delta = Math.round(d.progressDelta);
    if (d.goalCompleted) {
      this.message.set(
        `${d.goalName} defeated! +${d.goalXpReward} XP` +
          (delta > 0 ? ` · ${delta} damage` : '')
      );
    } else {
      this.message.set(
        `${d.goalName}: −${delta || Math.round(d.progressDelta)} HP · ${d.healthRemaining}% remaining`
      );
    }
    this.visible.set(true);
    setTimeout(() => this.visible.set(false), 2600);
  }
}
