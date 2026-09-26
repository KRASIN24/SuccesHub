import { Injectable, signal } from '@angular/core';

export type NotificationPrefKey =
  | 'notifyLootEarned'
  | 'notifyAchievementUnlocked'
  | 'notifyStreakMilestone'
  | 'notifyBossDefeated';

export interface AppPreferences {
  /** Prefer dark orbital theme (only dark is fully styled today). */
  darkTheme: boolean;
  soundEffects: boolean;
  animationsEnabled: boolean;
  /** In-app bell: loot box earned (achievement / weekly / manual grants). */
  notifyLootEarned: boolean;
  /** In-app bell: achievement unlocked. */
  notifyAchievementUnlocked: boolean;
  /** In-app bell: streak milestone cache. */
  notifyStreakMilestone: boolean;
  /** In-app bell: boss / goal slain. */
  notifyBossDefeated: boolean;
  /** BCP 47 locale from {@link SUPPORTED_LOCALES}, e.g. en-US */
  locale: string;
}

const PREFS_KEY = 'successhub_prefs';

const DEFAULT_PREFS: AppPreferences = {
  darkTheme: true,
  soundEffects: true,
  animationsEnabled: true,
  notifyLootEarned: true,
  notifyAchievementUnlocked: true,
  notifyStreakMilestone: true,
  notifyBossDefeated: true,
  locale: 'en-US',
};

/** Maps API {@code NotificationType} to a Settings preference key. */
export const NOTIFICATION_TYPE_PREF: Record<string, NotificationPrefKey> = {
  LOOT_EARNED: 'notifyLootEarned',
  ACHIEVEMENT_UNLOCKED: 'notifyAchievementUnlocked',
  STREAK_MILESTONE: 'notifyStreakMilestone',
  BOSS_DEFEATED: 'notifyBossDefeated',
};

/**
 * Client-side app preferences persisted in localStorage.
 * Shared so Settings and feature code (loot SFX, notification bell) read the same flags.
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

  /** Whether the navbar bell should surface this notification type. */
  isNotificationTypeEnabled(type: string): boolean {
    const key = NOTIFICATION_TYPE_PREF[type];
    if (!key) {
      return true;
    }
    return this.prefsState()[key];
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
        const parsed = JSON.parse(raw) as Partial<AppPreferences> & {
          compactView?: boolean;
          showStreakReminder?: boolean;
          dailyGoalReminder?: boolean;
          weeklySummary?: boolean;
        };
        const {
          compactView: _dropped,
          showStreakReminder,
          dailyGoalReminder: _daily,
          weeklySummary: _weekly,
          ...rest
        } = parsed;
        const migrated: Partial<AppPreferences> = { ...rest };
        // Old Settings “Streak Alert” → streak milestone bell preference.
        if (rest.notifyStreakMilestone === undefined && showStreakReminder !== undefined) {
          migrated.notifyStreakMilestone = showStreakReminder;
        }
        return { ...DEFAULT_PREFS, ...migrated };
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
