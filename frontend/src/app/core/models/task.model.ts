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
  parentTaskId?: string | null;
  starred?: boolean;
  sortOrder?: number;
  dueDate?: string | null;
  reminderAt?: string | null;
}

export interface TaskReorderRequest {
  categoryId: string | null;
  parentTaskId: string | null;
  orderedIds: string[];
}

export type TaskSortMode = 'manual' | 'starred' | 'title' | 'due';

export const TASK_DURATION_PRESETS = [5, 15, 30, 45, 60, 90, 120] as const;

export interface Task {
  id: string;
  categoryId?: string;
  categoryName?: string;
  goalId?: string;
  parentTaskId?: string | null;
  starred: boolean;
  sortOrder: number;
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
  dueDate?: string | null;
  reminderAt?: string | null;
  completedAt?: string;
  createdAt: string;
}
