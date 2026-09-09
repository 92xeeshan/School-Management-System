import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ApiError, ApiResponse } from '../../core/models/api.model';
import { AuthService } from '../../core/auth/auth.service';

interface SchoolClass {
  id: string;
  name: string;
  code: string;
  sections: SchoolSection[];
  subjects: BackendSubject[];
}

interface SchoolSection {
  id: string;
  name: string;
  capacity: number;
  room: string;
  studentCount: number;
  classTeacherId: string;
  classTeacherName: string;
}

interface ClassRow {
  rowId: string;
  classId: string;
  className: string;
  classCode: string;
  sectionId: string;
  sectionName: string;
  capacity: number | null;
  room: string;
  studentCount: number;
  classTeacherId: string;
  classTeacherName: string;
  subjects: BackendSubject[];
}

interface BackendClass {
  id: string;
  name: string;
  code: string;
  sections?: BackendSection[];
  subjects?: BackendSubject[];
}

interface BackendSection {
  id: string;
  classId: string;
  className: string;
  name: string;
  capacity: number;
  room?: string | null;
  studentCount?: number | null;
  classTeacherId?: string | null;
  classTeacherName?: string | null;
}

interface BackendSubject {
  id: string;
  name: string;
  code: string;
}

interface BackendTeacher {
  id: string;
  firstName: string;
  lastName: string;
  displayName: string;
  status: string;
}

@Component({
  selector: 'app-academics',
  imports: [TranslateModule, ReactiveFormsModule],
  template: `
    <div class="tab-page">
      <div class="page-header">
        <div>
          <h2>{{ 'academics.classesPage.title' | translate }}</h2>
          <p class="muted">{{ 'academics.classesPage.subtitle' | translate }}</p>
        </div>
        @if (canCreate) {
          <div class="header-actions">
            <button class="btn btn-primary" (click)="openAddModal()">{{ 'academics.addClass' | translate }}</button>
          </div>
        }
      </div>

      @if (successMessage) {
        <p class="banner success">{{ successMessage | translate }}</p>
      }
      @if (pageError) {
        <p class="banner error">{{ pageError }}</p>
      }

      <div class="card">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'academics.className' | translate }}</th>
                <th>{{ 'academics.sections' | translate }}</th>
                <th>{{ 'academics.classTeacher' | translate }}</th>
                <th>{{ 'academics.subjects' | translate }}</th>
                <th>{{ 'academics.room' | translate }}</th>
                <th>{{ 'academics.studentStrength' | translate }}</th>
                @if (canUpdate) {
                  <th>{{ 'common.actions' | translate }}</th>
                }
              </tr>
            </thead>
            <tbody>
              @for (row of classRows; track row.rowId) {
                <tr>
                  <td class="strong">{{ row.className }} <span class="muted code">{{ row.classCode }}</span></td>
                  <td>
                    @if (row.sectionName) {
                      <span class="section-chip">{{ row.sectionName }}</span>
                    } @else {
                      <span class="muted">—</span>
                    }
                  </td>
                  <td>{{ row.classTeacherName || '—' }}</td>
                  <td>
                    <div class="subject-tags">
                      @for (subject of row.subjects; track subject.id) {
                        <span class="tag">{{ subject.name }}</span>
                      } @empty {
                        <span class="muted">—</span>
                      }
                    </div>
                  </td>
                  <td>{{ row.room || '—' }}</td>
                  <td>
                    @if (row.capacity != null) {
                      {{ row.studentCount }}/{{ row.capacity }}
                    } @else {
                      <span class="muted">—</span>
                    }
                  </td>
                  @if (canUpdate) {
                    <td>
                      <div class="row-actions">
                        <button class="btn btn-sm" type="button" (click)="openEditModal(row)">{{ 'common.edit' | translate }}</button>
                        <button class="btn btn-sm btn-danger" type="button" [disabled]="deletingId === row.rowId" (click)="askDelete(row)">
                          {{ deletingId === row.rowId ? ('common.loading' | translate) : ('common.delete' | translate) }}
                        </button>
                      </div>
                    </td>
                  }
                </tr>
              } @empty {
                <tr><td [attr.colspan]="canUpdate ? 7 : 6" class="center">{{ 'common.noData' | translate }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>

    @if (showModal) {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editingRow ? ('academics.editClass' | translate) : ('academics.addClass' | translate) }}</h2>
          @if (formError) {
            <p class="form-error">{{ formError }}</p>
          }
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="form-grid">
              <div class="field">
                <label>{{ 'academics.className' | translate }} *</label>
                <input type="text" formControlName="name" placeholder="Class 8" (input)="onClassNameChange()" />
              </div>
              <div class="field">
                <label>Code</label>
                <input type="text" formControlName="code" placeholder="C8" />
              </div>
              <div class="field">
                <label>{{ 'academics.sections' | translate }} *</label>
                <select formControlName="sectionName">
                  <option value="" disabled>{{ 'academics.selectSection' | translate }}</option>
                  @for (section of availableSections; track section) {
                    <option [value]="section">{{ section }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'academics.capacity' | translate }}</label>
                <input type="number" formControlName="capacity" min="1" />
              </div>
              <div class="field">
                <label>{{ 'academics.room' | translate }}</label>
                <input type="text" formControlName="room" placeholder="R-12" />
              </div>
              <div class="field">
                <label>{{ 'academics.classTeacher' | translate }} *</label>
                <select formControlName="classTeacherId">
                  <option value="" disabled>{{ 'academics.selectTeacher' | translate }}</option>
                  @for (teacher of teachers; track teacher.id) {
                    <option [value]="teacher.id">{{ teacherLabel(teacher) }}</option>
                  }
                </select>
              </div>
              <div class="field span-2">
                <label>{{ 'academics.subjects' | translate }} *</label>
                <div class="multi-select">
                  @for (subject of subjects; track subject.id) {
                    <label class="check-item">
                      <input type="checkbox" [checked]="isSubjectSelected(subject.id)" (change)="toggleSubject(subject.id)" />
                      <span>{{ subject.name }}</span>
                    </label>
                  } @empty {
                    <span class="muted">{{ 'common.noData' | translate }}</span>
                  }
                </div>
              </div>
            </div>
            <div class="form-actions">
              <button class="btn" type="button" (click)="closeModal()">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="form.invalid || selectedSubjectIds.length === 0 || saving">
                {{ saving ? ('common.loading' | translate) : ('common.save' | translate) }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    @if (pendingDelete) {
      <div class="modal-backdrop" (click)="cancelDelete()">
        <div class="modal modal-sm" (click)="$event.stopPropagation()">
          <h2>{{ 'common.delete' | translate }}</h2>
          <p class="confirm-text">{{ 'academics.deleteConfirm' | translate:{ name: pendingDelete.className + (pendingDelete.sectionName ? ' ' + pendingDelete.sectionName : '') } }}</p>
          <div class="form-actions">
            <button class="btn" type="button" [disabled]="!!deletingId" (click)="cancelDelete()">{{ 'common.cancel' | translate }}</button>
            <button class="btn btn-danger" type="button" [disabled]="!!deletingId" (click)="confirmDelete()">
              {{ deletingId ? ('common.loading' | translate) : ('common.delete' | translate) }}
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
    .code { font-size: .8rem; font-weight: 400; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 14px 16px; border-bottom: 1px solid var(--color-border); font-size: .92rem; vertical-align: top; }
    th { color: var(--color-muted); font-weight: 600; font-size: .8rem; text-transform: uppercase; letter-spacing: .03em; background: var(--color-bg); }
    .strong { font-weight: 600; }
    .center { text-align: center; color: var(--color-muted); padding: 28px; }
    .section-chip {
      padding: 4px 10px; border-radius: 20px; font-size: .82rem;
      background: var(--color-primary-soft); color: var(--color-primary); font-weight: 600;
    }
    .subject-tags { display: flex; flex-wrap: wrap; gap: 6px; }
    .tag { padding: 3px 10px; border-radius: 6px; background: var(--color-bg); border: 1px solid var(--color-border); font-size: .82rem; }
    .banner { background: #eff6ff; border: 1px solid #bfdbfe; color: #1e40af; padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
    .banner.success { background: #ecfdf5; border-color: #a7f3d0; color: #047857; }
    .banner.error { background: #fef2f2; border-color: #fecaca; color: #b91c1c; }
    .row-actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .btn-sm { padding: 5px 10px; font-size: .82rem; }
    .btn-danger { background: #b91c1c; border-color: #b91c1c; color: #fff; }
    .btn-danger:hover { background: #991b1b; color: #fff; border-color: #991b1b; }
    .modal-sm { width: 420px; }
    .confirm-text { margin: 0 0 8px; color: var(--color-muted); line-height: 1.45; }

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
export class AcademicsComponent implements OnInit {
  classes: SchoolClass[] = [];
  subjects: BackendSubject[] = [];
  teachers: BackendTeacher[] = [];
  readonly sectionOptions = ['A', 'B', 'C'];
  showModal = false;
  saving = false;
  formError = '';
  successMessage = '';
  pageError = '';
  editingRow: ClassRow | null = null;
  pendingDelete: ClassRow | null = null;
  deletingId: string | null = null;

  readonly form = new FormGroup({
    name: new FormControl('', Validators.required),
    code: new FormControl(''),
    sectionName: new FormControl('', Validators.required),
    capacity: new FormControl(40),
    room: new FormControl(''),
    classTeacherId: new FormControl('', Validators.required),
  });
  selectedSubjectIds: string[] = [];

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef, private auth: AuthService) {}

  get canCreate(): boolean {
    return this.auth.hasPermission('CLASS_CREATE');
  }

  get canUpdate(): boolean {
    return this.auth.hasPermission('CLASS_UPDATE');
  }

  get classRows(): ClassRow[] {
    const rows: ClassRow[] = [];
    for (const klass of this.classes) {
      if (!klass.sections.length) {
        rows.push({
          rowId: klass.id,
          classId: klass.id,
          className: klass.name,
          classCode: klass.code,
          sectionId: '',
          sectionName: '',
          capacity: null,
          room: '',
          studentCount: 0,
          classTeacherId: '',
          classTeacherName: '',
          subjects: klass.subjects,
        });
        continue;
      }
      for (const section of klass.sections) {
        rows.push({
          rowId: section.id,
          classId: klass.id,
          className: klass.name,
          classCode: klass.code,
          sectionId: section.id,
          sectionName: section.name,
          capacity: section.capacity,
          room: section.room,
          studentCount: section.studentCount,
          classTeacherId: section.classTeacherId,
          classTeacherName: section.classTeacherName,
          subjects: klass.subjects,
        });
      }
    }
    return rows;
  }

  isSubjectSelected(id: string): boolean {
    return this.selectedSubjectIds.includes(id);
  }

  toggleSubject(id: string): void {
    if (this.selectedSubjectIds.includes(id)) {
      this.selectedSubjectIds = this.selectedSubjectIds.filter((item) => item !== id);
    } else {
      this.selectedSubjectIds = [...this.selectedSubjectIds, id];
    }
    this.cdr.markForCheck();
  }

  ngOnInit(): void {
    this.load();
  }

  teacherLabel(teacher: BackendTeacher): string {
    return teacher.displayName || [teacher.firstName, teacher.lastName].filter(Boolean).join(' ');
  }

  get availableSections(): string[] {
    const name = (this.form.value.name || '').trim().toLowerCase();
    const existing = this.classes.find((klass) => klass.name.toLowerCase() === name);
    if (!existing) {
      return this.sectionOptions;
    }
    const used = new Set(existing.sections.map((section) => section.name.toUpperCase()));
    if (this.editingRow?.sectionName) {
      used.delete(this.editingRow.sectionName.toUpperCase());
    }
    return this.sectionOptions.filter((section) => !used.has(section));
  }

  onClassNameChange(): void {
    const selected = this.form.value.sectionName;
    if (selected && !this.availableSections.includes(selected)) {
      this.form.patchValue({ sectionName: '' });
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
    this.selectedSubjectIds = [];
    this.form.reset({ name: '', code: '', sectionName: '', capacity: 40, room: '', classTeacherId: '' });
    this.loadLookups();
    this.cdr.markForCheck();
  }

  openEditModal(row: ClassRow): void {
    this.editingRow = row;
    this.showModal = true;
    this.saving = false;
    this.formError = '';
    this.successMessage = '';
    this.pageError = '';
    this.selectedSubjectIds = row.subjects.map((subject) => subject.id);
    this.form.reset({
      name: row.className,
      code: row.classCode,
      sectionName: row.sectionName,
      capacity: row.capacity ?? 40,
      room: row.room,
      classTeacherId: row.classTeacherId,
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

  askDelete(row: ClassRow): void {
    this.pendingDelete = row;
    this.cdr.markForCheck();
  }

  cancelDelete(): void {
    if (this.deletingId) {
      return;
    }
    this.pendingDelete = null;
    this.cdr.markForCheck();
  }

  confirmDelete(): void {
    if (!this.pendingDelete) {
      return;
    }
    const row = this.pendingDelete;
    this.deletingId = row.rowId;
    this.cdr.markForCheck();
    const request$ = row.sectionId
      ? this.http.delete<ApiResponse<unknown>>(`/api/sections/${row.sectionId}`)
      : this.http.delete<ApiResponse<unknown>>(`/api/classes/${row.classId}`);
    request$.subscribe({
      next: () => {
        this.deletingId = null;
        this.pendingDelete = null;
        this.successMessage = 'academics.deleteSuccess';
        this.pageError = '';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.deletingId = null;
        this.pendingDelete = null;
        const apiError = err.error as ApiError | undefined;
        this.successMessage = '';
        this.pageError = apiError?.message || 'Could not delete class. Please try again.';
        this.cdr.markForCheck();
      },
    });
  }

  onSubmit(): void {
    if (this.form.invalid || this.selectedSubjectIds.length === 0) {
      if (this.selectedSubjectIds.length === 0) {
        this.formError = 'Select at least one subject.';
        this.cdr.markForCheck();
      }
      return;
    }
    this.saving = true;
    this.formError = '';
    this.cdr.markForCheck();
    if (this.editingRow) {
      const body = {
        name: this.form.value.name,
        code: this.form.value.code || this.form.value.name,
        sectionId: this.editingRow.sectionId || null,
        sectionName: this.form.value.sectionName,
        capacity: this.form.value.capacity || 40,
        room: this.form.value.room || null,
        subjectIds: this.selectedSubjectIds,
        classTeacherId: this.form.value.classTeacherId,
      };
      this.http.put<ApiResponse<unknown>>(`/api/classes/${this.editingRow.classId}`, body).subscribe({
        next: () => this.afterSave('academics.updateSuccess'),
        error: (err: HttpErrorResponse) => this.onSaveError(err),
      });
      return;
    }
    const body = {
      name: this.form.value.name,
      code: this.form.value.code || this.form.value.name,
      sortOrder: 0,
      sectionName: this.form.value.sectionName,
      capacity: this.form.value.capacity || 40,
      room: this.form.value.room || null,
      subjectIds: this.selectedSubjectIds,
      classTeacherId: this.form.value.classTeacherId,
    };
    this.http.post<ApiResponse<unknown>>('/api/classes', body).subscribe({
      next: () => this.afterSave('academics.createSuccess'),
      error: (err: HttpErrorResponse) => this.onSaveError(err),
    });
  }

  private afterSave(messageKey: string): void {
    this.saving = false;
    this.showModal = false;
    this.editingRow = null;
    this.successMessage = messageKey;
    this.pageError = '';
    this.cdr.markForCheck();
    this.load();
  }

  private onSaveError(err: HttpErrorResponse): void {
    this.saving = false;
    const apiError = err.error as ApiError | undefined;
    this.formError = apiError?.message || 'Could not save class. Please try again.';
    this.cdr.markForCheck();
  }

  private load(): void {
    this.http.get<ApiResponse<BackendClass[]>>('/api/classes').subscribe({
      next: (res) => {
        this.classes = (res.data ?? []).map((klass) => ({
          id: klass.id,
          name: klass.name,
          code: klass.code,
          sections: (klass.sections ?? []).map((section) => ({
            id: section.id,
            name: section.name,
            capacity: section.capacity,
            room: section.room ?? '',
            studentCount: section.studentCount ?? 0,
            classTeacherId: section.classTeacherId ?? '',
            classTeacherName: section.classTeacherName ?? '',
          })),
          subjects: klass.subjects ?? [],
        }));
        this.cdr.markForCheck();
      },
      error: () => {
        this.classes = [];
        this.cdr.markForCheck();
      },
    });
  }

  private loadLookups(): void {
    this.http.get<ApiResponse<BackendSubject[]>>('/api/subjects').subscribe({
      next: (res) => {
        this.subjects = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
    this.http.get<ApiResponse<BackendTeacher[]>>('/api/teachers').subscribe({
      next: (res) => {
        this.teachers = (res.data ?? []).filter((teacher) => teacher.status !== 'INACTIVE');
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

}
