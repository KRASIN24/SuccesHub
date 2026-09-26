export interface AppNotification {
  id: string;
  type: string;
  title: string;
  body: string;
  actionUrl: string | null;
  read: boolean;
  createdAt: string;
}

export interface NotificationUnreadCount {
  count: number;
}
