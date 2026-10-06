import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';
import { Expense, ExpenseCategory, ExpenseChart, PAYMENT_METHODS } from './payroll.model';
import { PayrollService } from './payroll.service';
import { ExpenseChartComponent } from './expense-chart.component';

@Component({
  selector: 'app-payroll-expenses',
  imports: [
    TranslateModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    EmptyStateComponent,
    ExpenseChartComponent,
  ],
  template: `
    <div class="stack">
      <div class="toolbar">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>{{ 'payroll.year' | translate }}</mat-label>
          <mat-select [value]="year" (selectionChange)="onYear($event.value)">
            @for (option of years; track option) {
              <mat-option [value]="option">{{ option }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        @if (canManage) {
          <button mat-flat-button color="primary" type="button" (click)="showForm = !showForm">
            {{ 'payroll.addExpense' | translate }}
          </button>
        }
      </div>

      @if (chart) {
        <app-expense-chart [chart]="chart" />
      }

      @if (showForm) {
        <form class="card form" [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="outline">
            <mat-label>{{ 'payroll.category' | translate }}</mat-label>
            <mat-select formControlName="categoryId">
              @for (category of categories; track category.id) {
                <mat-option [value]="category.id">{{ category.name }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'payroll.amount' | translate }}</mat-label>
            <input matInput type="number" formControlName="amount" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'payroll.date' | translate }}</mat-label>
            <input matInput type="date" formControlName="expenseDate" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'payroll.vendor' | translate }}</mat-label>
            <input matInput formControlName="vendor" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'payroll.paymentMethod' | translate }}</mat-label>
            <mat-select formControlName="paymentMethod">
              @for (method of paymentMethods; track method) {
                <mat-option [value]="method">{{ method }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline" class="wide">
            <mat-label>{{ 'payroll.description' | translate }}</mat-label>
            <input matInput formControlName="description" />
          </mat-form-field>
          <div class="actions">
            <button mat-button type="button" (click)="showForm = false">{{ 'common.cancel' | translate }}</button>
            <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving">
              {{ 'common.save' | translate }}
            </button>
          </div>
        </form>
      }

      @if (expenses.length === 0) {
        <app-empty-state [title]="'common.noData' | translate" [hint]="'common.emptyHint' | translate" />
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'payroll.date' | translate }}</th>
                <th>{{ 'payroll.category' | translate }}</th>
                <th>{{ 'payroll.vendor' | translate }}</th>
                <th>{{ 'payroll.description' | translate }}</th>
                <th>{{ 'payroll.paymentMethod' | translate }}</th>
                <th class="num">{{ 'payroll.amount' | translate }}</th>
              </tr>
            </thead>
            <tbody>
              @for (row of expenses; track row.id) {
                <tr>
                  <td>{{ row.expenseDate }}</td>
                  <td>{{ row.categoryName }}</td>
                  <td>{{ row.vendor || '—' }}</td>
                  <td>{{ row.description || '—' }}</td>
                  <td>{{ row.paymentMethod }}</td>
                  <td class="num">{{ formatMoney(row.amount) }}</td>
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
    .toolbar { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
    .card { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 12px; padding: 16px; }
    .form { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
    .wide { grid-column: 1 / -1; }
    .actions { grid-column: 1 / -1; display: flex; justify-content: flex-end; gap: 8px; }
    .table-wrap { overflow: auto; border: 1px solid var(--color-border); border-radius: 12px; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--color-border); }
    th { font-size: .8rem; color: var(--color-muted); }
    .num { text-align: right; font-variant-numeric: tabular-nums; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExpensesComponent implements OnInit {
  year = new Date().getFullYear();
  years = [this.year, this.year - 1, this.year - 2];
  categories: ExpenseCategory[] = [];
  expenses: Expense[] = [];
  chart: ExpenseChart | null = null;
  showForm = false;
  saving = false;
  paymentMethods = PAYMENT_METHODS;
  form;

  constructor(
    private payroll: PayrollService,
    private auth: AuthService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.nonNullable.group({
      categoryId: ['', Validators.required],
      amount: [0, [Validators.required, Validators.min(0)]],
      expenseDate: [new Date().toISOString().slice(0, 10), Validators.required],
      vendor: [''],
      description: [''],
      paymentMethod: ['CASH' as const, Validators.required],
    });
  }

  get canManage(): boolean {
    return this.auth.hasPermission('EXPENSE_MANAGE');
  }

  ngOnInit(): void {
    this.payroll.listCategories().subscribe((rows) => {
      this.categories = rows;
      this.cdr.markForCheck();
    });
    this.refresh();
  }

  onYear(year: number): void {
    this.year = year;
    this.refresh();
  }

  refresh(): void {
    this.payroll.listExpenses(this.year).subscribe((rows) => {
      this.expenses = rows;
      this.cdr.markForCheck();
    });
    this.payroll.chart(this.year).subscribe((chart) => {
      this.chart = chart;
      this.cdr.markForCheck();
    });
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.saving = true;
    const value = this.form.getRawValue();
    this.payroll.createExpense({
      categoryId: value.categoryId,
      amount: Number(value.amount),
      expenseDate: value.expenseDate,
      vendor: value.vendor || null,
      description: value.description || null,
      paymentMethod: value.paymentMethod,
    }).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.form.reset({
          categoryId: '',
          amount: 0,
          expenseDate: new Date().toISOString().slice(0, 10),
          vendor: '',
          description: '',
          paymentMethod: 'CASH',
        });
        this.refresh();
      },
      error: () => {
        this.saving = false;
        this.cdr.markForCheck();
      },
    });
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value);
  }
}
