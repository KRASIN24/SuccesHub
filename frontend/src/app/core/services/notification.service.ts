import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { AppPreferencesService } from './app-preferences.service';
import { AppNotification, NotificationUnreadCount } from '../models/notification.model';
import { map, tap } from 'rxjs/operators';
import { Observable } from 'rxjs';

/**
 * Navbar bell inbox — recent durable game events (loot, achievements, streak, boss).
 * Rows are filtered by Settings → Notifications preferences (localStorage).
 */
@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly api = inject(ApiService);
  private readonly prefs = inject(AppPreferencesService);

  private readonly rawItems = signal<AppNotification[]>([]);

  readonly items = computed(() =>
    this.rawItems().filter((n) => this.prefs.isNotificationTypeEnabled(n.type))
  );

  readonly unreadCount = computed(
    () => this.items().filter((n) => !n.read).length
  );

  refresh(): void {
    // Fetch a wider window so disabled types do not starve the visible list.
    this.list(50).subscribe({
      next: (rows) => this.rawItems.set(rows),
      error: (err) => console.error('Failed to load notifications', err),
    });
  }

  list(limit = 20): Observable<AppNotification[]> {
    return this.api.get<AppNotification[]>('/notifications', { limit });
  }

  fetchUnreadCount(): Observable<NotificationUnreadCount> {
    return this.list(50).pipe(
      map((rows) => ({
        count: rows.filter(
          (n) => !n.read && this.prefs.isNotificationTypeEnabled(n.type)
        ).length,
      }))
    );
  }

  markRead(id: string): Observable<AppNotification> {
    return this.api.post<AppNotification>(`/notifications/${id}/read`, {}).pipe(
      tap((updated) => {
        this.rawItems.update((list) =>
          list.map((n) => (n.id === updated.id ? updated : n))
        );
      })
    );
  }

  markAllRead(): Observable<NotificationUnreadCount> {
    return this.api.post<NotificationUnreadCount>('/notifications/read-all', {}).pipe(
      tap(() => {
        this.rawItems.update((list) => list.map((n) => ({ ...n, read: true })));
      }),
      map(() => ({ count: 0 }))
    );
  }
}
