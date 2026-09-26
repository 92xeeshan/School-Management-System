import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, ViewChild } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { ApiResponse, PagedResponse } from '../../core/models/api.model';
import { AuthService } from '../../core/auth/auth.service';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';

interface StudentOption {
  id: string;
  admissionNo: string;
  displayName: string;
}

interface BackendStudentListItem {
  student: { id: string; admissionNo: string; firstName: string; lastName: string; displayName: string };
}

interface FeeAssignment {
  id: string;
  studentId: string;
  studentName: string;
  admissionNo: string;
  feeStructureName: string;
  amount: number;
  frequency: string;
  discountAmount: number;
  status: string;
  installments: Installment[];
}

interface Installment {
  id: string;
  studentFeeAssignmentId: string;
  dueDate: string;
  amountDue: number;
  amountPaid: number;
  status: string;
  balance: number;
}

interface InstallmentRow extends Installment {
  feeStructureName: string;
}

interface FeePayment {
  id: string;
  receiptNo: string;
  studentName: string;
  amountPaid: number;
  paidAt: string;
  paymentMethod: string;
  referenceNo: string;
  remarks: string;
}

const PAYMENT_METHODS = ['CASH', 'CARD', 'UPI', 'BANK_TRANSFER'];

@Component({
  selector: 'app-fees',
  imports: [
    TranslateModule,
    ReactiveFormsModule,
    NgClass,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatSelectModule,
    MatTableModule,
    PageHeaderComponent,
    EmptyStateComponent,
  ],
  template: `
    <div class="page">
      <app-page-header titleKey="fees.title" subtitleKey="fees.subtitle" />

      <div class="card">
        <div class="toolbar">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>{{ 'students.name' | translate }}</mat-label>
            <mat-select [formControl]="studentControl" (selectionChange)="onStudentChange()">
              @for (student of students; track student.id) {
                <mat-option [value]="student.id">{{ student.admissionNo }} — {{ student.displayName }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <button type="button" mat-stroked-button (click)="refresh()">
            <mat-icon>refresh</mat-icon>
            {{ 'common.refresh' | translate }}
          </button>
        </div>
      </div>

      @if (selectedStudent) {
        <div class="cards">
          <div class="card stat">
            <div class="stat-label">{{ 'fees.pendingAmount' | translate }}</div>
            <div class="stat-value">{{ formatMoney(totalPending) }}</div>
          </div>
          <div class="card stat">
            <div class="stat-label">{{ 'fees.totalCollected' | translate }}</div>
            <div class="stat-value">{{ formatMoney(totalPaid) }}</div>
          </div>
          <div class="card stat">
            <div class="stat-label">{{ 'fees.dues' | translate }}</div>
            <div class="stat-value">{{ overdueCount }}</div>
          </div>
        </div>

        @if (canCollect) {
          <div class="card">
            <div class="card-toolbar">
              <h3 class="card-title">{{ 'fees.collectFee' | translate }}</h3>
            </div>
            <form [formGroup]="paymentForm" (ngSubmit)="onCollect()">
              <div class="form-row">
                <mat-form-field appearance="outline" subscriptSizing="dynamic">
                  <mat-label>{{ 'fees.amount' | translate }}</mat-label>
                  <input matInput type="number" min="1" formControlName="amount" />
                </mat-form-field>
                <mat-form-field appearance="outline" subscriptSizing="dynamic">
                  <mat-label>{{ 'fees.method' | translate }}</mat-label>
                  <mat-select formControlName="method">
                    @for (method of paymentMethods; track method) {
                      <mat-option [value]="method">{{ method }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="outline" subscriptSizing="dynamic">
                  <mat-label>{{ 'fees.installment' | translate }}</mat-label>
                  <mat-select formControlName="assignmentId">
                    @for (assignment of assignments; track assignment.id) {
                      <mat-option [value]="assignment.id">
                        {{ assignment.feeStructureName }} ({{ formatMoney(assignmentAmount(assignment)) }})
                      </mat-option>
                    }
                  </mat-select>
                </mat-form-field>
                <button
                  type="submit"
                  mat-flat-button
                  color="primary"
                  class="collect-btn"
                  [disabled]="paymentForm.invalid || collecting"
                >
                  {{ collecting ? ('common.loading' | translate) : ('fees.collectFee' | translate) }}
                </button>
              </div>
            </form>
          </div>
        }

        <div class="card">
          <div class="card-toolbar">
            <h3 class="card-title">{{ 'fees.installments' | translate }}</h3>
          </div>

          @if (installmentsDataSource.data.length > 0) {
            <div class="table-scroll">
              <table mat-table [dataSource]="installmentsDataSource" class="app-table">
                <ng-container matColumnDef="feeStructure">
                  <th mat-header-cell *matHeaderCellDef>{{ 'fees.feeStructure' | translate }}</th>
                  <td mat-cell *matCellDef="let row" class="strong">{{ row.feeStructureName }}</td>
                </ng-container>
                <ng-container matColumnDef="dueDate">
                  <th mat-header-cell *matHeaderCellDef>{{ 'fees.dueDate' | translate }}</th>
                  <td mat-cell *matCellDef="let row">{{ row.dueDate }}</td>
                </ng-container>
                <ng-container matColumnDef="amount">
                  <th mat-header-cell *matHeaderCellDef>{{ 'fees.amount' | translate }}</th>
                  <td mat-cell *matCellDef="let row">{{ formatMoney(row.amountDue) }}</td>
                </ng-container>
                <ng-container matColumnDef="paidOn">
                  <th mat-header-cell *matHeaderCellDef>{{ 'fees.paidOn' | translate }}</th>
                  <td mat-cell *matCellDef="let row">{{ row.amountPaid > 0 ? formatMoney(row.amountPaid) : '—' }}</td>
                </ng-container>
                <ng-container matColumnDef="balance">
                  <th mat-header-cell *matHeaderCellDef>{{ 'fees.pendingAmount' | translate }}</th>
                  <td mat-cell *matCellDef="let row">{{ formatMoney(row.balance) }}</td>
                </ng-container>
                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef>{{ 'common.status' | translate }}</th>
                  <td mat-cell *matCellDef="let row">
                    <span class="badge" [ngClass]="badgeClass(row.status)">{{ row.status }}</span>
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="installmentColumns; sticky: true"></tr>
                <tr mat-row *matRowDef="let row; columns: installmentColumns"></tr>
              </table>
            </div>
            <mat-paginator #installmentsPaginator [pageSizeOptions]="[5, 10, 25]" [pageSize]="5" showFirstLastButtons />
          } @else {
            <app-empty-state icon="receipt_long" titleKey="fees.noInstallments" hintKey="fees.noInstallmentsHint">
              <button type="button" mat-stroked-button (click)="refresh()">
                <mat-icon>refresh</mat-icon>
                {{ 'common.refresh' | translate }}
              </button>
            </app-empty-state>
          }
        </div>

        <div class="card">
          <div class="card-toolbar">
            <h3 class="card-title">{{ 'fees.collections' | translate }}</h3>
          </div>

          @if (paymentsDataSource.data.length > 0) {
            <div class="table-scroll">
              <table mat-table [dataSource]="paymentsDataSource" class="app-table">
                <ng-container matColumnDef="receiptNo">
                  <th mat-header-cell *matHeaderCellDef>{{ 'common.status' | translate }} #</th>
                  <td mat-cell *matCellDef="let row" class="strong">{{ row.receiptNo }}</td>
                </ng-container>
                <ng-container matColumnDef="amount">
                  <th mat-header-cell *matHeaderCellDef>{{ 'fees.amount' | translate }}</th>
                  <td mat-cell *matCellDef="let row">{{ formatMoney(row.amountPaid) }}</td>
                </ng-container>
                <ng-container matColumnDef="paidOn">
                  <th mat-header-cell *matHeaderCellDef>{{ 'fees.paidOn' | translate }}</th>
                  <td mat-cell *matCellDef="let row">{{ row.paidAt ? formatDate(row.paidAt) : '—' }}</td>
                </ng-container>
                <ng-container matColumnDef="method">
                  <th mat-header-cell *matHeaderCellDef>{{ 'fees.method' | translate }}</th>
                  <td mat-cell *matCellDef="let row">{{ row.paymentMethod }}</td>
                </ng-container>
                <ng-container matColumnDef="reference">
                  <th mat-header-cell *matHeaderCellDef>{{ 'fees.installment' | translate }}</th>
                  <td mat-cell *matCellDef="let row">{{ row.referenceNo || '—' }}</td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="paymentColumns; sticky: true"></tr>
                <tr mat-row *matRowDef="let row; columns: paymentColumns"></tr>
              </table>
            </div>
            <mat-paginator #paymentsPaginator [pageSizeOptions]="[5, 10, 25]" [pageSize]="5" showFirstLastButtons />
          } @else {
            <app-empty-state icon="payments" titleKey="fees.noPayments" hintKey="fees.noPaymentsHint">
              <button type="button" mat-stroked-button (click)="refresh()">
                <mat-icon>refresh</mat-icon>
                {{ 'common.refresh' | translate }}
              </button>
            </app-empty-state>
          }
        </div>
      } @else {
        <div class="card">
          <app-empty-state icon="account_balance_wallet" titleKey="fees.selectStudentTitle" hintKey="fees.selectStudentHint">
            @if (canBrowseStudents) {
              <a mat-flat-button color="primary" routerLink="/students">
                <mat-icon>group</mat-icon>
                {{ 'fees.browseStudents' | translate }}
              </a>
            }
          </app-empty-state>
        </div>
      }
    </div>
  `,
  styles: `
    .muted { color: var(--color-muted); }
    .toolbar { display: flex; gap: 16px; flex-wrap: wrap; align-items: flex-start; padding: 16px; }
    .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .stat { padding: 20px; }
    .stat-value { font-size: 1.5rem; font-weight: 700; margin-top: 4px; }
    .stat-label { color: var(--color-muted); font-size: .9rem; }
    .card-title { margin: 0; font-size: 1.05rem; }
    .form-row { display: flex; gap: 16px; flex-wrap: wrap; align-items: flex-start; }
    .collect-btn { margin-top: 4px; height: 56px; }
    .strong { font-weight: 600; }
    .card { margin-bottom: 24px; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeesComponent implements OnInit, AfterViewInit {
  @ViewChild('installmentsPaginator') installmentsPaginator!: MatPaginator;
  @ViewChild('paymentsPaginator') paymentsPaginator!: MatPaginator;

  readonly paymentMethods = PAYMENT_METHODS;
  readonly studentControl = new FormControl('');
  readonly paymentForm = new FormGroup({
    amount: new FormControl<number | null>(null, Validators.required),
    method: new FormControl('CASH'),
    assignmentId: new FormControl(''),
  });

  readonly installmentColumns = ['feeStructure', 'dueDate', 'amount', 'paidOn', 'balance', 'status'];
  readonly paymentColumns = ['receiptNo', 'amount', 'paidOn', 'method', 'reference'];

  readonly installmentsDataSource = new MatTableDataSource<InstallmentRow>([]);
  readonly paymentsDataSource = new MatTableDataSource<FeePayment>([]);

  students: StudentOption[] = [];
  assignments: FeeAssignment[] = [];
  collecting = false;
  selectedStudentId: string | null = null;

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef, private auth: AuthService) {}

  get canCollect(): boolean {
    return this.auth.hasPermission('FEE_PAYMENT_RECORD');
  }

  get canBrowseStudents(): boolean {
    return this.auth.hasPermission('STUDENT_READ');
  }

  get selectedStudent(): boolean {
    return !!this.selectedStudentId;
  }

  formatDate(value: string): string {
    return new Date(value).toLocaleDateString();
  }

  get totalPending(): number {
    return this.assignments.reduce((sum, a) => sum + a.installments.reduce((s, i) => s + (i.balance ?? i.amountDue), 0), 0);
  }

  get totalPaid(): number {
    return this.paymentsDataSource.data.reduce((sum, p) => sum + p.amountPaid, 0);
  }

  get overdueCount(): number {
    return this.assignments.reduce((sum, a) => sum + a.installments.filter((i) => i.status === 'OVERDUE').length, 0);
  }

  ngOnInit(): void {
    this.loadStudents();
  }

  ngAfterViewInit(): void {
    this.installmentsDataSource.paginator = this.installmentsPaginator;
    this.paymentsDataSource.paginator = this.paymentsPaginator;
  }

  onStudentChange(): void {
    this.selectedStudentId = this.studentControl.value || null;
    this.paymentForm.patchValue({ assignmentId: '' });
    this.loadStudentData();
  }

  refresh(): void {
    this.loadStudentData();
  }

  assignmentAmount(assignment: FeeAssignment): number {
    return assignment.amount - (assignment.discountAmount ?? 0);
  }

  badgeClass(status: string): string {
    switch (status) {
      case 'PAID':
        return 'badge-success';
      case 'OVERDUE':
        return 'badge-danger';
      case 'PARTIAL':
        return 'badge-warning';
      default:
        return 'badge-muted';
    }
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(value);
  }

  onCollect(): void {
    const studentId = this.selectedStudentId;
    if (!studentId || this.paymentForm.invalid) {
      return;
    }
    this.collecting = true;
    const payload = {
      studentId,
      studentFeeAssignmentId: this.paymentForm.value.assignmentId || null,
      amountPaid: this.paymentForm.value.amount,
      paymentMethod: this.paymentForm.value.method,
    };
    this.http.post<ApiResponse<FeePayment>>('/api/fees/payments', payload).subscribe({
      next: (res) => {
        this.collecting = false;
        this.paymentsDataSource.data = [res.data, ...this.paymentsDataSource.data];
        this.loadInstallments();
        this.paymentForm.patchValue({ amount: null, assignmentId: '' });
        this.cdr.markForCheck();
      },
      error: () => {
        this.collecting = false;
        this.cdr.markForCheck();
        alert('Could not record payment. Please try again.');
      },
    });
  }

  private refreshInstallmentRows(): void {
    const rows: InstallmentRow[] = [];
    for (const assignment of this.assignments) {
      for (const installment of assignment.installments) {
        rows.push({ ...installment, feeStructureName: assignment.feeStructureName });
      }
    }
    this.installmentsDataSource.data = rows;
  }

  private loadStudents(): void {
    this.http.get<ApiResponse<PagedResponse<BackendStudentListItem>>>('/api/students', { params: { size: '100' } }).subscribe({
      next: (res) => {
        this.students = (res.data?.content ?? []).map((item) => ({
          id: item.student.id,
          admissionNo: item.student.admissionNo,
          displayName: item.student.displayName ?? `${item.student.firstName} ${item.student.lastName}`,
        }));
        this.cdr.markForCheck();
      },
      error: () => {
        this.students = [];
        this.cdr.markForCheck();
      },
    });
  }

  private loadStudentData(): void {
    const studentId = this.selectedStudentId;
    if (!studentId) {
      return;
    }
    this.http.get<ApiResponse<FeeAssignment[]>>(`/api/fees/students/${studentId}/assignments`).subscribe({
      next: (res) => {
        this.assignments = res.data.map((a) => ({ ...a, installments: [] }));
        this.loadInstallments();
        this.cdr.markForCheck();
      },
      error: () => {
        this.assignments = [];
        this.refreshInstallmentRows();
        this.cdr.markForCheck();
      },
    });
    this.http.get<ApiResponse<FeePayment[]>>(`/api/fees/students/${studentId}/payments`).subscribe({
      next: (res) => {
        this.paymentsDataSource.data = res.data;
        this.cdr.markForCheck();
      },
      error: () => {
        this.paymentsDataSource.data = [];
        this.cdr.markForCheck();
      },
    });
  }

  private loadInstallments(): void {
    if (this.assignments.length === 0) {
      this.refreshInstallmentRows();
      return;
    }
    let pending = this.assignments.length;
    for (const assignment of this.assignments) {
      this.http.get<ApiResponse<Installment[]>>(`/api/fees/assignments/${assignment.id}/installments`).subscribe({
        next: (res) => {
          assignment.installments = res.data;
          pending -= 1;
          if (pending <= 0) {
            this.refreshInstallmentRows();
            this.cdr.markForCheck();
          }
        },
        error: () => {
          pending -= 1;
          if (pending <= 0) {
            this.refreshInstallmentRows();
            this.cdr.markForCheck();
          }
        },
      });
    }
  }
}
