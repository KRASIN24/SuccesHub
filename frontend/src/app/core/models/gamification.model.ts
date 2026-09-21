import { Achievement } from './achievement.model';
import { Task } from './task.model';

export interface XpBreakdown {
  baseXp: number;
  streakBonus: number;
  firstTaskBonus: number;
  variableBonus: number;
  challengeBonus: number;
  totalXp: number;
  dailyXpRemaining: number;
}

export interface XpPreview {
  baseXp: number;
  streakBonus: number;
  firstTaskBonus: number;
  potentialVariableBonusMax: number;
  challengeBonus: number;
  estimatedTotalXp: number;
  dailyXpRemaining: number;
}

export interface BossDamage {
  goalId: string;
  goalName: string;
  progressDelta: number;
  healthRemaining: number;
  goalCompleted: boolean;
  goalXpReward: number;
}

export interface RewardEvent {
  xp: XpBreakdown;
  leveledUp: boolean;
  previousLevel: number;
  newLevel: number;
  variableBonusTriggered: boolean;
  bossDamage?: BossDamage | null;
  achievementsUnlocked: Achievement[];
  lootBoxEarned?: string | null;
}

export interface TaskCompletion {
  task: Task;
  reward: RewardEvent | null;
  updatedProfile: import('./profile.model').UserProfile;
}

export interface DailyStatus {
  missionsToday: number;
  xpEarnedToday: number;
  dailyXpRemaining: number;
  currentStreak: number;
  streakTier: string;
  streakShields: number;
  pendingCelebrations: boolean;
  /** Qualifying day already acknowledged via Celebrate. */
  daySealed: boolean;
  weeklyChallenges: Task[];
  scheduledToday: Task[];
}

export interface CloseDayResult {
  streakBefore: number;
  streakAfter: number;
  streakTier: string;
  achievementsUnlocked: Achievement[];
  lootBoxesEarned: string[];
  alreadyClosed: boolean;
}

export interface Forecast {
  nextStreakMilestone: number;
  nextStreakBonusRate: number;
  weeklyChallengeMinDifficulty: number;
  weeklyChallengeCount: number;
  streakShields: number;
}

export interface WeeklyInsight {
  tasksThisWeek: number;
  tasksLastWeek: number;
  hardTasksThisWeek: number;
  hardTasksLastWeek: number;
  xpThisWeek: number;
  xpLastWeek: number;
  hardTaskDeltaPercent: number;
  summaryText: string;
}

export interface RewardItem {
  id: string;
  key: string;
  label: string;
  type: string;
  rarity: string;
  icon: string;
  effect?: string | null;
}

export interface LootBox {
  id: string;
  source: string;
  boxType: string;
  status: string;
  createdAt: string;
  openedAt?: string | null;
  contents: RewardItem[];
}

export interface BoxType {
  id: string;
  name: string;
  source: string;
  feel: string;
  blurb: string;
  icon: string;
  commonWeight: number;
  rareWeight: number;
  legendaryWeight: number;
}

export interface InventoryItem {
  id: string;
  reward: RewardItem;
  quantity: number;
  equipped: boolean;
}

export interface GamificationClientConfig {
  enableLootDevGrants: boolean;
}

export type StreakDayStatus = 'COMPLETED' | 'MISSED' | 'SHIELDED' | 'TODAY' | 'FUTURE';

export interface StreakDay {
  /** ISO date string (yyyy-MM-dd). */
  date: string;
  status: StreakDayStatus;
  completedTasks: number;
}

export interface StreakCalendar {
  /** First day of the rendered month (yyyy-MM-dd). */
  month: string;
  currentStreak: number;
  streakTier: string;
  streakShields: number;
  shieldsAvailable: number;
  minTasksPerDay: number;
  days: StreakDay[];
}

export interface StreakActionResult {
  currentStreak: number;
  streakTier: string;
  streakShields: number;
  shieldsAvailable: number;
  message: string;
}

export interface UseItemResult {
  rewardKey: string;
  message: string;
  quantityRemaining: number;
  updatedProfile: import('./profile.model').UserProfile;
}
