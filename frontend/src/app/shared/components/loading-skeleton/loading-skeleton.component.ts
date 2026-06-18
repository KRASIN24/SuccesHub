import { Component, input } from '@angular/core';

@Component({
  selector: 'app-loading-skeleton',
  standalone: true,
  template: `
    <div class="skeleton-container" [style.height]="height()" [style.width]="width()">
      <div class="shimmer"></div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
    }
    .skeleton-container {
      position: relative;
      overflow: hidden;
      background: rgba(255, 255, 255, 0.05);
      border-radius: 8px;
      border: 1px solid rgba(255, 255, 255, 0.03);
    }
    .shimmer {
      position: absolute;
      top: 0;
      right: 0;
      bottom: 0;
      left: 0;
      transform: translateX(-100%);
      background: linear-gradient(
        90deg,
        rgba(255, 255, 255, 0) 0%,
        rgba(255, 255, 255, 0.03) 20%,
        rgba(255, 255, 255, 0.08) 60%,
        rgba(255, 255, 255, 0) 100%
      );
      animation: shimmer 1.8s infinite;
    }
    @keyframes shimmer {
      100% {
        transform: translateX(100%);
      }
    }
  `]
})
export class LoadingSkeletonComponent {
  readonly height = input<string>('150px');
  readonly width = input<string>('100%');
}
