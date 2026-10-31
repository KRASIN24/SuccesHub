export interface Goal {
  id: string;
  name: string;
  tier?: string;
  targetDescription?: string;
  targetValue: number;
  currentProgress: number;
  pendingDamage: number;
  healthRemaining: number;
  xpReward: number;
  icon?: string;
  status: 'ACTIVE' | 'COMPLETED' | 'ABANDONED';
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

export interface GoalCreateRequest {
  name: string;
  tier?: string;
  targetDescription?: string;
  targetValue: number;
  currentProgress?: number;
  xpReward: number;
  icon?: string;
  featured?: boolean;
}

export interface GoalUpdateRequest {
  name: string;
  tier?: string;
  targetDescription?: string;
  targetValue: number;
  currentProgress: number;
  xpReward: number;
  icon?: string;
  status?: string;
  featured: boolean;
}

export interface GoalSubscription {
  id: string;
  goalId: string;
  categoryId: string;
  categoryName: string;
  createdAt: string;
}
