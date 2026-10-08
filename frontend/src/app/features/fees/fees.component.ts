import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgClass } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ApiResponse, PagedResponse } from '../../core/models/api.model';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';

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
    EmptyStateComponent,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <div class="stack">
      <div class="card">
        <div class="card-toolbar">
          <mat-form-field appearance="outline" subscriptSizing="dynamic" class="toolbar-field">
            <mat-label>{{ 'fees.selectStudent' | translate }}</mat-label>
            <mat-select [formControl]="studentControl" (selectionChange)="onStudentChange()">
              @for (student of students; track student.id) {
                <mat-option [value]="student.id">{{ student.admissionNo }} — {{ student.displayName }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <button mat-stroked-button type="button" (click)="refresh()" [disabled]="!selectedStudent">
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
        <div class="card collect-card">
          <h3 class="card-title">{{ 'fees.collectFee' | translate }}</h3>
          <form [formGroup]="paymentForm" (ngSubmit)="onCollect()">
            <div class="card-toolbar">
              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="toolbar-field">
                <mat-label>{{ 'fees.amount' | translate }}</mat-label>
                <input matInput type="number" min="1" formControlName="amount" />
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="toolbar-field">
                <mat-label>{{ 'fees.method' | translate }}</mat-label>
                <mat-select formControlName="method">
                  @for (method of paymentMethods; track method) {
                    <mat-option [value]="method">{{ method }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="toolbar-field wide">
                <mat-label>{{ 'fees.installment' | translate }}</mat-label>
                <mat-select formControlName="assignmentId">
                  <mat-option value="">{{ 'common.all' | translate }}</mat-option>
                  @for (assignment of assignments; track assignment.id) {
                    <mat-option [value]="assignment.id">{{ assignment.feeStructureName }} ({{ formatMoney(assignmentAmount(assignment)) }})</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              <button mat-flat-button color="primary" type="submit" [disabled]="paymentForm.invalid || collecting">
                {{ collecting ? ('common.loading' | translate) : ('fees.collectFee' | translate) }}
              </button>
            </div>
          </form>
        </div>
        }

        <div class="card">
          <h3 class="card-title">{{ 'fees.installments' | translate }}</h3>
          @if (installmentRows.length === 0) {
            <app-empty-state icon="payments" [title]="'common.noData' | translate" [hint]="'common.emptyHint' | translate" />
          } @else {
            <div class="data-table-wrap">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>{{ 'fees.feeStructure' | translate }}</th>
                    <th>{{ 'fees.dueDate' | translate }}</th>
                    <th>{{ 'fees.amount' | translate }}</th>
                    <th>{{ 'fees.paidOn' | translate }}</th>
                    <th>{{ 'fees.pendingAmount' | translate }}</th>
                    <th>{{ 'common.status' | translate }}</th>
                  </tr>
                </thead>
                <tbody>
                  @for (row of installmentRows; track row.id) {
                    <tr>
                      <td class="strong">{{ row.feeStructureName }}</td>
                      <td>{{ row.dueDate }}</td>
                      <td>{{ formatMoney(row.amountDue) }}</td>
                      <td>{{ row.amountPaid > 0 ? formatMoney(row.amountPaid) : '—' }}</td>
                      <td>{{ formatMoney(row.balance) }}</td>
                      <td><span class="badge status-pill" [ngClass]="badgeClass(row.status)">{{ row.status }}</span></td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>

        <div class="card">
          <h3 class="card-title">{{ 'fees.collections' | translate }}</h3>
          @if (payments.length === 0) {
            <app-empty-state icon="receipt_long" [title]="'common.noData' | translate" [hint]="'common.emptyHint' | translate" />
          } @else {
            <div class="data-table-wrap">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>{{ 'common.status' | translate }} #</th>
                    <th>{{ 'fees.amount' | translate }}</th>
                    <th>{{ 'fees.paidOn' | translate }}</th>
                    <th>{{ 'fees.method' | translate }}</th>
                    <th>{{ 'fees.installment' | translate }}</th>
                  </tr>
                </thead>
                <tbody>
                  @for (payment of payments; track payment.id) {
                    <tr>
                      <td class="strong">{{ payment.receiptNo }}</td>
                      <td>{{ formatMoney(payment.amountPaid) }}</td>
                      <td>{{ payment.paidAt ? formatDate(payment.paidAt) : '—' }}</td>
                      <td>{{ payment.paymentMethod }}</td>
                      <td>{{ payment.referenceNo || '—' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>
      } @else {
        <div class="card">
          <app-empty-state
            icon="person_search"
            [title]="'fees.selectStudent' | translate"
            [hint]="'fees.selectStudentHint' | translate" />
        </div>
      }
    </div>
  `,
  styles: `
    .stack { display: flex; flex-direction: column; gap: 16px; }
    .card { margin-bottom: 0; }
    .toolbar-field { min-width: 220px; }
    .toolbar-field.wide { min-width: 280px; flex: 1; }
    .collect-card { padding-bottom: 8px; }
    .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .stat { padding: 20px; }
    .stat-value { font-size: 1.5rem; font-weight: 700; margin-top: 4px; }
    .stat-label { color: var(--color-muted); font-size: .9rem; }
    .card-title { margin: 0; padding: 16px 16px 0; font-size: 1.05rem; }
    .strong { font-weight: 600; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeesComponent implements OnInit {
  readonly paymentMethods = PAYMENT_METHODS;
  readonly studentControl = new FormControl('');
  readonly paymentForm = new FormGroup({
    amount: new FormControl<number | null>(null, Validators.required),
    method: new FormControl('CASH'),
    assignmentId: new FormControl(''),
  });

  students: StudentOption[] = [];
  assignments: FeeAssignment[] = [];
  payments: FeePayment[] = [];
  collecting = false;
  selectedStudentId: string | null = null;

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef, private auth: AuthService) {}

  get canCollect(): boolean {
    return this.auth.hasPermission('FEE_PAYMENT_RECORD');
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
    return this.payments.reduce((sum, p) => sum + p.amountPaid, 0);
  }

  get overdueCount(): number {
    return this.assignments.reduce((sum, a) => sum + a.installments.filter((i) => i.status === 'OVERDUE').length, 0);
  }

  get installmentRows(): Array<Installment & { feeStructureName: string }> {
    return this.assignments.flatMap((assignment) =>
      assignment.installments.map((inst) => ({ ...inst, feeStructureName: assignment.feeStructureName }))
    );
  }

  ngOnInit(): void {
    this.loadStudents();
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
      case 'PAID': return 'badge-success';
      case 'OVERDUE': return 'badge-danger';
      case 'PARTIAL': return 'badge-warning';
      default: return 'badge-muted';
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
        this.payments = [res.data, ...this.payments];
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
        this.cdr.markForCheck();
      },
    });
    this.http.get<ApiResponse<FeePayment[]>>(`/api/fees/students/${studentId}/payments`).subscribe({
      next: (res) => {
        this.payments = res.data;
        this.cdr.markForCheck();
      },
      error: () => {
        this.payments = [];
        this.cdr.markForCheck();
      },
    });
  }

  private loadInstallments(): void {
    for (const assignment of this.assignments) {
      this.http.get<ApiResponse<Installment[]>>(`/api/fees/assignments/${assignment.id}/installments`).subscribe({
        next: (res) => {
          assignment.installments = res.data;
          this.cdr.markForCheck();
        },
        error: () => undefined,
      });
    }
  }
}
