import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse } from '../../core/models/api.model';
import { DashboardNotice, DashboardSummary, SchoolEvent, SchoolEventPayload } from './dashboard.model';

interface BackendNotice {
  id: string;
  title: string;
  body: string;
  priority: string;
  publishAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(private http: HttpClient) {}

  summary(): Observable<DashboardSummary | null> {
    return this.http.get<ApiResponse<DashboardSummary>>('/api/dashboard').pipe(
      map((res) => res.data),
      catchError(() => of(null))
    );
  }

  events(from: string, to: string): Observable<SchoolEvent[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<ApiResponse<SchoolEvent[]>>('/api/events', { params }).pipe(
      map((res) => res.data ?? []),
      catchError(() => of([] as SchoolEvent[]))
    );
  }

  notices(): Observable<DashboardNotice[]> {
    return this.http.get<ApiResponse<BackendNotice[]>>('/api/notices/published').pipe(
      map((res) =>
        (res.data ?? []).map((n) => ({
          id: n.id,
          title: n.title,
          body: n.body,
          priority: n.priority,
          publishAt: n.publishAt,
        }))
      ),
      catchError(() => of([] as DashboardNotice[]))
    );
  }

  createEvent(payload: SchoolEventPayload): Observable<SchoolEvent | null> {
    return this.http.post<ApiResponse<SchoolEvent>>('/api/events', payload).pipe(
      map((res) => res.data ?? null),
      catchError(() => of(null))
    );
  }
}
