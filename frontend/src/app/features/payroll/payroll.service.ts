import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../../core/models/api.model';
import {
  Expense,
  ExpenseCategory,
  ExpenseChart,
  ExpensePayload,
  PayrollRun,
  PayrollRunPayload,
  Payslip,
} from './payroll.model';

@Injectable({ providedIn: 'root' })
export class PayrollService {
  constructor(private http: HttpClient) {}

  listCategories(): Observable<ExpenseCategory[]> {
    return this.http
      .get<ApiResponse<ExpenseCategory[]>>('/api/expenses/categories')
      .pipe(map((res) => res.data ?? []));
  }

  listExpenses(year: number): Observable<Expense[]> {
    const params = new HttpParams().set('year', String(year));
    return this.http
      .get<ApiResponse<Expense[]>>('/api/expenses', { params })
      .pipe(map((res) => res.data ?? []));
  }

  createExpense(payload: ExpensePayload): Observable<Expense> {
    return this.http
      .post<ApiResponse<Expense>>('/api/expenses', payload)
      .pipe(map((res) => res.data));
  }

  chart(year: number): Observable<ExpenseChart> {
    const params = new HttpParams().set('year', String(year));
    return this.http
      .get<ApiResponse<ExpenseChart>>('/api/expenses/chart', { params })
      .pipe(map((res) => res.data));
  }

  listRuns(): Observable<PayrollRun[]> {
    return this.http
      .get<ApiResponse<PayrollRun[]>>('/api/payroll/runs')
      .pipe(map((res) => res.data ?? []));
  }

  createRun(payload: PayrollRunPayload): Observable<PayrollRun> {
    return this.http
      .post<ApiResponse<PayrollRun>>('/api/payroll/runs', payload)
      .pipe(map((res) => res.data));
  }

  processRun(id: string): Observable<PayrollRun> {
    return this.http
      .post<ApiResponse<PayrollRun>>(`/api/payroll/run/${id}/process`, {})
      .pipe(map((res) => res.data));
  }

  publishRun(id: string): Observable<PayrollRun> {
    return this.http
      .post<ApiResponse<PayrollRun>>(`/api/payroll/run/${id}/publish`, {})
      .pipe(map((res) => res.data));
  }

  markPaid(id: string): Observable<PayrollRun> {
    return this.http
      .post<ApiResponse<PayrollRun>>(`/api/payroll/run/${id}/paid`, {})
      .pipe(map((res) => res.data));
  }

  listPayslips(runId: string): Observable<Payslip[]> {
    return this.http
      .get<ApiResponse<Payslip[]>>(`/api/payroll/run/${runId}/payslips`)
      .pipe(map((res) => res.data ?? []));
  }

  myPayslips(): Observable<Payslip[]> {
    return this.http
      .get<ApiResponse<Payslip[]>>('/api/payroll/my-payslips')
      .pipe(map((res) => res.data ?? []));
  }

  downloadMyPayslip(id: string): Observable<Blob> {
    return this.http.get(`/api/payroll/my-payslips/${id}/download`, { responseType: 'blob' });
  }
}
