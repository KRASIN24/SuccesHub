import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

@Component({
  selector: 'app-placeholder',
  standalone: true,
  imports: [],
  template: `
    <div class="placeholder-page">
      <div class="placeholder-content">
        <div class="placeholder-icon">
          <span class="material-icons">{{ icon() }}</span>
        </div>
        <h2 class="placeholder-title">{{ title() }}</h2>
        <p class="placeholder-desc">
          This protocol is under construction. The Sovereign Interface will unlock it soon.
        </p>
        <button type="button" class="gold-btn">Notify Me When Ready</button>
      </div>
    </div>
  `,
  styles: [`
    .placeholder-page {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: calc(100vh - var(--toolbar-height, 80px));
      padding: 2rem;
    }
    .placeholder-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: 1rem;
      max-width: 420px;
    }
    .placeholder-icon {
      width: 88px;
      height: 96px;
      clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
      background: rgba(232, 179, 62, 0.1);
      border: 1px solid rgba(232, 179, 62, 0.35);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 0.5rem;
    }
    .placeholder-icon .material-icons {
      font-size: 36px;
      color: var(--color-gold);
    }
    .placeholder-title {
      font-size: 1.6rem;
      font-weight: 700;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      color: var(--color-text);
      margin: 0;
    }
    .placeholder-desc {
      color: var(--color-text-muted);
      margin: 0 0 0.5rem;
      font-size: 0.9rem;
      line-height: 1.6;
    }
  `],
})
export class PlaceholderComponent {
  private readonly route = inject(ActivatedRoute);

  readonly title = toSignal(
    this.route.data.pipe(map((d) => (d['title'] as string) || 'Coming Soon')),
    { initialValue: 'Coming Soon' }
  );

  readonly icon = toSignal(
    this.route.data.pipe(map((d) => (d['icon'] as string) || 'construction')),
    { initialValue: 'construction' }
  );
}
