import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { ApiResponse, PagedResponse } from '../../core/models/api.model';
import { AuthService } from '../../core/auth/auth.service';

interface Student {
  id: string;
  admissionNo: string;
  firstName: string;
  lastName: string;
  className: string;
  section: string;
  rollNumber: number;
  gender: string;
  phone: string;
  guardianName: string;
  status: string;
}

interface BackendStudent {
  id: string;
  admissionNo: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string | null;
  gender: string;
  admissionDate: string | null;
  status: string;
}

interface BackendStudentListItem {
  student: {
    id: string;
    admissionNo: string;
    firstName: string;
    lastName: string;
    gender: string;
    status: string;
  };
  className: string;
  sectionName: string;
  rollNumber: number;
}

interface ClassOption {
  id: string;
  name: string;
}

interface SectionOption {
  id: string;
  classId: string;
  className: string;
  name: string;
}

interface AcademicYear {
  id: string;
  name: string;
  current: boolean;
}

@Component({
  selector: 'app-students',
  imports: [TranslateModule, ReactiveFormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'students.title' | translate }}</h1>
          <p class="muted">{{ 'students.subtitle' | translate }}</p>
        </div>
        @if (canCreate) {
          <button class="btn btn-primary" (click)="openAddModal()">{{ 'students.addStudent' | translate }}</button>
        }
      </div>

      <div class="card">
        <div class="table-toolbar">
          <input class="search" type="search" [placeholder]="'common.search' | translate" (input)="filter = $any($event.target).value" />
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'students.studentId' | translate }}</th>
                <th>{{ 'students.name' | translate }}</th>
                <th>{{ 'students.className' | translate }}</th>
                <th>{{ 'students.section' | translate }}</th>
                <th>{{ 'students.rollNumber' | translate }}</th>
                <th>{{ 'students.gender' | translate }}</th>
                <th>{{ 'common.status' | translate }}</th>
                @if (canUpdate || canDelete) {
                  <th>{{ 'common.actions' | translate }}</th>
                }
              </tr>
            </thead>
            <tbody>
              @for (student of filteredStudents; track student.id) {
                <tr>
                  <td>{{ student.admissionNo }}</td>
                  <td class="strong">{{ student.firstName }} {{ student.lastName }}</td>
                  <td>{{ student.className }}</td>
                  <td>{{ student.section }}</td>
                  <td>{{ student.rollNumber }}</td>
                  <td>{{ student.gender }}</td>
                  <td><span class="badge" [class.badge-success]="student.status === 'ACTIVE'" [class.badge-muted]="student.status !== 'ACTIVE'">{{ student.status }}</span></td>
                  @if (canUpdate || canDelete) {
                  <td>
                    <div class="row-actions">
                      @if (canUpdate) {
                        <button class="btn btn-sm" type="button" (click)="openEditModal(student)">{{ 'common.edit' | translate }}</button>
                      }
                      @if (canDelete && student.status === 'ACTIVE') {
                        <button class="btn btn-sm btn-danger" type="button" [disabled]="deletingId === student.id" (click)="askDelete(student)">
                          {{ deletingId === student.id ? ('common.loading' | translate) : ('common.delete' | translate) }}
                        </button>
                      }
                    </div>
                  </td>
                  }
                </tr>
              } @empty {
                <tr><td [attr.colspan]="canUpdate || canDelete ? 8 : 7" class="center">{{ 'common.noData' | translate }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>

    @if (showModal) {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editingId ? ('students.editStudent' | translate) : ('students.addStudent' | translate) }}</h2>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="form-grid">
              <div class="field">
                <label>{{ 'students.studentId' | translate }} *</label>
                <input type="text" formControlName="admissionNo" placeholder="ADM2026-010" />
              </div>
              <div class="field">
                <label>{{ 'students.name' | translate }} *</label>
                <input type="text" formControlName="firstName" />
              </div>
              <div class="field">
                <label>{{ 'students.name' | translate }} (last)</label>
                <input type="text" formControlName="lastName" />
              </div>
              <div class="field">
                <label>DOB</label>
                <input type="date" formControlName="dateOfBirth" />
              </div>
              <div class="field">
                <label>{{ 'students.gender' | translate }}</label>
                <select formControlName="gender">
                  <option value="MALE">MALE</option>
                  <option value="FEMALE">FEMALE</option>
                  <option value="OTHER">OTHER</option>
                </select>
              </div>
              <div class="field">
                <label>{{ 'students.enrollmentDate' | translate }}</label>
                <input type="date" formControlName="admissionDate" />
              </div>
              <div class="field">
                <label>Class</label>
                <select formControlName="classId" (change)="onClassSelect()">
                  <option value="" disabled selected>—</option>
                  @for (klass of classes; track klass.id) {
                    <option [value]="klass.id">{{ klass.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'students.section' | translate }}</label>
                <select formControlName="sectionId">
                  <option value="" disabled selected>—</option>
                  @for (section of sectionsForClass; track section.id) {
                    <option [value]="section.id">{{ section.name }}</option>
                  }
                </select>
              </div>
            </div>
            <div class="form-actions">
              <button class="btn" type="button" (click)="closeModal(true)">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving">
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
          <p class="confirm-text">{{ 'students.deleteConfirm' | translate:{ name: pendingDelete.firstName + ' ' + pendingDelete.lastName } }}</p>
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
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    h1 { font-size: 1.5rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); }
    .table-toolbar { padding: 14px 16px; border-bottom: 1px solid var(--color-border); }
    .search {
      padding: 8px 12px; border: 1px solid var(--color-border); border-radius: 8px;
      width: 260px; font: inherit;
    }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 12px 16px; border-bottom: 1px solid var(--color-border); font-size: .92rem; }
    th { color: var(--color-muted); font-weight: 600; font-size: .8rem; text-transform: uppercase; letter-spacing: .03em; background: var(--color-bg); }
    tbody tr:hover { background: #f8fafc; }
    .strong { font-weight: 600; }
    .center { text-align: center; color: var(--color-muted); padding: 28px; }

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
    .row-actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .btn-sm { padding: 5px 10px; font-size: .82rem; }
    .btn-danger { background: #b91c1c; border-color: #b91c1c; color: #fff; }
    .btn-danger:hover { background: #991b1b; color: #fff; border-color: #991b1b; }
    .modal-sm { width: 420px; }
    .confirm-text { margin: 0 0 8px; color: var(--color-muted); line-height: 1.45; }
    @media (max-width: 640px) { .form-grid { grid-template-columns: 1fr; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentsComponent implements OnInit {
  students: Student[] = [];
  filter = '';
  showModal = false;
  saving = false;
  editingId: string | null = null;
  pendingDelete: Student | null = null;
  deletingId: string | null = null;
  classes: ClassOption[] = [];
  sections: SectionOption[] = [];
  academicYears: AcademicYear[] = [];

  readonly form = new FormGroup({
    admissionNo: new FormControl('', Validators.required),
    firstName: new FormControl('', Validators.required),
    lastName: new FormControl(''),
    dateOfBirth: new FormControl(''),
    gender: new FormControl('MALE'),
    admissionDate: new FormControl(new Date().toISOString().slice(0, 10)),
    classId: new FormControl(''),
    sectionId: new FormControl(''),
  });

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef, private auth: AuthService) {}

  get canCreate(): boolean {
    return this.auth.hasPermission('STUDENT_CREATE');
  }

  get canUpdate(): boolean {
    return this.auth.hasPermission('STUDENT_UPDATE');
  }

  get canDelete(): boolean {
    return this.auth.hasPermission('STUDENT_DELETE');
  }

  get sectionsForClass(): SectionOption[] {
    const classId = this.form.value.classId;
    return this.sections.filter((s) => s.classId === classId);
  }

  get filteredStudents(): Student[] {
    const q = this.filter.trim().toLowerCase();
    if (!q) {
      return this.students;
    }
    return this.students.filter(
      (s) =>
        (s.firstName ?? '').toLowerCase().includes(q) ||
        (s.lastName ?? '').toLowerCase().includes(q) ||
        (s.admissionNo ?? '').toLowerCase().includes(q) ||
        (s.className ?? '').toLowerCase().includes(q)
    );
  }

  ngOnInit(): void {
    this.load();
    this.loadMeta();
  }

  openAddModal(): void {
    this.editingId = null;
    this.showModal = true;
    this.saving = false;
    this.form.reset({ gender: 'MALE', admissionDate: new Date().toISOString().slice(0, 10), classId: '', sectionId: '' });
    this.cdr.markForCheck();
  }

  openEditModal(student: Student): void {
    this.editingId = student.id;
    this.saving = false;
    this.showModal = true;
    this.form.reset({
      admissionNo: student.admissionNo,
      firstName: student.firstName,
      lastName: student.lastName,
      dateOfBirth: '',
      gender: student.gender || 'MALE',
      admissionDate: '',
      classId: this.classIdForName(student.className),
      sectionId: this.sectionIdFor(student.className, student.section),
    });
    this.cdr.markForCheck();
    this.http.get<ApiResponse<BackendStudent>>(`/api/students/${student.id}`).subscribe({
      next: (res) => {
        if (this.editingId !== student.id) {
          return;
        }
        const data = res.data;
        this.form.patchValue({
          admissionNo: data.admissionNo ?? student.admissionNo,
          firstName: data.firstName ?? student.firstName,
          lastName: data.lastName ?? student.lastName,
          dateOfBirth: data.dateOfBirth ?? '',
          gender: data.gender ?? student.gender ?? 'MALE',
          admissionDate: data.admissionDate ?? '',
        });
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  closeModal(force = false): void {
    if (this.saving && !force) {
      return;
    }
    this.saving = false;
    this.showModal = false;
    this.editingId = null;
    this.cdr.markForCheck();
  }

  askDelete(student: Student): void {
    this.pendingDelete = student;
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
    const student = this.pendingDelete;
    if (!student) {
      return;
    }
    this.deletingId = student.id;
    this.cdr.markForCheck();
    this.http.patch<ApiResponse<void>>(`/api/students/${student.id}/deactivate`, {}).subscribe({
      next: () => {
        this.deletingId = null;
        this.pendingDelete = null;
        this.load();
        this.cdr.markForCheck();
      },
      error: () => {
        this.deletingId = null;
        this.cdr.markForCheck();
        alert('Could not delete student. Please try again.');
      },
    });
  }

  onClassSelect(): void {
    this.form.patchValue({ sectionId: '' });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }
    this.saving = true;
    this.cdr.markForCheck();
    const { admissionNo, firstName, lastName, dateOfBirth, gender, admissionDate } = this.form.value;
    const body = {
      admissionNo,
      firstName,
      lastName: lastName || null,
      dateOfBirth: dateOfBirth || null,
      gender,
      admissionDate: admissionDate || null,
    };
    if (this.editingId) {
      const studentId = this.editingId;
      this.http.put<ApiResponse<unknown>>(`/api/students/${studentId}`, body).subscribe({
        next: () => this.enroll(studentId),
        error: () => {
          this.saving = false;
          this.cdr.markForCheck();
          alert('Could not update student. Please try again.');
        },
      });
      return;
    }
    this.http.post<ApiResponse<{ id: string }>>('/api/students', body).subscribe({
      next: (res) => this.enroll(res.data.id),
      error: () => {
        this.saving = false;
        this.cdr.markForCheck();
        alert('Could not save student. Please try again.');
      },
    });
  }

  private classIdForName(className: string): string {
    return this.classes.find((c) => c.name === className)?.id ?? '';
  }

  private sectionIdFor(className: string, sectionName: string): string {
    const classId = this.classIdForName(className);
    return this.sections.find((s) => s.classId === classId && s.name === sectionName)?.id ?? '';
  }

  private enroll(studentId: string): void {
    const classId = this.form.value.classId;
    const sectionId = this.form.value.sectionId;
    const academicYearId = this.academicYears.find((y) => y.current)?.id ?? this.academicYears[0]?.id;
    const finish = (): void => {
      this.saving = false;
      this.showModal = false;
      this.editingId = null;
      this.load();
      this.cdr.markForCheck();
    };
    if (!classId || !sectionId || !academicYearId) {
      finish();
      return;
    }
    this.http
      .post<ApiResponse<unknown>>(`/api/students/${studentId}/enroll`, { classId, sectionId, academicYearId })
      .pipe(finalize(finish))
      .subscribe({
        next: () => undefined,
        error: () => alert('Could not update class or section. Please try again.'),
      });
  }

  private load(): void {
    this.http.get<ApiResponse<PagedResponse<BackendStudentListItem>>>('/api/students', { params: { size: '100' } }).subscribe({
      next: (res) => {
        const items = res.data?.content ?? [];
        this.students = items.map((item) => ({
          id: item.student.id,
          admissionNo: item.student.admissionNo ?? '',
          firstName: item.student.firstName ?? '',
          lastName: item.student.lastName ?? '',
          className: item.className ?? '',
          section: item.sectionName ?? '',
          rollNumber: item.rollNumber ?? 0,
          gender: item.student.gender ?? '',
          phone: '',
          guardianName: '',
          status: item.student.status ?? '',
        }));
        this.cdr.markForCheck();
      },
      error: () => {
        this.students = [];
        this.cdr.markForCheck();
      },
    });
  }

  private loadMeta(): void {
    this.http.get<ApiResponse<AcademicYear[]>>('/api/academic-years').subscribe({
      next: (res) => {
        this.academicYears = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
    this.http.get<ApiResponse<ClassOption[]>>('/api/classes').subscribe({
      next: (res) => {
        this.classes = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
    this.http.get<ApiResponse<SectionOption[]>>('/api/sections').subscribe({
      next: (res) => {
        this.sections = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

}
