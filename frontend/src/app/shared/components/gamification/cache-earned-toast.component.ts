import { Component, Input, signal } from '@angular/core';

@Component({
  selector: 'app-cache-earned-toast',
  standalone: true,
  template: `
    @if (visible()) {
      <div class="cache-earned-toast">
        {{ message() }}
      </div>
    }
  `,
  styles: [`
    .cache-earned-toast {
      position: fixed;
      top: 18%;
      left: 50%;
      transform: translateX(-50%);
      max-width: min(90vw, 420px);
      padding: 0.85rem 1.25rem;
      border-radius: 12px;
      border: 1px solid rgba(245, 190, 66, 0.35);
      background: rgba(20, 12, 8, 0.92);
      color: var(--color-gold-bright, #f5be42);
      font-size: 0.95rem;
      font-weight: 600;
      text-align: center;
      box-shadow: 0 12px 40px rgba(0, 0, 0, 0.45);
      animation: cacheEarnedToast 2.5s ease-out forwards;
      pointer-events: none;
      z-index: 1000;
    }

    @keyframes cacheEarnedToast {
      0% { opacity: 0; transform: translateX(-50%) translateY(12px); }
      12% { opacity: 1; transform: translateX(-50%) translateY(0); }
      78% { opacity: 1; }
      100% { opacity: 0; transform: translateX(-50%) translateY(-10px); }
    }
  `],
})
export class CacheEarnedToastComponent {
  readonly visible = signal(false);
  readonly message = signal('');

  @Input() set trigger(value: { count: number; tick: number } | null) {
    if (!value) return;
    const label = value.count === 1 ? 'Cache earned' : `${value.count} caches earned`;
    this.message.set(`${label} — open on Celestial Cache`);
    this.visible.set(true);
    setTimeout(() => this.visible.set(false), 2500);
  }
}
