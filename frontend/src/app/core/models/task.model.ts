export interface TaskCategory {
  id: string;
  name: string;
  tag: string;
  grantXp: boolean;
  sortOrder: number;
}

export interface TaskCreateRequest {
  categoryId: string;
  title: string;
  description?: string;
  xpReward?: number;
  difficulty?: number;
  durationMinutes?: number;
  priority?: number;
  goalId?: string | null;
}

export interface Task {
  id: string;
  categoryId?: string;
  categoryName?: string;
  goalId?: string;
  title: string;
  description?: string;
  xpReward: number;
  difficulty: number;
  durationMinutes: number;
  priority: number;
  weeklyChallenge: boolean;
  scheduledDate?: string;
  status: 'TODO' | 'IN_PROGRESS' | 'DONE';
  metaLabel?: string;
  metaType?: 'urgent' | 'due' | 'ongoing' | 'time' | string;
  dueDate?: string;
  completedAt?: string;
  createdAt: string;
}
