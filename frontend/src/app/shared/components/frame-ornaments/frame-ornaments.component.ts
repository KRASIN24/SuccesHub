import { Component, input } from '@angular/core';

/**
 * Steam-style decorative ornaments that sit outside a hex avatar/frame.
 * Driven by reward keys (FRAME_BRONZE_CIRCUIT, FRAME_SILVER_SIGIL, FRAME_GOLD_SOVEREIGN, …).
 */
@Component({
  selector: 'app-frame-ornaments',
  standalone: true,
  template: `
    @if (frameKey(); as key) {
      <div
        class="frame-ornaments"
        [attr.data-frame]="key"
        [class.frame-ornaments--sm]="size() === 'sm'"
        [class.frame-ornaments--lg]="size() === 'lg'"
        aria-hidden="true"
      >
        @switch (key) {
          @case ('FRAME_BRONZE_CIRCUIT') {
            <!-- Devil horns -->
            <svg class="ornament ornament--horns" viewBox="0 0 120 140" fill="none">
              <path
                class="ornament__horn ornament__horn--left"
                d="M34 48 C28 28, 18 14, 10 8 C22 18, 30 30, 36 46 Z"
                fill="currentColor"
              />
              <path
                class="ornament__horn ornament__horn--right"
                d="M86 48 C92 28, 102 14, 110 8 C98 18, 90 30, 84 46 Z"
                fill="currentColor"
              />
              <path
                d="M32 40 C26 26, 18 16, 12 12"
                stroke="#5c2a12"
                stroke-width="2"
                stroke-linecap="round"
                opacity="0.45"
              />
              <path
                d="M88 40 C94 26, 102 16, 108 12"
                stroke="#5c2a12"
                stroke-width="2"
                stroke-linecap="round"
                opacity="0.45"
              />
            </svg>
          }
          @case ('FRAME_SILVER_SIGIL') {
            <!-- Bunny ears — each ear is one group so outer + inner move together -->
            <svg class="ornament ornament--ears" viewBox="0 0 120 140" fill="none">
              <g class="ornament__ear ornament__ear--left">
                <ellipse
                  cx="38"
                  cy="28"
                  rx="11"
                  ry="28"
                  fill="currentColor"
                />
                <ellipse
                  cx="38"
                  cy="30"
                  rx="5"
                  ry="16"
                  fill="#f2e8ef"
                  opacity="0.85"
                />
              </g>
              <g class="ornament__ear ornament__ear--right">
                <ellipse
                  cx="82"
                  cy="28"
                  rx="11"
                  ry="28"
                  fill="currentColor"
                />
                <ellipse
                  cx="82"
                  cy="30"
                  rx="5"
                  ry="16"
                  fill="#f2e8ef"
                  opacity="0.85"
                />
              </g>
            </svg>
          }
          @case ('FRAME_GOLD_SOVEREIGN') {
            <!-- Crown only — no side lines -->
            <svg class="ornament ornament--sovereign" viewBox="0 0 120 140" fill="none">
              <path
                d="M38 28 L46 12 L60 22 L74 12 L82 28 Z"
                fill="currentColor"
              />
              <path
                d="M38 28 H82"
                stroke="currentColor"
                stroke-width="3"
                stroke-linecap="round"
              />
              <circle cx="46" cy="12" r="3" fill="currentColor" />
              <circle cx="60" cy="10" r="3.5" fill="#fff6c8" />
              <circle cx="74" cy="12" r="3" fill="currentColor" />
            </svg>
          }
          @case ('FRAME_GOLD') {
            <svg class="ornament ornament--sovereign" viewBox="0 0 120 140" fill="none">
              <path
                d="M40 26 L50 14 L60 22 L70 14 L80 26 Z"
                fill="currentColor"
              />
              <path d="M40 26 H80" stroke="currentColor" stroke-width="3" stroke-linecap="round" />
            </svg>
          }
          @default {
            <svg class="ornament" viewBox="0 0 120 140" fill="none">
              <circle cx="20" cy="40" r="4" fill="currentColor" />
              <circle cx="100" cy="40" r="4" fill="currentColor" />
            </svg>
          }
        }
      </div>
    }
  `,
  styles: `
    :host {
      position: absolute;
      inset: 0;
      pointer-events: none;
      z-index: 3;
      display: block;
    }

    .frame-ornaments {
      position: absolute;
      inset: -14% -10% -4%;
      color: var(--color-gold-bright, #f6cf63);
    }

    .frame-ornaments--sm {
      inset: -16% -12% -6%;
    }

    .frame-ornaments--lg {
      inset: -12% -8% -2%;
    }

    .ornament {
      width: 100%;
      height: 100%;
      overflow: visible;
    }

    .frame-ornaments[data-frame='FRAME_BRONZE_CIRCUIT'] {
      color: #c45c2c;
    }

    .frame-ornaments[data-frame='FRAME_SILVER_SIGIL'] {
      color: #e8eef4;
    }

    .frame-ornaments[data-frame='FRAME_GOLD_SOVEREIGN'],
    .frame-ornaments[data-frame='FRAME_GOLD'] {
      color: #f6cf63;
    }

    .ornament__ear--left {
      transform-box: fill-box;
      transform-origin: center bottom;
      animation: ear-wiggle-left 2.8s ease-in-out infinite;
    }

    .ornament__ear--right {
      transform-box: fill-box;
      transform-origin: center bottom;
      animation: ear-wiggle-right 2.8s ease-in-out infinite 0.15s;
    }

    .ornament__horn--left {
      transform-origin: 34px 46px;
      animation: horn-tilt-left 3.6s ease-in-out infinite;
    }

    .ornament__horn--right {
      transform-origin: 86px 46px;
      animation: horn-tilt-right 3.6s ease-in-out infinite;
    }

    @keyframes horn-tilt-left {
      0%,
      100% {
        transform: rotate(0deg);
      }
      50% {
        transform: rotate(-4deg);
      }
    }

    @keyframes horn-tilt-right {
      0%,
      100% {
        transform: rotate(0deg);
      }
      50% {
        transform: rotate(4deg);
      }
    }

    @keyframes ear-wiggle-left {
      0%,
      100% {
        transform: rotate(-18deg);
      }
      50% {
        transform: rotate(-26deg);
      }
    }

    @keyframes ear-wiggle-right {
      0%,
      100% {
        transform: rotate(18deg);
      }
      50% {
        transform: rotate(26deg);
      }
    }

    @media (prefers-reduced-motion: reduce) {
      .ornament__horn--left,
      .ornament__horn--right,
      .ornament__ear--left,
      .ornament__ear--right {
        animation: none;
      }

      .ornament__ear--left {
        transform: rotate(-18deg);
      }

      .ornament__ear--right {
        transform: rotate(18deg);
      }
    }
  `,
})
export class FrameOrnamentsComponent {
  /** Reward definition key, e.g. FRAME_GOLD_SOVEREIGN. */
  readonly frameKey = input<string | null>(null);
  /** sm = collection preview, md = profile, lg = home hero. */
  readonly size = input<'sm' | 'md' | 'lg'>('md');
}
