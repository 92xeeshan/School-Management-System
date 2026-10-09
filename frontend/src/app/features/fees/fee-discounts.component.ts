import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';
import { AcademicYearOption, DiscountType, FeeHead, SiblingDiscountRule } from './fees.model';
import { FeesService } from './fees.service';

@Component({
  selector: 'app-fee-discounts',
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
      <div class="toolbar">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>{{ 'fees.academicYear' | translate }}</mat-label>
          <mat-select [value]="yearId" (selectionChange)="onYear($event.value)">
            @for (year of years; track year.id) {
              <mat-option [value]="year.id">{{ year.name }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        @if (canManage) {
          <button mat-flat-button color="primary" type="button" (click)="showForm = !showForm">
            {{ 'fees.addDiscount' | translate }}
          </button>
        }
      </div>
      <p class="hint">{{ 'fees.siblingHint' | translate }}</p>
      @if (showForm) {
        <form class="card form" [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.siblingOrder' | translate }}</mat-label>
            <mat-select formControlName="siblingOrder">
              <mat-option [value]="2">{{ 'fees.secondChild' | translate }}</mat-option>
              <mat-option [value]="3">{{ 'fees.thirdChild' | translate }}</mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.feeHead' | translate }}</mat-label>
            <mat-select formControlName="feeCategoryId">
              @for (head of activeHeads; track head.id) {
                <mat-option [value]="head.id">{{ head.name }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.discountType' | translate }}</mat-label>
            <mat-select formControlName="discountType">
              <mat-option value="PERCENT">{{ 'fees.percent' | translate }}</mat-option>
              <mat-option value="FIXED">{{ 'fees.fixed' | translate }}</mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.discountValue' | translate }}</mat-label>
            <input matInput type="number" formControlName="discountValue" />
          </mat-form-field>
          <div class="actions">
            <button mat-button type="button" (click)="cancel()">{{ 'common.cancel' | translate }}</button>
            <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving">
              {{ 'common.save' | translate }}
            </button>
          </div>
        </form>
      }
      @if (rules.length === 0) {
        <app-empty-state [title]="'common.noData' | translate" [hint]="'fees.siblingHint' | translate" />
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'fees.siblingOrder' | translate }}</th>
                <th>{{ 'fees.feeHead' | translate }}</th>
                <th>{{ 'fees.discountType' | translate }}</th>
                <th class="num">{{ 'fees.discountValue' | translate }}</th>
                <th>{{ 'common.status' | translate }}</th>
                @if (canManage) { <th></th> }
              </tr>
            </thead>
            <tbody>
              @for (row of rules; track row.id) {
                <tr>
                  <td>{{ row.siblingOrder === 2 ? ('fees.secondChild' | translate) : ('fees.thirdChild' | translate) }}</td>
                  <td>{{ row.feeCategoryName }}</td>
                  <td>{{ row.discountType === 'PERCENT' ? ('fees.percent' | translate) : ('fees.fixed' | translate) }}</td>
                  <td class="num">{{ row.discountType === 'PERCENT' ? row.discountValue + '%' : formatMoney(row.discountValue) }}</td>
                  <td><span class="badge" [class.badge-success]="row.status === 'ACTIVE'">{{ row.status }}</span></td>
                  @if (canManage) {
                    <td>
                      <button mat-button type="button" (click)="edit(row)">{{ 'common.edit' | translate }}</button>
                    </td>
                  }
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
    .hint { margin: 0; color: var(--color-muted); font-size: .9rem; }
    .card { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 12px; padding: 16px; }
    .form { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
    .actions { grid-column: 1 / -1; display: flex; justify-content: flex-end; gap: 8px; }
    .table-wrap { overflow: auto; border: 1px solid var(--color-border); border-radius: 12px; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--color-border); }
    th { font-size: .8rem; color: var(--color-muted); }
    .num { text-align: right; font-variant-numeric: tabular-nums; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeeDiscountsComponent implements OnInit {
  years: AcademicYearOption[] = [];
  heads: FeeHead[] = [];
  rules: SiblingDiscountRule[] = [];
  yearId = '';
  showForm = false;
  saving = false;
  editing: SiblingDiscountRule | null = null;
  form;

  constructor(
    private fees: FeesService,
    private auth: AuthService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.nonNullable.group({
      siblingOrder: [2, Validators.required],
      feeCategoryId: ['', Validators.required],
      discountType: ['PERCENT' as DiscountType, Validators.required],
      discountValue: [10, [Validators.required, Validators.min(0.01)]],
    });
  }

  get canManage(): boolean {
    return this.auth.hasPermission('FEE_STRUCTURE_MANAGE');
  }

  get activeHeads(): FeeHead[] {
    return this.heads.filter((head) => head.status === 'ACTIVE');
  }

  ngOnInit(): void {
    this.fees.academicYears().subscribe((years) => {
      this.years = years;
      const current = years.find((year) => year.current) ?? years[0];
      if (current) {
        this.yearId = current.id;
        this.refresh();
      }
      this.cdr.markForCheck();
    });
    this.fees.listHeads().subscribe((rows) => {
      this.heads = rows;
      this.cdr.markForCheck();
    });
  }

  onYear(id: string): void {
    this.yearId = id;
    this.refresh();
  }

  edit(row: SiblingDiscountRule): void {
    this.editing = row;
    this.showForm = true;
    this.form.patchValue({
      siblingOrder: row.siblingOrder,
      feeCategoryId: row.feeCategoryId,
      discountType: row.discountType,
      discountValue: row.discountValue,
    });
  }

  cancel(): void {
    this.showForm = false;
    this.editing = null;
    this.form.reset({ siblingOrder: 2, feeCategoryId: '', discountType: 'PERCENT', discountValue: 10 });
  }

  submit(): void {
    if (this.form.invalid || !this.yearId) {
      return;
    }
    this.saving = true;
    const value = this.form.getRawValue();
    const payload = {
      academicYearId: this.yearId,
      siblingOrder: Number(value.siblingOrder),
      feeCategoryId: value.feeCategoryId,
      discountType: value.discountType,
      discountValue: Number(value.discountValue),
      status: this.editing?.status ?? 'ACTIVE',
    };
    const req = this.editing
      ? this.fees.updateSiblingRule(this.editing.id, payload)
      : this.fees.createSiblingRule(payload);
    req.subscribe({
      next: () => {
        this.saving = false;
        this.cancel();
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

  private refresh(): void {
    if (!this.yearId) {
      return;
    }
    this.fees.listSiblingRules(this.yearId).subscribe((rows) => {
      this.rules = rows;
      this.cdr.markForCheck();
    });
  }
}
