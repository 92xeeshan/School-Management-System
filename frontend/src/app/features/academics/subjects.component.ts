import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ApiError, ApiResponse } from '../../core/models/api.model';
import { AuthService } from '../../core/auth/auth.service';

interface SubjectClass {
  id: string;
  name: string;
  code: string;
  teacherId: string | null;
  teacherName: string | null;
}

interface SubjectRow {
  id: string;
  name: string;
  code: string;
  type: string;
  description: string;
  weeklyPeriods: number | null;
  practical: boolean;
  status: string;
  teacherId: string | null;
  teacherName: string | null;
  classes: SubjectClass[];
}

interface SchoolClassOption {
  id: string;
  name: string;
  code: string;
}

interface TeacherOption {
  id: string;
  firstName: string;
  lastName: string;
  displayName: string;
  status: string;
}

@Component({
  selector: 'app-academics-subjects',
  imports: [TranslateModule, ReactiveFormsModule],
  template: `
    <div class="tab-page">
      <div class="page-header">
        <div>
          <h2>{{ 'academics.subjectsPage.title' | translate }}</h2>
          <p class="muted">{{ 'academics.subjectsPage.subtitle' | translate }}</p>
        </div>
        @if (canCreate) {
          <div class="header-actions">
            <button class="btn btn-primary" type="button" (click)="openAddModal()">{{ 'academics.addSubject' | translate }}</button>
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
        <select [value]="typeFilter" (change)="onTypeFilter($event)">
          <option value="">{{ 'academics.allTypes' | translate }}</option>
          <option value="CORE">{{ 'academics.typeCore' | translate }}</option>
          <option value="ELECTIVE">{{ 'academics.typeElective' | translate }}</option>
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
                <th>{{ 'academics.subjectName' | translate }}</th>
                <th>{{ 'academics.subjectCode' | translate }}</th>
                <th>{{ 'academics.subjectType' | translate }}</th>
                <th>{{ 'academics.applicableClasses' | translate }}</th>
                <th>{{ 'academics.subjectTeacher' | translate }}</th>
                <th>{{ 'academics.weeklyPeriods' | translate }}</th>
                <th>{{ 'academics.practical' | translate }}</th>
                <th>{{ 'common.status' | translate }}</th>
                @if (canUpdate) {
                  <th>{{ 'common.actions' | translate }}</th>
                }
              </tr>
            </thead>
            <tbody>
              @for (row of filteredRows; track row.id) {
                <tr>
                  <td class="strong">{{ row.name }}</td>
                  <td><span class="muted code">{{ row.code || '—' }}</span></td>
                  <td>
                    <span class="badge" [class.badge-success]="row.type === 'CORE'" [class.badge-muted]="row.type !== 'CORE'">
                      {{ typeLabel(row.type) | translate }}
                    </span>
                  </td>
                  <td>
                    <div class="subject-tags">
                      @for (klass of row.classes; track klass.id) {
                        <span class="tag">{{ klass.name }}</span>
                      } @empty {
                        <span class="muted">—</span>
                      }
                    </div>
                  </td>
                  <td>{{ row.teacherName || '—' }}</td>
                  <td>{{ row.weeklyPeriods ?? '—' }}</td>
                  <td>{{ row.practical ? ('academics.yes' | translate) : ('academics.no' | translate) }}</td>
                  <td>
                    <span class="badge" [class.badge-success]="row.status === 'ACTIVE'" [class.badge-muted]="row.status !== 'ACTIVE'">
                      {{ statusLabel(row.status) | translate }}
                    </span>
                  </td>
                  @if (canUpdate) {
                    <td>
                      <div class="row-actions">
                        <button class="btn btn-sm" type="button" (click)="openEditModal(row)">{{ 'common.edit' | translate }}</button>
                      </div>
                    </td>
                  }
                </tr>
              } @empty {
                <tr>
                  <td [attr.colspan]="canUpdate ? 9 : 8" class="center">
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
          <h2>{{ editingRow ? ('academics.editSubject' | translate) : ('academics.addSubject' | translate) }}</h2>
          @if (formError) {
            <p class="form-error">{{ formError }}</p>
          }
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="form-grid">
              <div class="field">
                <label>{{ 'academics.subjectName' | translate }} *</label>
                <input type="text" formControlName="name" />
              </div>
              <div class="field">
                <label>{{ 'academics.subjectCode' | translate }}</label>
                <input type="text" formControlName="code" placeholder="MATH" />
              </div>
              <div class="field">
                <label>{{ 'academics.subjectType' | translate }} *</label>
                <select formControlName="type">
                  <option value="CORE">{{ 'academics.typeCore' | translate }}</option>
                  <option value="ELECTIVE">{{ 'academics.typeElective' | translate }}</option>
                </select>
              </div>
              <div class="field">
                <label>{{ 'academics.weeklyPeriods' | translate }}</label>
                <input type="number" formControlName="weeklyPeriods" min="1" />
              </div>
              <div class="field">
                <label>{{ 'academics.subjectTeacher' | translate }}</label>
                <select formControlName="teacherId">
                  <option value="">{{ 'academics.selectSubjectTeacher' | translate }}</option>
                  @for (teacher of teachers; track teacher.id) {
                    <option [value]="teacher.id">{{ teacherLabel(teacher) }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'common.status' | translate }}</label>
                <select formControlName="status">
                  <option value="ACTIVE">{{ 'academics.statusActive' | translate }}</option>
                  <option value="INACTIVE">{{ 'academics.statusInactive' | translate }}</option>
                </select>
              </div>
              <div class="field span-2">
                <label class="check-inline">
                  <input type="checkbox" formControlName="practical" />
                  {{ 'academics.practicalRequired' | translate }}
                </label>
              </div>
              <div class="field span-2">
                <label>{{ 'academics.applicableClasses' | translate }}</label>
                <div class="multi-select">
                  @for (klass of classes; track klass.id) {
                    <label class="check-item">
                      <input type="checkbox" [checked]="isClassSelected(klass.id)" (change)="toggleClass(klass.id)" />
                      {{ klass.name }}
                    </label>
                  } @empty {
                    <span class="muted">{{ 'common.noData' | translate }}</span>
                  }
                </div>
              </div>
            </div>
            <div class="form-actions">
              <button class="btn" type="button" (click)="closeModal()" [disabled]="saving">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving">
                {{ saving ? ('common.loading' | translate) : ('common.save' | translate) }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; gap: 12px; flex-wrap: wrap; }
    h2 { font-size: 1.2rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); }
    .code { font-size: .8rem; font-weight: 400; }
    .filters { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
    .filters input, .filters select { padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; background: #fff; }
    .filters input { min-width: 220px; flex: 1; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 14px 16px; border-bottom: 1px solid var(--color-border); font-size: .92rem; vertical-align: top; }
    th { color: var(--color-muted); font-weight: 600; font-size: .8rem; text-transform: uppercase; letter-spacing: .03em; background: var(--color-bg); }
    .strong { font-weight: 600; }
    .center { text-align: center; color: var(--color-muted); padding: 28px; }
    .subject-tags { display: flex; flex-wrap: wrap; gap: 6px; }
    .tag { padding: 3px 10px; border-radius: 6px; background: var(--color-bg); border: 1px solid var(--color-border); font-size: .82rem; }
    .banner { background: #eff6ff; border: 1px solid #bfdbfe; color: #1e40af; padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
    .banner.success { background: #ecfdf5; border-color: #a7f3d0; color: #047857; }
    .banner.error { background: #fef2f2; border-color: #fecaca; color: #b91c1c; }
    .row-actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .btn-sm { padding: 5px 10px; font-size: .82rem; }
    .modal-backdrop {
      position: fixed; inset: 0; z-index: 100;
      background: rgba(15, 23, 42, .5);
      display: flex; align-items: flex-start; justify-content: center;
      padding: 40px 16px; overflow-y: auto;
    }
    .modal {
      background: #fff; border-radius: var(--radius);
      padding: 24px; width: 560px; max-width: 100%;
      box-shadow: 0 20px 50px rgba(0,0,0,.25);
    }
    .modal h2 { margin: 0 0 18px; font-size: 1.2rem; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    input, select {
      padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit;
    }
    .form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
    .form-error { margin: 0 0 14px; color: #b91c1c; font-size: .9rem; }
    .span-2 { grid-column: span 2; }
    .check-inline { display: flex; align-items: center; gap: 8px; cursor: pointer; }
    .multi-select {
      display: flex; flex-wrap: wrap; gap: 8px;
      padding: 10px 12px; border: 1px solid var(--color-border); border-radius: 8px;
      max-height: 160px; overflow-y: auto; background: #fff;
    }
    .check-item {
      display: flex; align-items: center; gap: 6px;
      padding: 4px 10px; border-radius: 16px; background: var(--color-bg);
      border: 1px solid var(--color-border); font-size: .85rem; cursor: pointer;
    }
    .check-item input { margin: 0; }
    @media (max-width: 640px) { .form-grid { grid-template-columns: 1fr; } .span-2 { grid-column: span 1; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicsSubjectsComponent implements OnInit {
  rows: SubjectRow[] = [];
  classes: SchoolClassOption[] = [];
  teachers: TeacherOption[] = [];
  loading = false;
  showModal = false;
  saving = false;
  formError = '';
  successMessage = '';
  pageError = '';
  search = '';
  typeFilter = '';
  statusFilter = '';
  editingRow: SubjectRow | null = null;
  selectedClassIds: string[] = [];

  readonly form = new FormGroup({
    name: new FormControl('', Validators.required),
    code: new FormControl(''),
    type: new FormControl('CORE', Validators.required),
    weeklyPeriods: new FormControl<number | null>(null),
    practical: new FormControl(false),
    status: new FormControl('ACTIVE'),
    teacherId: new FormControl(''),
  });

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef, private auth: AuthService) {}

  get canCreate(): boolean {
    return this.auth.hasPermission('SUBJECT_CREATE');
  }

  get canUpdate(): boolean {
    return this.auth.hasPermission('SUBJECT_UPDATE');
  }

  get filteredRows(): SubjectRow[] {
    const query = this.search.trim().toLowerCase();
    return this.rows.filter((row) => {
      if (this.typeFilter && row.type !== this.typeFilter) {
        return false;
      }
      if (this.statusFilter && row.status !== this.statusFilter) {
        return false;
      }
      if (!query) {
        return true;
      }
      return [row.name, row.code, row.teacherName].some((value) => (value || '').toLowerCase().includes(query));
    });
  }

  ngOnInit(): void {
    this.load();
  }

  typeLabel(type: string): string {
    return type === 'ELECTIVE' ? 'academics.typeElective' : 'academics.typeCore';
  }

  statusLabel(status: string): string {
    return status === 'INACTIVE' ? 'academics.statusInactive' : 'academics.statusActive';
  }

  teacherLabel(teacher: TeacherOption): string {
    return teacher.displayName || [teacher.firstName, teacher.lastName].filter(Boolean).join(' ');
  }

  onSearch(event: Event): void {
    this.search = (event.target as HTMLInputElement).value;
    this.cdr.markForCheck();
  }

  onTypeFilter(event: Event): void {
    this.typeFilter = (event.target as HTMLSelectElement).value;
    this.cdr.markForCheck();
  }

  onStatusFilter(event: Event): void {
    this.statusFilter = (event.target as HTMLSelectElement).value;
    this.cdr.markForCheck();
  }

  isClassSelected(id: string): boolean {
    return this.selectedClassIds.includes(id);
  }

  toggleClass(id: string): void {
    if (this.selectedClassIds.includes(id)) {
      this.selectedClassIds = this.selectedClassIds.filter((item) => item !== id);
    } else {
      this.selectedClassIds = [...this.selectedClassIds, id];
    }
    this.cdr.markForCheck();
  }

  openAddModal(): void {
    this.editingRow = null;
    this.showModal = true;
    this.saving = false;
    this.formError = '';
    this.successMessage = '';
    this.pageError = '';
    this.selectedClassIds = [];
    this.form.reset({
      name: '',
      code: '',
      type: 'CORE',
      weeklyPeriods: null,
      practical: false,
      status: 'ACTIVE',
      teacherId: '',
    });
    this.loadLookups();
    this.cdr.markForCheck();
  }

  openEditModal(row: SubjectRow): void {
    this.editingRow = row;
    this.showModal = true;
    this.saving = false;
    this.formError = '';
    this.successMessage = '';
    this.pageError = '';
    this.selectedClassIds = row.classes.map((klass) => klass.id);
    this.form.reset({
      name: row.name,
      code: row.code || '',
      type: row.type || 'CORE',
      weeklyPeriods: row.weeklyPeriods,
      practical: row.practical,
      status: row.status || 'ACTIVE',
      teacherId: row.teacherId || '',
    });
    this.loadLookups();
    this.cdr.markForCheck();
  }

  closeModal(): void {
    if (this.saving) {
      return;
    }
    this.showModal = false;
    this.editingRow = null;
    this.formError = '';
    this.cdr.markForCheck();
  }

  onSubmit(): void {
    if (this.form.invalid || this.saving) {
      return;
    }
    const value = this.form.getRawValue();
    const payload = {
      name: (value.name || '').trim(),
      code: (value.code || '').trim() || null,
      type: value.type,
      weeklyPeriods: value.weeklyPeriods ? Number(value.weeklyPeriods) : null,
      practical: !!value.practical,
      status: value.status,
      classIds: this.selectedClassIds,
      teacherId: value.teacherId || null,
    };
    this.saving = true;
    this.formError = '';
    const request$ = this.editingRow
      ? this.http.put<ApiResponse<SubjectRow>>(`/api/subjects/${this.editingRow.id}`, payload)
      : this.http.post<ApiResponse<SubjectRow>>('/api/subjects', payload);
    const wasEdit = !!this.editingRow;
    request$.subscribe({
      next: () => {
        this.saving = false;
        this.showModal = false;
        this.editingRow = null;
        this.successMessage = wasEdit ? 'academics.subjectUpdateSuccess' : 'academics.subjectCreateSuccess';
        this.load();
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        const apiError = err.error as ApiError | undefined;
        this.formError = apiError?.message || 'Could not save subject. Please try again.';
        this.saving = false;
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.loading = true;
    this.pageError = '';
    this.http.get<ApiResponse<SubjectRow[]>>('/api/subjects').subscribe({
      next: (res) => {
        this.rows = res.data ?? [];
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        const apiError = err.error as ApiError | undefined;
        this.pageError = apiError?.message || 'Could not load subjects.';
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
    this.http.get<ApiResponse<TeacherOption[]>>('/api/teachers').subscribe({
      next: (res) => {
        this.teachers = (res.data ?? []).filter((teacher) => teacher.status !== 'INACTIVE');
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }
}
