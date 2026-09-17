import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../../core/models/api.model';
import { SectionRef, StaffMember, StaffMemberPayload } from './staff.model';

@Injectable({ providedIn: 'root' })
export class StaffService {
  constructor(private http: HttpClient) {}

  listTeaching(): Observable<StaffMember[]> {
    return this.http
      .get<ApiResponse<StaffMember[]>>('/api/staff/teachers')
      .pipe(map((res) => res.data ?? []));
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

  listNonTeaching(): Observable<StaffMember[]> {
    return this.http
      .get<ApiResponse<StaffMember[]>>('/api/staff/non-teaching')
      .pipe(map((res) => res.data ?? []));
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
}
