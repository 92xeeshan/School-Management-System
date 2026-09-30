import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse, PagedResponse } from '../models/api.model';
import {
  CertificateIssued,
  CertificateStatus,
  CertificateTemplate,
  CertificateTemplateRequest,
  CertificateType,
  GenerateCertificateRequest,
} from './certificate.model';

@Injectable({ providedIn: 'root' })
export class CertificateService {
  constructor(private http: HttpClient) {}

  listTemplates(): Observable<CertificateTemplate[]> {
    return this.http.get<ApiResponse<CertificateTemplate[]>>('/api/certificates/templates').pipe(
      map((res) => res.data ?? []),
    );
  }

  saveTemplate(body: CertificateTemplateRequest): Observable<CertificateTemplate> {
    return this.http.post<ApiResponse<CertificateTemplate>>('/api/certificates/templates', body).pipe(
      map((res) => res.data),
    );
  }

  generate(body: GenerateCertificateRequest): Observable<CertificateIssued> {
    return this.http.post<ApiResponse<CertificateIssued>>('/api/certificates/generate', body).pipe(
      map((res) => res.data),
    );
  }

  register(opts: {
    type?: CertificateType | '';
    status?: CertificateStatus | '';
    query?: string;
    page?: number;
    size?: number;
  }): Observable<PagedResponse<CertificateIssued>> {
    let params = new HttpParams();
    if (opts.type) {
      params = params.set('type', opts.type);
    }
    if (opts.status) {
      params = params.set('status', opts.status);
    }
    if (opts.query) {
      params = params.set('query', opts.query);
    }
    params = params.set('page', String(opts.page ?? 0)).set('size', String(opts.size ?? 20));
    return this.http.get<ApiResponse<PagedResponse<CertificateIssued>>>('/api/certificates', { params }).pipe(
      map((res) => res.data),
    );
  }

  listForStudent(studentId: string): Observable<CertificateIssued[]> {
    return this.http.get<ApiResponse<CertificateIssued[]>>(`/api/certificates/student/${studentId}`).pipe(
      map((res) => res.data ?? []),
    );
  }

  mine(): Observable<CertificateIssued[]> {
    return this.http.get<ApiResponse<CertificateIssued[]>>('/api/certificates/mine').pipe(
      map((res) => res.data ?? []),
    );
  }

  approve(id: string): Observable<CertificateIssued> {
    return this.http.post<ApiResponse<CertificateIssued>>(`/api/certificates/${id}/approve`, {}).pipe(
      map((res) => res.data),
    );
  }

  download(id: string, reprint = false): Observable<Blob> {
    const params = new HttpParams().set('reprint', reprint ? 'true' : 'false');
    return this.http.get(`/api/certificates/${id}/download`, { params, responseType: 'blob' });
  }
}
