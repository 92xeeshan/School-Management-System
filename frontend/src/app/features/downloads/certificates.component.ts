import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { CertificateService } from '../../core/certificates/certificate.service';
import { CertificateIssued } from '../../core/certificates/certificate.model';

@Component({
  selector: 'app-download-certificates',
  imports: [TranslateModule],
  template: `
    <div class="card">
      <h2>{{ 'downloads.certificatesPage.title' | translate }}</h2>
      <p class="muted">{{ 'downloads.certificatesPage.subtitle' | translate }}</p>
      @if (error) {
        <p class="error">{{ error | translate }}</p>
      }
      <table>
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
          } @empty {
            <tr>
              <td colspan="5" class="center muted">{{ 'common.noData' | translate }}</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: `
    .card { background: #fff; border: 1px solid var(--color-border); border-radius: var(--radius); padding: 20px; }
    h2 { font-size: 1.05rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); margin: 0 0 16px; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 10px 8px; border-bottom: 1px solid var(--color-border); }
    .center { text-align: center; }
    .error { color: #b91c1c; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DownloadCertificatesComponent implements OnInit {
  rows: CertificateIssued[] = [];
  error = '';

  constructor(private certificates: CertificateService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
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

  download(row: CertificateIssued): void {
    this.certificates.download(row.id, false).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = (row.certificateNo || 'certificate') + '.pdf';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.error = 'certificates.downloadFailed';
        this.cdr.markForCheck();
      },
    });
  }
}
