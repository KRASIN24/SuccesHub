import { Component, Input, signal } from '@angular/core';

@Component({
  selector: 'app-level-up-overlay',
  standalone: true,
  template: `
    @if (visible()) {
      <div class="level-up-overlay" (click)="dismiss()">
        <div class="level-up-card">
          <span class="material-icons">emoji_events</span>
          <h2>Level Up!</h2>
          <p>Level {{ level() }}</p>
        </div>
      </div>
    }
  `,
  styles: [`
    .level-up-overlay {
      position: fixed;
      inset: 0;
      background: rgba(0,0,0,0.75);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1100;
      animation: fadeIn 0.2s ease;
    }
    .level-up-card {
      text-align: center;
      padding: 48px;
      background: linear-gradient(135deg, #1a1a2e, #16213e);
      border: 2px solid var(--color-gold, #d4af37);
      border-radius: 16px;
    }
    .level-up-card .material-icons { font-size: 64px; color: var(--color-gold, #d4af37); }
    .level-up-card h2 { margin: 16px 0 8px; font-size: 2rem; }
    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
  `],
})
export class LevelUpOverlayComponent {
  readonly visible = signal(false);
  readonly level = signal(1);

  @Input() set trigger(value: { level: number; tick: number } | null) {
    if (!value) return;
    this.level.set(value.level);
    this.visible.set(true);
    setTimeout(() => this.visible.set(false), 2000);
  }

  dismiss(): void {
    this.visible.set(false);
  }
}
