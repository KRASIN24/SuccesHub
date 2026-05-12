import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

@Component({
  selector: 'app-placeholder',
  standalone: true,
  imports: [MatIconModule, MatButtonModule],
  template: `
    <div class="placeholder-page">
      <div class="placeholder-content">
        <div class="placeholder-icon">
          <mat-icon>{{ icon() }}</mat-icon>
        </div>
        <h2 class="placeholder-title">{{ title() }}</h2>
        <p class="placeholder-desc">
          This section is coming soon. Stay tuned!
        </p>
        <button mat-flat-button color="primary">Notify me when ready</button>
      </div>
    </div>
  `,
  styles: [`
    .placeholder-page {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: calc(100vh - 64px);
      padding: 2rem;
    }
    .placeholder-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: 1rem;
      max-width: 400px;
    }
    .placeholder-icon {
      width: 80px;
      height: 80px;
      border-radius: 20px;
      background: #eff6ff;
      display: flex;
      align-items: center;
      justify-content: center;
      mat-icon {
        font-size: 40px;
        width: 40px;
        height: 40px;
        color: #3b82f6;
      }
    }
    .placeholder-title {
      font-size: 1.5rem;
      font-weight: 700;
      color: #1e293b;
      margin: 0;
    }
    .placeholder-desc {
      color: #64748b;
      margin: 0;
      font-size: 0.9375rem;
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
