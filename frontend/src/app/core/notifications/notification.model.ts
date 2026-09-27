export interface AppNotification {
  id: string;
  title: string;
  message: string;
  category: string;
  actionUrl: string | null;
  read: boolean;
  createdAt: string;
  readAt: string | null;
}

export interface UnreadCount {
  unreadCount: number;
}
