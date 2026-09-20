import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, ViewEncapsulation } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { ApiError, ApiResponse } from '../../core/models/api.model';

interface YearOption { id: string; name: string; current: boolean }
interface SectionOption { id: string; name: string }
interface ClassOption { id: string; name: string; sections: SectionOption[] }
interface AdmitOptions {
  academicYears: YearOption[];
  classes: ClassOption[];
  examTerms: string[];
  studentView: boolean;
  defaultStudentId: string | null;
  defaultClassId: string | null;
  defaultSectionId: string | null;
}
interface AdmitStudent {
  studentId: string;
  studentName: string;
  admissionNo: string;
  rollNumber: number | null;
  photoUrl: string | null;
  published: boolean;
  examCount: number;
}
interface AdmitSlot {
  scheduleId: string;
  subjectName: string;
  examDate: string;
  startTime: string;
  endTime: string;
  room: string | null;
}
interface AdmitCard {
  studentId: string;
  studentName: string;
  admissionNo: string;
  rollNumber: number | null;
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
  datesheet: AdmitSlot[];
  guidelines: string[];
}

@Component({
  selector: 'app-admit-cards',
  imports: [TranslateModule],
  template: `
    <div class="tab-page">
      <div class="page-header">
        <div>
          <h2>{{ 'examinations.admitCardsPage.title' | translate }}</h2>
          <p class="muted">{{ 'examinations.admitCardsPage.subtitle' | translate }}</p>
        </div>
      </div>

      @if (pageError) {
        <p class="banner error">{{ pageError }}</p>
      }
      @if (!allPublished && students.length > 0) {
        <p class="banner warn">{{ 'examinations.admitCardsPage.unpublished' | translate }}</p>
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
            <option value="">{{ 'examinations.admitCardsPage.selectClass' | translate }}</option>
            @for (klass of options?.classes ?? []; track klass.id) {
              <option [value]="klass.id">{{ klass.name }}</option>
            }
          </select>
          <select [value]="sectionId" (change)="onSectionChange($event)" [disabled]="!classId">
            <option value="">{{ 'examinations.admitCardsPage.selectSection' | translate }}</option>
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
            {{ 'examinations.admitCardsPage.selectAll' | translate }}
          </label>
          <span class="muted">{{ 'examinations.admitCardsPage.selected' | translate:{ count: selectedIds.size } }}</span>
          <span class="spacer"></span>
          <button class="btn" type="button" [disabled]="!canPrint" (click)="openPreview()">
            {{ 'examinations.admitCardsPage.printSelected' | translate }}
          </button>
          <button class="btn btn-primary" type="button" [disabled]="!canPrint || exporting" (click)="exportFile('pdf')">
            {{ 'examinations.admitCardsPage.exportPdf' | translate }}
          </button>
          <button class="btn" type="button" [disabled]="!canPrint || exporting" (click)="exportFile('zip')">
            {{ 'examinations.admitCardsPage.exportZip' | translate }}
          </button>
        </div>

        <div class="card">
          <table>
            <thead>
              <tr>
                <th></th>
                <th>{{ 'examinations.admitCardsPage.student' | translate }}</th>
                <th>{{ 'examinations.admitCardsPage.admissionNo' | translate }}</th>
                <th>{{ 'examinations.admitCardsPage.roll' | translate }}</th>
                <th>{{ 'examinations.admitCardsPage.exams' | translate }}</th>
                <th>{{ 'examinations.admitCardsPage.status' | translate }}</th>
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
                  <td>{{ row.examCount }}</td>
                  <td>
                    <span class="badge" [class.badge-success]="row.published" [class.badge-muted]="!row.published">
                      {{ (row.published ? 'examinations.admitCardsPage.published' : 'examinations.admitCardsPage.draft') | translate }}
                    </span>
                  </td>
                  <td>
                    <button class="btn" type="button" (click)="previewOne(row.studentId)">
                      {{ 'examinations.admitCardsPage.preview' | translate }}
                    </button>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="7" class="center muted">{{ 'examinations.admitCardsPage.empty' | translate }}</td>
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
            {{ 'examinations.admitCardsPage.print' | translate }}
          </button>
          <button class="btn btn-primary" type="button" [disabled]="!myCard.published || exporting" (click)="exportMine()">
            {{ 'examinations.admitCardsPage.downloadPdf' | translate }}
          </button>
        </div>
        @if (!myCard.published) {
          <p class="banner warn">{{ 'examinations.admitCardsPage.notPublished' | translate }}</p>
        }
        <div class="preview-wrap">
          <article class="admit-card">
            <header class="card-head">
              <div class="school">
                <h3>{{ myCard.schoolName }}</h3>
                <p>{{ myCard.schoolAddress }}</p>
                <p>{{ myCard.schoolPhone }}</p>
                <strong>{{ 'examinations.admitCardsPage.hallTicket' | translate }}</strong>
              </div>
              <div class="photo">
                @if (myCard.photoUrl) {
                  <img [src]="myCard.photoUrl" alt="" />
                } @else {
                  <span>{{ initial(myCard.studentName) }}</span>
                }
              </div>
            </header>
            <dl class="meta">
              <div><dt>{{ 'examinations.admitCardsPage.student' | translate }}</dt><dd>{{ myCard.studentName }}</dd></div>
              <div><dt>{{ 'examinations.admitCardsPage.admissionNo' | translate }}</dt><dd>{{ myCard.admissionNo }}</dd></div>
              <div><dt>{{ 'examinations.admitCardsPage.roll' | translate }}</dt><dd>{{ myCard.rollNumber ?? '—' }}</dd></div>
              <div><dt>{{ 'examinations.admitCardsPage.class' | translate }}</dt><dd>{{ myCard.className }} {{ myCard.sectionName }}</dd></div>
            </dl>
            <h4>{{ 'examinations.admitCardsPage.datesheet' | translate }}</h4>
            <table class="datesheet">
              <thead>
                <tr>
                  <th>{{ 'examinations.admitCardsPage.subject' | translate }}</th>
                  <th>{{ 'examinations.admitCardsPage.date' | translate }}</th>
                  <th>{{ 'examinations.admitCardsPage.time' | translate }}</th>
                  <th>{{ 'examinations.admitCardsPage.room' | translate }}</th>
                </tr>
              </thead>
              <tbody>
                @for (slot of myCard.datesheet; track slot.scheduleId) {
                  <tr>
                    <td>{{ slot.subjectName }}</td>
                    <td>{{ slot.examDate }}</td>
                    <td>{{ formatTime(slot.startTime) }}–{{ formatTime(slot.endTime) }}</td>
                    <td>{{ slot.room || '—' }}</td>
                  </tr>
                }
              </tbody>
            </table>
            <h4>{{ 'examinations.admitCardsPage.guidelines' | translate }}</h4>
            <ol>
              @for (line of myCard.guidelines; track line) {
                <li>{{ line }}</li>
              }
            </ol>
            <footer class="signs">
              <span>{{ 'examinations.admitCardsPage.signature' | translate }}</span>
              <span>{{ 'examinations.admitCardsPage.controller' | translate }}</span>
            </footer>
          </article>
        </div>
      }
    </div>

    @if (showPreview) {
      <div class="modal-backdrop" (click)="closePreview()">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="toolbar">
            <h2>{{ 'examinations.admitCardsPage.printPreview' | translate }}</h2>
            <span class="spacer"></span>
            <button class="btn" type="button" [disabled]="!canPrintPreview" (click)="printPreview()">
              {{ 'examinations.admitCardsPage.print' | translate }}
            </button>
            <button class="btn btn-primary" type="button" [disabled]="!canPrintPreview || exporting" (click)="exportFile('pdf')">
              {{ 'examinations.admitCardsPage.downloadPdf' | translate }}
            </button>
            <button class="btn" type="button" (click)="closePreview()">{{ 'examinations.admitCardsPage.close' | translate }}</button>
          </div>
          <div class="print-area">
            @for (card of cards; track card.studentId) {
              <article class="admit-card">
                <header class="card-head">
                  <div class="school">
                    <h3>{{ card.schoolName }}</h3>
                    <p>{{ card.schoolAddress }}</p>
                    <p>{{ card.schoolPhone }}</p>
                    <strong>{{ 'examinations.admitCardsPage.hallTicket' | translate }}</strong>
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
                  <div><dt>{{ 'examinations.admitCardsPage.student' | translate }}</dt><dd>{{ card.studentName }}</dd></div>
                  <div><dt>{{ 'examinations.admitCardsPage.admissionNo' | translate }}</dt><dd>{{ card.admissionNo }}</dd></div>
                  <div><dt>{{ 'examinations.admitCardsPage.roll' | translate }}</dt><dd>{{ card.rollNumber ?? '—' }}</dd></div>
                  <div><dt>{{ 'examinations.admitCardsPage.class' | translate }}</dt><dd>{{ card.className }} {{ card.sectionName }}</dd></div>
                </dl>
                <h4>{{ 'examinations.admitCardsPage.datesheet' | translate }}</h4>
                <table class="datesheet">
                  <thead>
                    <tr>
                      <th>{{ 'examinations.admitCardsPage.subject' | translate }}</th>
                      <th>{{ 'examinations.admitCardsPage.date' | translate }}</th>
                      <th>{{ 'examinations.admitCardsPage.time' | translate }}</th>
                      <th>{{ 'examinations.admitCardsPage.room' | translate }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (slot of card.datesheet; track slot.scheduleId) {
                      <tr>
                        <td>{{ slot.subjectName }}</td>
                        <td>{{ slot.examDate }}</td>
                        <td>{{ formatTime(slot.startTime) }}–{{ formatTime(slot.endTime) }}</td>
                        <td>{{ slot.room || '—' }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
                <h4>{{ 'examinations.admitCardsPage.guidelines' | translate }}</h4>
                <ol>
                  @for (line of card.guidelines; track line) {
                    <li>{{ line }}</li>
                  }
                </ol>
                <footer class="signs">
                  <span>{{ 'examinations.admitCardsPage.signature' | translate }}</span>
                  <span>{{ 'examinations.admitCardsPage.controller' | translate }}</span>
                </footer>
              </article>
            }
          </div>
        </div>
      </div>
    }
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
    .admit-card { border: 1px solid #cbd5e1; padding: 18px; margin-bottom: 18px; page-break-after: always; break-after: page; background: #fff; }
    .admit-card:last-child { page-break-after: auto; break-after: auto; }
    .card-head { display: flex; justify-content: space-between; gap: 16px; border-bottom: 1px solid #e2e8f0; padding-bottom: 12px; }
    .school h3 { margin: 0 0 4px; }
    .school p { margin: 0; color: #64748b; font-size: .85rem; }
    .photo { width: 88px; height: 110px; border: 1px solid #cbd5e1; display: flex; align-items: center; justify-content: center; font-weight: 700; overflow: hidden; }
    .photo img { width: 100%; height: 100%; object-fit: cover; }
    .meta { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 16px; margin: 12px 0; }
    dt { font-size: .75rem; color: #64748b; }
    dd { margin: 0; font-weight: 600; }
    .datesheet th, .datesheet td { font-size: .82rem; }
    ol { padding-left: 18px; font-size: .85rem; }
    .signs { display: flex; justify-content: space-between; margin-top: 24px; font-size: .82rem; }
    .preview-wrap { background: #fff; }
    @media print {
      body * { visibility: hidden; }
      .print-area, .print-area * { visibility: visible; }
      .print-area {
        position: absolute; inset: 0; width: 100%;
        background: #fff; padding: 0; margin: 0;
      }
      .admit-card { box-shadow: none; margin: 0 0 12px; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
})
export class AdmitCardsComponent implements OnInit {
  options: AdmitOptions | null = null;
  students: AdmitStudent[] = [];
  cards: AdmitCard[] = [];
  myCard: AdmitCard | null = null;
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
    return this.selectedIds.size > 0 && this.allPublished;
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

  formatTime(value: string): string {
    return (value || '').slice(0, 5);
  }

  initial(name: string): string {
    return (name || '?').slice(0, 1);
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
    this.http.get<ApiResponse<AdmitCard>>(`/api/admit-cards/${studentId}`, { params: this.baseParams() })
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
    this.http.get<ApiResponse<AdmitCard>>(`/api/admit-cards/${ids[0]}`, { params: this.baseParams() })
      .subscribe({
        next: () => this.loadCards(ids),
        error: (err) => this.fail(err),
      });
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
    const cards: AdmitCard[] = [];
    let remaining = ids.length;
    for (const id of ids) {
      this.http.get<ApiResponse<AdmitCard>>(`/api/admit-cards/${id}`, { params: this.baseParams() })
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
    this.http.get<ApiResponse<AdmitOptions>>('/api/admit-cards/options').subscribe({
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
      this.http.get<ApiResponse<AdmitCard>>('/api/admit-cards/mine', { params: this.baseParams() })
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
    this.http.get<ApiResponse<AdmitStudent[]>>('/api/admit-cards/students', { params: this.baseParams() })
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
    this.http.post(`/api/admit-cards/export/${kind}`, {
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
        link.download = kind === 'zip' ? 'admit-cards.zip' : 'admit-cards.pdf';
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
