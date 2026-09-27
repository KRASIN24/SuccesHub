import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { AccountService } from '../../core/services/account.service';
import { ProfileService } from '../../core/services/profile.service';
import {
  AppPreferences,
  AppPreferencesService,
} from '../../core/services/app-preferences.service';
import { LocaleService } from '../../core/services/locale.service';
import { ThemeService } from '../../core/services/theme.service';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';
import { GlitchCheckboxComponent } from '../../shared/components/glitch-checkbox/glitch-checkbox.component';
import { GlitchButtonComponent } from '../../shared/components/glitch-button/glitch-button.component';
import { ThemeToggleComponent } from '../../shared/components/theme-toggle/theme-toggle.component';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { TranslateService } from '../../core/i18n/translate.service';

type AccountDialog = 'email' | 'password' | 'delete' | null;

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    LoadingSkeletonComponent,
    ErrorStateComponent,
    GlitchCheckboxComponent,
    GlitchButtonComponent,
    ThemeToggleComponent,
    TranslatePipe,
  ],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly accountService = inject(AccountService);
  protected readonly profileService = inject(ProfileService);
  private readonly prefsService = inject(AppPreferencesService);
  protected readonly localeService = inject(LocaleService);
  private readonly themeService = inject(ThemeService);
  private readonly i18n = inject(TranslateService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly saveSuccess = signal<string | null>(null);
  readonly saveError = signal<string | null>(null);
  readonly saving = signal(false);

  readonly dialog = signal<AccountDialog>(null);
  readonly accountBusy = signal(false);
  readonly accountError = signal<string | null>(null);
  readonly mfaEnabled = signal(false);

  draftDisplayName = '';
  draftEmail = '';
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  deleteConfirmation = '';

  readonly preferences = this.prefsService.preferences;
  readonly user = this.authService.currentUser;
  readonly profile = this.profileService.profile;

  ngOnInit(): void {
    this.draftDisplayName = this.profileService.profile()?.displayName ?? '';
    this.draftEmail = this.user()?.email ?? '';
    this.accountService.getStatus().subscribe({
      next: (status) => {
        this.mfaEnabled.set(status.mfaEnabled);
        if (status.email) {
          this.draftEmail = status.email;
        }
      },
      error: () => {
        /* status is best-effort; Settings still works */
      },
    });
    if (this.profileService.profile()) {
      this.loading.set(false);
    } else {
      this.profileService.getProfile().subscribe({
        next: (p) => {
          this.draftDisplayName = p.displayName ?? '';
          this.loading.set(false);
        },
        error: (err) => {
          console.error('Failed to load profile', err);
          this.error.set('Could not load profile data.');
          this.loading.set(false);
        },
      });
    }
  }

  setPreference(key: keyof AppPreferences, value: boolean): void {
    this.prefsService.update({ [key]: value });
    this.flashSuccess('Preferences saved.');
  }

  setDarkTheme(dark: boolean): void {
    this.themeService.setDark(dark);
    this.profileService.updatePreferences({ darkTheme: dark }).subscribe({
      next: () => this.flashSuccess(this.i18n.t('settings.prefsSaved')),
      error: () => {
        this.saveError.set('Could not save theme preference.');
        this.profileService.refreshProfile().subscribe();
      },
    });
  }

  onLocaleChange(code: string): void {
    this.localeService.setLocale(code);
    this.themeService.syncBridgeCookies();
    this.profileService.updatePreferences({ locale: code }).subscribe({
      next: () => this.flashSuccess(this.i18n.t('settings.languageSaved')),
      error: () => {
        this.saveError.set('Could not save language preference.');
        this.profileService.refreshProfile().subscribe();
      },
    });
  }

  saveIdentity(): void {
    const name = this.draftDisplayName.trim();
    if (!name) {
      this.saveError.set('Display name is required.');
      return;
    }
    if (name === this.profileService.profile()?.displayName) {
      this.flashSuccess('Identity already up to date.');
      return;
    }

    this.saving.set(true);
    this.saveError.set(null);

    this.profileService.updateDisplayName(name).subscribe({
      next: () => {
        this.saving.set(false);
        this.flashSuccess('Identity updated.');
      },
      error: (err) => {
        console.error('Failed to update display name', err);
        this.saving.set(false);
        this.saveError.set(err?.error?.message ?? 'Failed to save display name.');
      },
    });
  }

  openDialog(kind: Exclude<AccountDialog, null>): void {
    this.accountError.set(null);
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.deleteConfirmation = '';
    this.draftEmail = this.user()?.email ?? this.draftEmail;
    this.dialog.set(kind);
  }

  closeDialog(): void {
    this.dialog.set(null);
    this.accountError.set(null);
    this.accountBusy.set(false);
  }

  updateEmail(): void {
    this.openDialog('email');
  }

  changePassword(): void {
    this.openDialog('password');
  }

  setupTwoFactor(): void {
    this.accountBusy.set(true);
    this.accountError.set(null);
    this.accountService.mfaSetup().subscribe({
      next: (res) => {
        window.location.href = res.redirectUrl;
      },
      error: (err) => {
        console.error('Failed to start MFA setup', err);
        this.accountBusy.set(false);
        this.saveError.set(err?.error?.message ?? 'Could not start two-factor setup.');
      },
    });
  }

  deleteAccount(): void {
    this.openDialog('delete');
  }

  submitEmail(): void {
    const email = this.draftEmail.trim();
    if (!email) {
      this.accountError.set('Email is required.');
      return;
    }
    this.accountBusy.set(true);
    this.accountError.set(null);
    this.accountService.updateEmail(email).subscribe({
      next: () => {
        this.accountBusy.set(false);
        this.closeDialog();
        this.authService.loadCurrentUser().subscribe();
        this.flashSuccess('Email updated. Sign out and back in if the sidebar still shows the old address.');
      },
      error: (err) => {
        this.accountBusy.set(false);
        this.accountError.set(err?.error?.message ?? 'Failed to update email.');
      },
    });
  }

  submitPassword(): void {
    if (!this.currentPassword || !this.newPassword) {
      this.accountError.set('Fill in all password fields.');
      return;
    }
    if (this.newPassword.length < 8) {
      this.accountError.set('New password must be at least 8 characters.');
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.accountError.set('New password and confirmation do not match.');
      return;
    }
    this.accountBusy.set(true);
    this.accountError.set(null);
    this.accountService.changePassword(this.currentPassword, this.newPassword).subscribe({
      next: () => {
        this.accountBusy.set(false);
        this.closeDialog();
        this.flashSuccess('Password changed.');
      },
      error: (err) => {
        this.accountBusy.set(false);
        this.accountError.set(err?.error?.message ?? 'Failed to change password.');
      },
    });
  }

  submitDelete(): void {
    if (this.deleteConfirmation !== 'DELETE') {
      this.accountError.set('Type DELETE to confirm.');
      return;
    }
    this.accountBusy.set(true);
    this.accountError.set(null);
    this.accountService.deleteAccount('DELETE').subscribe({
      next: () => {
        this.authService.logout();
      },
      error: (err) => {
        this.accountBusy.set(false);
        this.accountError.set(err?.error?.message ?? 'Failed to delete account.');
      },
    });
  }

  private flashSuccess(message: string): void {
    this.saveSuccess.set(message);
    setTimeout(() => this.saveSuccess.set(null), 3000);
  }
}
