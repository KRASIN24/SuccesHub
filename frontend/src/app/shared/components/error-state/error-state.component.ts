import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-error-state',
  standalone: true,
  template: `
    <div class="error-container">
      <span class="material-icons error-icon">error_outline</span>
      <p class="error-message">{{ message() }}</p>
      @if (showRetry()) {
        <button class="retry-btn" (click)="retry.emit()">
          <span class="material-icons btn-icon">replay</span>
          Retry
        </button>
      }
    </div>
  `,
  styles: [`
    .error-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 24px;
      text-align: center;
      background: rgba(239, 68, 68, 0.05);
      border: 1px solid rgba(239, 68, 68, 0.1);
      border-radius: 12px;
      backdrop-filter: blur(8px);
      margin: 16px 0;
    }
    .error-icon {
      font-size: 36px;
      color: #ef4444;
      margin-bottom: 12px;
    }
    .error-message {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.7);
      margin: 0 0 16px 0;
      font-family: inherit;
    }
    .retry-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.2);
      color: #fff;
      padding: 8px 16px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 13px;
      font-weight: 500;
      transition: all 0.2s ease;
    }
    .retry-btn:hover {
      background: rgba(255, 255, 255, 0.15);
      border-color: rgba(255, 255, 255, 0.3);
    }
    .btn-icon {
      font-size: 16px;
    }
  `]
})
export class ErrorStateComponent {
  readonly message = input<string>('Failed to synchronize status with the sovereign registry.');
  readonly showRetry = input<boolean>(true);
  readonly retry = output<void>();
}
