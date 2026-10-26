import { Component, computed, input } from '@angular/core';

/**
 * Maroon/gold numeric count pill — sidebar Cache, navbar notifications, loot pending.
 */
@Component({
  selector: 'app-count-badge',
  standalone: true,
  template: `{{ display() }}`,
  styleUrl: './count-badge.component.scss',
  host: {
    class: 'count-badge',
    '[class.count-badge--sm]': 'size() === "sm"',
    '[class.count-badge--md]': 'size() === "md"',
    '[attr.aria-hidden]': 'ariaHidden() ? "true" : null',
    '[attr.aria-label]': 'ariaLabel() || null',
  },
})
export class CountBadgeComponent {
  /** Raw count to show. */
  readonly count = input.required<number>();
  /** Cap display (e.g. 99 → "99+"). Omit for no cap. */
  readonly max = input<number | null>(null);
  /** sm = navbar (16px), md = sidebar / loot (20–24px). */
  readonly size = input<'sm' | 'md'>('md');
  readonly ariaLabel = input<string>('');
  readonly ariaHidden = input(false);

  readonly display = computed(() => {
    const n = this.count();
    const cap = this.max();
    if (cap != null && n > cap) {
      return `${cap}+`;
    }
    return String(n);
  });
}
