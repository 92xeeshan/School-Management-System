import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ApiError, ApiResponse } from '../../core/models/api.model';
import { AuthService } from '../../core/auth/auth.service';

interface AcademicYear {
  id: string;
  name: string;
  current: boolean;
}

interface SchoolClassOption {
  id: string;
  name: string;
  code: string;
}

interface GradeBoundary {
  id?: string;
  label: string;
  minPercent: number;
  maxPercent: number;
  gpaValue: number | null;
  sortOrder: number;
}

interface AssessmentWeightage {
  id?: string;
  assessmentType: string;
  weightPercent: number;
}

interface GradingScheme {
  id: string;
  academicYearId: string;
  academicYearName: string;
  classId: string;
  className: string;
  name: string;
  academicLevel: string;
  examType: string;
  scaleType: string;
  passMarks: number;
  passPercent: number;
  maxMarks: number;
  evaluationCriteria: string | null;
  status: string;
  boundaries: GradeBoundary[];
  weightages: AssessmentWeightage[];
}

const LEVELS = ['PRIMARY', 'MIDDLE', 'SECONDARY', 'SENIOR'];
const EXAM_TYPES = ['QUIZ', 'UNIT', 'MIDTERM', 'TERM', 'FINAL', 'CONTINUOUS'];
const SCALE_TYPES = ['PERCENTAGE', 'LETTER', 'GPA'];
const ASSESSMENT_TYPES = ['QUIZ', 'ASSIGNMENT', 'MIDTERM', 'PRACTICAL', 'FINAL'];

@Component({
  selector: 'app-academics-examinations',
  imports: [TranslateModule, ReactiveFormsModule],
  template: `
    <div class="tab-page">
      <div class="page-header">
        <div>
          <h2>{{ 'academics.examinationsPage.title' | translate }}</h2>
          <p class="muted">{{ 'academics.examinationsPage.subtitle' | translate }}</p>
        </div>
        @if (canManage) {
          <div class="header-actions">
            <button class="btn btn-primary" type="button" (click)="openAddModal()">{{ 'academics.addScheme' | translate }}</button>
          </div>
        }
      </div>

      @if (successMessage) {
        <p class="banner success">{{ successMessage | translate }}</p>
      }
      @if (pageError) {
        <p class="banner error">{{ pageError }}</p>
      }

      <div class="filters">
        <input type="search" [value]="search" (input)="onSearch($event)" [placeholder]="'common.search' | translate" />
        <select [value]="classFilter" (change)="onClassFilter($event)">
          <option value="">{{ 'academics.allClasses' | translate }}</option>
          @for (klass of classes; track klass.id) {
            <option [value]="klass.id">{{ klass.name }}</option>
          }
        </select>
        <select [value]="scaleFilter" (change)="onScaleFilter($event)">
          <option value="">{{ 'academics.allScales' | translate }}</option>
          @for (scale of scaleTypes; track scale) {
            <option [value]="scale">{{ scaleLabel(scale) | translate }}</option>
          }
        </select>
        <select [value]="statusFilter" (change)="onStatusFilter($event)">
          <option value="">{{ 'academics.allStatuses' | translate }}</option>
          <option value="ACTIVE">{{ 'academics.statusActive' | translate }}</option>
          <option value="INACTIVE">{{ 'academics.statusInactive' | translate }}</option>
        </select>
      </div>

      <div class="card">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'academics.schemeName' | translate }}</th>
                <th>{{ 'academics.className' | translate }}</th>
                <th>{{ 'academics.academicLevel' | translate }}</th>
                <th>{{ 'academics.examType' | translate }}</th>
                <th>{{ 'academics.scaleType' | translate }}</th>
                <th>{{ 'academics.passMarks' | translate }}</th>
                <th>{{ 'common.status' | translate }}</th>
                <th>{{ 'common.actions' | translate }}</th>
              </tr>
            </thead>
            <tbody>
              @for (row of filteredRows; track row.id) {
                <tr>
                  <td class="strong">
                    {{ row.name }}
                    @if (row.status === 'ACTIVE') {
                      <span class="badge badge-success">{{ 'academics.activeForClass' | translate }}</span>
                    }
                  </td>
                  <td>{{ row.className || '—' }}</td>
                  <td>{{ levelLabel(row.academicLevel) | translate }}</td>
                  <td>{{ examLabel(row.examType) | translate }}</td>
                  <td>{{ scaleLabel(row.scaleType) | translate }}</td>
                  <td>{{ row.passMarks }} / {{ row.maxMarks }} ({{ row.passPercent }}%)</td>
                  <td>
                    <span class="badge" [class.badge-success]="row.status === 'ACTIVE'" [class.badge-muted]="row.status !== 'ACTIVE'">
                      {{ statusLabel(row.status) | translate }}
                    </span>
                  </td>
                  <td>
                    <div class="row-actions">
                      <button class="btn btn-sm" type="button" (click)="openViewModal(row)">{{ 'academics.viewScheme' | translate }}</button>
                      @if (canManage) {
                        <button class="btn btn-sm" type="button" (click)="openEditModal(row)">{{ 'common.edit' | translate }}</button>
                        @if (row.status === 'ACTIVE') {
                          <button class="btn btn-sm" type="button" (click)="askToggle(row, false)">{{ 'academics.deactivate' | translate }}</button>
                        } @else {
                          <button class="btn btn-sm" type="button" (click)="askToggle(row, true)">{{ 'academics.activate' | translate }}</button>
                        }
                      }
                    </div>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="8" class="center">
                    {{ loading ? ('common.loading' | translate) : ('common.noData' | translate) }}
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>

    @if (showModal) {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ modalTitle | translate }}</h2>
          @if (formError) {
            <p class="form-error">{{ formError }}</p>
          }
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="form-grid">
              <div class="field">
                <label>{{ 'academics.schemeName' | translate }} *</label>
                <input type="text" formControlName="name" />
              </div>
              <div class="field">
                <label>{{ 'academics.academicYear' | translate }} *</label>
                <select formControlName="academicYearId">
                  @for (year of years; track year.id) {
                    <option [value]="year.id">{{ year.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'academics.className' | translate }} *</label>
                <select formControlName="classId">
                  <option value="">{{ 'academics.selectClass' | translate }}</option>
                  @for (klass of classes; track klass.id) {
                    <option [value]="klass.id">{{ klass.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'academics.academicLevel' | translate }} *</label>
                <select formControlName="academicLevel">
                  @for (level of levels; track level) {
                    <option [value]="level">{{ levelLabel(level) | translate }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'academics.examType' | translate }} *</label>
                <select formControlName="examType">
                  @for (type of examTypes; track type) {
                    <option [value]="type">{{ examLabel(type) | translate }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'academics.scaleType' | translate }} *</label>
                <select formControlName="scaleType">
                  @for (scale of scaleTypes; track scale) {
                    <option [value]="scale">{{ scaleLabel(scale) | translate }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'academics.passMarks' | translate }} *</label>
                <input type="number" formControlName="passMarks" min="0" step="0.01" />
              </div>
              <div class="field">
                <label>{{ 'academics.passPercent' | translate }} *</label>
                <input type="number" formControlName="passPercent" min="0" max="100" step="0.01" />
              </div>
              <div class="field">
                <label>{{ 'academics.maxMarks' | translate }} *</label>
                <input type="number" formControlName="maxMarks" min="1" step="0.01" />
              </div>
              <div class="field">
                <label>{{ 'common.status' | translate }}</label>
                <select formControlName="status">
                  <option value="ACTIVE">{{ 'academics.statusActive' | translate }}</option>
                  <option value="INACTIVE">{{ 'academics.statusInactive' | translate }}</option>
                </select>
              </div>
              <div class="field span-2">
                <label>{{ 'academics.evaluationCriteria' | translate }}</label>
                <textarea formControlName="evaluationCriteria" rows="2"></textarea>
              </div>
            </div>

            <div class="section-head">
              <h3>{{ 'academics.gradeBoundaries' | translate }}</h3>
              @if (!readOnly) {
                <button class="btn btn-sm" type="button" (click)="addBoundary()">{{ 'academics.addBoundary' | translate }}</button>
              }
            </div>
            <div class="table-wrap nested">
              <table>
                <thead>
                  <tr>
                    <th>{{ 'academics.gradeLabel' | translate }}</th>
                    <th>{{ 'academics.minPercent' | translate }}</th>
                    <th>{{ 'academics.maxPercent' | translate }}</th>
                    <th>{{ 'academics.gpaValue' | translate }}</th>
                    @if (!readOnly) {
                      <th></th>
                    }
                  </tr>
                </thead>
                <tbody formArrayName="boundaries">
                  @for (row of boundaries.controls; track $index) {
                    <tr [formGroupName]="$index">
                      <td><input type="text" formControlName="label" /></td>
                      <td><input type="number" formControlName="minPercent" min="0" max="100" step="0.01" /></td>
                      <td><input type="number" formControlName="maxPercent" min="0" max="100" step="0.01" /></td>
                      <td><input type="number" formControlName="gpaValue" min="0" step="0.01" /></td>
                      @if (!readOnly) {
                        <td><button class="btn btn-sm" type="button" (click)="removeBoundary($index)">{{ 'common.delete' | translate }}</button></td>
                      }
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <div class="section-head">
              <h3>{{ 'academics.assessmentWeightage' | translate }}</h3>
              @if (!readOnly) {
                <button class="btn btn-sm" type="button" (click)="addWeightage()">{{ 'academics.addWeightage' | translate }}</button>
              }
            </div>
            <p class="weight-total" [class.bad]="!weightageValid">
              {{ 'academics.weightageTotal' | translate }}: {{ weightageTotal }}%
            </p>
            <div class="table-wrap nested">
              <table>
                <thead>
                  <tr>
                    <th>{{ 'academics.assessmentType' | translate }}</th>
                    <th>{{ 'academics.weightPercent' | translate }}</th>
                    @if (!readOnly) {
                      <th></th>
                    }
                  </tr>
                </thead>
                <tbody formArrayName="weightages">
                  @for (row of weightages.controls; track $index) {
                    <tr [formGroupName]="$index">
                      <td>
                        <select formControlName="assessmentType">
                          @for (type of assessmentTypes; track type) {
                            <option [value]="type">{{ assessmentLabel(type) | translate }}</option>
                          }
                        </select>
                      </td>
                      <td><input type="number" formControlName="weightPercent" min="0.01" max="100" step="0.01" /></td>
                      @if (!readOnly) {
                        <td><button class="btn btn-sm" type="button" (click)="removeWeightage($index)">{{ 'common.delete' | translate }}</button></td>
                      }
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <div class="form-actions">
              <button class="btn" type="button" (click)="closeModal()" [disabled]="saving">{{ 'common.cancel' | translate }}</button>
              @if (!readOnly) {
                <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving || !weightageValid">
                  {{ saving ? ('common.loading' | translate) : ('common.save' | translate) }}
                </button>
              }
            </div>
          </form>
        </div>
      </div>
    }

    @if (pendingToggle) {
      <div class="modal-backdrop" (click)="cancelToggle()">
        <div class="modal modal-sm" (click)="$event.stopPropagation()">
          <h2>{{ pendingActivate ? ('academics.activate' | translate) : ('academics.deactivate' | translate) }}</h2>
          <p class="confirm-text">
            {{ (pendingActivate ? 'academics.activateConfirm' : 'academics.deactivateConfirm') | translate }}
          </p>
          <div class="form-actions">
            <button class="btn" type="button" (click)="cancelToggle()" [disabled]="toggling">{{ 'common.cancel' | translate }}</button>
            <button class="btn btn-primary" type="button" (click)="confirmToggle()" [disabled]="toggling">
              {{ toggling ? ('common.loading' | translate) : ('common.save' | translate) }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; gap: 12px; flex-wrap: wrap; }
    h2 { font-size: 1.2rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); }
    .filters { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
    .filters input, .filters select { padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; background: #fff; }
    .filters input { min-width: 220px; flex: 1; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 14px 16px; border-bottom: 1px solid var(--color-border); font-size: .92rem; vertical-align: top; }
    th { color: var(--color-muted); font-weight: 600; font-size: .8rem; text-transform: uppercase; letter-spacing: .03em; background: var(--color-bg); }
    .strong { font-weight: 600; }
    .center { text-align: center; color: var(--color-muted); padding: 28px; }
    .banner { background: #eff6ff; border: 1px solid #bfdbfe; color: #1e40af; padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
    .banner.success { background: #ecfdf5; border-color: #a7f3d0; color: #047857; }
    .banner.error { background: #fef2f2; border-color: #fecaca; color: #b91c1c; }
    .row-actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .btn-sm { padding: 5px 10px; font-size: .82rem; }
    .badge { display: inline-block; margin-left: 8px; padding: 2px 8px; border-radius: 999px; font-size: .72rem; font-weight: 700; vertical-align: middle; }
    .badge-success { background: #ecfdf5; color: #047857; }
    .badge-muted { background: var(--color-bg); color: var(--color-muted); }
    .modal-backdrop {
      position: fixed; inset: 0; z-index: 100;
      background: rgba(15, 23, 42, .5);
      display: flex; align-items: flex-start; justify-content: center;
      padding: 40px 16px; overflow-y: auto;
    }
    .modal {
      background: #fff; border-radius: var(--radius);
      padding: 24px; width: 760px; max-width: 100%;
      box-shadow: 0 20px 50px rgba(0,0,0,.25);
    }
    .modal-sm { width: 420px; }
    .modal h2 { margin: 0 0 18px; font-size: 1.2rem; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    input, select, textarea {
      padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; width: 100%; box-sizing: border-box;
    }
    .form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
    .form-error { margin: 0 0 14px; color: #b91c1c; font-size: .9rem; }
    .span-2 { grid-column: span 2; }
    .section-head { display: flex; justify-content: space-between; align-items: center; margin: 22px 0 10px; }
    .section-head h3 { margin: 0; font-size: 1rem; }
    .nested th, .nested td { padding: 8px; }
    .weight-total { margin: 0 0 8px; font-size: .9rem; color: var(--color-muted); }
    .weight-total.bad { color: #b91c1c; font-weight: 600; }
    .confirm-text { margin: 0 0 8px; color: var(--color-muted); line-height: 1.45; }
    @media (max-width: 640px) { .form-grid { grid-template-columns: 1fr; } .span-2 { grid-column: span 1; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicsExaminationsComponent implements OnInit {
  readonly levels = LEVELS;
  readonly examTypes = EXAM_TYPES;
  readonly scaleTypes = SCALE_TYPES;
  readonly assessmentTypes = ASSESSMENT_TYPES;

  rows: GradingScheme[] = [];
  classes: SchoolClassOption[] = [];
  years: AcademicYear[] = [];
  loading = false;
  showModal = false;
  saving = false;
  readOnly = false;
  formError = '';
  successMessage = '';
  pageError = '';
  search = '';
  classFilter = '';
  scaleFilter = '';
  statusFilter = '';
  editingRow: GradingScheme | null = null;
  pendingToggle: GradingScheme | null = null;
  pendingActivate = false;
  toggling = false;

  readonly form = new FormGroup({
    name: new FormControl('', Validators.required),
    academicYearId: new FormControl('', Validators.required),
    classId: new FormControl('', Validators.required),
    academicLevel: new FormControl('PRIMARY', Validators.required),
    examType: new FormControl('TERM', Validators.required),
    scaleType: new FormControl('LETTER', Validators.required),
    passMarks: new FormControl<number | null>(33, Validators.required),
    passPercent: new FormControl<number | null>(33, Validators.required),
    maxMarks: new FormControl<number | null>(100, Validators.required),
    evaluationCriteria: new FormControl(''),
    status: new FormControl('INACTIVE'),
    boundaries: new FormArray<FormGroup>([]),
    weightages: new FormArray<FormGroup>([]),
  });

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef, private auth: AuthService) {
    this.form.valueChanges.subscribe(() => this.cdr.markForCheck());
  }

  get canManage(): boolean {
    return this.auth.hasPermission('EXAM_MANAGE');
  }

  get boundaries(): FormArray<FormGroup> {
    return this.form.controls.boundaries;
  }

  get weightages(): FormArray<FormGroup> {
    return this.form.controls.weightages;
  }

  get weightageTotal(): number {
    return this.weightages.controls.reduce((sum, group) => {
      const value = Number(group.get('weightPercent')?.value || 0);
      return sum + (Number.isFinite(value) ? value : 0);
    }, 0);
  }

  get weightageValid(): boolean {
    return Math.abs(this.weightageTotal - 100) <= 0.01 && this.weightages.length > 0;
  }

  get modalTitle(): string {
    if (this.readOnly) {
      return 'academics.viewScheme';
    }
    return this.editingRow ? 'academics.editScheme' : 'academics.addScheme';
  }

  get filteredRows(): GradingScheme[] {
    const query = this.search.trim().toLowerCase();
    return this.rows.filter((row) => {
      if (this.classFilter && row.classId !== this.classFilter) {
        return false;
      }
      if (this.scaleFilter && row.scaleType !== this.scaleFilter) {
        return false;
      }
      if (this.statusFilter && row.status !== this.statusFilter) {
        return false;
      }
      if (!query) {
        return true;
      }
      return [row.name, row.className, row.academicYearName].some((value) => (value || '').toLowerCase().includes(query));
    });
  }

  ngOnInit(): void {
    this.loadLookups();
    this.load();
  }

  levelLabel(value: string): string {
    return `academics.level${this.cap(value)}`;
  }

  examLabel(value: string): string {
    return `academics.exam${this.cap(value)}`;
  }

  scaleLabel(value: string): string {
    return `academics.scale${this.cap(value)}`;
  }

  assessmentLabel(value: string): string {
    return `academics.assess${this.cap(value)}`;
  }

  statusLabel(status: string): string {
    return status === 'INACTIVE' ? 'academics.statusInactive' : 'academics.statusActive';
  }

  onSearch(event: Event): void {
    this.search = (event.target as HTMLInputElement).value;
    this.cdr.markForCheck();
  }

  onClassFilter(event: Event): void {
    this.classFilter = (event.target as HTMLSelectElement).value;
    this.cdr.markForCheck();
  }

  onScaleFilter(event: Event): void {
    this.scaleFilter = (event.target as HTMLSelectElement).value;
    this.cdr.markForCheck();
  }

  onStatusFilter(event: Event): void {
    this.statusFilter = (event.target as HTMLSelectElement).value;
    this.cdr.markForCheck();
  }

  openAddModal(): void {
    this.editingRow = null;
    this.readOnly = false;
    this.resetForm(null);
    this.showModal = true;
    this.cdr.markForCheck();
  }

  openEditModal(row: GradingScheme): void {
    this.editingRow = row;
    this.readOnly = false;
    this.resetForm(row);
    this.showModal = true;
    this.cdr.markForCheck();
  }

  openViewModal(row: GradingScheme): void {
    this.editingRow = row;
    this.readOnly = true;
    this.resetForm(row);
    this.form.disable();
    this.showModal = true;
    this.cdr.markForCheck();
  }

  closeModal(): void {
    if (this.saving) {
      return;
    }
    this.showModal = false;
    this.editingRow = null;
    this.readOnly = false;
    this.formError = '';
    this.form.enable();
    this.cdr.markForCheck();
  }

  addBoundary(boundary?: GradeBoundary): void {
    this.boundaries.push(this.boundaryGroup(boundary));
    this.cdr.markForCheck();
  }

  removeBoundary(index: number): void {
    this.boundaries.removeAt(index);
    this.cdr.markForCheck();
  }

  addWeightage(weightage?: AssessmentWeightage): void {
    this.weightages.push(this.weightageGroup(weightage));
    this.cdr.markForCheck();
  }

  removeWeightage(index: number): void {
    this.weightages.removeAt(index);
    this.cdr.markForCheck();
  }

  onSubmit(): void {
    if (this.readOnly || this.form.invalid || this.saving || !this.weightageValid) {
      if (!this.weightageValid) {
        this.formError = 'Assessment weightage must total 100%.';
        this.cdr.markForCheck();
      }
      return;
    }
    const value = this.form.getRawValue();
    const payload = {
      academicYearId: value.academicYearId,
      classId: value.classId,
      name: (value.name || '').trim(),
      academicLevel: value.academicLevel,
      examType: value.examType,
      scaleType: value.scaleType,
      passMarks: Number(value.passMarks),
      passPercent: Number(value.passPercent),
      maxMarks: Number(value.maxMarks),
      evaluationCriteria: (value.evaluationCriteria || '').trim() || null,
      status: value.status,
      boundaries: this.boundaries.controls.map((group, index) => {
        const row = group.getRawValue();
        return {
          label: (row.label || '').trim(),
          minPercent: Number(row.minPercent),
          maxPercent: Number(row.maxPercent),
          gpaValue: row.gpaValue === '' || row.gpaValue == null ? null : Number(row.gpaValue),
          sortOrder: index + 1,
        };
      }),
      weightages: this.weightages.controls.map((group) => {
        const row = group.getRawValue();
        return {
          assessmentType: row.assessmentType,
          weightPercent: Number(row.weightPercent),
        };
      }),
    };
    this.saving = true;
    this.formError = '';
    const request$ = this.editingRow
      ? this.http.put<ApiResponse<GradingScheme>>(`/api/grading-schemes/${this.editingRow.id}`, payload)
      : this.http.post<ApiResponse<GradingScheme>>('/api/grading-schemes', payload);
    const wasEdit = !!this.editingRow;
    request$.subscribe({
      next: () => {
        this.saving = false;
        this.showModal = false;
        this.editingRow = null;
        this.successMessage = wasEdit ? 'academics.schemeUpdateSuccess' : 'academics.schemeCreateSuccess';
        this.load();
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        const apiError = err.error as ApiError | undefined;
        this.formError = apiError?.message || 'Could not save grading scheme. Please try again.';
        this.saving = false;
        this.cdr.markForCheck();
      },
    });
  }

  askToggle(row: GradingScheme, activate: boolean): void {
    this.pendingToggle = row;
    this.pendingActivate = activate;
    this.cdr.markForCheck();
  }

  cancelToggle(): void {
    if (this.toggling) {
      return;
    }
    this.pendingToggle = null;
    this.cdr.markForCheck();
  }

  confirmToggle(): void {
    if (!this.pendingToggle || this.toggling) {
      return;
    }
    const id = this.pendingToggle.id;
    const activate = this.pendingActivate;
    this.toggling = true;
    const url = activate ? `/api/grading-schemes/${id}/activate` : `/api/grading-schemes/${id}/deactivate`;
    this.http.put<ApiResponse<GradingScheme>>(url, {}).subscribe({
      next: () => {
        this.toggling = false;
        this.pendingToggle = null;
        this.successMessage = activate ? 'academics.schemeActivateSuccess' : 'academics.schemeDeactivateSuccess';
        this.load();
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        const apiError = err.error as ApiError | undefined;
        this.pageError = apiError?.message || 'Could not update scheme status.';
        this.toggling = false;
        this.pendingToggle = null;
        this.cdr.markForCheck();
      },
    });
  }

  private resetForm(row: GradingScheme | null): void {
    this.saving = false;
    this.formError = '';
    this.successMessage = '';
    this.pageError = '';
    this.form.enable();
    this.boundaries.clear();
    this.weightages.clear();
    const yearId = row?.academicYearId || this.years.find((year) => year.current)?.id || this.years[0]?.id || '';
    this.form.reset({
      name: row?.name || '',
      academicYearId: yearId,
      classId: row?.classId || '',
      academicLevel: row?.academicLevel || 'PRIMARY',
      examType: row?.examType || 'TERM',
      scaleType: row?.scaleType || 'LETTER',
      passMarks: row?.passMarks ?? 33,
      passPercent: row?.passPercent ?? 33,
      maxMarks: row?.maxMarks ?? 100,
      evaluationCriteria: row?.evaluationCriteria || '',
      status: row?.status || 'INACTIVE',
    });
    const boundaries = row?.boundaries?.length
      ? row.boundaries
      : [
          { label: 'A', minPercent: 80, maxPercent: 100, gpaValue: 4, sortOrder: 1 },
          { label: 'B', minPercent: 60, maxPercent: 79.99, gpaValue: 3, sortOrder: 2 },
          { label: 'C', minPercent: 45, maxPercent: 59.99, gpaValue: 2, sortOrder: 3 },
          { label: 'D', minPercent: 33, maxPercent: 44.99, gpaValue: 1, sortOrder: 4 },
          { label: 'F', minPercent: 0, maxPercent: 32.99, gpaValue: 0, sortOrder: 5 },
        ];
    for (const boundary of boundaries) {
      this.addBoundary(boundary);
    }
    const weights = row?.weightages?.length
      ? row.weightages
      : [
          { assessmentType: 'QUIZ', weightPercent: 20 },
          { assessmentType: 'MIDTERM', weightPercent: 30 },
          { assessmentType: 'FINAL', weightPercent: 50 },
        ];
    for (const weight of weights) {
      this.addWeightage(weight);
    }
  }

  private boundaryGroup(boundary?: GradeBoundary): FormGroup {
    return new FormGroup({
      label: new FormControl(boundary?.label || '', Validators.required),
      minPercent: new FormControl(boundary?.minPercent ?? 0, Validators.required),
      maxPercent: new FormControl(boundary?.maxPercent ?? 100, Validators.required),
      gpaValue: new FormControl(boundary?.gpaValue ?? null),
    });
  }

  private weightageGroup(weightage?: AssessmentWeightage): FormGroup {
    return new FormGroup({
      assessmentType: new FormControl(weightage?.assessmentType || 'QUIZ', Validators.required),
      weightPercent: new FormControl(weightage?.weightPercent ?? 0, Validators.required),
    });
  }

  private load(): void {
    this.loading = true;
    this.pageError = '';
    this.http.get<ApiResponse<GradingScheme[]>>('/api/grading-schemes').subscribe({
      next: (res) => {
        this.rows = res.data ?? [];
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        const apiError = err.error as ApiError | undefined;
        this.pageError = apiError?.message || 'Could not load grading schemes.';
        this.rows = [];
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  private loadLookups(): void {
    this.http.get<ApiResponse<SchoolClassOption[]>>('/api/classes').subscribe({
      next: (res) => {
        this.classes = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
    this.http.get<ApiResponse<AcademicYear[]>>('/api/academic-years').subscribe({
      next: (res) => {
        this.years = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  private cap(value: string): string {
    const text = (value || '').toLowerCase();
    return text ? text.charAt(0).toUpperCase() + text.slice(1) : '';
  }
}
