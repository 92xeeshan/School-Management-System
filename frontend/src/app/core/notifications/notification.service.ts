import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse, PagedResponse } from '../models/api.model';
import { AppNotification, UnreadCount } from './notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly unreadSubject = new BehaviorSubject<number>(0);
  readonly unreadCount$ = this.unreadSubject.asObservable();

  constructor(private http: HttpClient) {}

  get unreadCount(): number {
    return this.unreadSubject.value;
  }

  loadUnreadCount(): Observable<number> {
    return this.http.get<ApiResponse<UnreadCount>>('/api/notifications/unread-count').pipe(
      map((res) => res.data?.unreadCount ?? 0),
      tap((count) => this.unreadSubject.next(count))
    );
  }

  list(page = 0, size = 20, read?: boolean): Observable<PagedResponse<AppNotification>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (read !== undefined) {
      params = params.set('read', String(read));
    }
    return this.http
      .get<ApiResponse<PagedResponse<AppNotification>>>('/api/notifications', { params })
      .pipe(map((res) => res.data));
  }

  markRead(id: string): Observable<AppNotification> {
    return this.http.patch<ApiResponse<AppNotification>>(`/api/notifications/${id}/read`, {}).pipe(
      map((res) => res.data),
      tap(() => this.unreadSubject.next(Math.max(0, this.unreadSubject.value - 1)))
    );
  }

  markAllRead(): Observable<UnreadCount> {
    return this.http.patch<ApiResponse<UnreadCount>>('/api/notifications/read-all', {}).pipe(
      map((res) => res.data),
      tap(() => this.unreadSubject.next(0))
    );
  }

  setUnreadCount(count: number): void {
    this.unreadSubject.next(Math.max(0, count));
  }
}
