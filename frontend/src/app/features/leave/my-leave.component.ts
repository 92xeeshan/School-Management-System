import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';
import { LeaveBalance, LeaveRequest, LeaveRequestStatus, LeaveType } from './leave.model';
import { LeaveService } from './leave.service';
import { PagedResponse } from '../../core/models/api.model';

@Component({
  selector: 'app-my-leave',
  imports: [TranslateModule, ReactiveFormsModule, MatButtonModule, EmptyStateComponent],
  template: `
    <div class="stack">
      @if (error) {
        <p class="error">{{ error }}</p>
      }
      @if (message) {
        <p class="banner success">{{ message }}</p>
      }

      <section>
        <h2>{{ 'leave.balances' | translate }}</h2>
        @if (balances.length === 0) {
          <app-empty-state [title]="'leave.noBalances' | translate" [hint]="'leave.noBalancesHint' | translate" />
        } @else {
          <div class="cards">
            @for (row of balances; track row.leaveTypeId) {
              <article class="stat-card">
                <p class="muted">{{ row.leaveTypeName }}</p>
                <h3>{{ row.available }}</h3>
                <p class="muted">{{ 'leave.available' | translate }}</p>
                <p class="meta">
                  {{ 'leave.entitled' | translate }} {{ row.entitled }}
                  · {{ 'leave.used' | translate }} {{ row.used }}
                  · {{ 'leave.pending' | translate }} {{ row.pending }}
                </p>
              </article>
            }
          </div>
        }
      </section>

      @if (canApply) {
        <form class="card form" [formGroup]="form" (ngSubmit)="submit()">
          <h2>{{ 'leave.apply' | translate }}</h2>
          <label>
            {{ 'leave.type' | translate }}
            <select formControlName="leaveTypeId">
              @for (type of activeTypes; track type.id) {
                <option [value]="type.id">{{ type.name }}</option>
              }
            </select>
          </label>
          <label>
            {{ 'leave.from' | translate }}
            <input type="date" formControlName="fromDate" (change)="refreshDays()" />
          </label>
          <label>
            {{ 'leave.to' | translate }}
            <input type="date" formControlName="toDate" (change)="refreshDays()" />
          </label>
          <p class="muted">{{ 'leave.days' | translate }}: {{ previewDays ?? '—' }}</p>
          <label class="wide">
            {{ 'leave.reason' | translate }}
            <textarea rows="3" formControlName="reason"></textarea>
          </label>
          <label>
            {{ 'leave.document' | translate }}
            <input type="file" (change)="onFile($event)" />
          </label>
          <div class="actions">
            <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving">
              {{ 'leave.submit' | translate }}
            </button>
          </div>
        </form>
      }

      <section class="card">
        <div class="toolbar">
          <h2>{{ 'leave.history' | translate }}</h2>
          <select [value]="statusFilter" (change)="onStatus($any($event.target).value)">
            <option value="">{{ 'leave.allStatuses' | translate }}</option>
            <option value="PENDING">{{ 'leave.status.PENDING' | translate }}</option>
            <option value="APPROVED">{{ 'leave.status.APPROVED' | translate }}</option>
            <option value="REJECTED">{{ 'leave.status.REJECTED' | translate }}</option>
            <option value="CANCELLED">{{ 'leave.status.CANCELLED' | translate }}</option>
          </select>
        </div>
        @if (history.length === 0) {
          <app-empty-state [title]="'leave.noRequests' | translate" />
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{{ 'leave.type' | translate }}</th>
                  <th>{{ 'leave.from' | translate }}</th>
                  <th>{{ 'leave.to' | translate }}</th>
                  <th>{{ 'leave.days' | translate }}</th>
                  <th>{{ 'common.status' | translate }}</th>
                  <th>{{ 'common.actions' | translate }}</th>
                </tr>
              </thead>
              <tbody>
                @for (row of history; track row.id) {
                  <tr>
                    <td>{{ row.leaveTypeName }}</td>
                    <td>{{ row.fromDate }}</td>
                    <td>{{ row.toDate }}</td>
                    <td>{{ row.days }}</td>
                    <td>{{ ('leave.status.' + row.status) | translate }}</td>
                    <td>
                      @if (row.canCancel) {
                        <button mat-button type="button" (click)="cancel(row)">{{ 'leave.cancel' | translate }}</button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>
    </div>
  `,
  styles: `
    .stack { display: flex; flex-direction: column; gap: 20px; }
    .cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; }
    .stat-card { border: 1px solid var(--color-border); border-radius: 12px; padding: 16px; background: var(--color-surface); }
    .stat-card h3 { margin: 4px 0; font-size: 1.6rem; }
    .meta { font-size: .8rem; color: var(--color-muted); }
    .form, .card { display: grid; gap: 12px; padding: 16px; border: 1px solid var(--color-border); border-radius: 12px; }
    label { display: flex; flex-direction: column; gap: 6px; font-size: .86rem; }
    input, select, textarea { padding: 8px 10px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; }
    .toolbar { display: flex; justify-content: space-between; gap: 12px; align-items: center; }
    .table-wrap { overflow: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--color-border); }
    .error { color: #b91c1c; }
    .banner.success { color: #166534; }
    @media (max-width: 768px) { .cards { grid-template-columns: 1fr 1fr; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyLeaveComponent implements OnInit {
  balances: LeaveBalance[] = [];
  types: LeaveType[] = [];
  history: LeaveRequest[] = [];
  statusFilter: LeaveRequestStatus | '' = '';
  previewDays: number | null = null;
  file: File | null = null;
  saving = false;
  error = '';
  message = '';
  form;

  constructor(
    private leave: LeaveService,
    private auth: AuthService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef,
  ) {
    this.form = this.fb.nonNullable.group({
      leaveTypeId: ['', Validators.required],
      fromDate: ['', Validators.required],
      toDate: ['', Validators.required],
      reason: ['', Validators.required],
    });
  }

  get canApply(): boolean {
    return this.auth.hasPermission('LEAVE_APPLY');
  }

  get activeTypes(): LeaveType[] {
    return this.types.filter((type) => type.active);
  }

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.leave.myBalances().subscribe({
      next: (rows) => {
        this.balances = rows;
        this.cdr.markForCheck();
      },
      error: () => this.fail('leave.loadFailed'),
    });
    this.leave.listTypes().subscribe({
      next: (rows) => {
        this.types = rows;
        if (!this.form.value.leaveTypeId && rows.length) {
          this.form.patchValue({ leaveTypeId: rows[0].id });
        }
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
    this.loadHistory();
  }

  loadHistory(): void {
    this.leave.myRequests({ status: this.statusFilter }).subscribe({
      next: (page: PagedResponse<LeaveRequest>) => {
        this.history = page.content ?? [];
        this.cdr.markForCheck();
      },
      error: () => this.fail('leave.loadFailed'),
    });
  }

  onStatus(status: LeaveRequestStatus | ''): void {
    this.statusFilter = status;
    this.loadHistory();
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.file = input.files?.[0] ?? null;
  }

  refreshDays(): void {
    const from = this.form.value.fromDate;
    const to = this.form.value.toDate;
    if (!from || !to) {
      this.previewDays = null;
      return;
    }
    this.leave.previewDays(from, to).subscribe({
      next: (result) => {
        this.previewDays = result.days;
        this.cdr.markForCheck();
      },
      error: () => {
        this.previewDays = null;
        this.cdr.markForCheck();
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.saving) {
      return;
    }
    this.saving = true;
    this.error = '';
    this.message = '';
    this.leave.apply(this.form.getRawValue(), this.file).subscribe({
      next: () => {
        this.saving = false;
        this.message = 'Leave request submitted.';
        this.form.patchValue({ reason: '' });
        this.file = null;
        this.reload();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Could not submit leave request.';
        this.cdr.markForCheck();
      },
    });
  }

  cancel(row: LeaveRequest): void {
    const reason = window.prompt('Cancellation reason');
    if (!reason) {
      return;
    }
    this.leave.cancel(row.id, reason).subscribe({
      next: () => this.reload(),
      error: (err) => this.fail(err?.error?.message || 'Could not cancel leave.'),
    });
  }

  private fail(message: string): void {
    this.error = message;
    this.cdr.markForCheck();
  }
}
