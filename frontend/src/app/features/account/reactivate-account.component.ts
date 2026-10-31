import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { AccountService } from '../../core/services/account.service';
import { AuthService } from '../../core/services/auth.service';
import { GlitchButtonComponent } from '../../shared/components/glitch-button/glitch-button.component';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { TranslateService } from '../../core/i18n/translate.service';

@Component({
  selector: 'app-reactivate-account',
  standalone: true,
  imports: [DatePipe, GlitchButtonComponent, TranslatePipe],
  template: `
    <section class="reactivate" aria-labelledby="reactivate-title">
      <p class="reactivate__eyebrow">{{ 'account.reactivateEyebrow' | translate }}</p>
      <h1 id="reactivate-title" class="reactivate__title">{{ 'account.reactivateTitle' | translate }}</h1>
      <p class="reactivate__body">
        {{ 'account.reactivateBodyBefore' | translate }}
        <strong>{{ purgeAt() ? (purgeAt() | date: 'mediumDate') : ('account.reactivateBodyGrace' | translate) }}</strong>{{ 'account.reactivateBodyAfter' | translate }}
      </p>
      @if (error()) {
        <p class="reactivate__error" role="alert">{{ error() }}</p>
      }
      <div class="reactivate__actions">
        <app-glitch-button
          [label]="'account.reactivateCta' | translate"
          [disabled]="busy()"
          (pressed)="reactivate()"
        />
        <button type="button" class="ghost-btn" [disabled]="busy()" (click)="logout()">
          {{ 'account.logOut' | translate }}
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
      font-family: var(--font-headline, sans-serif);
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
      align-items: stretch;
    }
    .reactivate__actions .ghost-btn {
      width: 100%;
    }
    .reactivate__actions ::ng-deep app-glitch-button,
    .reactivate__actions ::ng-deep .glitch-btn {
      width: 100%;
    }
  `,
})
export class ReactivateAccountComponent implements OnInit {
  private readonly account = inject(AccountService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly i18n = inject(TranslateService);

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
      error: () => this.error.set(this.i18n.t('errors.syncFailed')),
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
        this.error.set(err?.error?.message ?? this.i18n.t('errors.syncFailed'));
      },
    });
  }

  logout(): void {
    this.auth.logout();
  }
}
