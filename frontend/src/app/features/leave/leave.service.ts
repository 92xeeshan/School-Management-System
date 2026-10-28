import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse, PagedResponse } from '../../core/models/api.model';
import {
  LeaveAssignment,
  LeaveBalance,
  LeaveDayCount,
  LeavePolicy,
  LeavePolicyPayload,
  LeaveRequest,
  LeaveRequestStatus,
  LeaveType,
  LeaveTypePayload,
  LeaveYearMode,
  LeaveYearSetting,
  StaffLeaveOption,
  StaffType,
} from './leave.model';

@Injectable({ providedIn: 'root' })
export class LeaveService {
  constructor(private http: HttpClient) {}

  listTypes(): Observable<LeaveType[]> {
    return this.http.get<ApiResponse<LeaveType[]>>('/api/leave/types').pipe(map((res) => res.data ?? []));
  }

  createType(payload: LeaveTypePayload): Observable<LeaveType> {
    return this.http.post<ApiResponse<LeaveType>>('/api/leave/types', payload).pipe(map((res) => res.data));
  }

  updateType(id: string, payload: LeaveTypePayload): Observable<LeaveType> {
    return this.http.put<ApiResponse<LeaveType>>(`/api/leave/types/${id}`, payload).pipe(map((res) => res.data));
  }

  deactivateType(id: string): Observable<LeaveType> {
    return this.http.post<ApiResponse<LeaveType>>(`/api/leave/types/${id}/deactivate`, {}).pipe(map((res) => res.data));
  }

  listPolicies(): Observable<LeavePolicy[]> {
    return this.http.get<ApiResponse<LeavePolicy[]>>('/api/leave/policies').pipe(map((res) => res.data ?? []));
  }

  savePolicy(payload: LeavePolicyPayload, id?: string): Observable<LeavePolicy> {
    const req = id
      ? this.http.put<ApiResponse<LeavePolicy>>(`/api/leave/policies/${id}`, payload)
      : this.http.post<ApiResponse<LeavePolicy>>('/api/leave/policies', payload);
    return req.pipe(map((res) => res.data));
  }

  yearSetting(): Observable<LeaveYearSetting> {
    return this.http.get<ApiResponse<LeaveYearSetting>>('/api/leave/year').pipe(map((res) => res.data));
  }

  updateYear(mode: LeaveYearMode): Observable<LeaveYearSetting> {
    return this.http.put<ApiResponse<LeaveYearSetting>>('/api/leave/year', { mode }).pipe(map((res) => res.data));
  }

  listStaff(): Observable<StaffLeaveOption[]> {
    return this.http.get<ApiResponse<StaffLeaveOption[]>>('/api/leave/staff').pipe(map((res) => res.data ?? []));
  }

  assignPolicy(policyId: string, staffType: StaffType, staffIds: string[]): Observable<LeaveAssignment[]> {
    return this.http
      .post<ApiResponse<LeaveAssignment[]>>('/api/leave/assignments', { policyId, staffType, staffIds })
      .pipe(map((res) => res.data ?? []));
  }

  myBalances(): Observable<LeaveBalance[]> {
    return this.http.get<ApiResponse<LeaveBalance[]>>('/api/leave/balances/mine').pipe(map((res) => res.data ?? []));
  }

  balances(): Observable<LeaveBalance[]> {
    return this.http.get<ApiResponse<LeaveBalance[]>>('/api/leave/balances').pipe(map((res) => res.data ?? []));
  }

  adjustBalance(payload: {
    staffType: StaffType;
    staffId: string;
    leaveTypeId: string;
    days: number;
    reason: string;
  }): Observable<LeaveBalance> {
    return this.http.post<ApiResponse<LeaveBalance>>('/api/leave/balances/adjust', payload).pipe(map((res) => res.data));
  }

  previewDays(from: string, to: string): Observable<LeaveDayCount> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<ApiResponse<LeaveDayCount>>('/api/leave/days', { params }).pipe(map((res) => res.data));
  }

  apply(payload: { leaveTypeId: string; fromDate: string; toDate: string; reason: string }, file?: File | null): Observable<LeaveRequest> {
    if (file) {
      const form = new FormData();
      form.append('leaveTypeId', payload.leaveTypeId);
      form.append('fromDate', payload.fromDate);
      form.append('toDate', payload.toDate);
      form.append('reason', payload.reason);
      form.append('file', file);
      return this.http.post<ApiResponse<LeaveRequest>>('/api/leave/requests', form).pipe(map((res) => res.data));
    }
    return this.http.post<ApiResponse<LeaveRequest>>('/api/leave/requests', payload).pipe(map((res) => res.data));
  }

  myRequests(opts: { status?: LeaveRequestStatus | ''; leaveTypeId?: string; page?: number; size?: number } = {}): Observable<PagedResponse<LeaveRequest>> {
    let params = new HttpParams()
      .set('page', String(opts.page ?? 0))
      .set('size', String(opts.size ?? 20));
    if (opts.status) {
      params = params.set('status', opts.status);
    }
    if (opts.leaveTypeId) {
      params = params.set('leaveTypeId', opts.leaveTypeId);
    }
    return this.http.get<ApiResponse<PagedResponse<LeaveRequest>>>('/api/leave/requests/mine', { params }).pipe(map((res) => res.data));
  }

  listRequests(opts: { status?: LeaveRequestStatus | ''; page?: number; size?: number } = {}): Observable<PagedResponse<LeaveRequest>> {
    let params = new HttpParams()
      .set('page', String(opts.page ?? 0))
      .set('size', String(opts.size ?? 20));
    if (opts.status) {
      params = params.set('status', opts.status);
    }
    return this.http.get<ApiResponse<PagedResponse<LeaveRequest>>>('/api/leave/requests', { params }).pipe(map((res) => res.data));
  }

  approve(id: string): Observable<LeaveRequest> {
    return this.http.post<ApiResponse<LeaveRequest>>(`/api/leave/requests/${id}/approve`, {}).pipe(map((res) => res.data));
  }

  reject(id: string, rejectionReason: string): Observable<LeaveRequest> {
    return this.http.post<ApiResponse<LeaveRequest>>(`/api/leave/requests/${id}/reject`, { rejectionReason }).pipe(map((res) => res.data));
  }

  cancel(id: string, reason: string): Observable<LeaveRequest> {
    return this.http.post<ApiResponse<LeaveRequest>>(`/api/leave/requests/${id}/cancel`, { reason }).pipe(map((res) => res.data));
  }

  onLeaveToday(): Observable<LeaveRequest[]> {
    return this.http.get<ApiResponse<LeaveRequest[]>>('/api/leave/reports/today').pipe(map((res) => res.data ?? []));
  }

  taken(): Observable<LeaveRequest[]> {
    return this.http.get<ApiResponse<LeaveRequest[]>>('/api/leave/reports/taken').pipe(map((res) => res.data ?? []));
  }

  exportBalances(): Observable<Blob> {
    return this.http.get('/api/leave/reports/balances.xlsx', { responseType: 'blob' });
  }

  exportTaken(): Observable<Blob> {
    return this.http.get('/api/leave/reports/taken.xlsx', { responseType: 'blob' });
  }
}
