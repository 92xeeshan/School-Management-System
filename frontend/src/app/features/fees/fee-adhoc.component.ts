import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';
import {
  AcademicYearOption,
  AdhocLevy,
  ClassOption,
  FeeHead,
  LEVY_SCOPES,
  LevyScope,
  SectionOption,
  StudentOption,
} from './fees.model';
import { FeesService } from './fees.service';

@Component({
  selector: 'app-fee-adhoc',
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
            {{ 'fees.addAdhoc' | translate }}
          </button>
        }
      </div>
      @if (showForm) {
        <form class="card form" [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.feeHead' | translate }}</mat-label>
            <mat-select formControlName="feeCategoryId">
              @for (head of activeHeads; track head.id) {
                <mat-option [value]="head.id">{{ head.name }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.amount' | translate }}</mat-label>
            <input matInput type="number" formControlName="amount" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.dueDate' | translate }}</mat-label>
            <input matInput type="date" formControlName="dueDate" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.scopeLabel' | translate }}</mat-label>
            <mat-select formControlName="scope">
              @for (scope of scopes; track scope) {
                <mat-option [value]="scope">{{ 'fees.scope.' + scope | translate }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          @if (form.controls.scope.value === 'CLASS' || form.controls.scope.value === 'SECTION') {
            <mat-form-field appearance="outline">
              <mat-label>{{ 'fees.class' | translate }}</mat-label>
              <mat-select formControlName="classId">
                @for (row of classes; track row.id) {
                  <mat-option [value]="row.id">{{ row.name }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
          }
          @if (form.controls.scope.value === 'SECTION') {
            <mat-form-field appearance="outline">
              <mat-label>{{ 'fees.section' | translate }}</mat-label>
              <mat-select formControlName="sectionId">
                @for (row of filteredSections; track row.id) {
                  <mat-option [value]="row.id">{{ row.name }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
          }
          @if (form.controls.scope.value === 'STUDENT') {
            <mat-form-field appearance="outline">
              <mat-label>{{ 'fees.selectStudent' | translate }}</mat-label>
              <mat-select formControlName="studentId">
                @for (row of students; track row.id) {
                  <mat-option [value]="row.id">{{ row.displayName }} ({{ row.admissionNo }})</mat-option>
                }
              </mat-select>
            </mat-form-field>
          }
          <mat-form-field appearance="outline" class="wide">
            <mat-label>{{ 'fees.remarks' | translate }}</mat-label>
            <input matInput formControlName="remarks" />
          </mat-form-field>
          <div class="actions">
            <button mat-button type="button" (click)="showForm = false">{{ 'common.cancel' | translate }}</button>
            <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving">
              {{ 'common.save' | translate }}
            </button>
          </div>
        </form>
      }
      @if (levies.length === 0) {
        <app-empty-state [title]="'common.noData' | translate" [hint]="'fees.adhocHint' | translate" />
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'fees.feeHead' | translate }}</th>
                <th class="num">{{ 'fees.amount' | translate }}</th>
                <th>{{ 'fees.dueDate' | translate }}</th>
                <th>{{ 'fees.scopeLabel' | translate }}</th>
                <th>{{ 'fees.target' | translate }}</th>
                <th class="num">{{ 'fees.assignedCount' | translate }}</th>
              </tr>
            </thead>
            <tbody>
              @for (row of levies; track row.id) {
                <tr>
                  <td>{{ row.feeCategoryName }}</td>
                  <td class="num">{{ formatMoney(row.amount) }}</td>
                  <td>{{ row.dueDate }}</td>
                  <td>{{ 'fees.scope.' + row.scope | translate }}</td>
                  <td>{{ targetLabel(row) }}</td>
                  <td class="num">{{ row.assignedCount }}</td>
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
export class FeeAdhocComponent implements OnInit {
  years: AcademicYearOption[] = [];
  classes: ClassOption[] = [];
  sections: SectionOption[] = [];
  students: StudentOption[] = [];
  heads: FeeHead[] = [];
  levies: AdhocLevy[] = [];
  yearId = '';
  showForm = false;
  saving = false;
  scopes = LEVY_SCOPES;
  form;

  constructor(
    private fees: FeesService,
    private auth: AuthService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.nonNullable.group({
      feeCategoryId: ['', Validators.required],
      amount: [0, [Validators.required, Validators.min(0.01)]],
      dueDate: ['', Validators.required],
      scope: ['STUDENT' as LevyScope, Validators.required],
      classId: [''],
      sectionId: [''],
      studentId: [''],
      remarks: [''],
    });
  }

  get canManage(): boolean {
    return this.auth.hasPermission('FEE_STRUCTURE_MANAGE');
  }

  get activeHeads(): FeeHead[] {
    return this.heads.filter((head) => head.status === 'ACTIVE');
  }

  get filteredSections(): SectionOption[] {
    const classId = this.form.controls.classId.value;
    return classId ? this.sections.filter((section) => section.classId === classId) : this.sections;
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
    this.fees.sections().subscribe((rows) => {
      this.sections = rows;
      this.cdr.markForCheck();
    });
    this.fees.students().subscribe((rows) => {
      this.students = rows;
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

  submit(): void {
    if (this.form.invalid || !this.yearId) {
      return;
    }
    this.saving = true;
    const value = this.form.getRawValue();
    this.fees.createLevy({
      academicYearId: this.yearId,
      feeCategoryId: value.feeCategoryId,
      amount: Number(value.amount),
      dueDate: value.dueDate,
      scope: value.scope,
      classId: value.classId || null,
      sectionId: value.sectionId || null,
      studentId: value.studentId || null,
      remarks: value.remarks || null,
    }).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.form.reset({
          feeCategoryId: '',
          amount: 0,
          dueDate: '',
          scope: 'STUDENT',
          classId: '',
          sectionId: '',
          studentId: '',
          remarks: '',
        });
        this.refresh();
      },
      error: () => {
        this.saving = false;
        this.cdr.markForCheck();
      },
    });
  }

  targetLabel(row: AdhocLevy): string {
    if (row.studentName) {
      return row.studentName;
    }
    if (row.sectionName) {
      return `${row.className ?? ''} ${row.sectionName}`.trim();
    }
    if (row.className) {
      return row.className;
    }
    return '—';
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value);
  }

  private refresh(): void {
    if (!this.yearId) {
      return;
    }
    this.fees.listLevies(this.yearId).subscribe((rows) => {
      this.levies = rows;
      this.cdr.markForCheck();
    });
  }
}
