import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ApiError, ApiResponse } from '../../core/models/api.model';

interface AttendanceSummary {
  present: number;
  absent: number;
  late: number;
  leave: number;
  total: number;
  percentage: number;
}

interface StudentProfile {
  id: string;
  admissionNo: string;
  firstName: string;
  lastName: string;
  displayName: string;
  dateOfBirth: string | null;
  gender: string | null;
  bloodGroup: string | null;
  religion: string | null;
  nationality: string | null;
  phone: string | null;
  emergencyContact: string | null;
  permanentAddress: string | null;
  presentAddress: string | null;
  previousSchool: string | null;
  fatherName: string | null;
  motherName: string | null;
  fatherPhone: string | null;
  motherPhone: string | null;
  admissionDate: string | null;
  photoUrl: string | null;
  status: string;
  classId: string | null;
  className: string | null;
  sectionId: string | null;
  sectionName: string | null;
  rollNumber: number | null;
  academicYearId: string | null;
  academicYearName: string | null;
  enrollmentDate: string | null;
  attendance: AttendanceSummary | null;
  canEdit: boolean;
  editableFields: string[];
}

interface ClassOption {
  id: string;
  name: string;
  sections?: SectionOption[];
}

interface SectionOption {
  id: string;
  classId: string;
  name: string;
}

@Component({
  selector: 'app-student-profile',
  imports: [TranslateModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <a class="back" routerLink="/students">{{ 'common.back' | translate }}</a>
          <h1>{{ profile?.displayName || ('students.profileTitle' | translate) }}</h1>
          <p class="muted">{{ 'students.profileSubtitle' | translate }}</p>
        </div>
        @if (profile?.canEdit && !editing) {
          <button class="btn btn-primary" type="button" (click)="startEdit()">{{ 'common.edit' | translate }}</button>
        }
      </div>

      @if (loading) {
        <div class="card pad">{{ 'common.loading' | translate }}</div>
      } @else if (error) {
        <div class="card pad error">{{ error | translate }}</div>
      } @else if (profile) {
        @if (successMessage) {
          <p class="banner success">{{ successMessage | translate }}</p>
        }
        @if (!profile.canEdit) {
          <p class="banner">{{ 'students.viewOnly' | translate }}</p>
        }
        <form [formGroup]="form" (ngSubmit)="onSave()">
          <div class="card">
            <h2>{{ 'students.personal' | translate }}</h2>
            <div class="form-grid">
              <div class="field">
                <label>{{ 'students.studentId' | translate }}</label>
                <input type="text" formControlName="admissionNo" />
              </div>
              <div class="field">
                <label>{{ 'students.firstName' | translate }} *</label>
                <input type="text" formControlName="firstName" />
              </div>
              <div class="field">
                <label>{{ 'students.lastName' | translate }}</label>
                <input type="text" formControlName="lastName" />
              </div>
              <div class="field">
                <label>{{ 'students.dateOfBirth' | translate }}</label>
                <input type="date" formControlName="dateOfBirth" />
              </div>
              <div class="field">
                <label>{{ 'students.gender' | translate }}</label>
                <select formControlName="gender">
                  <option value="">—</option>
                  <option value="MALE">MALE</option>
                  <option value="FEMALE">FEMALE</option>
                  <option value="OTHER">OTHER</option>
                </select>
              </div>
              <div class="field">
                <label>{{ 'students.bloodGroup' | translate }}</label>
                <input type="text" formControlName="bloodGroup" />
              </div>
              <div class="field">
                <label>{{ 'students.religion' | translate }}</label>
                <input type="text" formControlName="religion" />
              </div>
              <div class="field">
                <label>{{ 'students.nationality' | translate }}</label>
                <input type="text" formControlName="nationality" />
              </div>
              <div class="field">
                <label>{{ 'students.phone' | translate }}</label>
                <input type="text" formControlName="phone" />
                @if (editing && form.controls.phone.invalid && form.controls.phone.touched) {
                  <span class="field-error">{{ 'students.invalidPhone' | translate }}</span>
                }
              </div>
              <div class="field">
                <label>{{ 'students.emergencyContact' | translate }}</label>
                <input type="text" formControlName="emergencyContact" />
                @if (editing && form.controls.emergencyContact.invalid && form.controls.emergencyContact.touched) {
                  <span class="field-error">{{ 'students.invalidPhone' | translate }}</span>
                }
              </div>
              <div class="field span-2">
                <label>{{ 'students.permanentAddress' | translate }}</label>
                <input type="text" formControlName="permanentAddress" />
              </div>
              <div class="field span-2">
                <label>{{ 'students.presentAddress' | translate }}</label>
                <input type="text" formControlName="presentAddress" />
              </div>
              <div class="field span-2">
                <label>{{ 'students.previousSchool' | translate }}</label>
                <input type="text" formControlName="previousSchool" />
              </div>
            </div>
          </div>

          <div class="card">
            <h2>{{ 'students.family' | translate }}</h2>
            <div class="form-grid">
              <div class="field">
                <label>{{ 'students.fatherName' | translate }}</label>
                <input type="text" formControlName="fatherName" />
              </div>
              <div class="field">
                <label>{{ 'students.fatherPhone' | translate }}</label>
                <input type="text" formControlName="fatherPhone" />
                @if (editing && form.controls.fatherPhone.invalid && form.controls.fatherPhone.touched) {
                  <span class="field-error">{{ 'students.invalidPhone' | translate }}</span>
                }
              </div>
              <div class="field">
                <label>{{ 'students.motherName' | translate }}</label>
                <input type="text" formControlName="motherName" />
              </div>
              <div class="field">
                <label>{{ 'students.motherPhone' | translate }}</label>
                <input type="text" formControlName="motherPhone" />
                @if (editing && form.controls.motherPhone.invalid && form.controls.motherPhone.touched) {
                  <span class="field-error">{{ 'students.invalidPhone' | translate }}</span>
                }
              </div>
            </div>
          </div>

          <div class="card">
            <h2>{{ 'students.academic' | translate }}</h2>
            <div class="form-grid">
              <div class="field">
                <label>{{ 'students.className' | translate }}</label>
                <select formControlName="classId" (change)="onClassChange()">
                  <option value="">—</option>
                  @for (klass of classes; track klass.id) {
                    <option [value]="klass.id">{{ klass.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'students.section' | translate }}</label>
                <select formControlName="sectionId">
                  <option value="">—</option>
                  @for (section of sectionsForClass; track section.id) {
                    <option [value]="section.id">{{ section.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'students.rollNumber' | translate }}</label>
                <input type="number" formControlName="rollNumber" />
              </div>
              <div class="field">
                <label>{{ 'students.admissionDate' | translate }}</label>
                <input type="date" formControlName="admissionDate" />
              </div>
              <div class="field">
                <label>{{ 'students.academicYear' | translate }}</label>
                <input type="text" [value]="profile.academicYearName || '—'" disabled />
              </div>
              <div class="field">
                <label>{{ 'common.status' | translate }}</label>
                <input type="text" [value]="profile.status" disabled />
              </div>
            </div>
          </div>

          <div class="card">
            <h2>{{ 'students.attendance' | translate }}</h2>
            @if (profile.attendance; as att) {
              <div class="stats">
                <div><span class="muted">{{ 'attendance.present' | translate }}</span><strong>{{ att.present }}</strong></div>
                <div><span class="muted">{{ 'attendance.absent' | translate }}</span><strong>{{ att.absent }}</strong></div>
                <div><span class="muted">{{ 'attendance.late' | translate }}</span><strong>{{ att.late }}</strong></div>
                <div><span class="muted">{{ 'students.percentage' | translate }}</span><strong>{{ att.percentage }}%</strong></div>
              </div>
            } @else {
              <p class="muted">{{ 'common.noData' | translate }}</p>
            }
          </div>

          @if (editing) {
            @if (saveError) {
              <p class="error">{{ saveError | translate }}</p>
            }
            <div class="form-actions">
              <button class="btn" type="button" [disabled]="saving" (click)="cancelEdit()">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving">
                {{ saving ? ('common.loading' | translate) : ('common.save' | translate) }}
              </button>
            </div>
          }
        </form>
      }
    </div>
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; gap: 16px; }
    h1 { font-size: 1.5rem; margin: 4px 0; }
    h2 { font-size: 1.05rem; margin: 0 0 16px; }
    .muted { color: var(--color-muted); }
    .back { color: var(--color-primary, #2563eb); text-decoration: none; font-size: .9rem; }
    .card { background: #fff; border: 1px solid var(--color-border); border-radius: var(--radius); padding: 20px; margin-bottom: 16px; }
    .pad { padding: 28px; }
    .banner { background: #eff6ff; border: 1px solid #bfdbfe; color: #1e40af; padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
    .banner.success { background: #ecfdf5; border-color: #a7f3d0; color: #047857; }
    .field-error { color: #b91c1c; font-size: .8rem; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .span-2 { grid-column: span 2; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    input, select { padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; }
    input:disabled, select:disabled { background: #f8fafc; color: #475569; }
    .form-actions { display: flex; justify-content: flex-end; gap: 10px; margin: 8px 0 24px; }
    .stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
    .stats div { display: flex; flex-direction: column; gap: 4px; background: var(--color-bg); padding: 12px; border-radius: 8px; }
    .error { color: #b91c1c; }
    @media (max-width: 720px) {
      .form-grid, .stats { grid-template-columns: 1fr; }
      .span-2 { grid-column: span 1; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StudentProfileComponent implements OnInit {
  profile: StudentProfile | null = null;
  classes: ClassOption[] = [];
  loading = true;
  editing = false;
  saving = false;
  error = '';
  saveError = '';
  successMessage = '';
  private readonly phonePattern = Validators.pattern(/^(?:[+0-9][0-9\s-]{6,29})?$/);

  readonly form = new FormGroup({
    admissionNo: new FormControl({ value: '', disabled: true }),
    firstName: new FormControl('', Validators.required),
    lastName: new FormControl(''),
    dateOfBirth: new FormControl(''),
    gender: new FormControl(''),
    bloodGroup: new FormControl(''),
    religion: new FormControl(''),
    nationality: new FormControl(''),
    phone: new FormControl('', this.phonePattern),
    emergencyContact: new FormControl('', this.phonePattern),
    permanentAddress: new FormControl(''),
    presentAddress: new FormControl(''),
    previousSchool: new FormControl(''),
    fatherName: new FormControl(''),
    motherName: new FormControl(''),
    fatherPhone: new FormControl('', this.phonePattern),
    motherPhone: new FormControl('', this.phonePattern),
    admissionDate: new FormControl(''),
    classId: new FormControl(''),
    sectionId: new FormControl(''),
    rollNumber: new FormControl<number | null>(null),
  });

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  get sectionsForClass(): SectionOption[] {
    const classId = this.form.getRawValue().classId;
    const klass = this.classes.find((c) => c.id === classId);
    return klass?.sections ?? [];
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/students']);
      return;
    }
    this.loadMeta();
    this.load(id);
  }

  startEdit(): void {
    if (!this.profile?.canEdit) {
      return;
    }
    this.editing = true;
    this.saveError = '';
    this.successMessage = '';
    this.applyFieldLocks();
    this.cdr.markForCheck();
  }

  cancelEdit(): void {
    if (this.saving || !this.profile) {
      return;
    }
    this.editing = false;
    this.saveError = '';
    this.patchForm(this.profile);
    this.applyFieldLocks();
    this.cdr.markForCheck();
  }

  onClassChange(): void {
    this.form.patchValue({ sectionId: '' });
  }

  onSave(): void {
    if (!this.profile) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.cdr.markForCheck();
      return;
    }
    this.saving = true;
    this.saveError = '';
    this.cdr.markForCheck();
    const value = this.form.getRawValue();
    const body = {
      firstName: value.firstName,
      lastName: value.lastName || null,
      dateOfBirth: value.dateOfBirth || null,
      gender: value.gender || null,
      bloodGroup: value.bloodGroup || null,
      religion: value.religion || null,
      nationality: value.nationality || null,
      phone: value.phone || null,
      emergencyContact: value.emergencyContact || null,
      permanentAddress: value.permanentAddress || null,
      presentAddress: value.presentAddress || null,
      previousSchool: value.previousSchool || null,
      fatherName: value.fatherName || null,
      motherName: value.motherName || null,
      fatherPhone: value.fatherPhone || null,
      motherPhone: value.motherPhone || null,
      admissionDate: value.admissionDate || null,
      classId: value.classId || null,
      sectionId: value.sectionId || null,
      rollNumber: value.rollNumber === null || value.rollNumber === undefined
        ? null
        : Number(value.rollNumber),
    };
    this.http.put<ApiResponse<StudentProfile>>(`/api/students/${this.profile.id}/profile`, body).subscribe({
      next: (res) => {
        this.profile = res.data;
        this.editing = false;
        this.saving = false;
        this.successMessage = 'students.saveSuccess';
        this.patchForm(res.data);
        this.applyFieldLocks();
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        const api = err.error as ApiError | undefined;
        this.saveError = api?.message || 'students.saveFailed';
        this.cdr.markForCheck();
      },
    });
  }

  private load(id: string): void {
    this.loading = true;
    this.error = '';
    this.http.get<ApiResponse<StudentProfile>>(`/api/students/${id}/profile`).subscribe({
      next: (res) => {
        this.profile = res.data;
        this.loading = false;
        this.patchForm(res.data);
        this.applyFieldLocks();
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.error = 'students.loadFailed';
        this.cdr.markForCheck();
      },
    });
  }

  private loadMeta(): void {
    this.http.get<ApiResponse<ClassOption[]>>('/api/classes').subscribe({
      next: (res) => {
        this.classes = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  private patchForm(profile: StudentProfile): void {
    this.form.reset({
      admissionNo: profile.admissionNo ?? '',
      firstName: profile.firstName ?? '',
      lastName: profile.lastName ?? '',
      dateOfBirth: profile.dateOfBirth ?? '',
      gender: profile.gender ?? '',
      bloodGroup: profile.bloodGroup ?? '',
      religion: profile.religion ?? '',
      nationality: profile.nationality ?? '',
      phone: profile.phone ?? '',
      emergencyContact: profile.emergencyContact ?? '',
      permanentAddress: profile.permanentAddress ?? '',
      presentAddress: profile.presentAddress ?? '',
      previousSchool: profile.previousSchool ?? '',
      fatherName: profile.fatherName ?? '',
      motherName: profile.motherName ?? '',
      fatherPhone: profile.fatherPhone ?? '',
      motherPhone: profile.motherPhone ?? '',
      admissionDate: profile.admissionDate ?? '',
      classId: profile.classId ?? '',
      sectionId: profile.sectionId ?? '',
      rollNumber: profile.rollNumber,
    });
  }

  private applyFieldLocks(): void {
    const editable = new Set(this.editing ? this.profile?.editableFields ?? [] : []);
    Object.keys(this.form.controls).forEach((name) => {
      const control = this.form.get(name);
      if (!control) {
        return;
      }
      if (name === 'admissionNo' || !editable.has(name)) {
        control.disable({ emitEvent: false });
      } else {
        control.enable({ emitEvent: false });
      }
    });
  }
}
