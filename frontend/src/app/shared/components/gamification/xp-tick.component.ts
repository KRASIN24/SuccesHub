import { Component, Input, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-xp-tick',
  standalone: true,
  imports: [DecimalPipe],
  template: `
    @if (visible()) {
      <div class="xp-tick">+{{ amount() | number }} XP</div>
    }
  `,
  styles: [`
    .xp-tick {
      position: fixed;
      top: 40%;
      left: 50%;
      transform: translateX(-50%);
      font-size: 2rem;
      font-weight: 700;
      color: var(--color-gold, #d4af37);
      animation: xpTick 0.3s ease-out forwards;
      pointer-events: none;
      z-index: 1000;
    }
    @keyframes xpTick {
      0% { opacity: 0; transform: translateX(-50%) translateY(10px); }
      50% { opacity: 1; }
      100% { opacity: 0; transform: translateX(-50%) translateY(-20px); }
    }
  `],
})
export class XpTickComponent {
  readonly visible = signal(false);
  readonly amount = signal(0);

  @Input() set trigger(value: { xp: number; tick: number } | null) {
    if (!value) return;
    this.amount.set(value.xp);
    this.visible.set(true);
    setTimeout(() => this.visible.set(false), 300);
  }
}
