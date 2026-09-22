import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, ViewEncapsulation } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { ApiError, ApiResponse, PagedResponse } from '../../core/models/api.model';
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

interface StudentReportSubject {
  subjectId: string;
  subjectName: string;
  theory: number | null;
  practical: number | null;
  assignment: number | null;
  total: number | null;
  maxTotal: number | null;
  grade: string | null;
  remarks: string | null;
}

interface StudentReportCard {
  studentId: string;
  studentName: string;
  admissionNo: string;
  rollNumber: number | null;
  dateOfBirth: string | null;
  guardianName: string | null;
  photoUrl: string | null;
  classId: string;
  className: string;
  sectionId: string;
  sectionName: string;
  academicYearId: string;
  academicYearName: string;
  examTerm: string;
  published: boolean;
  schoolName: string;
  schoolAddress: string;
  schoolPhone: string;
  schoolEmail: string;
  affiliation: string;
  classTeacherName: string;
  totalObtained: number;
  totalMax: number;
  percentage: number;
  overallGrade: string | null;
  result: string;
  attendance: {
    workingDays: number;
    daysPresent: number;
    daysAbsent: number;
    daysLate: number;
    daysLeave: number;
    percent: number;
  } | null;
  subjects: StudentReportSubject[];
  behaviour: {
    conduct: string;
    discipline: string;
    punctuality: string;
    coCurricular: string | null;
  } | null;
  teacherComment: string;
  principalComment: string;
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
                @if (showActions) {
                  <th>{{ 'common.actions' | translate }}</th>
                }
              </tr>
            </thead>
            <tbody>
              @for (student of filteredStudents; track student.id) {
                <tr class="clickable" (click)="openProfile(student)">
                  <td>{{ student.admissionNo }}</td>
                  <td class="strong">{{ student.firstName }} {{ student.lastName }}</td>
                  <td>{{ student.className }}</td>
                  <td>{{ student.section }}</td>
                  <td>{{ student.rollNumber }}</td>
                  <td>{{ student.gender }}</td>
                  <td><span class="badge" [class.badge-success]="student.status === 'ACTIVE'" [class.badge-muted]="student.status !== 'ACTIVE'">{{ student.status }}</span></td>
                  @if (showActions) {
                  <td>
                    <div class="row-actions">
                      @if (canViewReportCard) {
                        <button class="icon-btn" type="button"
                                [attr.title]="'students.viewReportCard' | translate"
                                [attr.aria-label]="'students.viewReportCard' | translate"
                                (click)="openReportCard(student); $event.stopPropagation()">
                          <svg viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M7 3h8l5 5v13a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zm8 1.5V9h4.5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
                            <path d="M9 13h6M9 17h6M9 9h4" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                          </svg>
                        </button>
                      }
                      @if (canUpdate) {
                        <button class="icon-btn" type="button"
                                [attr.title]="'common.edit' | translate"
                                [attr.aria-label]="'common.edit' | translate"
                                (click)="openEditModal(student); $event.stopPropagation()">
                          <svg viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M4 20h4.5L19 9.5 14.5 5 4 15.5V20z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
                            <path d="M13.2 6.3l4.5 4.5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                          </svg>
                        </button>
                      }
                      @if (canDelete && student.status === 'ACTIVE') {
                        <button class="icon-btn icon-btn-danger" type="button"
                                [disabled]="deletingId === student.id"
                                [attr.title]="'common.delete' | translate"
                                [attr.aria-label]="'common.delete' | translate"
                                (click)="askDelete(student); $event.stopPropagation()">
                          <svg viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M5 7h14M10 7V5h4v2M8 7l1 12h6l1-12" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                          </svg>
                        </button>
                      }
                    </div>
                  </td>
                  }
                </tr>
              } @empty {
                <tr><td [attr.colspan]="showActions ? 8 : 7" class="center">{{ 'common.noData' | translate }}</td></tr>
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

    @if (showReportCard) {
      <div class="modal-backdrop report-backdrop" (click)="closeReportCard()">
        <div class="modal report-modal" (click)="$event.stopPropagation()">
          <div class="report-toolbar">
            <h2>{{ 'examinations.reportCardsPage.printPreview' | translate }}</h2>
            <span class="spacer"></span>
            <button class="btn" type="button" [disabled]="!reportCard?.published" (click)="printReportCard()">
              {{ 'examinations.reportCardsPage.print' | translate }}
            </button>
            <button class="btn btn-primary" type="button" [disabled]="!reportCard?.published || exportingReport" (click)="downloadReportCard()">
              {{ 'examinations.reportCardsPage.downloadPdf' | translate }}
            </button>
            <button class="btn" type="button" (click)="closeReportCard()">{{ 'examinations.reportCardsPage.close' | translate }}</button>
          </div>
          @if (reportCardError) {
            <p class="banner error">{{ reportCardError }}</p>
          }
          @if (loadingReportCard) {
            <p class="center muted">{{ 'common.loading' | translate }}</p>
          }
          @if (reportCard && !reportCard.published) {
            <p class="banner warn">{{ 'examinations.reportCardsPage.notPublished' | translate }}</p>
          }
          @if (reportCard) {
            <div class="print-area">
              <article class="report-card">
                <header class="card-head">
                  <div class="school">
                    <h3>{{ reportCard.schoolName }}</h3>
                    <p>{{ reportCard.affiliation }}</p>
                    <p>{{ reportCard.schoolAddress }}</p>
                    <p>{{ reportCard.schoolPhone }} {{ reportCard.schoolEmail }}</p>
                    <strong>{{ 'examinations.reportCardsPage.heading' | translate }} · {{ reportCard.academicYearName }} · {{ reportCard.examTerm }}</strong>
                  </div>
                  <div class="photo">
                    @if (reportCard.photoUrl) {
                      <img [src]="reportCard.photoUrl" alt="" />
                    } @else {
                      <span>{{ initial(reportCard.studentName) }}</span>
                    }
                  </div>
                </header>
                <dl class="meta">
                  <div><dt>{{ 'examinations.reportCardsPage.student' | translate }}</dt><dd>{{ reportCard.studentName }}</dd></div>
                  <div><dt>{{ 'examinations.reportCardsPage.admissionNo' | translate }}</dt><dd>{{ reportCard.admissionNo }}</dd></div>
                  <div><dt>{{ 'examinations.reportCardsPage.roll' | translate }}</dt><dd>{{ reportCard.rollNumber ?? '—' }}</dd></div>
                  <div><dt>{{ 'examinations.reportCardsPage.class' | translate }}</dt><dd>{{ reportCard.className }} {{ reportCard.sectionName }}</dd></div>
                  <div><dt>{{ 'examinations.reportCardsPage.dob' | translate }}</dt><dd>{{ reportCard.dateOfBirth || '—' }}</dd></div>
                  <div><dt>{{ 'examinations.reportCardsPage.guardian' | translate }}</dt><dd>{{ reportCard.guardianName || '—' }}</dd></div>
                  <div><dt>{{ 'examinations.reportCardsPage.classTeacher' | translate }}</dt><dd>{{ reportCard.classTeacherName || '—' }}</dd></div>
                  <div><dt>{{ 'examinations.reportCardsPage.result' | translate }}</dt><dd>{{ reportCard.result }} / {{ reportCard.overallGrade || '—' }}</dd></div>
                </dl>
                <h4>{{ 'examinations.reportCardsPage.attendance' | translate }}</h4>
                <table class="sheet">
                  <thead>
                    <tr>
                      <th>{{ 'examinations.reportCardsPage.workingDays' | translate }}</th>
                      <th>{{ 'examinations.reportCardsPage.present' | translate }}</th>
                      <th>{{ 'examinations.reportCardsPage.absent' | translate }}</th>
                      <th>{{ 'examinations.reportCardsPage.late' | translate }}</th>
                      <th>{{ 'examinations.reportCardsPage.leave' | translate }}</th>
                      <th>%</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>{{ reportCard.attendance?.workingDays ?? 0 }}</td>
                      <td>{{ reportCard.attendance?.daysPresent ?? 0 }}</td>
                      <td>{{ reportCard.attendance?.daysAbsent ?? 0 }}</td>
                      <td>{{ reportCard.attendance?.daysLate ?? 0 }}</td>
                      <td>{{ reportCard.attendance?.daysLeave ?? 0 }}</td>
                      <td>{{ reportCard.attendance?.percent ?? 0 }}</td>
                    </tr>
                  </tbody>
                </table>
                <h4>{{ 'examinations.reportCardsPage.subjects' | translate }}</h4>
                <table class="sheet">
                  <thead>
                    <tr>
                      <th>{{ 'examinations.reportCardsPage.subject' | translate }}</th>
                      <th>{{ 'examinations.reportCardsPage.formative' | translate }}</th>
                      <th>{{ 'examinations.reportCardsPage.theory' | translate }}</th>
                      <th>{{ 'examinations.reportCardsPage.practical' | translate }}</th>
                      <th>{{ 'examinations.reportCardsPage.total' | translate }}</th>
                      <th>{{ 'examinations.reportCardsPage.max' | translate }}</th>
                      <th>{{ 'examinations.reportCardsPage.grade' | translate }}</th>
                      <th>{{ 'examinations.reportCardsPage.remarks' | translate }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (row of reportCard.subjects; track row.subjectId) {
                      <tr>
                        <td>{{ row.subjectName }}</td>
                        <td>{{ row.assignment ?? '—' }}</td>
                        <td>{{ row.theory ?? '—' }}</td>
                        <td>{{ row.practical ?? '—' }}</td>
                        <td>{{ row.total ?? '—' }}</td>
                        <td>{{ row.maxTotal ?? '—' }}</td>
                        <td>{{ row.grade || '—' }}</td>
                        <td>{{ row.remarks || '—' }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
                <dl class="meta">
                  <div><dt>{{ 'examinations.reportCardsPage.grandTotal' | translate }}</dt><dd>{{ reportCard.totalObtained }} / {{ reportCard.totalMax }}</dd></div>
                  <div><dt>{{ 'examinations.reportCardsPage.percentage' | translate }}</dt><dd>{{ reportCard.percentage }}%</dd></div>
                </dl>
                <h4>{{ 'examinations.reportCardsPage.behaviour' | translate }}</h4>
                <table class="sheet">
                  <thead>
                    <tr>
                      <th>{{ 'examinations.reportCardsPage.conduct' | translate }}</th>
                      <th>{{ 'examinations.reportCardsPage.discipline' | translate }}</th>
                      <th>{{ 'examinations.reportCardsPage.punctuality' | translate }}</th>
                      <th>{{ 'examinations.reportCardsPage.coCurricular' | translate }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>{{ label(reportCard.behaviour?.conduct) }}</td>
                      <td>{{ label(reportCard.behaviour?.discipline) }}</td>
                      <td>{{ label(reportCard.behaviour?.punctuality) }}</td>
                      <td>{{ reportCard.behaviour?.coCurricular || '—' }}</td>
                    </tr>
                  </tbody>
                </table>
                <div class="comment">
                  <strong>{{ 'examinations.reportCardsPage.teacherRemarks' | translate }}</strong>
                  <p>{{ reportCard.teacherComment }}</p>
                </div>
                <div class="comment">
                  <strong>{{ 'examinations.reportCardsPage.principalRemarks' | translate }}</strong>
                  <p>{{ reportCard.principalComment }}</p>
                </div>
                <footer class="signs">
                  <span>{{ 'examinations.reportCardsPage.classTeacher' | translate }}</span>
                  <span>{{ 'examinations.reportCardsPage.parentSign' | translate }}</span>
                  <span>{{ 'examinations.reportCardsPage.principalSign' | translate }}</span>
                </footer>
              </article>
            </div>
          }
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
    tbody tr.clickable { cursor: pointer; }
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
    .row-actions { display: flex; gap: 4px; flex-wrap: nowrap; align-items: center; }
    .icon-btn {
      display: inline-flex; align-items: center; justify-content: center;
      width: 32px; height: 32px; padding: 0; border-radius: 8px;
      border: 1px solid var(--color-border); background: #fff; color: #334155;
      cursor: pointer;
    }
    .icon-btn svg { width: 16px; height: 16px; display: block; }
    .icon-btn:hover { background: var(--color-primary-soft); color: var(--color-primary); border-color: var(--color-primary); }
    .icon-btn-danger { color: #b91c1c; }
    .icon-btn-danger:hover { background: #fef2f2; color: #991b1b; border-color: #fecaca; }
    .icon-btn:disabled { opacity: .5; cursor: not-allowed; }
    .btn-danger { background: #b91c1c; border-color: #b91c1c; color: #fff; }
    .btn-danger:hover { background: #991b1b; color: #fff; border-color: #991b1b; }
    .modal-sm { width: 420px; }
    .confirm-text { margin: 0 0 8px; color: var(--color-muted); line-height: 1.45; }
    .report-backdrop { align-items: stretch; }
    .report-modal { width: 900px; max-width: 100%; }
    .report-toolbar { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; margin-bottom: 16px; }
    .report-toolbar h2 { margin: 0; }
    .spacer { flex: 1; }
    .banner { padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
    .banner.error { background: #fef2f2; border: 1px solid #fecaca; color: #b91c1c; }
    .banner.warn { background: #fff7ed; border: 1px solid #fed7aa; color: #9a3412; }
    .report-card { border: 1px solid #cbd5e1; padding: 18px; background: #fff; }
    .card-head { display: flex; justify-content: space-between; gap: 16px; border-bottom: 1px solid #e2e8f0; padding-bottom: 12px; }
    .school h3 { margin: 0 0 4px; }
    .school p { margin: 0; color: #64748b; font-size: .85rem; }
    .photo { width: 88px; height: 110px; border: 1px solid #cbd5e1; display: flex; align-items: center; justify-content: center; font-weight: 700; overflow: hidden; }
    .photo img { width: 100%; height: 100%; object-fit: cover; }
    .meta { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 16px; margin: 12px 0; }
    dt { font-size: .75rem; color: #64748b; }
    dd { margin: 0; font-weight: 600; }
    .sheet { width: 100%; border-collapse: collapse; }
    .sheet th, .sheet td { font-size: .82rem; padding: 8px 10px; }
    .comment { border: 1px solid #e2e8f0; padding: 8px 10px; margin-top: 10px; }
    .comment p { margin: 6px 0 0; font-size: .88rem; white-space: pre-wrap; }
    .signs { display: flex; justify-content: space-between; margin-top: 24px; font-size: .82rem; gap: 12px; }
    @media print {
      body * { visibility: hidden; }
      .print-area, .print-area * { visibility: visible; }
      .print-area { position: absolute; inset: 0; width: 100%; background: #fff; padding: 0; margin: 0; }
      .report-card { box-shadow: none; margin: 0; }
    }
    @media (max-width: 640px) { .form-grid { grid-template-columns: 1fr; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
})
export class StudentsComponent implements OnInit {
  students: Student[] = [];
  filter = '';
  showModal = false;
  saving = false;
  editingId: string | null = null;
  pendingDelete: Student | null = null;
  deletingId: string | null = null;
  showReportCard = false;
  loadingReportCard = false;
  exportingReport = false;
  reportCard: StudentReportCard | null = null;
  reportCardError = '';
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

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private auth: AuthService,
    private router: Router,
  ) {}

  get canCreate(): boolean {
    return this.auth.hasPermission('STUDENT_CREATE');
  }

  get canUpdate(): boolean {
    return this.auth.hasPermission('STUDENT_UPDATE');
  }

  get canDelete(): boolean {
    return this.auth.hasPermission('STUDENT_DELETE');
  }

  get canViewReportCard(): boolean {
    return this.auth.hasPermission('REPORT_CARD_READ');
  }

  get showActions(): boolean {
    return this.canUpdate || this.canDelete || this.canViewReportCard;
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

  openProfile(student: Student): void {
    this.router.navigate(['/students', student.id]);
  }

  openReportCard(student: Student): void {
    this.showReportCard = true;
    this.loadingReportCard = true;
    this.exportingReport = false;
    this.reportCard = null;
    this.reportCardError = '';
    this.cdr.markForCheck();
    this.http.get<ApiResponse<StudentReportCard>>(`/api/report-cards/${student.id}`, {
      params: { examTerm: 'TERM' },
    }).subscribe({
      next: (res) => {
        this.reportCard = res.data;
        this.loadingReportCard = false;
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        const body = err.error as ApiError | undefined;
        this.reportCardError = body?.message || err.message || 'Error';
        this.loadingReportCard = false;
        this.cdr.markForCheck();
      },
    });
  }

  closeReportCard(): void {
    this.showReportCard = false;
    this.reportCard = null;
    this.reportCardError = '';
    this.loadingReportCard = false;
    this.exportingReport = false;
    this.cdr.markForCheck();
  }

  printReportCard(): void {
    window.print();
  }

  downloadReportCard(): void {
    if (!this.reportCard?.published) {
      return;
    }
    this.exportingReport = true;
    this.http.post('/api/report-cards/export/pdf', {
      academicYearId: this.reportCard.academicYearId,
      examTerm: this.reportCard.examTerm || 'TERM',
      classId: this.reportCard.classId,
      sectionId: this.reportCard.sectionId,
      studentIds: [this.reportCard.studentId],
    }, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        this.exportingReport = false;
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'report-cards.pdf';
        link.click();
        URL.revokeObjectURL(url);
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.exportingReport = false;
        const body = err.error as ApiError | undefined;
        this.reportCardError = body?.message || err.message || 'Error';
        this.cdr.markForCheck();
      },
    });
  }

  initial(name: string): string {
    return (name || '?').slice(0, 1);
  }

  label(value: string | null | undefined): string {
    return (value || '—').replace(/_/g, ' ');
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
