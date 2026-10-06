import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';
import { Payslip } from './payroll.model';
import { PayrollService } from './payroll.service';

@Component({
  selector: 'app-my-payslips',
  imports: [TranslateModule, MatButtonModule, EmptyStateComponent],
  template: `
    <div class="stack">
      @if (error) {
        <p class="error">{{ error }}</p>
      }
      @if (payslips.length === 0) {
        <app-empty-state [title]="'payroll.noPayslips' | translate" [hint]="'payroll.noPayslipsHint' | translate" />
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'payroll.period' | translate }}</th>
                <th>{{ 'common.status' | translate }}</th>
                <th class="num">{{ 'payroll.gross' | translate }}</th>
                <th class="num">{{ 'payroll.net' | translate }}</th>
                <th>{{ 'common.actions' | translate }}</th>
              </tr>
            </thead>
            <tbody>
              @for (slip of payslips; track slip.id) {
                <tr>
                  <td>{{ 'payroll.months.' + slip.month | translate }} {{ slip.year }}</td>
                  <td>{{ 'payroll.status.' + slip.runStatus | translate }}</td>
                  <td class="num">{{ formatMoney(slip.gross) }}</td>
                  <td class="num">{{ formatMoney(slip.net) }}</td>
                  <td>
                    <button mat-stroked-button type="button" (click)="preview(slip)">
                      {{ 'payroll.viewPdf' | translate }}
                    </button>
                    <button mat-button type="button" (click)="download(slip)">
                      {{ 'payroll.downloadPdf' | translate }}
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

      @if (previewUrl) {
        <div class="modal-backdrop" (click)="closePreview()">
          <div class="modal" (click)="$event.stopPropagation()">
            <div class="toolbar">
              <h2>{{ 'payroll.pdfPreview' | translate }}</h2>
              <span class="spacer"></span>
              <button mat-button type="button" (click)="closePreview()">{{ 'common.cancel' | translate }}</button>
            </div>
            <iframe class="preview" [src]="previewUrl" title="Payslip PDF"></iframe>
          </div>
        </div>
      }
    </div>
  `,
  styles: `
    .stack { display: flex; flex-direction: column; gap: 16px; }
    .table-wrap { overflow: auto; border: 1px solid var(--color-border); border-radius: 12px; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--color-border); }
    .num { text-align: right; font-variant-numeric: tabular-nums; }
    .error { color: #b91c1c; }
    .modal-backdrop {
      position: fixed; inset: 0; background: rgba(0,0,0,.45);
      display: flex; align-items: center; justify-content: center; z-index: 40; padding: 16px;
    }
    .modal {
      width: min(900px, 100%); height: min(90vh, 900px);
      background: var(--color-surface); border-radius: 12px;
      display: flex; flex-direction: column; overflow: hidden;
    }
    .toolbar { display: flex; align-items: center; gap: 8px; padding: 12px 16px; border-bottom: 1px solid var(--color-border); }
    .spacer { flex: 1; }
    .preview { flex: 1; width: 100%; border: 0; background: #fff; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyPayslipsComponent implements OnInit, OnDestroy {
  payslips: Payslip[] = [];
  error: string | null = null;
  previewUrl: SafeResourceUrl | null = null;
  private objectUrl: string | null = null;

  constructor(
    private payroll: PayrollService,
    private sanitizer: DomSanitizer,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.payroll.myPayslips().subscribe({
      next: (rows) => {
        this.payslips = rows;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'common.error';
        this.cdr.markForCheck();
      },
    });
  }

  ngOnDestroy(): void {
    this.revoke();
  }

  preview(slip: Payslip): void {
    this.payroll.downloadMyPayslip(slip.id).subscribe({
      next: (blob) => {
        this.revoke();
        this.objectUrl = URL.createObjectURL(blob);
        this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl);
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'payroll.exportFailed';
        this.cdr.markForCheck();
      },
    });
  }

  download(slip: Payslip): void {
    this.payroll.downloadMyPayslip(slip.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `payslip_${String(slip.month).padStart(2, '0')}_${slip.year}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.error = 'payroll.exportFailed';
        this.cdr.markForCheck();
      },
    });
  }

  closePreview(): void {
    this.previewUrl = null;
    this.revoke();
    this.cdr.markForCheck();
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value);
  }

  private revoke(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }
}
