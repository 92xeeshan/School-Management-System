import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';
import { MONTHS, PayrollRun, Payslip } from './payroll.model';
import { PayrollService } from './payroll.service';

@Component({
  selector: 'app-payroll-runs',
  imports: [
    TranslateModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    EmptyStateComponent,
  ],
  template: `
    <div class="stack">
      @if (canManage) {
        <form class="toolbar" [formGroup]="form" (ngSubmit)="create()">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>{{ 'payroll.year' | translate }}</mat-label>
            <input matInput type="number" formControlName="year" />
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>{{ 'payroll.month' | translate }}</mat-label>
            <mat-select formControlName="month">
              @for (month of months; track month) {
                <mat-option [value]="month">{{ 'payroll.months.' + month | translate }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving">
            {{ 'payroll.createRun' | translate }}
          </button>
        </form>
      }

      @if (error) {
        <p class="error">{{ error }}</p>
      }

      @if (runs.length === 0) {
        <app-empty-state [title]="'common.noData' | translate" [hint]="'common.emptyHint' | translate" />
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'payroll.period' | translate }}</th>
                <th>{{ 'common.status' | translate }}</th>
                <th class="num">{{ 'payroll.payslips' | translate }}</th>
                <th class="num">{{ 'payroll.netTotal' | translate }}</th>
                <th>{{ 'common.actions' | translate }}</th>
              </tr>
            </thead>
            <tbody>
              @for (run of runs; track run.id) {
                <tr>
                  <td>{{ 'payroll.months.' + run.month | translate }} {{ run.year }}</td>
                  <td><span class="badge" [attr.data-status]="run.status">{{ 'payroll.status.' + run.status | translate }}</span></td>
                  <td class="num">{{ run.payslipCount }}</td>
                  <td class="num">{{ formatMoney(run.netTotal) }}</td>
                  <td class="actions">
                    @if (run.status === 'DRAFT' && canManage) {
                      <button mat-stroked-button type="button" (click)="process(run)">{{ 'payroll.process' | translate }}</button>
                    }
                    @if (run.status === 'PROCESSED' && canPublish) {
                      <button mat-flat-button color="primary" type="button" (click)="publish(run)">
                        {{ 'payroll.publishSlips' | translate }}
                      </button>
                    }
                    @if (run.status === 'PUBLISHED' && canManage) {
                      <button mat-stroked-button type="button" (click)="markPaid(run)">{{ 'payroll.markPaid' | translate }}</button>
                    }
                    @if (run.status !== 'DRAFT') {
                      <button mat-button type="button" (click)="togglePayslips(run)">
                        {{ selectedRunId === run.id ? ('payroll.hidePayslips' | translate) : ('payroll.viewPayslips' | translate) }}
                      </button>
                    }
                  </td>
                </tr>
                @if (selectedRunId === run.id) {
                  <tr class="detail">
                    <td colspan="5">
                      @if (payslips.length === 0) {
                        <p class="muted">{{ 'common.noData' | translate }}</p>
                      } @else {
                        <table class="inner">
                          <thead>
                            <tr>
                              <th>{{ 'payroll.employee' | translate }}</th>
                              <th>{{ 'payroll.employeeNo' | translate }}</th>
                              <th class="num">{{ 'payroll.gross' | translate }}</th>
                              <th class="num">{{ 'payroll.net' | translate }}</th>
                            </tr>
                          </thead>
                          <tbody>
                            @for (slip of payslips; track slip.id) {
                              <tr>
                                <td>{{ slip.staffName }}</td>
                                <td>{{ slip.employeeNo }}</td>
                                <td class="num">{{ formatMoney(slip.gross) }}</td>
                                <td class="num">{{ formatMoney(slip.net) }}</td>
                              </tr>
                            }
                          </tbody>
                        </table>
                      }
                    </td>
                  </tr>
                }
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  styles: `
    .stack { display: flex; flex-direction: column; gap: 16px; }
    .toolbar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
    .table-wrap { overflow: auto; border: 1px solid var(--color-border); border-radius: 12px; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--color-border); vertical-align: middle; }
    th { font-size: .8rem; color: var(--color-muted); }
    .num { text-align: right; font-variant-numeric: tabular-nums; }
    .actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .badge { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: .75rem; font-weight: 700; background: var(--color-primary-soft); }
    .badge[data-status='DRAFT'] { background: #e2e8f0; }
    .badge[data-status='PROCESSED'] { background: #fde68a; }
    .badge[data-status='PUBLISHED'] { background: #bbf7d0; }
    .badge[data-status='PAID'] { background: #c7d2fe; }
    .error { color: #b91c1c; }
    .muted { color: var(--color-muted); }
    .inner { margin-top: 8px; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PayrollRunsComponent implements OnInit {
  runs: PayrollRun[] = [];
  payslips: Payslip[] = [];
  selectedRunId: string | null = null;
  saving = false;
  error: string | null = null;
  months = MONTHS;
  form;

  constructor(
    private payroll: PayrollService,
    private auth: AuthService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    const now = new Date();
    this.form = this.fb.nonNullable.group({
      year: [now.getFullYear(), Validators.required],
      month: [now.getMonth() + 1, Validators.required],
    });
  }

  get canManage(): boolean {
    return this.auth.hasPermission('PAYROLL_MANAGE');
  }

  get canPublish(): boolean {
    return this.auth.hasPermission('PAYROLL_PUBLISH');
  }

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.payroll.listRuns().subscribe((rows) => {
      this.runs = rows;
      this.cdr.markForCheck();
    });
  }

  create(): void {
    if (this.form.invalid) {
      return;
    }
    this.saving = true;
    this.error = null;
    const value = this.form.getRawValue();
    this.payroll.createRun({ year: value.year, month: value.month, notes: null }).subscribe({
      next: () => {
        this.saving = false;
        this.refresh();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message ?? 'common.error';
        this.cdr.markForCheck();
      },
    });
  }

  process(run: PayrollRun): void {
    this.payroll.processRun(run.id).subscribe({
      next: () => this.refresh(),
      error: (err) => {
        this.error = err?.error?.message ?? 'common.error';
        this.cdr.markForCheck();
      },
    });
  }

  publish(run: PayrollRun): void {
    this.payroll.publishRun(run.id).subscribe({
      next: () => this.refresh(),
      error: (err) => {
        this.error = err?.error?.message ?? 'common.error';
        this.cdr.markForCheck();
      },
    });
  }

  markPaid(run: PayrollRun): void {
    this.payroll.markPaid(run.id).subscribe({
      next: () => this.refresh(),
      error: (err) => {
        this.error = err?.error?.message ?? 'common.error';
        this.cdr.markForCheck();
      },
    });
  }

  togglePayslips(run: PayrollRun): void {
    if (this.selectedRunId === run.id) {
      this.selectedRunId = null;
      this.payslips = [];
      this.cdr.markForCheck();
      return;
    }
    this.selectedRunId = run.id;
    this.payroll.listPayslips(run.id).subscribe((rows) => {
      this.payslips = rows;
      this.cdr.markForCheck();
    });
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value);
  }
}
