export interface UserProfile {
  keycloakId: string;
  displayName: string;
  level: number;
  currentXp: number;
  nextLevelXp: number;
  currentStreak: number;
  globalRank: number;
  darkTheme: boolean;
  locale: string;
  notifyLootEarned: boolean;
  notifyAchievementUnlocked: boolean;
  notifyStreakMilestone: boolean;
  notifyCampaignDefeated: boolean;
  emailAchievementUnlocked: boolean;
  emailStreakMilestone: boolean;
  emailCampaignDefeated: boolean;
}
