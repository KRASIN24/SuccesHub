import { Injectable, signal } from '@angular/core';

export interface AppPreferences {
  /** Prefer dark orbital theme (only dark is fully styled today). */
  darkTheme: boolean;
  soundEffects: boolean;
  animationsEnabled: boolean;
  showStreakReminder: boolean;
  dailyGoalReminder: boolean;
  weeklySummary: boolean;
  /** BCP 47 locale from {@link SUPPORTED_LOCALES}, e.g. en-US */
  locale: string;
}

const PREFS_KEY = 'successhub_prefs';

const DEFAULT_PREFS: AppPreferences = {
  darkTheme: true,
  soundEffects: true,
  animationsEnabled: true,
  showStreakReminder: true,
  dailyGoalReminder: true,
  weeklySummary: false,
  locale: 'en-US',
};

/**
 * Client-side app preferences persisted in localStorage.
 * Shared so Settings and feature code (e.g. loot SFX) read the same flags.
 */
@Injectable({ providedIn: 'root' })
export class AppPreferencesService {
  private readonly prefsState = signal<AppPreferences>(this.read());

  readonly preferences = this.prefsState.asReadonly();

  soundEffectsEnabled(): boolean {
    return this.prefsState().soundEffects;
  }

  animationsEnabled(): boolean {
    return this.prefsState().animationsEnabled;
  }

  update(partial: Partial<AppPreferences>): AppPreferences {
    const next = { ...this.prefsState(), ...partial };
    this.prefsState.set(next);
    this.write(next);
    return next;
  }

  toggle(key: keyof AppPreferences): AppPreferences {
    return this.update({ [key]: !this.prefsState()[key] });
  }

  setDarkTheme(dark: boolean): AppPreferences {
    return this.update({ darkTheme: dark });
  }

  private read(): AppPreferences {
    try {
      const raw = localStorage.getItem(PREFS_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as Partial<AppPreferences> & { compactView?: boolean };
        // Migrate older compactView key if present.
        const { compactView: _dropped, ...rest } = parsed;
        return { ...DEFAULT_PREFS, ...rest };
      }
    } catch {
      // ignore corrupt storage
    }
    return { ...DEFAULT_PREFS };
  }

  private write(prefs: AppPreferences): void {
    try {
      localStorage.setItem(PREFS_KEY, JSON.stringify(prefs));
    } catch {
      // Storage not available
    }
  }
}
