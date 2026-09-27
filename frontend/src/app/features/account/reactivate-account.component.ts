import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { AccountService } from '../../core/services/account.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-reactivate-account',
  standalone: true,
  imports: [DatePipe],
  template: `
    <section class="reactivate" aria-labelledby="reactivate-title">
      <p class="reactivate__eyebrow">Account status</p>
      <h1 id="reactivate-title" class="reactivate__title">Scheduled for deletion</h1>
      <p class="reactivate__body">
        Your account is deactivated. Progress is kept until
        <strong>{{ purgeAt() ? (purgeAt() | date: 'mediumDate') : 'the end of the grace period' }}</strong>,
        then it will be permanently deleted.
      </p>
      @if (error()) {
        <p class="reactivate__error" role="alert">{{ error() }}</p>
      }
      <div class="reactivate__actions">
        <button type="button" class="reactivate__primary" [disabled]="busy()" (click)="reactivate()">
          Reactivate account
        </button>
        <button type="button" class="reactivate__secondary" [disabled]="busy()" (click)="logout()">
          Log out
        </button>
      </div>
    </section>
  `,
  styles: `
    .reactivate {
      max-width: 28rem;
      margin: 4rem auto;
      padding: 2rem 1.5rem;
      text-align: center;
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
    .reactivate__eyebrow {
      margin: 0;
      font-size: 0.6875rem;
      letter-spacing: 0.2em;
      text-transform: uppercase;
      color: var(--color-on-surface-variant, #c9b896);
    }
    .reactivate__title {
      margin: 0;
      font-family: var(--font-headline);
      font-size: 1.5rem;
      color: var(--color-primary, #f5be42);
    }
    .reactivate__body {
      margin: 0;
      line-height: 1.5;
      color: var(--color-on-surface-variant, #c9b896);
    }
    .reactivate__error {
      margin: 0;
      color: #ff8a80;
      font-size: 0.875rem;
    }
    .reactivate__actions {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      margin-top: 0.5rem;
    }
    .reactivate__primary,
    .reactivate__secondary {
      border-radius: 0.75rem;
      padding: 0.85rem 1.25rem;
      font-weight: 600;
      cursor: pointer;
    }
    .reactivate__primary {
      border: none;
      background: linear-gradient(135deg, #f5be42, #e0a82e);
      color: #402d00;
    }
    .reactivate__secondary {
      border: 1px solid rgba(211, 197, 175, 0.35);
      background: transparent;
      color: var(--color-on-surface-variant, #c9b896);
    }
    .reactivate__primary:disabled,
    .reactivate__secondary:disabled {
      opacity: 0.55;
      cursor: wait;
    }
  `,
})
export class ReactivateAccountComponent implements OnInit {
  private readonly account = inject(AccountService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly purgeAt = signal<string | null>(null);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.account.getStatus().subscribe({
      next: (status) => {
        this.purgeAt.set(status.purgeAt);
        if (!status.deactivated) {
          void this.router.navigateByUrl('/dashboard');
        }
      },
      error: () => this.error.set('Could not load account status.'),
    });
  }

  reactivate(): void {
    this.busy.set(true);
    this.error.set(null);
    this.account.reactivate().subscribe({
      next: () => {
        this.busy.set(false);
        void this.router.navigateByUrl('/dashboard');
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(err?.error?.message ?? 'Failed to reactivate account.');
      },
    });
  }

  logout(): void {
    this.auth.logout();
  }
}
