import { Component, input } from '@angular/core';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';

/**
 * Shared page chrome: title + optional subtitle.
 * Use on every main feature route for consistent hierarchy.
 */
@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [TranslatePipe],
  template: `
    <header class="page-head">
      <div class="page-head__text">
        <h1 class="page-title">{{ titleKey() | translate }}</h1>
        @if (subtitleKey()) {
          <p class="page-subtitle">{{ subtitleKey()! | translate }}</p>
        }
      </div>
      <ng-content />
    </header>
  `,
  styles: `
    .page-head {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 1rem;
      padding-bottom: 1rem;
      margin-bottom: 1.5rem;
      border-bottom: 1px solid var(--color-border, rgba(255, 255, 255, 0.08));
    }

    .page-head__text {
      min-width: 0;
    }

    .page-title {
      margin: 0;
      font-family: var(--font-display, var(--font-headline, sans-serif));
      font-size: clamp(1.5rem, 2.5vw, 1.9rem);
      font-weight: 700;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      color: var(--color-text, var(--color-on-surface, #fff));
      line-height: 1.15;
    }

    .page-subtitle {
      margin: 0.45rem 0 0;
      max-width: 36rem;
      font-size: 0.9rem;
      line-height: 1.5;
      color: var(--color-text-muted, var(--color-on-surface-variant, #9a9a9a));
      text-transform: none;
      letter-spacing: normal;
    }
  `,
})
export class PageHeaderComponent {
  /** i18n key for the H1 */
  readonly titleKey = input.required<string>();
  /** Optional i18n key for supporting line */
  readonly subtitleKey = input<string | null>(null);
}
