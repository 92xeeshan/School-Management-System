import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ApiError, ApiResponse } from '../../core/models/api.model';

interface SchoolClass {
  id: string;
  name: string;
  code: string;
  sections: SchoolSection[];
  subjects: string[];
}

interface SchoolSection {
  id: string;
  name: string;
  capacity: number;
  classTeacherName: string;
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
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'academics.title' | translate }}</h1>
          <p class="muted">{{ 'academics.subtitle' | translate }}</p>
        </div>
        <div class="header-actions">
          <button class="btn btn-primary" (click)="openAddModal()">{{ 'academics.addClass' | translate }}</button>
        </div>
      </div>

      <div class="card">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'academics.className' | translate }}</th>
                <th>{{ 'academics.sections' | translate }}</th>
                <th>{{ 'academics.classTeacher' | translate }}</th>
                <th>{{ 'academics.subjects' | translate }}</th>
              </tr>
            </thead>
            <tbody>
              @for (klass of classes; track klass.id) {
                <tr>
                  <td class="strong">{{ klass.name }} <span class="muted code">{{ klass.code }}</span></td>
                  <td>
                    <div class="sections">
                      @for (section of klass.sections; track section.id) {
                        <div class="section-chip">
                          {{ section.name }} · {{ section.capacity }}
                        </div>
                      } @empty {
                        <span class="muted">—</span>
                      }
                    </div>
                  </td>
                  <td>
                    <div class="teacher-list">
                      @for (section of klass.sections; track section.id) {
                        <div>{{ section.name }}: {{ section.classTeacherName || '—' }}</div>
                      } @empty {
                        <span class="muted">—</span>
                      }
                    </div>
                  </td>
                  <td>
                    <div class="subject-tags">
                      @for (subject of klass.subjects; track subject) {
                        <span class="tag">{{ subject }}</span>
                      } @empty {
                        <span class="muted">—</span>
                      }
                    </div>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="4" class="center">{{ 'common.noData' | translate }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>

    @if (showModal) {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ 'academics.addClass' | translate }}</h2>
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
                <label>{{ 'academics.subjects' | translate }} *</label>
                <select formControlName="subjectId">
                  <option value="" disabled>{{ 'academics.selectSubject' | translate }}</option>
                  @for (subject of subjects; track subject.id) {
                    <option [value]="subject.id">{{ subject.name }}</option>
                  }
                </select>
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
            </div>
            <div class="form-actions">
              <button class="btn" type="button" (click)="closeModal()">{{ 'common.cancel' | translate }}</button>
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
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    h1 { font-size: 1.5rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); }
    .code { font-size: .8rem; font-weight: 400; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 14px 16px; border-bottom: 1px solid var(--color-border); font-size: .92rem; vertical-align: top; }
    th { color: var(--color-muted); font-weight: 600; font-size: .8rem; text-transform: uppercase; letter-spacing: .03em; background: var(--color-bg); }
    .strong { font-weight: 600; }
    .center { text-align: center; color: var(--color-muted); padding: 28px; }
    .sections { display: flex; flex-wrap: wrap; gap: 8px; }
    .section-chip {
      padding: 4px 10px; border-radius: 20px; font-size: .82rem;
      background: var(--color-primary-soft); color: var(--color-primary); font-weight: 600;
    }
    .subject-tags { display: flex; flex-wrap: wrap; gap: 6px; }
    .tag { padding: 3px 10px; border-radius: 6px; background: var(--color-bg); border: 1px solid var(--color-border); font-size: .82rem; }
    .teacher-list { display: flex; flex-direction: column; gap: 4px; font-size: .88rem; }

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
    @media (max-width: 640px) { .form-grid { grid-template-columns: 1fr; } }
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

  readonly form = new FormGroup({
    name: new FormControl('', Validators.required),
    code: new FormControl(''),
    sectionName: new FormControl('', Validators.required),
    capacity: new FormControl(40),
    subjectId: new FormControl('', Validators.required),
    classTeacherId: new FormControl('', Validators.required),
  });

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

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
    this.showModal = true;
    this.saving = false;
    this.formError = '';
    this.form.reset({ name: '', code: '', sectionName: '', capacity: 40, subjectId: '', classTeacherId: '' });
    this.loadLookups();
    this.cdr.markForCheck();
  }

  closeModal(): void {
    if (this.saving) {
      return;
    }
    this.showModal = false;
    this.formError = '';
    this.cdr.markForCheck();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }
    this.saving = true;
    this.formError = '';
    this.cdr.markForCheck();
    const body = {
      name: this.form.value.name,
      code: this.form.value.code || this.form.value.name,
      sortOrder: 0,
      sectionName: this.form.value.sectionName,
      capacity: this.form.value.capacity || 40,
      subjectIds: this.form.value.subjectId ? [this.form.value.subjectId] : [],
      classTeacherId: this.form.value.classTeacherId,
    };
    this.http.post<ApiResponse<unknown>>('/api/classes', body).subscribe({
      next: () => {
        this.saving = false;
        this.showModal = false;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        const apiError = err.error as ApiError | undefined;
        this.formError = apiError?.message || 'Could not save class. Please try again.';
        this.cdr.markForCheck();
      },
    });
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
            classTeacherName: section.classTeacherName ?? '',
          })),
          subjects: (klass.subjects ?? []).map((subject) => subject.name),
        }));
        this.cdr.markForCheck();
      },
      error: () => this.loadDemo(),
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

  private loadDemo(): void {
    this.classes = [
      { id: 'c1', name: 'VI', code: 'VI', sections: [
        { id: 's1', name: 'A', capacity: 40, classTeacherName: 'Asha Sharma' },
        { id: 's2', name: 'B', capacity: 40, classTeacherName: '—' },
      ], subjects: ['English', 'Hindi', 'Mathematics', 'Science', 'Social Studies'] },
      { id: 'c2', name: 'VII', code: 'VII', sections: [
        { id: 's3', name: 'A', capacity: 40, classTeacherName: 'Asha Sharma' },
      ], subjects: ['English', 'Hindi', 'Mathematics', 'Science', 'Social Studies', 'Urdu'] },
      { id: 'c3', name: 'VIII', code: 'VIII', sections: [
        { id: 's4', name: 'A', capacity: 40, classTeacherName: '—' },
        { id: 's5', name: 'B', capacity: 40, classTeacherName: '—' },
      ], subjects: ['English', 'Mathematics', 'Science', 'Social Studies', 'Computer Science'] },
    ];
    this.cdr.markForCheck();
  }
}
