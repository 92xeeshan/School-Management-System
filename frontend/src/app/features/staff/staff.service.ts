import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse, PagedResponse } from '../../core/models/api.model';
import { SectionRef, StaffMember, StaffMemberPayload } from './staff.model';

export interface DirectoryQuery {
  query?: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: string;
}

@Injectable({ providedIn: 'root' })
export class StaffService {
  constructor(private http: HttpClient) {}

  listTeaching(opts: DirectoryQuery = {}): Observable<PagedResponse<StaffMember>> {
    return this.http
      .get<ApiResponse<PagedResponse<StaffMember>>>('/api/staff/teachers', { params: this.params(opts) })
      .pipe(map((res) => res.data));
  }

  createTeaching(payload: StaffMemberPayload): Observable<StaffMember> {
    return this.http
      .post<ApiResponse<StaffMember>>('/api/staff/teachers', payload)
      .pipe(map((res) => res.data));
  }

  updateTeaching(id: string, payload: StaffMemberPayload): Observable<StaffMember> {
    return this.http
      .put<ApiResponse<StaffMember>>(`/api/staff/teachers/${id}`, payload)
      .pipe(map((res) => res.data));
  }

  deactivateTeaching(id: string): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`/api/staff/teachers/${id}/deactivate`, {});
  }

  assignClassTeacher(id: string, sectionId: string | null): Observable<StaffMember> {
    return this.http
      .put<ApiResponse<StaffMember>>(`/api/staff/teachers/${id}/class-teacher`, { sectionId })
      .pipe(map((res) => res.data));
  }

  listNonTeaching(opts: DirectoryQuery = {}): Observable<PagedResponse<StaffMember>> {
    return this.http
      .get<ApiResponse<PagedResponse<StaffMember>>>('/api/staff/non-teaching', { params: this.params(opts) })
      .pipe(map((res) => res.data));
  }

  createNonTeaching(payload: StaffMemberPayload): Observable<StaffMember> {
    return this.http
      .post<ApiResponse<StaffMember>>('/api/staff/non-teaching', payload)
      .pipe(map((res) => res.data));
  }

  updateNonTeaching(id: string, payload: StaffMemberPayload): Observable<StaffMember> {
    return this.http
      .put<ApiResponse<StaffMember>>(`/api/staff/non-teaching/${id}`, payload)
      .pipe(map((res) => res.data));
  }

  deactivateNonTeaching(id: string): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`/api/staff/non-teaching/${id}/deactivate`, {});
  }

  listSections(): Observable<SectionRef[]> {
    return this.http
      .get<ApiResponse<SectionRef[]>>('/api/staff/sections')
      .pipe(map((res) => res.data ?? []));
  }

  private params(opts: DirectoryQuery): HttpParams {
    let params = new HttpParams()
      .set('page', String(opts.page ?? 0))
      .set('size', String(opts.size ?? 25));
    if (opts.query) {
      params = params.set('query', opts.query);
    }
    if (opts.sort) {
      params = params.set('sort', opts.sort);
    }
    if (opts.direction) {
      params = params.set('direction', opts.direction);
    }
    return params;
  }
}
