import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { CertificateService } from '../../core/certificates/certificate.service';
import {
  CertificateIssued,
  CertificateStatus,
  CertificateTemplate,
  CertificateType,
} from '../../core/certificates/certificate.model';
import { ApiError } from '../../core/models/api.model';

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
                <option value="BONAFIDE">{{ 'certificates.types.BONAFIDE' | translate }}</option>
                <option value="TC">{{ 'certificates.types.TC' | translate }}</option>
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
        <div class="toolbar">
          <input type="text" [(ngModel)]="query" (keyup.enter)="load()" [placeholder]="'certificates.search' | translate" />
          <select [(ngModel)]="typeFilter" (ngModelChange)="load()">
            <option value="">{{ 'certificates.allTypes' | translate }}</option>
            <option value="BONAFIDE">{{ 'certificates.types.BONAFIDE' | translate }}</option>
            <option value="TC">{{ 'certificates.types.TC' | translate }}</option>
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
    .field { display: flex; flex-direction: column; gap: 6px; }
    .span-2 { grid-column: span 2; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    .check { display: flex; align-items: center; gap: 8px; margin: 12px 0; }
    .form-actions { display: flex; justify-content: flex-end; margin-top: 12px; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 10px 8px; border-bottom: 1px solid var(--color-border); vertical-align: top; }
    .actions { display: flex; gap: 6px; flex-wrap: wrap; }
    .badge { display: inline-block; margin-left: 6px; background: #fef3c7; color: #92400e; padding: 2px 6px; border-radius: 999px; font-size: .75rem; }
    .center { text-align: center; }
    .error { color: #b91c1c; }
    .banner.success { background: #ecfdf5; border: 1px solid #a7f3d0; color: #047857; padding: 10px 14px; border-radius: 8px; }
    @media (max-width: 720px) { .form-grid { grid-template-columns: 1fr; } .span-2 { grid-column: span 1; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CertificatesComponent implements OnInit {
  rows: CertificateIssued[] = [];
  templates: CertificateTemplate[] = [];
  query = '';
  typeFilter: CertificateType | '' = '';
  statusFilter: CertificateStatus | '' = '';
  error = '';
  templateType: CertificateType = 'BONAFIDE';
  headerHtml = '';
  footerHtml = '';
  requiresApproval = true;
  savingTemplate = false;
  templateMessage = '';
  templateError = '';

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
    this.certificates.download(row.id, reprint).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = (row.certificateNo || 'certificate') + (reprint ? '-duplicate' : '') + '.pdf';
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
}
