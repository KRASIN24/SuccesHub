export interface TaskCategory {
  id: string;
  name: string;
  tag: string;
  grantXp: boolean;
  muted: boolean;
  sortOrder: number;
}

export interface Task {
  id: string;
  categoryId?: string;
  categoryName?: string;
  title: string;
  description?: string;
  xpReward: number;
  status: 'TODO' | 'IN_PROGRESS' | 'DONE';
  metaLabel?: string;
  metaType?: 'urgent' | 'due' | 'ongoing' | 'time' | string;
  dueDate?: string;
  completedAt?: string;
  createdAt: string;
}
