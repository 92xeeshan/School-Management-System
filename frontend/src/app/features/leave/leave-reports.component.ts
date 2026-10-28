import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';
import { LeaveBalance, LeaveRequest } from './leave.model';
import { LeaveService } from './leave.service';

@Component({
  selector: 'app-leave-reports',
  imports: [TranslateModule, MatButtonModule, EmptyStateComponent],
  template: `
    <div class="stack">
      <section class="card">
        <div class="toolbar">
          <h2>{{ 'leave.onLeaveToday' | translate }}</h2>
        </div>
        @if (today.length === 0) {
          <app-empty-state [title]="'leave.nobodyOnLeave' | translate" />
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{{ 'leave.employee' | translate }}</th>
                  <th>{{ 'leave.type' | translate }}</th>
                  <th>{{ 'leave.from' | translate }}</th>
                  <th>{{ 'leave.to' | translate }}</th>
                </tr>
              </thead>
              <tbody>
                @for (row of today; track row.id) {
                  <tr>
                    <td>{{ row.staffName }}</td>
                    <td>{{ row.leaveTypeName }}</td>
                    <td>{{ row.fromDate }}</td>
                    <td>{{ row.toDate }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>

      <section class="card">
        <div class="toolbar">
          <h2>{{ 'leave.balanceReport' | translate }}</h2>
          <button mat-stroked-button type="button" (click)="downloadBalances()">{{ 'leave.exportExcel' | translate }}</button>
        </div>
        @if (balances.length === 0) {
          <app-empty-state [title]="'leave.noBalances' | translate" />
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{{ 'leave.employee' | translate }}</th>
                  <th>{{ 'leave.type' | translate }}</th>
                  <th>{{ 'leave.entitled' | translate }}</th>
                  <th>{{ 'leave.used' | translate }}</th>
                  <th>{{ 'leave.pending' | translate }}</th>
                  <th>{{ 'leave.available' | translate }}</th>
                </tr>
              </thead>
              <tbody>
                @for (row of balances; track row.staffId + row.leaveTypeId) {
                  <tr>
                    <td>{{ row.staffName }}</td>
                    <td>{{ row.leaveTypeName }}</td>
                    <td>{{ row.entitled }}</td>
                    <td>{{ row.used }}</td>
                    <td>{{ row.pending }}</td>
                    <td>{{ row.available }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>

      <section class="card">
        <div class="toolbar">
          <h2>{{ 'leave.takenReport' | translate }}</h2>
          <button mat-stroked-button type="button" (click)="downloadTaken()">{{ 'leave.exportExcel' | translate }}</button>
        </div>
        @if (taken.length === 0) {
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
                </tr>
              </thead>
              <tbody>
                @for (row of taken; track row.id) {
                  <tr>
                    <td>{{ row.staffName }}</td>
                    <td>{{ row.leaveTypeName }}</td>
                    <td>{{ row.fromDate }}</td>
                    <td>{{ row.toDate }}</td>
                    <td>{{ row.days }}</td>
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
    .card { padding: 16px; border: 1px solid var(--color-border); border-radius: 12px; }
    .toolbar { display: flex; justify-content: space-between; gap: 12px; align-items: center; margin-bottom: 12px; }
    .table-wrap { overflow: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--color-border); }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeaveReportsComponent implements OnInit {
  today: LeaveRequest[] = [];
  balances: LeaveBalance[] = [];
  taken: LeaveRequest[] = [];

  constructor(private leave: LeaveService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.leave.onLeaveToday().subscribe((rows) => {
      this.today = rows;
      this.cdr.markForCheck();
    });
    this.leave.balances().subscribe((rows) => {
      this.balances = rows;
      this.cdr.markForCheck();
    });
    this.leave.taken().subscribe((rows) => {
      this.taken = rows;
      this.cdr.markForCheck();
    });
  }

  downloadBalances(): void {
    this.leave.exportBalances().subscribe((blob) => this.save(blob, 'leave-balances.xlsx'));
  }

  downloadTaken(): void {
    this.leave.exportTaken().subscribe((blob) => this.save(blob, 'leave-taken.xlsx'));
  }

  private save(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }
}
