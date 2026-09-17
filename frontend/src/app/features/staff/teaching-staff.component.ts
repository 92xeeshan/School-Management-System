import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { EMPLOYMENT_TYPES, GENDERS, SectionRef, StaffMember } from './staff.model';
import { StaffService } from './staff.service';
import { createStaffForm, staffFormToPayload, staffFormValue } from './staff-form.util';

@Component({
  selector: 'app-teaching-staff',
  imports: [TranslateModule, ReactiveFormsModule],
  template: `
    @if (pageError) {
      <p class="banner error">{{ pageError }}</p>
    }

    <div class="card">
      <div class="table-toolbar">
        <input class="search" type="search" [placeholder]="'common.search' | translate"
               (input)="filter = $any($event.target).value" />
        @if (canCreate) {
          <button class="btn btn-primary" type="button" (click)="openAddModal()">
            {{ 'staff.addTeacher' | translate }}
          </button>
        }
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ 'staff.employeeNo' | translate }}</th>
              <th>{{ 'staff.name' | translate }}</th>
              <th>{{ 'staff.designation' | translate }}</th>
              <th>{{ 'staff.department' | translate }}</th>
              <th>{{ 'staff.employmentType' | translate }}</th>
              <th>{{ 'staff.classTeacher' | translate }}</th>
              <th>{{ 'common.status' | translate }}</th>
              @if (canUpdate || canDelete) {
                <th>{{ 'common.actions' | translate }}</th>
              }
            </tr>
          </thead>
          <tbody>
            @for (member of filteredMembers; track member.id) {
              <tr>
                <td class="strong">{{ member.employeeNo }}</td>
                <td>
                  <div class="strong">{{ member.displayName }}</div>
                  @if (member.qualification) {
                    <div class="sub">{{ member.qualification }}</div>
                  }
                </td>
                <td>{{ member.designation || '—' }}</td>
                <td>{{ member.department || '—' }}</td>
                <td>
                  @if (member.employmentType) {
                    <span class="badge badge-info">{{ employmentLabel(member.employmentType) | translate }}</span>
                  } @else {
                    —
                  }
                </td>
                <td>
                  @if (classTeacherLabel(member)) {
                    <span class="badge badge-info">{{ classTeacherLabel(member) }}</span>
                  } @else {
                    <span class="muted">—</span>
                  }
                </td>
                <td>
                  <span class="badge"
                        [class.badge-success]="member.status === 'ACTIVE'"
                        [class.badge-muted]="member.status !== 'ACTIVE'">
                    {{ member.status }}
                  </span>
                </td>
                @if (canUpdate || canDelete) {
                  <td>
                    <div class="row-actions">
                      @if (canUpdate) {
                        <button class="btn btn-sm" type="button" (click)="openEditModal(member)">
                          {{ 'common.edit' | translate }}
                        </button>
                        <button class="btn btn-sm" type="button" (click)="openAssignModal(member)">
                          {{ 'staff.assignClassTeacher' | translate }}
                        </button>
                      }
                      @if (canDelete && member.status === 'ACTIVE') {
                        <button class="btn btn-sm btn-danger" type="button"
                                [disabled]="deletingId === member.id" (click)="askDelete(member)">
                          {{ deletingId === member.id ? ('common.loading' | translate) : ('common.delete' | translate) }}
                        </button>
                      }
                    </div>
                  </td>
                }
              </tr>
            } @empty {
              <tr>
                <td [attr.colspan]="canUpdate || canDelete ? 8 : 7" class="center">
                  {{ 'common.noData' | translate }}
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>

    @if (showModal) {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editingId ? ('staff.editTeacher' | translate) : ('staff.addTeacher' | translate) }}</h2>
          @if (formError) {
            <p class="banner error">{{ formError }}</p>
          }
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="form-grid">
              <div class="field">
                <label>{{ 'staff.employeeNo' | translate }} *</label>
                <input type="text" formControlName="employeeNo" placeholder="EMP010" />
              </div>
              <div class="field">
                <label>{{ 'staff.firstName' | translate }} *</label>
                <input type="text" formControlName="firstName" />
              </div>
              <div class="field">
                <label>{{ 'staff.lastName' | translate }}</label>
                <input type="text" formControlName="lastName" />
              </div>
              <div class="field">
                <label>{{ 'staff.email' | translate }}</label>
                <input type="email" formControlName="email" />
              </div>
              <div class="field">
                <label>{{ 'staff.phone' | translate }}</label>
                <input type="text" formControlName="phone" />
              </div>
              <div class="field">
                <label>{{ 'staff.gender' | translate }}</label>
                <select formControlName="gender">
                  <option value="">—</option>
                  @for (gender of genders; track gender) {
                    <option [value]="gender">{{ gender }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'staff.dateOfBirth' | translate }}</label>
                <input type="date" formControlName="dateOfBirth" />
              </div>
              <div class="field">
                <label>{{ 'staff.designation' | translate }}</label>
                <input type="text" formControlName="designation" placeholder="Senior Teacher" />
              </div>
              <div class="field">
                <label>{{ 'staff.department' | translate }}</label>
                <input type="text" formControlName="department" placeholder="Mathematics" />
              </div>
              <div class="field">
                <label>{{ 'staff.qualification' | translate }}</label>
                <input type="text" formControlName="qualification" placeholder="M.Sc, B.Ed" />
              </div>
              <div class="field">
                <label>{{ 'staff.employmentType' | translate }}</label>
                <select formControlName="employmentType">
                  <option value="">—</option>
                  @for (type of employmentTypes; track type) {
                    <option [value]="type">{{ employmentLabel(type) | translate }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'staff.joinDate' | translate }}</label>
                <input type="date" formControlName="joinDate" />
              </div>
              <div class="field span-2">
                <label>{{ 'staff.address' | translate }}</label>
                <input type="text" formControlName="address" />
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

    @if (assignmentTarget) {
      <div class="modal-backdrop" (click)="closeAssignModal()">
        <div class="modal modal-sm" (click)="$event.stopPropagation()">
          <h2>{{ 'staff.assignClassTeacher' | translate }}</h2>
          <p class="confirm-text">
            {{ 'staff.assignClassTeacherFor' | translate:{ name: assignmentTarget.displayName } }}
          </p>
          @if (assignError) {
            <p class="banner error">{{ assignError }}</p>
          }
          <div class="field">
            <label>{{ 'staff.section' | translate }}</label>
            <select [value]="assignmentSectionId" (change)="onSectionSelect($event)">
              <option value="">{{ 'staff.noClassTeacher' | translate }}</option>
              @for (section of sections; track section.id) {
                <option [value]="section.id">{{ sectionLabel(section) }}</option>
              }
            </select>
          </div>
          <div class="form-actions">
            <button class="btn" type="button" [disabled]="assigning" (click)="closeAssignModal()">
              {{ 'common.cancel' | translate }}
            </button>
            <button class="btn btn-primary" type="button" [disabled]="assigning" (click)="saveAssignment()">
              {{ assigning ? ('common.loading' | translate) : ('common.save' | translate) }}
            </button>
          </div>
        </div>
      </div>
    }

    @if (pendingDelete) {
      <div class="modal-backdrop" (click)="cancelDelete()">
        <div class="modal modal-sm" (click)="$event.stopPropagation()">
          <h2>{{ 'common.delete' | translate }}</h2>
          <p class="confirm-text">{{ 'staff.deleteConfirm' | translate:{ name: pendingDelete.displayName } }}</p>
          <div class="form-actions">
            <button class="btn" type="button" [disabled]="!!deletingId" (click)="cancelDelete()">
              {{ 'common.cancel' | translate }}
            </button>
            <button class="btn btn-danger" type="button" [disabled]="!!deletingId" (click)="confirmDelete()">
              {{ deletingId ? ('common.loading' | translate) : ('common.delete' | translate) }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    .banner { padding: 10px 14px; border-radius: 8px; margin-bottom: 14px; font-size: .9rem; }
    .banner.error { background: #fef2f2; border: 1px solid #fecaca; color: #b91c1c; }
    .table-toolbar { display: flex; justify-content: space-between; gap: 12px; align-items: center;
                     padding: 14px 16px; border-bottom: 1px solid var(--color-border); }
    .search { padding: 8px 12px; border: 1px solid var(--color-border); border-radius: 8px;
              width: 260px; font: inherit; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 12px 16px; border-bottom: 1px solid var(--color-border); font-size: .92rem; }
    th { color: var(--color-muted); font-weight: 600; font-size: .8rem; text-transform: uppercase;
         letter-spacing: .03em; background: var(--color-bg); }
    tbody tr:hover { background: var(--color-primary-soft); }
    .strong { font-weight: 600; }
    .sub { color: var(--color-muted); font-size: .8rem; }
    .muted { color: var(--color-muted); }
    .badge-info { background: var(--color-primary-soft); color: var(--color-primary); }
    .center { text-align: center; color: var(--color-muted); padding: 28px; }

    .modal-backdrop { position: fixed; inset: 0; z-index: 100; background: rgba(15, 23, 42, .5);
                      display: flex; align-items: flex-start; justify-content: center;
                      padding: 40px 16px; overflow-y: auto; }
    .modal { background: var(--color-surface); border-radius: var(--radius); padding: 24px;
             width: 620px; max-width: 100%; box-shadow: 0 20px 50px rgba(0,0,0,.25); }
    .modal h2 { margin: 0 0 18px; font-size: 1.2rem; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    input, select { padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px;
                    font: inherit; background: var(--color-surface); color: var(--color-text); }
    .span-2 { grid-column: span 2; }
    .form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
    .row-actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .btn-sm { padding: 5px 10px; font-size: .82rem; }
    .btn-danger { background: #b91c1c; border-color: #b91c1c; color: #fff; }
    .btn-danger:hover { background: #991b1b; color: #fff; border-color: #991b1b; }
    .modal-sm { width: 440px; }
    .confirm-text { margin: 0 0 14px; color: var(--color-muted); line-height: 1.45; }
    @media (max-width: 640px) { .form-grid { grid-template-columns: 1fr; } .span-2 { grid-column: span 1; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TeachingStaffComponent implements OnInit {
  members: StaffMember[] = [];
  sections: SectionRef[] = [];
  filter = '';
  pageError: string | null = null;

  showModal = false;
  saving = false;
  editingId: string | null = null;
  formError: string | null = null;
  readonly form: FormGroup = createStaffForm();

  assignmentTarget: StaffMember | null = null;
  assignmentSectionId = '';
  assigning = false;
  assignError: string | null = null;

  pendingDelete: StaffMember | null = null;
  deletingId: string | null = null;

  readonly genders = GENDERS;
  readonly employmentTypes = EMPLOYMENT_TYPES;

  constructor(
    private staffService: StaffService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  get canCreate(): boolean {
    return this.auth.hasPermission('STAFF_CREATE');
  }

  get canUpdate(): boolean {
    return this.auth.hasPermission('STAFF_UPDATE');
  }

  get canDelete(): boolean {
    return this.auth.hasPermission('STAFF_DELETE');
  }

  get filteredMembers(): StaffMember[] {
    const query = this.filter.trim().toLowerCase();
    if (!query) {
      return this.members;
    }
    return this.members.filter((member) =>
      [member.displayName, member.employeeNo, member.designation, member.department]
        .some((value) => (value ?? '').toLowerCase().includes(query))
    );
  }

  employmentLabel(type: string): string {
    return `staff.employment.${type}`;
  }

  classTeacherLabel(member: StaffMember): string {
    const assignment = member.classTeacherOf?.[0];
    return assignment ? assignment.label : '';
  }

  sectionLabel(section: SectionRef): string {
    const suffix =
      section.classTeacherId && section.classTeacherId !== this.assignmentTarget?.id && section.classTeacherName
        ? ` (${section.classTeacherName})`
        : '';
    return section.label + suffix;
  }

  openAddModal(): void {
    this.editingId = null;
    this.formError = null;
    this.form.reset(staffFormValue(this.emptyMember()));
    this.showModal = true;
    this.cdr.markForCheck();
  }

  openEditModal(member: StaffMember): void {
    this.editingId = member.id;
    this.formError = null;
    this.form.reset(staffFormValue(member));
    this.showModal = true;
    this.cdr.markForCheck();
  }

  closeModal(): void {
    this.showModal = false;
    this.formError = null;
    this.cdr.markForCheck();
  }

  onSubmit(): void {
    if (this.form.invalid || this.saving) {
      return;
    }
    this.saving = true;
    this.formError = null;
    const payload = staffFormToPayload(this.form);
    const request$ = this.editingId
      ? this.staffService.updateTeaching(this.editingId, payload)
      : this.staffService.createTeaching(payload);
    request$.pipe(finalize(() => {
      this.saving = false;
      this.cdr.markForCheck();
    })).subscribe({
      next: () => {
        this.showModal = false;
        this.load();
      },
      error: () => {
        this.formError = 'Could not save staff member. Please check the details and try again.';
      },
    });
  }

  openAssignModal(member: StaffMember): void {
    this.assignmentTarget = member;
    this.assignmentSectionId = member.classTeacherOf?.[0]?.id ?? '';
    this.assignError = null;
    this.loadSections();
    this.cdr.markForCheck();
  }

  closeAssignModal(): void {
    this.assignmentTarget = null;
    this.assignError = null;
    this.cdr.markForCheck();
  }

  onSectionSelect(event: Event): void {
    this.assignmentSectionId = (event.target as HTMLSelectElement).value;
  }

  saveAssignment(): void {
    const target = this.assignmentTarget;
    if (!target || this.assigning) {
      return;
    }
    this.assigning = true;
    this.assignError = null;
    this.staffService
      .assignClassTeacher(target.id, this.assignmentSectionId || null)
      .pipe(finalize(() => {
        this.assigning = false;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: () => {
          this.assignmentTarget = null;
          this.load();
        },
        error: () => {
          this.assignError = 'Could not update the class teacher assignment.';
        },
      });
  }

  askDelete(member: StaffMember): void {
    this.pendingDelete = member;
    this.cdr.markForCheck();
  }

  cancelDelete(): void {
    this.pendingDelete = null;
    this.cdr.markForCheck();
  }

  confirmDelete(): void {
    const member = this.pendingDelete;
    if (!member) {
      return;
    }
    this.deletingId = member.id;
    this.cdr.markForCheck();
    this.staffService
      .deactivateTeaching(member.id)
      .pipe(finalize(() => {
        this.deletingId = null;
        this.cdr.markForCheck();
      }))
      .subscribe({
        next: () => {
          this.pendingDelete = null;
          this.load();
        },
        error: () => {
          this.pendingDelete = null;
          this.pageError = 'Could not deactivate staff member. Please try again.';
        },
      });
  }

  private load(): void {
    this.staffService
      .listTeaching()
      .pipe(finalize(() => this.cdr.markForCheck()))
      .subscribe({
        next: (members) => {
          this.members = members;
          this.pageError = null;
        },
        error: () => {
          this.members = [];
          this.pageError = 'Could not load teaching staff.';
        },
      });
  }

  private loadSections(): void {
    if (this.sections.length > 0) {
      return;
    }
    this.staffService
      .listSections()
      .pipe(finalize(() => this.cdr.markForCheck()))
      .subscribe({
        next: (sections) => {
          this.sections = sections;
        },
        error: () => {
          this.sections = [];
          this.assignError = 'Could not load sections.';
        },
      });
  }

  private emptyMember(): StaffMember {
    return {
      id: '',
      staffType: 'TEACHING',
      userId: null,
      employeeNo: '',
      firstName: '',
      lastName: null,
      displayName: '',
      email: null,
      phone: null,
      gender: null,
      dateOfBirth: null,
      designation: null,
      department: null,
      qualification: null,
      employmentType: null,
      joinDate: null,
      address: null,
      status: 'ACTIVE',
      subjectIds: null,
      classTeacherOf: null,
    };
  }
}
