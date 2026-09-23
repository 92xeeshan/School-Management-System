import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiResponse } from '../../core/models/api.model';
import { CalendarEvent, CalendarEventPayload, CalendarOptions } from './calendar.model';

@Injectable({ providedIn: 'root' })
export class CalendarService {
  constructor(private http: HttpClient) {}

  list(from: string, to: string, type?: string): Observable<CalendarEvent[]> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (type) {
      params = params.set('type', type);
    }
    return this.http.get<ApiResponse<CalendarEvent[]>>('/api/events', { params }).pipe(
      map((res) => res.data ?? [])
    );
  }

  upcoming(days = 30): Observable<CalendarEvent[]> {
    const params = new HttpParams().set('days', String(days));
    return this.http.get<ApiResponse<CalendarEvent[]>>('/api/events/upcoming', { params }).pipe(
      map((res) => res.data ?? [])
    );
  }

  options(): Observable<CalendarOptions> {
    return this.http.get<ApiResponse<CalendarOptions>>('/api/events/options').pipe(
      map((res) => res.data)
    );
  }

  create(payload: CalendarEventPayload): Observable<CalendarEvent> {
    return this.http.post<ApiResponse<CalendarEvent>>('/api/events', payload).pipe(
      map((res) => res.data)
    );
  }

  update(id: string, payload: CalendarEventPayload): Observable<CalendarEvent> {
    return this.http.put<ApiResponse<CalendarEvent>>(`/api/events/${id}`, payload).pipe(
      map((res) => res.data)
    );
  }

  delete(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`/api/events/${id}`).pipe(map(() => undefined));
  }

  exportHolidays(year: number): Observable<Blob> {
    const params = new HttpParams().set('year', String(year));
    return this.http.get('/api/events/export', { params, responseType: 'blob' });
  }
}
