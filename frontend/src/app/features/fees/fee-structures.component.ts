import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';
import {
  AcademicYearOption,
  ClassOption,
  FEE_FREQUENCIES,
  FeeFrequency,
  FeeHead,
  FeeStructureAudit,
  FeeStructureRow,
} from './fees.model';
import { FeesService } from './fees.service';

@Component({
  selector: 'app-fee-structures',
  imports: [
    TranslateModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
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
            {{ 'fees.addStructure' | translate }}
          </button>
          <button mat-stroked-button type="button" (click)="showClone = !showClone">
            {{ 'fees.cloneStructure' | translate }}
          </button>
        }
      </div>

      @if (showClone) {
        <form class="card form" [formGroup]="cloneForm" (ngSubmit)="clone()">
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.sourceYear' | translate }}</mat-label>
            <mat-select formControlName="sourceAcademicYearId">
              @for (year of years; track year.id) {
                <mat-option [value]="year.id">{{ year.name }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <div class="actions">
            <button mat-button type="button" (click)="showClone = false">{{ 'common.cancel' | translate }}</button>
            <button mat-flat-button color="primary" type="submit" [disabled]="cloneForm.invalid || cloning">
              {{ 'fees.cloneStructure' | translate }}
            </button>
          </div>
          @if (cloneMessage) {
            <p class="hint">{{ cloneMessage }}</p>
          }
        </form>
      }

      @if (showForm) {
        <form class="card form" [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.feeHead' | translate }}</mat-label>
            <mat-select formControlName="categoryId">
              @for (head of activeHeads; track head.id) {
                <mat-option [value]="head.id">{{ head.name }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.class' | translate }}</mat-label>
            <mat-select formControlName="classId" [disabled]="form.controls.applyToAllClasses.value">
              @for (row of classes; track row.id) {
                <mat-option [value]="row.id">{{ row.name }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-checkbox formControlName="applyToAllClasses">{{ 'fees.applyAllClasses' | translate }}</mat-checkbox>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.amount' | translate }}</mat-label>
            <input matInput type="number" formControlName="amount" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.frequency' | translate }}</mat-label>
            <mat-select formControlName="frequency">
              @for (freq of frequencies; track freq) {
                <mat-option [value]="freq">{{ 'fees.freq.' + freq | translate }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.dueDay' | translate }}</mat-label>
            <input matInput type="number" formControlName="dueDay" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.dueDate' | translate }}</mat-label>
            <input matInput type="date" formControlName="dueDate" />
          </mat-form-field>
          <div class="actions">
            <button mat-button type="button" (click)="showForm = false">{{ 'common.cancel' | translate }}</button>
            <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving">
              {{ 'common.save' | translate }}
            </button>
          </div>
        </form>
      }

      @if (rows.length === 0) {
        <app-empty-state [title]="'common.noData' | translate" [hint]="'fees.structureHint' | translate" />
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'fees.class' | translate }}</th>
                <th>{{ 'fees.feeHead' | translate }}</th>
                <th class="num">{{ 'fees.amount' | translate }}</th>
                <th>{{ 'fees.frequency' | translate }}</th>
                <th>{{ 'fees.dueDay' | translate }}</th>
                @if (canManage) { <th></th> }
              </tr>
            </thead>
            <tbody>
              @for (row of rows; track row.id) {
                <tr>
                  <td>{{ row.className }}</td>
                  <td>{{ row.categoryName }}</td>
                  <td class="num">{{ formatMoney(row.amount) }}</td>
                  <td>{{ 'fees.freq.' + row.frequency | translate }}</td>
                  <td>{{ row.dueDate || row.dueDay || '—' }}</td>
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

      @if (audit.length) {
        <h3 class="card-title">{{ 'fees.audit' | translate }}</h3>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'fees.changedAt' | translate }}</th>
                <th>{{ 'common.actions' | translate }}</th>
                <th>{{ 'fees.class' | translate }}</th>
                <th>{{ 'fees.feeHead' | translate }}</th>
                <th class="num">{{ 'fees.previousAmount' | translate }}</th>
                <th class="num">{{ 'fees.newAmount' | translate }}</th>
                <th>{{ 'fees.changedBy' | translate }}</th>
              </tr>
            </thead>
            <tbody>
              @for (row of audit; track row.id) {
                <tr>
                  <td>{{ formatDate(row.changedAt) }}</td>
                  <td>{{ row.action }}</td>
                  <td>{{ row.className || '—' }}</td>
                  <td>{{ row.categoryName || '—' }}</td>
                  <td class="num">{{ row.previousAmount == null ? '—' : formatMoney(row.previousAmount) }}</td>
                  <td class="num">{{ row.newAmount == null ? '—' : formatMoney(row.newAmount) }}</td>
                  <td>{{ row.changedByName || '—' }}</td>
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
    .form { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; align-items: center; }
    .actions { grid-column: 1 / -1; display: flex; justify-content: flex-end; gap: 8px; }
    .table-wrap { overflow: auto; border: 1px solid var(--color-border); border-radius: 12px; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--color-border); }
    th { font-size: .8rem; color: var(--color-muted); }
    .num { text-align: right; font-variant-numeric: tabular-nums; }
    .card-title { margin: 8px 0 0; font-size: 1rem; }
    .hint { grid-column: 1 / -1; color: var(--color-muted); margin: 0; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeeStructuresComponent implements OnInit {
  years: AcademicYearOption[] = [];
  classes: ClassOption[] = [];
  heads: FeeHead[] = [];
  rows: FeeStructureRow[] = [];
  audit: FeeStructureAudit[] = [];
  yearId = '';
  showForm = false;
  showClone = false;
  saving = false;
  cloning = false;
  cloneMessage = '';
  editing: FeeStructureRow | null = null;
  frequencies = FEE_FREQUENCIES;
  form;
  cloneForm;

  constructor(
    private fees: FeesService,
    private auth: AuthService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.nonNullable.group({
      categoryId: ['', Validators.required],
      classId: [''],
      applyToAllClasses: [false],
      amount: [0, [Validators.required, Validators.min(0.01)]],
      frequency: ['MONTHLY' as FeeFrequency, Validators.required],
      dueDay: [10 as number | null],
      dueDate: [''],
    });
    this.cloneForm = this.fb.nonNullable.group({
      sourceAcademicYearId: ['', Validators.required],
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
    this.fees.classes().subscribe((rows) => {
      this.classes = rows;
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

  edit(row: FeeStructureRow): void {
    this.editing = row;
    this.showForm = true;
    this.form.patchValue({
      categoryId: row.categoryId,
      classId: row.classId,
      applyToAllClasses: false,
      amount: row.amount,
      frequency: row.frequency,
      dueDay: row.dueDay,
      dueDate: row.dueDate ?? '',
    });
  }

  submit(): void {
    if (this.form.invalid || !this.yearId) {
      return;
    }
    this.saving = true;
    const value = this.form.getRawValue();
    if (this.editing) {
      this.fees.updateStructure(this.editing.id, {
        amount: Number(value.amount),
        frequency: value.frequency,
        dueDay: value.dueDay,
        dueDate: value.dueDate || null,
      }).subscribe({
        next: () => {
          this.saving = false;
          this.showForm = false;
          this.editing = null;
          this.refresh();
        },
        error: () => {
          this.saving = false;
          this.cdr.markForCheck();
        },
      });
      return;
    }
    this.fees.createStructure({
      classId: value.applyToAllClasses ? null : value.classId,
      applyToAllClasses: value.applyToAllClasses,
      academicYearId: this.yearId,
      categoryId: value.categoryId,
      amount: Number(value.amount),
      frequency: value.frequency,
      dueDay: value.dueDay,
      dueDate: value.dueDate || null,
    }).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.refresh();
      },
      error: () => {
        this.saving = false;
        this.cdr.markForCheck();
      },
    });
  }

  clone(): void {
    if (this.cloneForm.invalid || !this.yearId) {
      return;
    }
    this.cloning = true;
    this.cloneMessage = '';
    this.fees.cloneStructures(this.cloneForm.getRawValue().sourceAcademicYearId, this.yearId).subscribe({
      next: (result) => {
        this.cloning = false;
        this.cloneMessage = `${result.cloned} cloned, ${result.skipped} skipped`;
        this.refresh();
      },
      error: () => {
        this.cloning = false;
        this.cdr.markForCheck();
      },
    });
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value);
  }

  formatDate(value: string): string {
    return new Date(value).toLocaleString();
  }

  private refresh(): void {
    if (!this.yearId) {
      return;
    }
    this.fees.listStructures(this.yearId).subscribe((rows) => {
      this.rows = rows;
      this.cdr.markForCheck();
    });
    this.fees.listAudit(this.yearId).subscribe((rows) => {
      this.audit = rows;
      this.cdr.markForCheck();
    });
  }
}
