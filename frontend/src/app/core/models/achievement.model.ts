export interface Achievement {
  id: string;
  key: string;
  label: string;
  icon: string;
  description: string;
  locked: boolean;
  unlockedAt?: string;
}
