import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService } from '../../core/services/profile.service';
import {
  AppPreferences,
  AppPreferencesService,
} from '../../core/services/app-preferences.service';
import { LocaleService } from '../../core/services/locale.service';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { ErrorStateComponent } from '../../shared/components/error-state/error-state.component';
import { GlitchCheckboxComponent } from '../../shared/components/glitch-checkbox/glitch-checkbox.component';
import { GlitchButtonComponent } from '../../shared/components/glitch-button/glitch-button.component';
import { ThemeToggleComponent } from '../../shared/components/theme-toggle/theme-toggle.component';

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
  ],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent implements OnInit {
  private readonly authService = inject(AuthService);
  protected readonly profileService = inject(ProfileService);
  private readonly prefsService = inject(AppPreferencesService);
  protected readonly localeService = inject(LocaleService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly saveSuccess = signal<string | null>(null);
  readonly saveError = signal<string | null>(null);
  readonly saving = signal(false);

  draftDisplayName = '';

  readonly preferences = this.prefsService.preferences;
  readonly user = this.authService.currentUser;
  readonly profile = this.profileService.profile;

  ngOnInit(): void {
    this.draftDisplayName = this.profileService.profile()?.displayName ?? '';
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
    this.prefsService.setDarkTheme(dark);
    this.flashSuccess('Preferences saved.');
  }

  onLocaleChange(code: string): void {
    this.localeService.setLocale(code);
    this.flashSuccess('Language preference saved.');
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

  /** In-Settings email flow — wire to BFF/Keycloak in a later bite. */
  updateEmail(): void {
    this.flashSuccess('Update email will be available in Settings soon.');
  }

  /** In-Settings password flow — wire to BFF/Keycloak in a later bite. */
  changePassword(): void {
    this.flashSuccess('Change password will be available in Settings soon.');
  }

  /** In-Settings 2FA flow — wire to BFF/Keycloak in a later bite. */
  setupTwoFactor(): void {
    this.flashSuccess('Two-factor setup will be available in Settings soon.');
  }

  /** In-Settings delete flow — wire to BFF/Keycloak in a later bite. */
  deleteAccount(): void {
    this.flashSuccess('Account deletion will be available in Settings soon.');
  }

  private flashSuccess(message: string): void {
    this.saveSuccess.set(message);
    setTimeout(() => this.saveSuccess.set(null), 3000);
  }
}
