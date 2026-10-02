import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { CertificateService } from '../../core/certificates/certificate.service';
import { CertificateIssued, CertificateRequest, CertificateType } from '../../core/certificates/certificate.model';
import { ApiError } from '../../core/models/api.model';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';

const CERT_TYPES: CertificateType[] = ['BONAFIDE', 'TC', 'CHARACTER', 'COURSE_COMPLETION'];

@Component({
  selector: 'app-download-certificates',
  imports: [TranslateModule, FormsModule, EmptyStateComponent],
  template: `
    <div class="card">
      <h2>{{ 'certificates.requestTitle' | translate }}</h2>
      <p class="muted">{{ 'downloads.certificatesPage.subtitle' | translate }}</p>
      <div class="form-grid">
        <div class="field">
          <label>{{ 'certificates.type' | translate }}</label>
          <select [(ngModel)]="requestType">
            @for (t of types; track t) {
              <option [value]="t">{{ ('certificates.types.' + t) | translate }}</option>
            }
          </select>
        </div>
        <div class="field span-2">
          <label>{{ 'certificates.requestReason' | translate }} *</label>
          <textarea rows="3" [(ngModel)]="requestReason"></textarea>
        </div>
        <div class="field span-2">
          <label>{{ 'certificates.supportingDoc' | translate }}</label>
          <input type="file" (change)="onFile($event)" />
        </div>
      </div>
      @if (requestError) {
        <p class="error">{{ requestError | translate }}</p>
      }
      @if (requestSuccess) {
        <p class="banner success">{{ requestSuccess | translate }}</p>
      }
      <div class="form-actions">
        <button class="btn btn-primary" type="button" [disabled]="submitting" (click)="submit()">
          {{ submitting ? ('common.loading' | translate) : ('certificates.requestTitle' | translate) }}
        </button>
      </div>
    </div>

    <div class="card">
      <h2>{{ 'certificates.history' | translate }}</h2>
      @if (historyError) {
        <p class="error">{{ historyError | translate }}</p>
      }
      @if (history.length === 0) {
        <app-empty-state icon="history" [title]="'common.noData' | translate" [hint]="'common.emptyHint' | translate" />
      } @else {
      <div class="data-table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>{{ 'certificates.type' | translate }}</th>
              <th>{{ 'common.status' | translate }}</th>
              <th>{{ 'certificates.requestReason' | translate }}</th>
              <th>{{ 'certificates.number' | translate }}</th>
              <th>{{ 'common.actions' | translate }}</th>
            </tr>
          </thead>
          <tbody>
            @for (row of history; track row.id) {
              <tr>
                <td>{{ ('certificates.types.' + row.certificateType) | translate }}</td>
                <td><span class="badge status-pill">{{ ('certificates.statuses.' + row.status) | translate }}</span></td>
                <td>{{ row.reason || '—' }}</td>
                <td>{{ row.certificateNo || '—' }}</td>
                <td class="actions">
                  @if (row.canCancel) {
                    <button class="btn" type="button" (click)="cancel(row)">{{ 'common.cancel' | translate }}</button>
                  }
                  @if (row.canDownload && row.issuedId) {
                    <button class="btn" type="button" (click)="downloadIssued(row.issuedId, row.certificateNo)">{{ 'certificates.download' | translate }}</button>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      }
    </div>

    <div class="card">
      <h2>{{ 'downloads.certificatesPage.title' | translate }}</h2>
      @if (error) {
        <p class="error">{{ error | translate }}</p>
      }
      @if (rows.length === 0) {
        <app-empty-state icon="workspace_premium" [title]="'common.noData' | translate" [hint]="'common.emptyHint' | translate" />
      } @else {
      <div class="data-table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>{{ 'certificates.number' | translate }}</th>
              <th>{{ 'certificates.type' | translate }}</th>
              <th>{{ 'certificates.student' | translate }}</th>
              <th>{{ 'certificates.issuedOn' | translate }}</th>
              <th>{{ 'common.actions' | translate }}</th>
            </tr>
          </thead>
          <tbody>
            @for (row of rows; track row.id) {
              <tr>
                <td>{{ row.certificateNo || '—' }}</td>
                <td>{{ ('certificates.types.' + row.certificateType) | translate }}</td>
                <td>{{ row.studentName }}</td>
                <td>{{ row.issuedDate || '—' }}</td>
                <td>
                  @if (row.canDownload) {
                    <button class="btn" type="button" (click)="download(row)">{{ 'certificates.download' | translate }}</button>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      }
    </div>
  `,
  styles: `
    .card { padding: 20px; margin-bottom: 16px; }
    h2 { font-size: 1.05rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); margin: 0 0 16px; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .span-2 { grid-column: span 2; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    input, select, textarea { padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; background: var(--color-surface); color: var(--color-text); }
    .form-actions { display: flex; justify-content: flex-end; margin-top: 12px; }
    .actions { display: flex; gap: 6px; flex-wrap: wrap; }
    .error { color: #b91c1c; }
    .banner.success { background: #ecfdf5; border: 1px solid #a7f3d0; color: #047857; padding: 10px 14px; border-radius: 8px; }
    @media (max-width: 720px) { .form-grid { grid-template-columns: 1fr; } .span-2 { grid-column: span 1; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DownloadCertificatesComponent implements OnInit {
  readonly types = CERT_TYPES;
  rows: CertificateIssued[] = [];
  history: CertificateRequest[] = [];
  error = '';
  historyError = '';
  requestType: CertificateType = 'BONAFIDE';
  requestReason = '';
  requestFile: File | null = null;
  requestError = '';
  requestSuccess = '';
  submitting = false;

  constructor(private certificates: CertificateService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadIssued();
    this.loadHistory();
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.requestFile = input.files?.[0] ?? null;
  }

  submit(): void {
    if (!this.requestReason.trim()) {
      this.requestError = 'certificates.reasonRequired';
      this.cdr.markForCheck();
      return;
    }
    this.submitting = true;
    this.requestError = '';
    this.requestSuccess = '';
    this.certificates.submitRequest(this.requestType, this.requestReason.trim(), this.requestFile).subscribe({
      next: () => {
        this.submitting = false;
        this.requestSuccess = 'certificates.submitted';
        this.requestReason = '';
        this.requestFile = null;
        this.loadHistory();
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.submitting = false;
        const api = err.error as ApiError | undefined;
        this.requestError = api?.message || 'certificates.saveFailed';
        this.cdr.markForCheck();
      },
    });
  }

  cancel(row: CertificateRequest): void {
    this.certificates.cancelRequest(row.id).subscribe({
      next: () => this.loadHistory(),
      error: (err: HttpErrorResponse) => {
        const api = err.error as ApiError | undefined;
        this.historyError = api?.message || 'certificates.saveFailed';
        this.cdr.markForCheck();
      },
    });
  }

  download(row: CertificateIssued): void {
    this.downloadIssued(row.id, row.certificateNo);
  }

  downloadIssued(id: string, certificateNo: string | null): void {
    this.certificates.download(id, false).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = (certificateNo || 'certificate') + '.pdf';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.error = 'certificates.downloadFailed';
        this.cdr.markForCheck();
      },
    });
  }

  private loadIssued(): void {
    this.certificates.mine().subscribe({
      next: (rows) => {
        this.rows = rows.filter((row) => row.canDownload);
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'certificates.loadFailed';
        this.cdr.markForCheck();
      },
    });
  }

  private loadHistory(): void {
    this.certificates.myRequests().subscribe({
      next: (rows) => {
        this.history = rows;
        this.cdr.markForCheck();
      },
      error: () => {
        this.historyError = 'certificates.loadFailed';
        this.cdr.markForCheck();
      },
    });
  }
}
