import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse, PagedResponse } from '../../core/models/api.model';
import {
  AcademicYearOption,
  AdhocLevy,
  AdhocLevyPayload,
  ClassOption,
  CloneResult,
  FeeAssignment,
  FeeHead,
  FeeHeadPayload,
  FeePayment,
  FeeStructureAudit,
  FeeStructurePayload,
  FeeStructureRow,
  Installment,
  SectionOption,
  SiblingDiscountPayload,
  SiblingDiscountRule,
  StudentOption,
} from './fees.model';

interface BackendStudentListItem {
  student: { id: string; admissionNo: string; firstName: string; lastName: string; displayName: string };
}

@Injectable({ providedIn: 'root' })
export class FeesService {
  constructor(private http: HttpClient) {}

  academicYears(): Observable<AcademicYearOption[]> {
    return this.http.get<ApiResponse<AcademicYearOption[]>>('/api/academic-years').pipe(map((res) => res.data ?? []));
  }

  classes(): Observable<ClassOption[]> {
    return this.http.get<ApiResponse<ClassOption[]>>('/api/classes').pipe(map((res) => res.data ?? []));
  }

  sections(): Observable<SectionOption[]> {
    return this.http.get<ApiResponse<SectionOption[]>>('/api/sections').pipe(map((res) => res.data ?? []));
  }

  students(): Observable<StudentOption[]> {
    return this.http.get<ApiResponse<PagedResponse<BackendStudentListItem>>>('/api/students', { params: { size: '200' } }).pipe(
      map((res) =>
        (res.data?.content ?? []).map((item) => ({
          id: item.student.id,
          admissionNo: item.student.admissionNo,
          displayName: item.student.displayName ?? `${item.student.firstName} ${item.student.lastName}`,
        }))
      )
    );
  }

  listHeads(): Observable<FeeHead[]> {
    return this.http.get<ApiResponse<FeeHead[]>>('/api/fees/categories').pipe(map((res) => res.data ?? []));
  }

  createHead(payload: FeeHeadPayload): Observable<FeeHead> {
    return this.http.post<ApiResponse<FeeHead>>('/api/fees/categories', payload).pipe(map((res) => res.data));
  }

  updateHead(id: string, payload: FeeHeadPayload): Observable<FeeHead> {
    return this.http.put<ApiResponse<FeeHead>>(`/api/fees/categories/${id}`, payload).pipe(map((res) => res.data));
  }

  deactivateHead(id: string): Observable<FeeHead> {
    return this.http.post<ApiResponse<FeeHead>>(`/api/fees/categories/${id}/deactivate`, {}).pipe(map((res) => res.data));
  }

  listStructures(academicYearId: string): Observable<FeeStructureRow[]> {
    const params = new HttpParams().set('academicYearId', academicYearId);
    return this.http.get<ApiResponse<FeeStructureRow[]>>('/api/fees/structures', { params }).pipe(map((res) => res.data ?? []));
  }

  createStructure(payload: FeeStructurePayload): Observable<FeeStructureRow[]> {
    return this.http.post<ApiResponse<FeeStructureRow[]>>('/api/fees/structures', payload).pipe(map((res) => res.data ?? []));
  }

  updateStructure(id: string, payload: Partial<FeeStructurePayload>): Observable<FeeStructureRow> {
    return this.http.put<ApiResponse<FeeStructureRow>>(`/api/fees/structures/${id}`, payload).pipe(map((res) => res.data));
  }

  cloneStructures(sourceAcademicYearId: string, targetAcademicYearId: string): Observable<CloneResult> {
    return this.http
      .post<ApiResponse<CloneResult>>('/api/fees/structures/clone', { sourceAcademicYearId, targetAcademicYearId })
      .pipe(map((res) => res.data));
  }

  listAudit(academicYearId?: string): Observable<FeeStructureAudit[]> {
    let params = new HttpParams();
    if (academicYearId) {
      params = params.set('academicYearId', academicYearId);
    }
    return this.http.get<ApiResponse<FeeStructureAudit[]>>('/api/fees/structures/audit', { params }).pipe(map((res) => res.data ?? []));
  }

  listSiblingRules(academicYearId?: string): Observable<SiblingDiscountRule[]> {
    let params = new HttpParams();
    if (academicYearId) {
      params = params.set('academicYearId', academicYearId);
    }
    return this.http.get<ApiResponse<SiblingDiscountRule[]>>('/api/fees/discounts/sibling', { params }).pipe(map((res) => res.data ?? []));
  }

  createSiblingRule(payload: SiblingDiscountPayload): Observable<SiblingDiscountRule> {
    return this.http.post<ApiResponse<SiblingDiscountRule>>('/api/fees/discounts/sibling', payload).pipe(map((res) => res.data));
  }

  updateSiblingRule(id: string, payload: SiblingDiscountPayload): Observable<SiblingDiscountRule> {
    return this.http.put<ApiResponse<SiblingDiscountRule>>(`/api/fees/discounts/sibling/${id}`, payload).pipe(map((res) => res.data));
  }

  listLevies(academicYearId?: string): Observable<AdhocLevy[]> {
    let params = new HttpParams();
    if (academicYearId) {
      params = params.set('academicYearId', academicYearId);
    }
    return this.http.get<ApiResponse<AdhocLevy[]>>('/api/fees/adhoc', { params }).pipe(map((res) => res.data ?? []));
  }

  createLevy(payload: AdhocLevyPayload): Observable<AdhocLevy> {
    return this.http.post<ApiResponse<AdhocLevy>>('/api/fees/adhoc', payload).pipe(map((res) => res.data));
  }

  listAssignments(studentId: string): Observable<FeeAssignment[]> {
    return this.http.get<ApiResponse<FeeAssignment[]>>(`/api/fees/students/${studentId}/assignments`).pipe(map((res) => res.data ?? []));
  }

  listInstallments(assignmentId: string): Observable<Installment[]> {
    return this.http.get<ApiResponse<Installment[]>>(`/api/fees/assignments/${assignmentId}/installments`).pipe(map((res) => res.data ?? []));
  }

  listPayments(studentId: string): Observable<FeePayment[]> {
    return this.http.get<ApiResponse<FeePayment[]>>(`/api/fees/students/${studentId}/payments`).pipe(map((res) => res.data ?? []));
  }

  recordPayment(payload: {
    studentId: string;
    studentFeeAssignmentId: string | null;
    amountPaid: number;
    paymentMethod: string;
  }): Observable<FeePayment> {
    return this.http.post<ApiResponse<FeePayment>>('/api/fees/payments', payload).pipe(map((res) => res.data));
  }
}
