import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';
import { LeaveRequest, LeaveRequestStatus } from './leave.model';
import { LeaveService } from './leave.service';

@Component({
  selector: 'app-leave-approvals',
  imports: [TranslateModule, MatButtonModule, EmptyStateComponent],
  template: `
    <div class="stack">
      @if (error) {
        <p class="error">{{ error }}</p>
      }
      <div class="toolbar">
        <select [value]="statusFilter" (change)="onStatus($any($event.target).value)">
          <option value="PENDING">{{ 'leave.status.PENDING' | translate }}</option>
          <option value="APPROVED">{{ 'leave.status.APPROVED' | translate }}</option>
          <option value="REJECTED">{{ 'leave.status.REJECTED' | translate }}</option>
          <option value="CANCELLED">{{ 'leave.status.CANCELLED' | translate }}</option>
          <option value="">{{ 'leave.allStatuses' | translate }}</option>
        </select>
      </div>
      @if (rows.length === 0) {
        <app-empty-state [title]="'leave.noRequests' | translate" />
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'leave.employee' | translate }}</th>
                <th>{{ 'leave.type' | translate }}</th>
                <th>{{ 'leave.from' | translate }}</th>
                <th>{{ 'leave.to' | translate }}</th>
                <th>{{ 'leave.days' | translate }}</th>
                <th>{{ 'leave.balance' | translate }}</th>
                <th>{{ 'leave.reason' | translate }}</th>
                <th>{{ 'common.status' | translate }}</th>
                <th>{{ 'common.actions' | translate }}</th>
              </tr>
            </thead>
            <tbody>
              @for (row of rows; track row.id) {
                <tr>
                  <td>{{ row.staffName }}</td>
                  <td>{{ row.leaveTypeName }}</td>
                  <td>{{ row.fromDate }}</td>
                  <td>{{ row.toDate }}</td>
                  <td>{{ row.days }}</td>
                  <td>
                    @if (row.balance) {
                      {{ row.balance.available }} / {{ row.balance.entitled }}
                    } @else {
                      —
                    }
                  </td>
                  <td>{{ row.reason }}</td>
                  <td>{{ ('leave.status.' + row.status) | translate }}</td>
                  <td>
                    @if (row.canApprove) {
                      <button mat-flat-button color="primary" type="button" (click)="approve(row)">
                        {{ 'leave.approve' | translate }}
                      </button>
                      <button mat-button type="button" (click)="reject(row)">
                        {{ 'leave.reject' | translate }}
                      </button>
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
    .stack { display: flex; flex-direction: column; gap: 16px; }
    .toolbar { display: flex; gap: 12px; }
    select { padding: 8px 10px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; }
    .table-wrap { overflow: auto; border: 1px solid var(--color-border); border-radius: 12px; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--color-border); }
    .error { color: #b91c1c; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeaveApprovalsComponent implements OnInit {
  rows: LeaveRequest[] = [];
  statusFilter: LeaveRequestStatus | '' = 'PENDING';
  error = '';

  constructor(private leave: LeaveService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.reload();
  }

  onStatus(status: LeaveRequestStatus | ''): void {
    this.statusFilter = status;
    this.reload();
  }

  reload(): void {
    this.leave.listRequests({ status: this.statusFilter }).subscribe({
      next: (page) => {
        this.rows = page.content ?? [];
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not load leave requests.';
        this.cdr.markForCheck();
      },
    });
  }

  approve(row: LeaveRequest): void {
    this.leave.approve(row.id).subscribe({
      next: () => this.reload(),
      error: (err) => {
        this.error = err?.error?.message || 'Could not approve leave.';
        this.cdr.markForCheck();
      },
    });
  }

  reject(row: LeaveRequest): void {
    const reason = window.prompt('Rejection reason');
    if (!reason) {
      return;
    }
    this.leave.reject(row.id, reason).subscribe({
      next: () => this.reload(),
      error: (err) => {
        this.error = err?.error?.message || 'Could not reject leave.';
        this.cdr.markForCheck();
      },
    });
  }
}
