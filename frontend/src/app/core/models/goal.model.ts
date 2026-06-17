export interface Goal {
  id: string;
  name: string;
  tier?: string;
  targetDescription?: string;
  targetValue: number;
  currentProgress: number;
  healthRemaining: number;
  xpReward: number;
  icon?: string;
  status: 'ACTIVE' | 'COMPLETED';
  featured: boolean;
  slainLabel?: string;
  completedAt?: string;
  createdAt: string;
}

export interface GoalSummary {
  totalGoals: number;
  completedGoals: number;
  overallPercent: number;
}
