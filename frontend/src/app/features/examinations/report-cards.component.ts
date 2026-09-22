import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, ViewEncapsulation } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { ApiError, ApiResponse } from '../../core/models/api.model';

interface YearOption { id: string; name: string; current: boolean }
interface SectionOption { id: string; name: string }
interface ClassOption { id: string; name: string; sections: SectionOption[] }
interface ReportOptions {
  academicYears: YearOption[];
  classes: ClassOption[];
  examTerms: string[];
  studentView: boolean;
  defaultStudentId: string | null;
  defaultClassId: string | null;
  defaultSectionId: string | null;
}
interface ReportStudent {
  studentId: string;
  studentName: string;
  admissionNo: string;
  rollNumber: number | null;
  photoUrl: string | null;
  ready: boolean;
  published: boolean;
  percentage: number | null;
  overallGrade: string | null;
}
interface ReportSubject {
  subjectId: string;
  subjectName: string;
  subjectCode: string | null;
  theory: number | null;
  maxTheory: number | null;
  practical: number | null;
  maxPractical: number | null;
  assignment: number | null;
  maxAssignment: number | null;
  total: number | null;
  maxTotal: number | null;
  percentage: number | null;
  grade: string | null;
  remarks: string | null;
}
interface ReportAttendance {
  workingDays: number;
  daysPresent: number;
  daysAbsent: number;
  daysLate: number;
  daysLeave: number;
  percent: number;
  fromDate: string;
  toDate: string;
}
interface ReportBehaviour {
  conduct: string;
  discipline: string;
  punctuality: string;
  coCurricular: string | null;
}
interface ReportCard {
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
  ready: boolean;
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
  attendance: ReportAttendance;
  subjects: ReportSubject[];
  behaviour: ReportBehaviour;
  teacherComment: string;
  principalComment: string;
}

@Component({
  selector: 'app-report-cards',
  imports: [TranslateModule, NgTemplateOutlet],
  template: `
    <div class="tab-page">
      <div class="page-header">
        <div>
          <h2>{{ 'examinations.reportCardsPage.title' | translate }}</h2>
          <p class="muted">{{ 'examinations.reportCardsPage.subtitle' | translate }}</p>
        </div>
      </div>

      @if (pageError) {
        <p class="banner error">{{ pageError }}</p>
      }
      @if (!allPublished && students.length > 0) {
        <p class="banner warn">{{ 'examinations.reportCardsPage.unpublished' | translate }}</p>
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
            <option value="">{{ 'examinations.reportCardsPage.selectClass' | translate }}</option>
            @for (klass of options?.classes ?? []; track klass.id) {
              <option [value]="klass.id">{{ klass.name }}</option>
            }
          </select>
          <select [value]="sectionId" (change)="onSectionChange($event)" [disabled]="!classId">
            <option value="">{{ 'examinations.reportCardsPage.selectSection' | translate }}</option>
            @for (section of filterSections; track section.id) {
              <option [value]="section.id">{{ section.name }}</option>
            }
          </select>
        }
      </div>

      @if (!studentView && sectionId) {
        <div class="toolbar">
          <label class="check">
            <input type="checkbox" [checked]="allSelected" (change)="toggleAll($event)" />
            {{ 'examinations.reportCardsPage.selectAll' | translate }}
          </label>
          <span class="muted">{{ 'examinations.reportCardsPage.selected' | translate:{ count: selectedIds.size } }}</span>
          <span class="spacer"></span>
          <button class="btn" type="button" [disabled]="!canPrint" (click)="openPreview()">
            {{ 'examinations.reportCardsPage.printSelected' | translate }}
          </button>
          <button class="btn btn-primary" type="button" [disabled]="!canPrint || exporting" (click)="exportFile('pdf')">
            {{ 'examinations.reportCardsPage.exportPdf' | translate }}
          </button>
          <button class="btn" type="button" [disabled]="!canPrint || exporting" (click)="exportFile('zip')">
            {{ 'examinations.reportCardsPage.exportZip' | translate }}
          </button>
        </div>

        <div class="card">
          <table>
            <thead>
              <tr>
                <th></th>
                <th>{{ 'examinations.reportCardsPage.student' | translate }}</th>
                <th>{{ 'examinations.reportCardsPage.admissionNo' | translate }}</th>
                <th>{{ 'examinations.reportCardsPage.roll' | translate }}</th>
                <th>{{ 'examinations.reportCardsPage.percentage' | translate }}</th>
                <th>{{ 'examinations.reportCardsPage.grade' | translate }}</th>
                <th>{{ 'examinations.reportCardsPage.status' | translate }}</th>
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
                  <td>{{ row.overallGrade || '—' }}</td>
                  <td>
                    <span class="badge" [class.badge-success]="row.published" [class.badge-muted]="!row.published">
                      {{ (row.published ? 'examinations.reportCardsPage.published' : 'examinations.reportCardsPage.draft') | translate }}
                    </span>
                  </td>
                  <td>
                    <button class="btn" type="button" (click)="previewOne(row.studentId)">
                      {{ 'examinations.reportCardsPage.preview' | translate }}
                    </button>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="8" class="center muted">{{ 'examinations.reportCardsPage.empty' | translate }}</td>
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
            {{ 'examinations.reportCardsPage.print' | translate }}
          </button>
          <button class="btn btn-primary" type="button" [disabled]="!myCard.published || exporting" (click)="exportMine()">
            {{ 'examinations.reportCardsPage.downloadPdf' | translate }}
          </button>
        </div>
        @if (!myCard.published) {
          <p class="banner warn">{{ 'examinations.reportCardsPage.notPublished' | translate }}</p>
        }
        <div class="preview-wrap">
          <ng-container *ngTemplateOutlet="cardTpl; context: { $implicit: myCard }"></ng-container>
        </div>
      }
    </div>

    @if (showPreview) {
      <div class="modal-backdrop" (click)="closePreview()">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="toolbar">
            <h2>{{ 'examinations.reportCardsPage.printPreview' | translate }}</h2>
            <span class="spacer"></span>
            <button class="btn" type="button" [disabled]="!canPrintPreview" (click)="printPreview()">
              {{ 'examinations.reportCardsPage.print' | translate }}
            </button>
            <button class="btn btn-primary" type="button" [disabled]="!canPrintPreview || exporting" (click)="exportFile('pdf')">
              {{ 'examinations.reportCardsPage.downloadPdf' | translate }}
            </button>
            <button class="btn" type="button" (click)="closePreview()">{{ 'examinations.reportCardsPage.close' | translate }}</button>
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
      <article class="report-card">
        <header class="card-head">
          <div class="school">
            <h3>{{ card.schoolName }}</h3>
            <p>{{ card.affiliation }}</p>
            <p>{{ card.schoolAddress }}</p>
            <p>{{ card.schoolPhone }} {{ card.schoolEmail }}</p>
            <strong>{{ 'examinations.reportCardsPage.heading' | translate }} · {{ card.academicYearName }} · {{ card.examTerm }}</strong>
          </div>
          <div class="photo">
            @if (card.photoUrl) {
              <img [src]="card.photoUrl" alt="" />
            } @else {
              <span>{{ initial(card.studentName) }}</span>
            }
          </div>
        </header>
        <dl class="meta">
          <div><dt>{{ 'examinations.reportCardsPage.student' | translate }}</dt><dd>{{ card.studentName }}</dd></div>
          <div><dt>{{ 'examinations.reportCardsPage.admissionNo' | translate }}</dt><dd>{{ card.admissionNo }}</dd></div>
          <div><dt>{{ 'examinations.reportCardsPage.roll' | translate }}</dt><dd>{{ card.rollNumber ?? '—' }}</dd></div>
          <div><dt>{{ 'examinations.reportCardsPage.class' | translate }}</dt><dd>{{ card.className }} {{ card.sectionName }}</dd></div>
          <div><dt>{{ 'examinations.reportCardsPage.dob' | translate }}</dt><dd>{{ card.dateOfBirth || '—' }}</dd></div>
          <div><dt>{{ 'examinations.reportCardsPage.guardian' | translate }}</dt><dd>{{ card.guardianName || '—' }}</dd></div>
          <div><dt>{{ 'examinations.reportCardsPage.classTeacher' | translate }}</dt><dd>{{ card.classTeacherName || '—' }}</dd></div>
          <div><dt>{{ 'examinations.reportCardsPage.result' | translate }}</dt><dd>{{ card.result }} / {{ card.overallGrade || '—' }}</dd></div>
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
              <td>{{ card.attendance?.workingDays ?? 0 }}</td>
              <td>{{ card.attendance?.daysPresent ?? 0 }}</td>
              <td>{{ card.attendance?.daysAbsent ?? 0 }}</td>
              <td>{{ card.attendance?.daysLate ?? 0 }}</td>
              <td>{{ card.attendance?.daysLeave ?? 0 }}</td>
              <td>{{ card.attendance?.percent ?? 0 }}</td>
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
            @for (row of card.subjects; track row.subjectId) {
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
          <div><dt>{{ 'examinations.reportCardsPage.grandTotal' | translate }}</dt><dd>{{ card.totalObtained }} / {{ card.totalMax }}</dd></div>
          <div><dt>{{ 'examinations.reportCardsPage.percentage' | translate }}</dt><dd>{{ card.percentage }}%</dd></div>
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
              <td>{{ label(card.behaviour?.conduct) }}</td>
              <td>{{ label(card.behaviour?.discipline) }}</td>
              <td>{{ label(card.behaviour?.punctuality) }}</td>
              <td>{{ card.behaviour?.coCurricular || '—' }}</td>
            </tr>
          </tbody>
        </table>
        <div class="comment">
          <strong>{{ 'examinations.reportCardsPage.teacherRemarks' | translate }}</strong>
          <p>{{ card.teacherComment }}</p>
        </div>
        <div class="comment">
          <strong>{{ 'examinations.reportCardsPage.principalRemarks' | translate }}</strong>
          <p>{{ card.principalComment }}</p>
        </div>
        <footer class="signs">
          <span>{{ 'examinations.reportCardsPage.classTeacher' | translate }}</span>
          <span>{{ 'examinations.reportCardsPage.parentSign' | translate }}</span>
          <span>{{ 'examinations.reportCardsPage.principalSign' | translate }}</span>
        </footer>
      </article>
    </ng-template>
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    h2 { font-size: 1.2rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); margin: 0; }
    .filters, .toolbar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; align-items: center; }
    select { padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; background: #fff; min-width: 160px; }
    .spacer { flex: 1; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 10px; border-bottom: 1px solid var(--color-border); font-size: .9rem; }
    .center { text-align: center; padding: 18px; }
    .banner { padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
    .banner.error { background: #fef2f2; border: 1px solid #fecaca; color: #b91c1c; }
    .banner.warn { background: #fff7ed; border: 1px solid #fed7aa; color: #9a3412; }
    .badge { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: .7rem; font-weight: 600; }
    .badge-success { background: #d1fae5; color: #047857; }
    .badge-muted { background: #e2e8f0; color: #475569; }
    .check { display: flex; align-items: center; gap: 8px; }
    .modal-backdrop { position: fixed; inset: 0; z-index: 100; background: rgba(15, 23, 42, .5); overflow: auto; padding: 24px; }
    .modal { background: #fff; border-radius: var(--radius); padding: 20px; max-width: 900px; margin: 0 auto; }
    .report-card { border: 1px solid #cbd5e1; padding: 18px; margin-bottom: 18px; page-break-after: always; break-after: page; background: #fff; }
    .report-card:last-child { page-break-after: auto; break-after: auto; }
    .card-head { display: flex; justify-content: space-between; gap: 16px; border-bottom: 1px solid #e2e8f0; padding-bottom: 12px; }
    .school h3 { margin: 0 0 4px; }
    .school p { margin: 0; color: #64748b; font-size: .85rem; }
    .photo { width: 88px; height: 110px; border: 1px solid #cbd5e1; display: flex; align-items: center; justify-content: center; font-weight: 700; overflow: hidden; }
    .photo img { width: 100%; height: 100%; object-fit: cover; }
    .meta { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 16px; margin: 12px 0; }
    dt { font-size: .75rem; color: #64748b; }
    dd { margin: 0; font-weight: 600; }
    .sheet th, .sheet td { font-size: .82rem; }
    .comment { border: 1px solid #e2e8f0; padding: 8px 10px; margin-top: 10px; }
    .comment p { margin: 6px 0 0; font-size: .88rem; white-space: pre-wrap; }
    .signs { display: flex; justify-content: space-between; margin-top: 24px; font-size: .82rem; gap: 12px; }
    .preview-wrap { background: #fff; }
    @media print {
      body * { visibility: hidden; }
      .print-area, .print-area * { visibility: visible; }
      .print-area {
        position: absolute; inset: 0; width: 100%;
        background: #fff; padding: 0; margin: 0;
      }
      .report-card { box-shadow: none; margin: 0 0 12px; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
})
export class ReportCardsComponent implements OnInit {
  options: ReportOptions | null = null;
  students: ReportStudent[] = [];
  cards: ReportCard[] = [];
  myCard: ReportCard | null = null;
  yearId = '';
  classId = '';
  sectionId = '';
  examTerm = 'TERM';
  studentView = false;
  loading = false;
  exporting = false;
  pageError = '';
  showPreview = false;
  selectedIds = new Set<string>();

  constructor(private http: HttpClient, public cdr: ChangeDetectorRef) {}

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

  ngOnInit(): void {
    this.loadOptions();
  }

  termLabel(term: string): string {
    return `academics.exam${term.charAt(0)}${term.slice(1).toLowerCase()}`;
  }

  initial(name: string): string {
    return (name || '?').slice(0, 1);
  }

  label(value: string | null | undefined): string {
    return (value || '—').replace(/_/g, ' ');
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
    this.http.get<ApiResponse<ReportCard>>(`/api/report-cards/${studentId}`, { params: this.baseParams() })
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

  private loadCards(ids: string[]): void {
    const cards: ReportCard[] = [];
    let remaining = ids.length;
    for (const id of ids) {
      this.http.get<ApiResponse<ReportCard>>(`/api/report-cards/${id}`, { params: this.baseParams() })
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
    this.http.get<ApiResponse<ReportOptions>>('/api/report-cards/options').subscribe({
      next: (res) => {
        this.options = res.data;
        this.studentView = res.data.studentView;
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
      this.http.get<ApiResponse<ReportCard>>('/api/report-cards/mine', { params: this.baseParams() })
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
    this.http.get<ApiResponse<ReportStudent[]>>('/api/report-cards/students', { params: this.baseParams() })
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
    this.http.post(`/api/report-cards/export/${kind}`, {
      academicYearId: this.yearId,
      examTerm: this.examTerm,
      classId: this.classId || null,
      sectionId: this.sectionId || null,
      studentIds,
    }, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        this.exporting = false;
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = kind === 'zip' ? 'report-cards.zip' : 'report-cards.pdf';
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

  private baseParams(): HttpParams {
    let params = new HttpParams().set('academicYearId', this.yearId).set('examTerm', this.examTerm);
    if (this.classId) {
      params = params.set('classId', this.classId);
    }
    if (this.sectionId) {
      params = params.set('sectionId', this.sectionId);
    }
    return params;
  }

  private fail(err: HttpErrorResponse): void {
    const body = err.error as ApiError | undefined;
    this.pageError = body?.message || err.message || 'Error';
    this.cdr.markForCheck();
  }
}
