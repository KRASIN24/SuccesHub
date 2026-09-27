import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { Observable, tap } from 'rxjs';
import { UserProfile } from '../models/profile.model';
import { AppPreferencesService } from './app-preferences.service';
import { ThemeService } from './theme.service';
import { LocaleService } from './locale.service';

@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  private readonly api = inject(ApiService);
  private readonly prefs = inject(AppPreferencesService);
  private readonly theme = inject(ThemeService);
  private readonly locale = inject(LocaleService);
  private readonly profileState = signal<UserProfile | null>(null);
  private hydrating = false;

  /** Live profile shared across navbar, sidebar, and gamification flows. */
  readonly profile = this.profileState.asReadonly();

  getProfile(): Observable<UserProfile> {
    return this.api.get<UserProfile>('/profile').pipe(
      tap((profile) => {
        this.profileState.set(profile);
        void this.hydrateUiPreferences(profile);
      })
    );
  }

  applyProfile(profile: UserProfile): void {
    this.profileState.set(profile);
    void this.hydrateUiPreferences(profile);
  }

  refreshProfile(): Observable<UserProfile> {
    return this.getProfile();
  }

  updateDisplayName(displayName: string): Observable<UserProfile> {
    return this.api
      .patch<UserProfile>('/profile/display-name', { displayName })
      .pipe(tap((profile) => this.profileState.set(profile)));
  }

  updatePreferences(body: {
    darkTheme?: boolean;
    locale?: string;
  }): Observable<UserProfile> {
    return this.api
      .patch<UserProfile>('/profile/preferences', body)
      .pipe(
        tap((profile) => {
          this.profileState.set(profile);
          // Already applied locally; keep prefs aligned with server response.
          this.hydrating = true;
          this.prefs.update({
            darkTheme: profile.darkTheme,
            locale: profile.locale,
          });
          this.theme.applyFromPreference();
          void this.locale.hydrate(profile.locale).finally(() => {
            this.hydrating = false;
          });
        })
      );
  }

  private async hydrateUiPreferences(profile: UserProfile): Promise<void> {
    if (this.hydrating) {
      return;
    }
    this.hydrating = true;
    try {
      this.prefs.update({
        darkTheme: profile.darkTheme ?? true,
        locale: profile.locale ?? 'en-US',
      });
      this.theme.applyFromPreference();
      await this.locale.hydrate(profile.locale ?? 'en-US');
    } finally {
      this.hydrating = false;
    }
  }
}
