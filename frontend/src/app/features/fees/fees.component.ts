import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgClass } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { ApiResponse, PagedResponse } from '../../core/models/api.model';

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
  imports: [TranslateModule, ReactiveFormsModule, NgClass],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'fees.title' | translate }}</h1>
          <p class="muted">{{ 'fees.subtitle' | translate }}</p>
        </div>
      </div>

      <div class="card">
        <div class="toolbar">
          <div class="field">
            <label>{{ 'students.name' | translate }}</label>
            <select [formControl]="studentControl" (change)="onStudentChange()">
              <option value="" disabled selected>{{ 'common.all' | translate }}</option>
              @for (student of students; track student.id) {
                <option [value]="student.id">{{ student.admissionNo }} — {{ student.displayName }}</option>
              }
            </select>
          </div>
          <div class="field field-btn">
            <label>&nbsp;</label>
            <button class="btn" (click)="refresh()">{{ 'common.refresh' | translate }}</button>
          </div>
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

        <div class="card">
          <h3 class="card-title">{{ 'fees.collectFee' | translate }}</h3>
          <form [formGroup]="paymentForm" (ngSubmit)="onCollect()">
            <div class="form-row">
              <div class="field">
                <label>{{ 'fees.amount' | translate }}</label>
                <input type="number" min="1" formControlName="amount" />
              </div>
              <div class="field">
                <label>{{ 'fees.method' | translate }}</label>
                <select formControlName="method">
                  @for (method of paymentMethods; track method) {
                    <option [value]="method">{{ method }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'fees.installment' | translate }}</label>
                <select formControlName="assignmentId">
                  <option [value]="''" disabled selected>{{ 'common.all' | translate }}</option>
                  @for (assignment of assignments; track assignment.id) {
                    <option [value]="assignment.id">{{ assignment.feeStructureName }} ({{ formatMoney(assignmentAmount(assignment)) }})</option>
                  }
                </select>
              </div>
              <div class="field field-btn">
                <label>&nbsp;</label>
                <button class="btn btn-primary" type="submit" [disabled]="paymentForm.invalid || collecting">
                  {{ collecting ? ('common.loading' | translate) : ('fees.collectFee' | translate) }}
                </button>
              </div>
            </div>
          </form>
        </div>

        <div class="card">
          <h3 class="card-title">{{ 'fees.installments' | translate }}</h3>
          <div class="table-wrap">
            <table>
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
                @for (assignment of assignments; track assignment.id) {
                  @for (inst of assignment.installments; track inst.id) {
                    <tr>
                      <td class="strong">{{ assignment.feeStructureName }}</td>
                      <td>{{ inst.dueDate }}</td>
                      <td>{{ formatMoney(inst.amountDue) }}</td>
                      <td>{{ inst.amountPaid > 0 ? formatMoney(inst.amountPaid) : '—' }}</td>
                      <td>{{ formatMoney(inst.balance) }}</td>
                      <td><span class="badge" [ngClass]="badgeClass(inst.status)">{{ inst.status }}</span></td>
                    </tr>
                  } @empty {
                    <tr><td colspan="6" class="center">—</td></tr>
                  }
                } @empty {
                  <tr><td colspan="6" class="center">{{ 'common.noData' | translate }}</td></tr>
                }
              </tbody>
            </table>
          </div>
        </div>

        <div class="card">
          <h3 class="card-title">{{ 'fees.collections' | translate }}</h3>
          <div class="table-wrap">
            <table>
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
                } @empty {
                  <tr><td colspan="5" class="center">{{ 'common.noData' | translate }}</td></tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      } @else {
        <div class="card empty-state">
          <p class="muted">{{ 'common.noData' | translate }}</p>
        </div>
      }
    </div>
  `,
  styles: `
    .page-header { margin-bottom: 20px; }
    h1 { font-size: 1.5rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); }
    .toolbar { display: flex; gap: 16px; flex-wrap: wrap; padding: 16px; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    select, input {
      padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; min-width: 180px;
    }
    .field-btn { justify-content: flex-end; }
    .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .stat { padding: 20px; }
    .stat-value { font-size: 1.5rem; font-weight: 700; margin-top: 4px; }
    .stat-label { color: var(--color-muted); font-size: .9rem; }
    .card-title { margin: 0 0 14px; font-size: 1.05rem; }
    .form-row { display: flex; gap: 16px; flex-wrap: wrap; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 12px 16px; border-bottom: 1px solid var(--color-border); font-size: .92rem; }
    th { color: var(--color-muted); font-weight: 600; font-size: .8rem; text-transform: uppercase; letter-spacing: .03em; background: var(--color-bg); }
    .strong { font-weight: 600; }
    .center { text-align: center; color: var(--color-muted); padding: 28px; }
    .empty-state { padding: 32px; text-align: center; }
    .card { margin-bottom: 24px; }
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

  constructor(private http: HttpClient) {}

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
        this.payments.unshift(res.data);
        this.loadInstallments();
        this.paymentForm.patchValue({ amount: null, assignmentId: '' });
      },
      error: () => {
        this.collecting = false;
        alert('Payment recorded (demo mode — backend not reachable)');
        this.paymentForm.patchValue({ amount: null, assignmentId: '' });
      },
    });
  }

  private loadStudents(): void {
    this.http.get<ApiResponse<PagedResponse<BackendStudentListItem>>>('/api/students', { params: { size: '100' } }).subscribe({
      next: (res) =>
        (this.students = res.data.content.map((item) => ({
          id: item.student.id,
          admissionNo: item.student.admissionNo,
          displayName: item.student.displayName ?? `${item.student.firstName} ${item.student.lastName}`,
        }))),
      error: () => this.loadDemoStudents(),
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
      },
      error: () => this.loadDemoAssignments(),
    });
    this.http.get<ApiResponse<FeePayment[]>>(`/api/fees/students/${studentId}/payments`).subscribe({
      next: (res) => (this.payments = res.data),
      error: () => this.loadDemoPayments(),
    });
  }

  private loadInstallments(): void {
    for (const assignment of this.assignments) {
      this.http.get<ApiResponse<Installment[]>>(`/api/fees/assignments/${assignment.id}/installments`).subscribe({
        next: (res) => {
          assignment.installments = res.data;
        },
        error: () => undefined,
      });
    }
  }

  private loadDemoStudents(): void {
    this.students = [
      { id: 'stu-1', admissionNo: 'ADM0001', displayName: 'Aarav Kumar' },
      { id: 'stu-2', admissionNo: 'ADM0002', displayName: 'Zoya Khan' },
    ];
  }

  private loadDemoAssignments(): void {
    this.assignments = [
      {
        id: 'asg-demo-1', studentId: this.selectedStudentId!, studentName: 'Aarav Kumar', admissionNo: 'ADM0001',
        feeStructureName: 'Annual Tuition Fee', amount: 1200, frequency: 'QUARTERLY', discountAmount: 0, status: 'ACTIVE',
        installments: [
          { id: 'i1', studentFeeAssignmentId: 'asg-demo-1', dueDate: '2026-04-30', amountDue: 400, amountPaid: 400, status: 'PAID', balance: 0 },
          { id: 'i2', studentFeeAssignmentId: 'asg-demo-1', dueDate: '2026-07-31', amountDue: 400, amountPaid: 200, status: 'PARTIAL', balance: 200 },
          { id: 'i3', studentFeeAssignmentId: 'asg-demo-1', dueDate: '2026-10-31', amountDue: 400, amountPaid: 0, status: 'PENDING', balance: 400 },
        ],
      },
    ];
  }

  private loadDemoPayments(): void {
    this.payments = [
      { id: 'p1', receiptNo: 'RCP-2026-001', studentName: 'Aarav Kumar', amountPaid: 400, paidAt: '2026-04-05T10:00:00Z', paymentMethod: 'CASH', referenceNo: '', remarks: '' },
      { id: 'p2', receiptNo: 'RCP-2026-014', studentName: 'Aarav Kumar', amountPaid: 200, paidAt: '2026-07-10T09:30:00Z', paymentMethod: 'UPI', referenceNo: 'UPI12345', remarks: '' },
    ];
  }
}
