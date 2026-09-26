import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, ViewEncapsulation } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { ApiError, ApiResponse } from '../../core/models/api.model';
import { AuthService } from '../../core/auth/auth.service';

interface YearOption { id: string; name: string; current: boolean }
interface SectionOption { id: string; name: string }
interface ClassOption { id: string; name: string; sections: SectionOption[] }
interface MarksheetOptions {
  academicYears: YearOption[];
  classes: ClassOption[];
  examTerms: string[];
  studentView: boolean;
  canManage: boolean;
  canSubmit: boolean;
  defaultStudentId: string | null;
  defaultClassId: string | null;
  defaultSectionId: string | null;
}
interface MarksheetStudent {
  studentId: string;
  studentName: string;
  admissionNo: string;
  rollNumber: number | null;
  gender: string | null;
  photoUrl: string | null;
  ready: boolean;
  status: string;
  published: boolean;
  locked: boolean;
  classRank: number | null;
  rejectionReason: string | null;
  percentage: number | null;
  gpa: number | null;
  overallGrade: string | null;
  result: string | null;
}
interface MarksheetSubject {
  subjectId: string;
  subjectCode: string | null;
  subjectName: string;
  maxMarks: number | null;
  passingMarks: number | null;
  marksObtained: number | null;
  percentage: number | null;
  grade: string | null;
  gradePoint: number | null;
  result: string | null;
}
interface Marksheet {
  studentId: string;
  studentName: string;
  admissionNo: string;
  rollNumber: number | null;
  dateOfBirth: string | null;
  gender: string | null;
  photoUrl: string | null;
  photoPlaceholder: boolean;
  classId: string;
  className: string;
  sectionId: string;
  sectionName: string;
  academicYearId: string;
  academicYearName: string;
  examTerm: string;
  ready: boolean;
  status: string;
  published: boolean;
  locked: boolean;
  classRank: number | null;
  rejectionReason: string | null;
  serialNo: string;
  issueDate: string | null;
  schoolName: string;
  schoolAddress: string;
  schoolPhone: string;
  schoolEmail: string;
  affiliation: string;
  totalObtained: number;
  totalMax: number;
  percentage: number;
  gpa: number | null;
  overallGrade: string | null;
  result: string;
  subjects: MarksheetSubject[];
}

@Component({
  selector: 'app-marksheet',
  imports: [TranslateModule, NgTemplateOutlet],
  template: `
    <div class="tab-page">
      <div class="page-header">
        <div>
          <h2>{{ 'downloads.marksheetPage.title' | translate }}</h2>
          <p class="muted">{{ 'downloads.marksheetPage.subtitle' | translate }}</p>
        </div>
      </div>

      @if (pageError) {
        <p class="banner error">{{ pageError }}</p>
      }
      @if (!allPublished && students.length > 0) {
        <p class="banner warn">{{ 'downloads.marksheetPage.unpublished' | translate }}</p>
      }

      <div class="filters">
        <select [value]="yearId" (change)="onYearChange($event)">
          @for (year of options?.academicYears ?? []; track year.id) {
            <option [value]="year.id">{{ year.name }}</option>
          }
        </select>
        <select [value]="examTerm" (change)="onTermChange($event)">
          @for (term of options?.examTerms ?? []; track term) {
            <option [value]="term">{{ termLabel(term) | translate }}</option>
          }
        </select>
        @if (!studentView) {
          <select [value]="classId" (change)="onClassChange($event)">
            <option value="">{{ 'downloads.marksheetPage.selectClass' | translate }}</option>
            @for (klass of options?.classes ?? []; track klass.id) {
              <option [value]="klass.id">{{ klass.name }}</option>
            }
          </select>
          <select [value]="sectionId" (change)="onSectionChange($event)" [disabled]="!classId">
            <option value="">{{ 'downloads.marksheetPage.selectSection' | translate }}</option>
            @for (section of filterSections; track section.id) {
              <option [value]="section.id">{{ section.name }}</option>
            }
          </select>
          <select [value]="statusFilter" (change)="onStatusChange($event)">
            @for (status of statusOptions; track status) {
              <option [value]="status">{{ statusLabel(status) | translate }}</option>
            }
          </select>
          <input class="search" type="search"
                 [value]="query"
                 (input)="onQueryChange($event)"
                 [placeholder]="'downloads.marksheetPage.search' | translate" />
        }
      </div>

      @if (!studentView && sectionId) {
        <div class="toolbar">
          <label class="check">
            <input type="checkbox" [checked]="allSelected" (change)="toggleAll($event)" />
            {{ 'downloads.marksheetPage.selectAll' | translate }}
          </label>
          <span class="muted">{{ 'downloads.marksheetPage.selected' | translate:{ count: selectedIds.size } }}</span>
          <span class="spacer"></span>
          @if (canSubmit) {
            <button class="btn" type="button" [disabled]="!canSubmitSelected || publishing" (click)="submitForApproval()">
              {{ 'downloads.marksheetPage.submit' | translate }}
            </button>
          }
          @if (canManage) {
            <button class="btn btn-primary" type="button" [disabled]="!canApproveSelected || publishing" (click)="approve(false)">
              {{ 'downloads.marksheetPage.approve' | translate }}
            </button>
            <button class="btn" type="button" [disabled]="!canApproveSelected || publishing" (click)="approve(true)">
              {{ 'downloads.marksheetPage.approveLock' | translate }}
            </button>
            <button class="btn" type="button" [disabled]="!canApproveSelected || publishing" (click)="rejectSelected()">
              {{ 'downloads.marksheetPage.reject' | translate }}
            </button>
            <button class="btn" type="button" [disabled]="selectedIds.size === 0 || publishing" (click)="publish(false, false)">
              {{ 'downloads.marksheetPage.unlock' | translate }}
            </button>
          }
          <button class="btn" type="button" [disabled]="!canPrint" (click)="openPreview()">
            {{ 'downloads.marksheetPage.printSelected' | translate }}
          </button>
          <button class="btn btn-primary" type="button" [disabled]="!canPrint || exporting" (click)="exportFile('pdf')">
            {{ 'downloads.marksheetPage.exportPdf' | translate }}
          </button>
          <button class="btn" type="button" [disabled]="!canPrint || exporting" (click)="exportFile('zip')">
            {{ 'downloads.marksheetPage.exportZip' | translate }}
          </button>
        </div>

        <div class="card">
          <table>
            <thead>
              <tr>
                <th></th>
                <th>{{ 'downloads.marksheetPage.student' | translate }}</th>
                <th>{{ 'downloads.marksheetPage.admissionNo' | translate }}</th>
                <th>{{ 'downloads.marksheetPage.roll' | translate }}</th>
                <th>{{ 'downloads.marksheetPage.percentage' | translate }}</th>
                <th>{{ 'downloads.marksheetPage.gpa' | translate }}</th>
                <th>{{ 'downloads.marksheetPage.rank' | translate }}</th>
                <th>{{ 'downloads.marksheetPage.result' | translate }}</th>
                <th>{{ 'downloads.marksheetPage.status' | translate }}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (row of students; track row.studentId) {
                <tr>
                  <td>
                    <input type="checkbox" [checked]="selectedIds.has(row.studentId)"
                           (change)="toggleStudent(row.studentId, $event)" />
                  </td>
                  <td>{{ row.studentName }}</td>
                  <td>{{ row.admissionNo }}</td>
                  <td>{{ row.rollNumber ?? '—' }}</td>
                  <td>{{ row.percentage == null ? '—' : row.percentage + '%' }}</td>
                  <td>{{ row.gpa == null ? '—' : row.gpa }}</td>
                  <td>{{ row.classRank ?? '—' }}</td>
                  <td>{{ row.result || '—' }}</td>
                  <td>
                    <span class="badge" [class.badge-success]="row.status === 'PUBLISHED'"
                          [class.badge-warn]="row.status === 'PENDING_APPROVAL'"
                          [class.badge-lock]="row.status === 'REJECTED'"
                          [class.badge-muted]="row.status === 'DRAFT'">
                      {{ statusLabel(row.status) | translate }}
                    </span>
                    @if (row.locked) {
                      <span class="badge badge-lock">{{ 'downloads.marksheetPage.locked' | translate }}</span>
                    }
                    @if (row.rejectionReason) {
                      <div class="muted">{{ row.rejectionReason }}</div>
                    }
                  </td>
                  <td>
                    <button class="btn" type="button" (click)="previewOne(row.studentId)">
                      {{ 'downloads.marksheetPage.preview' | translate }}
                    </button>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="10" class="center muted">{{ 'downloads.marksheetPage.empty' | translate }}</td>
                </tr>
              }
            </tbody>
          </table>
          @if (loading) {
            <p class="center muted">{{ 'common.loading' | translate }}</p>
          }
        </div>
      }

      @if (studentView && myCard) {
        <div class="toolbar">
          <span class="spacer"></span>
          <button class="btn" type="button" [disabled]="!myCard.published" (click)="previewMine()">
            {{ 'downloads.marksheetPage.print' | translate }}
          </button>
          <button class="btn btn-primary" type="button" [disabled]="!myCard.published || exporting" (click)="exportMine()">
            {{ 'downloads.marksheetPage.downloadPdf' | translate }}
          </button>
        </div>
        @if (!myCard.published) {
          <p class="banner warn">{{ 'downloads.marksheetPage.notPublished' | translate }}</p>
        } @else {
          <div class="preview-wrap">
            <ng-container *ngTemplateOutlet="cardTpl; context: { $implicit: myCard }"></ng-container>
          </div>
        }
      }
    </div>

    @if (showPreview) {
      <div class="modal-backdrop" (click)="closePreview()">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="toolbar">
            <h2>{{ 'downloads.marksheetPage.printPreview' | translate }}</h2>
            <span class="spacer"></span>
            <button class="btn" type="button" [disabled]="!canPrintPreview" (click)="printPreview()">
              {{ 'downloads.marksheetPage.print' | translate }}
            </button>
            <button class="btn btn-primary" type="button" [disabled]="!canPrintPreview || exporting" (click)="exportFile('pdf')">
              {{ 'downloads.marksheetPage.downloadPdf' | translate }}
            </button>
            <button class="btn" type="button" (click)="closePreview()">{{ 'downloads.marksheetPage.close' | translate }}</button>
          </div>
          <div class="print-area">
            @for (card of cards; track card.studentId) {
              <ng-container *ngTemplateOutlet="cardTpl; context: { $implicit: card }"></ng-container>
            }
          </div>
        </div>
      </div>
    }

    <ng-template #cardTpl let-card>
      <article class="marksheet">
        <header class="card-head">
          <div class="school">
            <h3>{{ card.schoolName }}</h3>
            <p>{{ card.affiliation }}</p>
            <p>{{ card.schoolAddress }}</p>
            <p>{{ card.schoolPhone }} {{ card.schoolEmail }}</p>
            <strong>{{ 'downloads.marksheetPage.heading' | translate }} · {{ card.academicYearName }} · {{ card.examTerm }}</strong>
          </div>
          <div class="photo" [class.male]="!card.photoUrl && card.gender === 'MALE'"
               [class.female]="!card.photoUrl && card.gender === 'FEMALE'"
               [class.other]="!card.photoUrl && card.gender !== 'MALE' && card.gender !== 'FEMALE'">
            @if (card.photoUrl) {
              <img [src]="card.photoUrl" alt="" />
            } @else {
              <span>{{ photoLabel(card.gender) }}</span>
            }
          </div>
        </header>
        <dl class="meta">
          <div><dt>{{ 'downloads.marksheetPage.student' | translate }}</dt><dd>{{ card.studentName }}</dd></div>
          <div><dt>{{ 'downloads.marksheetPage.enrollmentId' | translate }}</dt><dd>{{ card.admissionNo }}</dd></div>
          <div><dt>{{ 'downloads.marksheetPage.roll' | translate }}</dt><dd>{{ card.rollNumber ?? '—' }}</dd></div>
          <div><dt>{{ 'downloads.marksheetPage.class' | translate }}</dt><dd>{{ card.className }} {{ card.sectionName }}</dd></div>
          <div><dt>{{ 'downloads.marksheetPage.dob' | translate }}</dt><dd>{{ card.dateOfBirth || '—' }}</dd></div>
          <div><dt>{{ 'downloads.marksheetPage.gender' | translate }}</dt><dd>{{ genderLabel(card.gender) }}</dd></div>
          <div><dt>{{ 'downloads.marksheetPage.serialNo' | translate }}</dt><dd>{{ card.serialNo }}</dd></div>
          <div><dt>{{ 'downloads.marksheetPage.issueDate' | translate }}</dt><dd>{{ card.issueDate || '—' }}</dd></div>
        </dl>
        <h4>{{ 'downloads.marksheetPage.subjects' | translate }}</h4>
        <table class="sheet">
          <thead>
            <tr>
              <th>{{ 'downloads.marksheetPage.subjectCode' | translate }}</th>
              <th>{{ 'downloads.marksheetPage.subject' | translate }}</th>
              <th>{{ 'downloads.marksheetPage.maxMarks' | translate }}</th>
              <th>{{ 'downloads.marksheetPage.passingMarks' | translate }}</th>
              <th>{{ 'downloads.marksheetPage.marksObtained' | translate }}</th>
              <th>{{ 'downloads.marksheetPage.grade' | translate }}</th>
              <th>{{ 'downloads.marksheetPage.gradePoint' | translate }}</th>
              <th>{{ 'downloads.marksheetPage.result' | translate }}</th>
            </tr>
          </thead>
          <tbody>
            @for (row of card.subjects; track row.subjectId) {
              <tr>
                <td>{{ row.subjectCode || '—' }}</td>
                <td>{{ row.subjectName }}</td>
                <td>{{ row.maxMarks ?? '—' }}</td>
                <td>{{ row.passingMarks ?? '—' }}</td>
                <td>{{ row.marksObtained ?? '—' }}</td>
                <td>{{ row.grade || '—' }}</td>
                <td>{{ row.gradePoint ?? '—' }}</td>
                <td>{{ row.result || '—' }}</td>
              </tr>
            }
          </tbody>
        </table>
        <dl class="meta">
          <div><dt>{{ 'downloads.marksheetPage.grandTotal' | translate }}</dt><dd>{{ card.totalObtained }} / {{ card.totalMax }}</dd></div>
          <div><dt>{{ 'downloads.marksheetPage.percentage' | translate }}</dt><dd>{{ card.percentage }}%</dd></div>
          <div><dt>{{ 'downloads.marksheetPage.gpa' | translate }}</dt><dd>{{ card.gpa ?? '—' }}</dd></div>
          <div><dt>{{ 'downloads.marksheetPage.result' | translate }}</dt><dd>{{ card.result }} / {{ card.overallGrade || '—' }}</dd></div>
          <div><dt>{{ 'downloads.marksheetPage.rank' | translate }}</dt><dd>{{ card.classRank ?? '—' }}</dd></div>
        </dl>
        <div class="security">
          <div>
            <strong>{{ 'downloads.marksheetPage.security' | translate }}</strong>
            <p>{{ 'downloads.marksheetPage.serialNo' | translate }}: {{ card.serialNo }}</p>
            <p>{{ 'downloads.marksheetPage.enrollmentId' | translate }}: {{ card.admissionNo }}</p>
            <p>{{ 'downloads.marksheetPage.issueDate' | translate }}: {{ card.issueDate || '—' }}</p>
          </div>
          <div class="qr">
            <span>QR</span>
            <small>{{ 'downloads.marksheetPage.qrPlaceholder' | translate }}</small>
          </div>
        </div>
        <footer class="signs">
          <span>{{ 'downloads.marksheetPage.controller' | translate }}</span>
          <span>{{ 'downloads.marksheetPage.schoolSeal' | translate }}</span>
        </footer>
      </article>
    </ng-template>
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    h2 { font-size: 1.2rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); margin: 0; }
    .filters, .toolbar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; align-items: center; }
    select, .search { padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; background: #fff; min-width: 160px; }
    .search { min-width: 220px; }
    .spacer { flex: 1; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 10px; border-bottom: 1px solid var(--color-border); font-size: .9rem; }
    .center { text-align: center; padding: 18px; }
    .banner { padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
    .banner.error { background: #fef2f2; border: 1px solid #fecaca; color: #b91c1c; }
    .banner.warn { background: #fff7ed; border: 1px solid #fed7aa; color: #9a3412; }
    .badge { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: .7rem; font-weight: 600; margin-right: 4px; }
    .badge-success { background: #d1fae5; color: #047857; }
    .badge-muted { background: #e2e8f0; color: #475569; }
    .badge-warn { background: #fef3c7; color: #92400e; }
    .badge-lock { background: #fee2e2; color: #b91c1c; }
    .check { display: flex; align-items: center; gap: 8px; }
    .modal-backdrop { position: fixed; inset: 0; z-index: 100; background: rgba(15, 23, 42, .5); overflow: auto; padding: 24px; }
    .modal { background: #fff; border-radius: var(--radius); padding: 20px; max-width: 920px; margin: 0 auto; }
    .marksheet { border: 1px solid #cbd5e1; padding: 18px; margin-bottom: 18px; page-break-after: always; break-after: page; background: #fff; }
    .marksheet:last-child { page-break-after: auto; break-after: auto; }
    .card-head { display: flex; justify-content: space-between; gap: 16px; border-bottom: 1px solid #e2e8f0; padding-bottom: 12px; }
    .school h3 { margin: 0 0 4px; }
    .school p { margin: 0; color: #64748b; font-size: .85rem; }
    .photo { width: 88px; height: 110px; border: 1px solid #cbd5e1; display: flex; align-items: center; justify-content: center; font-weight: 700; overflow: hidden; text-align: center; font-size: .72rem; padding: 6px; }
    .photo.male { background: #bfdbfe; color: #1e3a8a; }
    .photo.female { background: #fbcfe8; color: #9d174d; }
    .photo.other { background: #e2e8f0; color: #334155; }
    .photo img { width: 100%; height: 100%; object-fit: cover; }
    .meta { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 16px; margin: 12px 0; }
    dt { font-size: .75rem; color: #64748b; }
    dd { margin: 0; font-weight: 600; }
    .sheet th, .sheet td { font-size: .82rem; }
    .security { display: grid; grid-template-columns: 1fr 110px; gap: 16px; border: 1px solid #e2e8f0; padding: 10px; margin-top: 12px; }
    .security p { margin: 4px 0 0; font-size: .85rem; }
    .qr { border: 1px dashed #94a3b8; background: #f1f5f9; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 90px; font-weight: 700; }
    .qr small { font-weight: 500; color: #64748b; text-align: center; padding: 0 6px; }
    .signs { display: flex; justify-content: space-between; margin-top: 28px; font-size: .82rem; gap: 12px; }
    .preview-wrap { background: #fff; }
    @media print {
      body * { visibility: hidden; }
      .print-area, .print-area * { visibility: visible; }
      .print-area { position: absolute; inset: 0; width: 100%; background: #fff; padding: 0; margin: 0; }
      .marksheet { box-shadow: none; margin: 0 0 12px; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
})
export class MarksheetComponent implements OnInit {
  options: MarksheetOptions | null = null;
  students: MarksheetStudent[] = [];
  cards: Marksheet[] = [];
  myCard: Marksheet | null = null;
  yearId = '';
  classId = '';
  sectionId = '';
  examTerm = 'TERM';
  query = '';
  statusFilter = 'ALL';
  studentView = false;
  canManage = false;
  canSubmit = false;
  readonly statusOptions = ['ALL', 'DRAFT', 'PENDING_APPROVAL', 'PUBLISHED', 'REJECTED'];
  loading = false;
  exporting = false;
  publishing = false;
  pageError = '';
  showPreview = false;
  selectedIds = new Set<string>();
  private queryTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(private http: HttpClient, private auth: AuthService, public cdr: ChangeDetectorRef) {}

  get filterSections(): SectionOption[] {
    return this.options?.classes.find((klass) => klass.id === this.classId)?.sections ?? [];
  }

  get allSelected(): boolean {
    return this.students.length > 0 && this.students.every((row) => this.selectedIds.has(row.studentId));
  }

  get allPublished(): boolean {
    return this.students.length > 0 && this.students.every((row) => row.published);
  }

  get canPrint(): boolean {
    return this.selectedIds.size > 0 && this.students
      .filter((row) => this.selectedIds.has(row.studentId))
      .every((row) => row.published);
  }

  get canPrintPreview(): boolean {
    return this.cards.length > 0 && this.cards.every((card) => card.published);
  }

  get selectedRows(): MarksheetStudent[] {
    return this.students.filter((row) => this.selectedIds.has(row.studentId));
  }

  get canSubmitSelected(): boolean {
    return this.selectedRows.length > 0
      && this.selectedRows.every((row) => row.ready && (row.status === 'DRAFT' || row.status === 'REJECTED'));
  }

  get canApproveSelected(): boolean {
    return this.selectedRows.length > 0
      && this.selectedRows.every((row) => row.status === 'PENDING_APPROVAL');
  }

  ngOnInit(): void {
    this.canManage = this.auth.hasPermission('MARKSHEET_MANAGE');
    this.loadOptions();
  }

  termLabel(term: string): string {
    return `academics.exam${term.charAt(0)}${term.slice(1).toLowerCase()}`;
  }

  photoLabel(gender: string | null | undefined): string {
    if (gender === 'FEMALE') {
      return 'Female photo';
    }
    if (gender === 'MALE') {
      return 'Male photo';
    }
    return 'Photo';
  }

  genderLabel(gender: string | null | undefined): string {
    return gender || '—';
  }

  statusLabel(status: string | null | undefined): string {
    switch (status) {
      case 'PENDING_APPROVAL':
        return 'downloads.marksheetPage.statusPending';
      case 'PUBLISHED':
        return 'downloads.marksheetPage.statusPublished';
      case 'REJECTED':
        return 'downloads.marksheetPage.statusRejected';
      case 'DRAFT':
        return 'downloads.marksheetPage.statusDraft';
      default:
        return 'downloads.marksheetPage.statusAll';
    }
  }

  previewMine(): void {
    if (!this.myCard) {
      return;
    }
    this.cards = [this.myCard];
    this.showPreview = true;
    this.cdr.markForCheck();
  }

  onYearChange(event: Event): void {
    this.yearId = (event.target as HTMLSelectElement).value;
    this.reload();
  }

  onTermChange(event: Event): void {
    this.examTerm = (event.target as HTMLSelectElement).value;
    this.reload();
  }

  onClassChange(event: Event): void {
    this.classId = (event.target as HTMLSelectElement).value;
    this.sectionId = '';
    this.students = [];
    this.selectedIds.clear();
    this.cdr.markForCheck();
  }

  onSectionChange(event: Event): void {
    this.sectionId = (event.target as HTMLSelectElement).value;
    this.reload();
  }

  onStatusChange(event: Event): void {
    this.statusFilter = (event.target as HTMLSelectElement).value;
    this.reload();
  }

  onQueryChange(event: Event): void {
    this.query = (event.target as HTMLInputElement).value;
    if (this.queryTimer) {
      clearTimeout(this.queryTimer);
    }
    this.queryTimer = setTimeout(() => this.reload(), 250);
  }

  toggleStudent(id: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      this.selectedIds.add(id);
    } else {
      this.selectedIds.delete(id);
    }
    this.selectedIds = new Set(this.selectedIds);
    this.cdr.markForCheck();
  }

  toggleAll(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.selectedIds = checked ? new Set(this.students.map((row) => row.studentId)) : new Set();
    this.cdr.markForCheck();
  }

  previewOne(studentId: string): void {
    this.http.get<ApiResponse<Marksheet>>(`/api/marksheets/${studentId}`, { params: this.baseParams() })
      .subscribe({
        next: (res) => {
          this.cards = [res.data];
          this.showPreview = true;
          this.cdr.markForCheck();
        },
        error: (err) => this.fail(err),
      });
  }

  openPreview(): void {
    const ids = [...this.selectedIds];
    if (!ids.length) {
      return;
    }
    this.loadCards(ids);
  }

  closePreview(): void {
    this.showPreview = false;
    this.cdr.markForCheck();
  }

  printPreview(): void {
    window.print();
  }

  exportMine(): void {
    if (!this.myCard) {
      return;
    }
    this.download('pdf', [this.myCard.studentId]);
  }

  exportFile(kind: 'pdf' | 'zip'): void {
    const ids = this.cards.length ? this.cards.map((card) => card.studentId) : [...this.selectedIds];
    this.download(kind, ids);
  }

  submitForApproval(): void {
    if (!this.canSubmitSelected) {
      return;
    }
    this.postAction('/api/marksheets/submit', {});
  }

  approve(locked: boolean): void {
    if (!this.canManage || !this.canApproveSelected) {
      return;
    }
    this.postAction('/api/marksheets/approve', { locked });
  }

  rejectSelected(): void {
    if (!this.canManage || !this.canApproveSelected) {
      return;
    }
    const reason = window.prompt('Return these results to teachers. Enter a reason.') ?? '';
    if (!reason.trim()) {
      this.pageError = 'Enter a reason when returning results to teachers.';
      this.cdr.markForCheck();
      return;
    }
    this.postAction('/api/marksheets/reject', { reason: reason.trim() });
  }

  publish(published: boolean, locked: boolean): void {
    if (!this.canManage || this.selectedIds.size === 0) {
      return;
    }
    this.publishing = true;
    this.http.put<ApiResponse<MarksheetStudent[]>>('/api/marksheets/publish', {
      academicYearId: this.yearId,
      examTerm: this.examTerm,
      classId: this.classId || null,
      sectionId: this.sectionId || null,
      studentIds: [...this.selectedIds],
      published,
      locked,
    }).subscribe({
      next: () => {
        this.publishing = false;
        this.reload();
      },
      error: (err) => {
        this.publishing = false;
        this.fail(err);
      },
    });
  }

  private postAction(url: string, extra: Record<string, unknown>): void {
    this.publishing = true;
    this.http.post<ApiResponse<MarksheetStudent[]>>(url, {
      academicYearId: this.yearId,
      examTerm: this.examTerm,
      classId: this.classId || null,
      sectionId: this.sectionId || null,
      studentIds: [...this.selectedIds],
      ...extra,
    }).subscribe({
      next: () => {
        this.publishing = false;
        this.reload();
      },
      error: (err) => {
        this.publishing = false;
        this.fail(err);
      },
    });
  }

  private loadCards(ids: string[]): void {
    const cards: Marksheet[] = [];
    let remaining = ids.length;
    for (const id of ids) {
      this.http.get<ApiResponse<Marksheet>>(`/api/marksheets/${id}`, { params: this.baseParams() })
        .subscribe({
          next: (res) => {
            cards.push(res.data);
            remaining -= 1;
            if (remaining === 0) {
              this.cards = cards;
              this.showPreview = true;
              this.cdr.markForCheck();
            }
          },
          error: (err) => this.fail(err),
        });
    }
  }

  private loadOptions(): void {
    this.http.get<ApiResponse<MarksheetOptions>>('/api/marksheets/options').subscribe({
      next: (res) => {
        this.options = res.data;
        this.studentView = res.data.studentView;
        this.canManage = res.data.canManage || this.auth.hasPermission('MARKSHEET_MANAGE');
        this.canSubmit = res.data.canSubmit || this.canManage;
        this.yearId = res.data.academicYears.find((year) => year.current)?.id ?? res.data.academicYears[0]?.id ?? '';
        this.classId = res.data.defaultClassId ?? '';
        this.sectionId = res.data.defaultSectionId ?? '';
        this.reload();
      },
      error: (err) => this.fail(err),
    });
  }

  private reload(): void {
    if (!this.yearId) {
      return;
    }
    this.pageError = '';
    if (this.studentView) {
      this.http.get<ApiResponse<Marksheet>>('/api/marksheets/mine', { params: this.baseParams() })
        .subscribe({
          next: (res) => {
            this.myCard = res.data;
            this.cdr.markForCheck();
          },
          error: (err) => this.fail(err),
        });
      return;
    }
    if (!this.sectionId) {
      this.students = [];
      this.cdr.markForCheck();
      return;
    }
    this.loading = true;
    this.http.get<ApiResponse<MarksheetStudent[]>>('/api/marksheets/students', { params: this.baseParams() })
      .subscribe({
        next: (res) => {
          this.students = res.data ?? [];
          this.selectedIds = new Set();
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.loading = false;
          this.fail(err);
        },
      });
  }

  private download(kind: 'pdf' | 'zip', studentIds: string[]): void {
    this.exporting = true;
    this.http.post(`/api/marksheets/export/${kind}`, {
      academicYearId: this.yearId,
      examTerm: this.examTerm,
      classId: this.classId || null,
      sectionId: this.sectionId || null,
      studentIds,
    }, { responseType: 'blob', observe: 'response' }).subscribe({
      next: (res) => {
        this.exporting = false;
        const blob = res.body;
        if (!blob) {
          return;
        }
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = this.filenameFromHeader(res.headers.get('content-disposition'), kind, studentIds);
        link.click();
        URL.revokeObjectURL(url);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.exporting = false;
        this.fail(err);
      },
    });
  }

  private filenameFromHeader(header: string | null, kind: 'pdf' | 'zip', studentIds: string[]): string {
    if (header) {
      const match = /filename="?([^"]+)"?/i.exec(header);
      if (match?.[1]) {
        return match[1];
      }
    }
    if (kind === 'zip') {
      return 'marksheets.zip';
    }
    if (studentIds.length === 1) {
      const row = this.students.find((item) => item.studentId === studentIds[0])
        ?? (this.myCard && this.myCard.studentId === studentIds[0] ? this.myCard : null);
      const roll = row?.rollNumber ?? 'NA';
      return `Marksheet_${roll}_${this.examTerm}.pdf`;
    }
    return 'marksheets.pdf';
  }

  private baseParams(): HttpParams {
    let params = new HttpParams().set('academicYearId', this.yearId).set('examTerm', this.examTerm);
    if (this.classId) {
      params = params.set('classId', this.classId);
    }
    if (this.sectionId) {
      params = params.set('sectionId', this.sectionId);
    }
    if (this.query.trim()) {
      params = params.set('query', this.query.trim());
    }
    if (!this.studentView && this.statusFilter && this.statusFilter !== 'ALL') {
      params = params.set('status', this.statusFilter);
    }
    return params;
  }

  private fail(err: HttpErrorResponse): void {
    const body = err.error as ApiError | undefined;
    this.pageError = body?.message || err.message || 'Error';
    this.cdr.markForCheck();
  }
}
