import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { CertificateService } from '../../core/certificates/certificate.service';
import {
  CertificateIssued,
  CertificateRequest,
  CertificateRequestStatus,
  CertificateStatus,
  CertificateTemplate,
  CertificateType,
  ReviewCertificateRequest,
} from '../../core/certificates/certificate.model';
import { ApiError } from '../../core/models/api.model';

const CERT_TYPES: CertificateType[] = ['BONAFIDE', 'TC', 'CHARACTER', 'COURSE_COMPLETION'];

@Component({
  selector: 'app-certificates',
  imports: [TranslateModule, FormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'certificates.title' | translate }}</h1>
          <p class="muted">{{ 'certificates.subtitle' | translate }}</p>
        </div>
      </div>

      @if (canManage) {
        <div class="card">
          <h2>{{ 'certificates.templates' | translate }}</h2>
          <div class="form-grid">
            <div class="field">
              <label>{{ 'certificates.type' | translate }}</label>
              <select [(ngModel)]="templateType" (ngModelChange)="onTemplateTypeChange()">
                @for (t of types; track t) {
                  <option [value]="t">{{ ('certificates.types.' + t) | translate }}</option>
                }
              </select>
            </div>
            <div class="field span-2">
              <label>{{ 'certificates.header' | translate }}</label>
              <input type="text" [(ngModel)]="headerHtml" />
            </div>
            <div class="field span-2">
              <label>{{ 'certificates.footer' | translate }}</label>
              <textarea rows="2" [(ngModel)]="footerHtml"></textarea>
            </div>
            <label class="check">
              <input type="checkbox" [(ngModel)]="requiresApproval" />
              {{ 'certificates.requiresApproval' | translate }}
            </label>
          </div>
          @if (templateMessage) {
            <p class="banner success">{{ templateMessage | translate }}</p>
          }
          @if (templateError) {
            <p class="error">{{ templateError | translate }}</p>
          }
          <div class="form-actions">
            <button class="btn btn-primary" type="button" [disabled]="savingTemplate" (click)="saveTemplate()">
              {{ savingTemplate ? ('common.loading' | translate) : ('common.save' | translate) }}
            </button>
          </div>
        </div>
      }

      <div class="card">
        <h2>{{ 'certificates.queue' | translate }}</h2>
        <div class="toolbar">
          <select [(ngModel)]="requestTypeFilter" (ngModelChange)="loadRequests()">
            <option value="">{{ 'certificates.allTypes' | translate }}</option>
            @for (t of types; track t) {
              <option [value]="t">{{ ('certificates.types.' + t) | translate }}</option>
            }
          </select>
          <select [(ngModel)]="requestStatusFilter" (ngModelChange)="loadRequests()">
            <option value="">{{ 'certificates.allStatuses' | translate }}</option>
            <option value="SUBMITTED">{{ 'certificates.statuses.SUBMITTED' | translate }}</option>
            <option value="TEACHER_REVIEWED">{{ 'certificates.statuses.TEACHER_REVIEWED' | translate }}</option>
            <option value="ISSUED">{{ 'certificates.statuses.ISSUED' | translate }}</option>
            <option value="REJECTED">{{ 'certificates.statuses.REJECTED' | translate }}</option>
            <option value="CANCELLED">{{ 'certificates.statuses.CANCELLED' | translate }}</option>
          </select>
          <button class="btn" type="button" (click)="loadRequests()">{{ 'common.refresh' | translate }}</button>
        </div>
        @if (requestError) {
          <p class="error">{{ requestError | translate }}</p>
        }
        <table>
          <thead>
            <tr>
              <th>{{ 'certificates.student' | translate }}</th>
              <th>{{ 'certificates.type' | translate }}</th>
              <th>{{ 'common.status' | translate }}</th>
              <th>{{ 'certificates.requestReason' | translate }}</th>
              <th>{{ 'common.actions' | translate }}</th>
            </tr>
          </thead>
          <tbody>
            @for (row of requests; track row.id) {
              <tr>
                <td>
                  <strong>{{ row.studentName }}</strong>
                  <div class="muted">{{ row.admissionNo }} · {{ row.className }} {{ row.sectionName }}</div>
                  <div class="muted">{{ 'certificates.rollNo' | translate }}: {{ row.rollNo || '—' }}</div>
                </td>
                <td>{{ ('certificates.types.' + row.certificateType) | translate }}</td>
                <td>{{ ('certificates.statuses.' + row.status) | translate }}</td>
                <td>{{ row.reason || '—' }}</td>
                <td class="actions">
                  @if (row.canReview) {
                    <button class="btn btn-primary" type="button" (click)="openReview(row)">{{ 'certificates.forward' | translate }}</button>
                  }
                  @if (row.canApprove) {
                    <button class="btn btn-primary" type="button" (click)="approveRequest(row)">{{ 'certificates.approve' | translate }}</button>
                  }
                  @if (row.canReject) {
                    <button class="btn" type="button" (click)="openReject(row)">{{ 'certificates.reject' | translate }}</button>
                  }
                  @if (row.canCancel) {
                    <button class="btn" type="button" (click)="sendBack(row)">{{ 'certificates.sendBack' | translate }}</button>
                  }
                  @if (row.canDownload && row.issuedId) {
                    <button class="btn" type="button" (click)="downloadIssued(row.issuedId, row.certificateNo, false)">{{ 'certificates.download' | translate }}</button>
                  }
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="5" class="center muted">{{ 'common.noData' | translate }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <div class="card">
        <h2>{{ 'certificates.register' | translate }}</h2>
        <div class="toolbar">
          <input type="text" [(ngModel)]="query" (keyup.enter)="load()" [placeholder]="'certificates.search' | translate" />
          <select [(ngModel)]="typeFilter" (ngModelChange)="load()">
            <option value="">{{ 'certificates.allTypes' | translate }}</option>
            @for (t of types; track t) {
              <option [value]="t">{{ ('certificates.types.' + t) | translate }}</option>
            }
          </select>
          <select [(ngModel)]="statusFilter" (ngModelChange)="load()">
            <option value="">{{ 'certificates.allStatuses' | translate }}</option>
            <option value="DRAFT">{{ 'certificates.statuses.DRAFT' | translate }}</option>
            <option value="ISSUED">{{ 'certificates.statuses.ISSUED' | translate }}</option>
          </select>
          <button class="btn" type="button" (click)="load()">{{ 'common.refresh' | translate }}</button>
        </div>
        @if (error) {
          <p class="error">{{ error | translate }}</p>
        }
        <table>
          <thead>
            <tr>
              <th>{{ 'certificates.number' | translate }}</th>
              <th>{{ 'certificates.student' | translate }}</th>
              <th>{{ 'certificates.type' | translate }}</th>
              <th>{{ 'common.status' | translate }}</th>
              <th>{{ 'certificates.issuedOn' | translate }}</th>
              <th>{{ 'certificates.issuedBy' | translate }}</th>
              <th>{{ 'common.actions' | translate }}</th>
            </tr>
          </thead>
          <tbody>
            @for (row of rows; track row.id) {
              <tr>
                <td>{{ row.certificateNo || '—' }}</td>
                <td>
                  <strong>{{ row.studentName }}</strong>
                  <div class="muted">{{ row.admissionNo }} · {{ row.className }} {{ row.sectionName }}</div>
                </td>
                <td>{{ ('certificates.types.' + row.certificateType) | translate }}</td>
                <td>
                  {{ ('certificates.statuses.' + row.status) | translate }}
                  @if (row.duplicate) {
                    <span class="badge">{{ 'certificates.duplicate' | translate }}</span>
                  }
                </td>
                <td>{{ row.issuedDate || '—' }}</td>
                <td>{{ row.issuedByName || '—' }}</td>
                <td class="actions">
                  @if (row.canApprove) {
                    <button class="btn btn-primary" type="button" (click)="approve(row)">{{ 'certificates.approve' | translate }}</button>
                  }
                  @if (row.canDownload) {
                    <button class="btn" type="button" (click)="download(row, false)">{{ 'certificates.download' | translate }}</button>
                    <button class="btn" type="button" (click)="download(row, true)">{{ 'certificates.reprint' | translate }}</button>
                  }
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="7" class="center muted">{{ 'common.noData' | translate }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>

    @if (reviewing) {
      <div class="modal-backdrop" (click)="closeReview()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ 'certificates.forward' | translate }}</h2>
          <p class="muted">{{ reviewing.studentName }} · {{ reviewing.admissionNo }} · {{ reviewing.className }} {{ reviewing.sectionName }}</p>
          <div class="field">
            <label>{{ 'certificates.studentDetails' | translate }}</label>
            <p class="muted">{{ 'certificates.rollNo' | translate }}: {{ reviewing.rollNo || '—' }}</p>
          </div>
          <div class="field">
            <label>{{ 'certificates.conduct' | translate }}</label>
            <textarea rows="2" [(ngModel)]="reviewForm.conductRemarks"></textarea>
          </div>
          <div class="field">
            <label>{{ 'certificates.academicProgress' | translate }}</label>
            <textarea rows="2" [(ngModel)]="reviewForm.academicProgress"></textarea>
          </div>
          <div class="field">
            <label>{{ 'certificates.lastExam' | translate }}</label>
            <input type="text" [(ngModel)]="reviewForm.lastExamAttended" />
          </div>
          @if (reviewing.certificateType === 'TC') {
            <div class="field">
              <label>{{ 'certificates.reason' | translate }} *</label>
              <input type="text" [(ngModel)]="reviewForm.reason" />
            </div>
          }
          <div class="field">
            <label>{{ 'certificates.teacherNotes' | translate }}</label>
            <textarea rows="2" [(ngModel)]="reviewForm.teacherNotes"></textarea>
          </div>
          <div class="dues">
            <span>{{ 'certificates.dues' | translate }}</span>
            <label class="check"><input type="checkbox" [(ngModel)]="reviewForm.duesLibrary" /> {{ 'certificates.duesLibrary' | translate }}</label>
            <label class="check"><input type="checkbox" [(ngModel)]="reviewForm.duesAccounts" /> {{ 'certificates.duesAccounts' | translate }}</label>
            <label class="check"><input type="checkbox" [(ngModel)]="reviewForm.duesSports" /> {{ 'certificates.duesSports' | translate }}</label>
          </div>
          @if (reviewError) {
            <p class="error">{{ reviewError | translate }}</p>
          }
          <div class="form-actions">
            <button class="btn" type="button" (click)="closeReview()">{{ 'common.cancel' | translate }}</button>
            <button class="btn btn-primary" type="button" [disabled]="savingReview" (click)="submitReview()">
              {{ savingReview ? ('common.loading' | translate) : ('certificates.forward' | translate) }}
            </button>
          </div>
        </div>
      </div>
    }

    @if (rejecting) {
      <div class="modal-backdrop" (click)="closeReject()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ 'certificates.reject' | translate }}</h2>
          <p class="muted">{{ rejecting.studentName }} · {{ ('certificates.types.' + rejecting.certificateType) | translate }}</p>
          <div class="field">
            <label>{{ 'certificates.rejectionReason' | translate }} *</label>
            <textarea rows="3" [(ngModel)]="rejectionReason"></textarea>
          </div>
          @if (rejectError) {
            <p class="error">{{ rejectError | translate }}</p>
          }
          <div class="form-actions">
            <button class="btn" type="button" (click)="closeReject()">{{ 'common.cancel' | translate }}</button>
            <button class="btn btn-primary" type="button" [disabled]="savingReject" (click)="submitReject()">
              {{ savingReject ? ('common.loading' | translate) : ('certificates.reject' | translate) }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    .page-header { margin-bottom: 16px; }
    h1 { font-size: 1.5rem; margin: 0 0 4px; }
    h2 { font-size: 1.05rem; margin: 0 0 16px; }
    .muted { color: var(--color-muted); margin: 0; }
    .card { background: #fff; border: 1px solid var(--color-border); border-radius: var(--radius); padding: 20px; margin-bottom: 16px; }
    .toolbar { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
    .toolbar input, .toolbar select, input, select, textarea {
      padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit;
    }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .field { display: flex; flex-direction: column; gap: 6px; margin-bottom: 10px; }
    .span-2 { grid-column: span 2; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    .check { display: flex; align-items: center; gap: 8px; margin: 12px 0; }
    .dues { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; margin: 8px 0 16px; }
    .dues .check { margin: 0; }
    .form-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 10px 8px; border-bottom: 1px solid var(--color-border); vertical-align: top; }
    .actions { display: flex; gap: 6px; flex-wrap: wrap; }
    .badge { display: inline-block; margin-left: 6px; background: #fef3c7; color: #92400e; padding: 2px 6px; border-radius: 999px; font-size: .75rem; }
    .center { text-align: center; }
    .error { color: #b91c1c; }
    .banner.success { background: #ecfdf5; border: 1px solid #a7f3d0; color: #047857; padding: 10px 14px; border-radius: 8px; }
    .modal-backdrop { position: fixed; inset: 0; background: rgba(15, 23, 42, .45); display: flex; align-items: center; justify-content: center; z-index: 40; padding: 16px; }
    .modal { background: #fff; border-radius: 12px; padding: 20px; width: min(560px, 100%); max-height: 90vh; overflow: auto; }
    @media (max-width: 720px) { .form-grid { grid-template-columns: 1fr; } .span-2 { grid-column: span 1; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CertificatesComponent implements OnInit {
  readonly types = CERT_TYPES;
  rows: CertificateIssued[] = [];
  requests: CertificateRequest[] = [];
  templates: CertificateTemplate[] = [];
  query = '';
  typeFilter: CertificateType | '' = '';
  statusFilter: CertificateStatus | '' = '';
  requestTypeFilter: CertificateType | '' = '';
  requestStatusFilter: CertificateRequestStatus | '' = '';
  error = '';
  requestError = '';
  templateType: CertificateType = 'BONAFIDE';
  headerHtml = '';
  footerHtml = '';
  requiresApproval = true;
  savingTemplate = false;
  templateMessage = '';
  templateError = '';
  reviewing: CertificateRequest | null = null;
  reviewForm: ReviewCertificateRequest = this.emptyReview();
  reviewError = '';
  savingReview = false;
  rejecting: CertificateRequest | null = null;
  rejectionReason = '';
  rejectError = '';
  savingReject = false;

  constructor(
    private certificates: CertificateService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef,
  ) {}

  get canManage(): boolean {
    return this.auth.hasPermission('CERTIFICATE_MANAGE');
  }

  ngOnInit(): void {
    this.load();
    this.loadRequests();
    if (this.canManage) {
      this.loadTemplates();
    }
  }

  load(): void {
    this.error = '';
    this.certificates.register({
      type: this.typeFilter,
      status: this.statusFilter,
      query: this.query.trim(),
    }).subscribe({
      next: (page) => {
        this.rows = page.content ?? [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'certificates.loadFailed';
        this.cdr.markForCheck();
      },
    });
  }

  loadRequests(): void {
    this.requestError = '';
    this.certificates.listRequests({
      type: this.requestTypeFilter,
      status: this.requestStatusFilter,
    }).subscribe({
      next: (page) => {
        this.requests = page.content ?? [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.requestError = 'certificates.loadFailed';
        this.cdr.markForCheck();
      },
    });
  }

  loadTemplates(): void {
    this.certificates.listTemplates().subscribe({
      next: (templates) => {
        this.templates = templates;
        this.onTemplateTypeChange();
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  onTemplateTypeChange(): void {
    const current = this.templates.find((t) => t.type === this.templateType);
    this.headerHtml = current?.headerHtml ?? '';
    this.footerHtml = current?.footerHtml ?? '';
    this.requiresApproval = current?.requiresApproval ?? true;
  }

  saveTemplate(): void {
    this.savingTemplate = true;
    this.templateError = '';
    this.templateMessage = '';
    this.certificates.saveTemplate({
      type: this.templateType,
      headerHtml: this.headerHtml,
      footerHtml: this.footerHtml,
      requiresApproval: this.requiresApproval,
      active: true,
    }).subscribe({
      next: () => {
        this.savingTemplate = false;
        this.templateMessage = 'certificates.templateSaved';
        this.loadTemplates();
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.savingTemplate = false;
        const api = err.error as ApiError | undefined;
        this.templateError = api?.message || 'certificates.saveFailed';
        this.cdr.markForCheck();
      },
    });
  }

  approve(row: CertificateIssued): void {
    this.certificates.approve(row.id).subscribe({
      next: () => this.load(),
      error: (err: HttpErrorResponse) => {
        const api = err.error as ApiError | undefined;
        this.error = api?.message || 'certificates.approveFailed';
        this.cdr.markForCheck();
      },
    });
  }

  download(row: CertificateIssued, reprint: boolean): void {
    this.downloadIssued(row.id, row.certificateNo, reprint);
  }

  downloadIssued(id: string, certificateNo: string | null, reprint: boolean): void {
    this.certificates.download(id, reprint).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = (certificateNo || 'certificate') + (reprint ? '-duplicate' : '') + '.pdf';
        a.click();
        URL.revokeObjectURL(url);
        if (reprint) {
          this.load();
        }
      },
      error: () => {
        this.error = 'certificates.downloadFailed';
        this.cdr.markForCheck();
      },
    });
  }

  openReview(row: CertificateRequest): void {
    this.reviewing = row;
    this.reviewForm = {
      conductRemarks: row.conductRemarks,
      academicProgress: row.academicProgress,
      lastExamAttended: row.lastExamAttended,
      reason: row.reason,
      teacherNotes: row.teacherNotes,
      duesLibrary: row.duesLibrary ?? true,
      duesAccounts: row.duesAccounts ?? true,
      duesSports: row.duesSports ?? true,
    };
    this.reviewError = '';
    this.cdr.markForCheck();
  }

  closeReview(): void {
    this.reviewing = null;
    this.savingReview = false;
    this.cdr.markForCheck();
  }

  submitReview(): void {
    if (!this.reviewing) {
      return;
    }
    if (this.reviewing.certificateType === 'TC' && !this.reviewForm.reason?.trim()) {
      this.reviewError = 'certificates.reasonRequired';
      this.cdr.markForCheck();
      return;
    }
    this.savingReview = true;
    this.reviewError = '';
    this.certificates.review(this.reviewing.id, {
      ...this.reviewForm,
      reason: this.reviewForm.reason?.trim() || null,
      conductRemarks: this.reviewForm.conductRemarks?.trim() || null,
      academicProgress: this.reviewForm.academicProgress?.trim() || null,
      lastExamAttended: this.reviewForm.lastExamAttended?.trim() || null,
      teacherNotes: this.reviewForm.teacherNotes?.trim() || null,
    }).subscribe({
      next: () => {
        this.closeReview();
        this.loadRequests();
      },
      error: (err: HttpErrorResponse) => {
        this.savingReview = false;
        const api = err.error as ApiError | undefined;
        this.reviewError = api?.message || 'certificates.saveFailed';
        this.cdr.markForCheck();
      },
    });
  }

  approveRequest(row: CertificateRequest): void {
    this.certificates.approveRequest(row.id).subscribe({
      next: () => {
        this.loadRequests();
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        const api = err.error as ApiError | undefined;
        this.requestError = api?.message || 'certificates.approveFailed';
        this.cdr.markForCheck();
      },
    });
  }

  openReject(row: CertificateRequest): void {
    this.rejecting = row;
    this.rejectionReason = '';
    this.rejectError = '';
    this.cdr.markForCheck();
  }

  closeReject(): void {
    this.rejecting = null;
    this.savingReject = false;
    this.cdr.markForCheck();
  }

  submitReject(): void {
    if (!this.rejecting) {
      return;
    }
    if (!this.rejectionReason.trim()) {
      this.rejectError = 'certificates.rejectReasonRequired';
      this.cdr.markForCheck();
      return;
    }
    this.savingReject = true;
    this.rejectError = '';
    this.certificates.rejectRequest(this.rejecting.id, this.rejectionReason.trim()).subscribe({
      next: () => {
        this.closeReject();
        this.loadRequests();
      },
      error: (err: HttpErrorResponse) => {
        this.savingReject = false;
        const api = err.error as ApiError | undefined;
        this.rejectError = api?.message || 'certificates.saveFailed';
        this.cdr.markForCheck();
      },
    });
  }

  sendBack(row: CertificateRequest): void {
    this.certificates.cancelRequest(row.id).subscribe({
      next: () => this.loadRequests(),
      error: (err: HttpErrorResponse) => {
        const api = err.error as ApiError | undefined;
        this.requestError = api?.message || 'certificates.saveFailed';
        this.cdr.markForCheck();
      },
    });
  }

  private emptyReview(): ReviewCertificateRequest {
    return {
      conductRemarks: '',
      academicProgress: '',
      lastExamAttended: '',
      reason: '',
      teacherNotes: '',
      duesLibrary: true,
      duesAccounts: true,
      duesSports: true,
    };
  }
}
