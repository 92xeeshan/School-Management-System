import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse, PagedResponse } from '../../core/models/api.model';
import { environment } from '../../../environments/environment';
import {
  Enquiry,
  EnquiryPayload,
  PublicSite,
  PublicSiteAdmin,
} from './public.model';

@Injectable({ providedIn: 'root' })
export class PublicService {
  readonly schoolCode = environment.defaultSchoolCode ?? 'DEMO';

  constructor(private http: HttpClient) {}

  getSite(locale?: string): Observable<PublicSite> {
    let params = new HttpParams();
    if (locale) {
      params = params.set('locale', locale);
    }
    return this.http
      .get<ApiResponse<PublicSite>>(`/api/public/${this.schoolCode}`, { params })
      .pipe(map((res) => res.data));
  }

  submitEnquiry(payload: EnquiryPayload): Observable<Enquiry> {
    return this.http
      .post<ApiResponse<Enquiry>>(`/api/public/${this.schoolCode}/enquiries`, payload)
      .pipe(map((res) => res.data));
  }

  adminSite(): Observable<PublicSiteAdmin> {
    return this.http.get<ApiResponse<PublicSiteAdmin>>('/api/website').pipe(map((res) => res.data));
  }

  saveSite(payload: {
    published: boolean;
    heroImageUrl: string | null;
    principalName: string | null;
    copies: PublicSiteAdmin['copies'];
  }): Observable<PublicSiteAdmin> {
    return this.http
      .put<ApiResponse<PublicSiteAdmin>>('/api/website', payload)
      .pipe(map((res) => res.data));
  }

  listEnquiries(opts: { status?: string; page?: number; size?: number } = {}): Observable<PagedResponse<Enquiry>> {
    let params = new HttpParams()
      .set('page', String(opts.page ?? 0))
      .set('size', String(opts.size ?? 20));
    if (opts.status) {
      params = params.set('status', opts.status);
    }
    return this.http
      .get<ApiResponse<PagedResponse<Enquiry>>>('/api/enquiries', { params })
      .pipe(map((res) => res.data));
  }

  updateEnquiry(id: string, payload: { status?: string; notes?: string | null }): Observable<Enquiry> {
    return this.http
      .put<ApiResponse<Enquiry>>(`/api/enquiries/${id}`, payload)
      .pipe(map((res) => res.data));
  }
}
