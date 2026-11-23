import { Injectable, signal } from '@angular/core';

export interface AppPreferences {
  /** Prefer dark orbital theme (only dark is fully styled today). */
  darkTheme: boolean;
  soundEffects: boolean;
  animationsEnabled: boolean;
  /** Master switch for in-app game notifications (bell / toasts). */
  notifyInApp: boolean;
  /** Master switch for transactional game emails. */
  emailNotifications: boolean;
  /** BCP 47 locale from {@link SUPPORTED_LOCALES}, e.g. en-US */
  locale: string;
}

const PREFS_KEY = 'successhub_prefs';

const DEFAULT_PREFS: AppPreferences = {
  darkTheme: true,
  soundEffects: true,
  animationsEnabled: true,
  notifyInApp: true,
  emailNotifications: true,
  locale: 'en-US',
};

/**
 * Client-side app preferences persisted in localStorage and hydrated from the profile API.
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
    if (type === 'TASK_REMINDER') {
      return true;
    }
    if (
      type === 'LOOT_EARNED' ||
      type === 'ACHIEVEMENT_UNLOCKED' ||
      type === 'STREAK_MILESTONE' ||
      type === 'CAMPAIGN_DEFEATED'
    ) {
      return this.prefsState().notifyInApp;
    }
    return true;
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
          notifyLootEarned?: boolean;
          notifyAchievementUnlocked?: boolean;
          notifyStreakMilestone?: boolean;
          notifyBossDefeated?: boolean;
          emailAchievementUnlocked?: boolean;
          emailStreakMilestone?: boolean;
          emailCampaignDefeated?: boolean;
        };
        const {
          compactView: _dropped,
          showStreakReminder,
          dailyGoalReminder: _daily,
          weeklySummary: _weekly,
          notifyLootEarned,
          notifyAchievementUnlocked,
          notifyStreakMilestone,
          notifyBossDefeated,
          emailAchievementUnlocked,
          emailStreakMilestone,
          emailCampaignDefeated,
          ...rest
        } = parsed;
        const migrated: Partial<AppPreferences> = { ...rest };
        if (rest.notifyInApp === undefined) {
          const legacyInApp = [
            notifyLootEarned,
            notifyAchievementUnlocked,
            notifyStreakMilestone,
            notifyBossDefeated,
            showStreakReminder,
          ].find((v) => v !== undefined);
          if (legacyInApp !== undefined) {
            migrated.notifyInApp =
              (notifyLootEarned ?? true) ||
              (notifyAchievementUnlocked ?? true) ||
              (notifyStreakMilestone ?? showStreakReminder ?? true) ||
              (notifyBossDefeated ?? true);
          }
        }
        if (rest.emailNotifications === undefined) {
          const legacyEmail = [
            emailAchievementUnlocked,
            emailStreakMilestone,
            emailCampaignDefeated,
          ].find((v) => v !== undefined);
          if (legacyEmail !== undefined) {
            migrated.emailNotifications =
              (emailAchievementUnlocked ?? true) ||
              (emailStreakMilestone ?? true) ||
              (emailCampaignDefeated ?? true);
          }
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
