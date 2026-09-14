import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ApiError, ApiResponse } from '../../core/models/api.model';

interface YearOption { id: string; name: string; current: boolean }
interface SectionOption { id: string; name: string }
interface SubjectOption { id: string; name: string; practical: boolean }
interface ClassOption { id: string; name: string; sections: SectionOption[]; subjects: SubjectOption[] }
interface MarksOptions { academicYears: YearOption[]; classes: ClassOption[]; examTerms: string[] }
interface GradeBoundary { label: string; minPercent: number; maxPercent: number }

interface MarkRow {
  studentId: string;
  admissionNo: string;
  studentName: string;
  rollNumber: number;
  theory: number | null;
  practical: number | null;
  assignment: number | null;
  attendanceStatus: 'PRESENT' | 'ABSENT' | 'EXCUSED';
  remarks: string;
  total: number | null;
  percentage: number | null;
  gradeLabel: string | null;
  status: string;
}

interface MarksGrid {
  examEntryId: string;
  academicYearId: string;
  academicYearName: string;
  classId: string;
  className: string;
  sectionId: string;
  sectionName: string;
  subjectId: string;
  subjectName: string;
  practicalEnabled: boolean;
  examTerm: string;
  maxTheory: number;
  maxPractical: number;
  maxAssignment: number;
  maxTotal: number;
  passMarks: number | null;
  scaleType: string;
  schemeName: string | null;
  entryDeadline: string | null;
  locked: boolean;
  canEdit: boolean;
  canLock: boolean;
  boundaries: GradeBoundary[];
  rows: MarkRow[];
}

interface ImportResult {
  total: number;
  succeeded: number;
  errors: string[];
  grid: MarksGrid;
}

type EditableField = 'theory' | 'practical' | 'assignment' | 'attendanceStatus' | 'remarks';

const FIELD_ORDER: EditableField[] = ['theory', 'practical', 'assignment', 'attendanceStatus', 'remarks'];
const ATTENDANCE = ['PRESENT', 'ABSENT', 'EXCUSED'] as const;

@Component({
  selector: 'app-marks-entry',
  imports: [TranslateModule, ReactiveFormsModule],
  template: `
    <div class="tab-page">
      <div class="page-header">
        <div>
          <h2>{{ 'examinations.marksPage.title' | translate }}</h2>
          <p class="muted">{{ 'examinations.marksPage.subtitle' | translate }}</p>
        </div>
      </div>

      @if (successMessage) {
        <p class="banner success">{{ successMessage | translate }}</p>
      }
      @if (pageError) {
        <p class="banner error">{{ pageError }}</p>
      }

      <div class="card toolbar-card">
        <div class="toolbar">
          <div class="field">
            <label>{{ 'examinations.marksPage.class' | translate }}</label>
            <select [formControl]="classControl" (change)="onClassChange()">
              @for (klass of options?.classes ?? []; track klass.id) {
                <option [value]="klass.id">{{ klass.name }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label>{{ 'examinations.marksPage.section' | translate }}</label>
            <select [formControl]="sectionControl" (change)="reloadGrid()">
              @for (section of selectedClass?.sections ?? []; track section.id) {
                <option [value]="section.id">{{ section.name }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label>{{ 'examinations.marksPage.subject' | translate }}</label>
            <select [formControl]="subjectControl" (change)="reloadGrid()">
              @for (subject of selectedClass?.subjects ?? []; track subject.id) {
                <option [value]="subject.id">{{ subject.name }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label>{{ 'examinations.marksPage.examTerm' | translate }}</label>
            <select [formControl]="termControl" (change)="reloadGrid()">
              @for (term of options?.examTerms ?? []; track term) {
                <option [value]="term">{{ termLabel(term) | translate }}</option>
              }
            </select>
          </div>
          <div class="field field-btn">
            <label>&nbsp;</label>
            <button class="btn" type="button" (click)="reloadGrid()" [disabled]="loading">
              {{ 'common.refresh' | translate }}
            </button>
          </div>
        </div>
      </div>

      @if (grid) {
        <div class="meta">
          <span class="chip">{{ grid.academicYearName }}</span>
          <span class="chip">{{ 'examinations.marksPage.maxTheory' | translate }}: {{ grid.maxTheory }}</span>
          @if (showPractical) {
            <span class="chip">{{ 'examinations.marksPage.maxPractical' | translate }}: {{ grid.maxPractical }}</span>
          }
          <span class="chip">{{ 'examinations.marksPage.maxAssignment' | translate }}: {{ grid.maxAssignment }}</span>
          <span class="chip">{{ 'examinations.marksPage.maxTotal' | translate }}: {{ grid.maxTotal }}</span>
          @if (grid.schemeName) {
            <span class="chip">{{ grid.schemeName }}</span>
          }
          @if (grid.locked) {
            <span class="badge badge-danger">{{ 'examinations.marksPage.locked' | translate }}</span>
          } @else if (grid.canEdit) {
            <span class="badge badge-success">{{ 'examinations.marksPage.editable' | translate }}</span>
          }
        </div>

        <div class="card actions-card">
          <div class="actions">
            <button class="btn" type="button" (click)="downloadTemplate('xlsx')" [disabled]="busy">
              {{ 'examinations.marksPage.downloadExcel' | translate }}
            </button>
            <button class="btn" type="button" (click)="downloadTemplate('csv')" [disabled]="busy">
              {{ 'examinations.marksPage.downloadCsv' | translate }}
            </button>
            @if (grid.canEdit) {
              <label class="btn file-btn">
                {{ 'examinations.marksPage.upload' | translate }}
                <input #fileInput type="file" accept=".csv,.xlsx,.xls" (change)="onFileSelected($event)" hidden />
              </label>
            }
            @if (grid.canEdit) {
              <button class="btn" type="button" (click)="save(false)" [disabled]="busy || hasInvalid">
                {{ 'examinations.marksPage.saveDraft' | translate }}
              </button>
              <button class="btn btn-primary" type="button" (click)="save(true)" [disabled]="busy || hasInvalid">
                {{ 'examinations.marksPage.submit' | translate }}
              </button>
            }
          </div>
          @if (grid.canLock) {
            <div class="lock-row">
              <label>{{ 'examinations.marksPage.deadline' | translate }}</label>
              <input type="date" [formControl]="deadlineControl" />
              @if (grid.locked) {
                <button class="btn" type="button" (click)="setLock(false)" [disabled]="busy">
                  {{ 'examinations.marksPage.unlock' | translate }}
                </button>
              } @else {
                <button class="btn" type="button" (click)="setLock(true)" [disabled]="busy">
                  {{ 'examinations.marksPage.lock' | translate }}
                </button>
              }
            </div>
          }
        </div>

        <div class="card">
          <div class="table-wrap" #gridWrap>
            <table class="sheet">
              <thead>
                <tr>
                  <th>{{ 'students.rollNumber' | translate }}</th>
                  <th>{{ 'students.studentId' | translate }}</th>
                  <th>{{ 'students.name' | translate }}</th>
                  <th>{{ 'examinations.marksPage.theory' | translate }}</th>
                  @if (showPractical) {
                    <th>{{ 'examinations.marksPage.practical' | translate }}</th>
                  }
                  <th>{{ 'examinations.marksPage.assignment' | translate }}</th>
                  <th>{{ 'examinations.marksPage.attendance' | translate }}</th>
                  <th>{{ 'examinations.marksPage.remarks' | translate }}</th>
                  <th>{{ 'examinations.marksPage.total' | translate }}</th>
                  <th>{{ 'examinations.marksPage.percentage' | translate }}</th>
                  <th>{{ 'examinations.marksPage.grade' | translate }}</th>
                </tr>
              </thead>
              <tbody>
                @for (row of grid.rows; track row.studentId; let i = $index) {
                  <tr>
                    <td>{{ row.rollNumber || '' }}</td>
                    <td>{{ row.admissionNo }}</td>
                    <td class="strong">{{ row.studentName }}</td>
                    <td>
                      <input class="cell"
                             [class.invalid]="invalidTheory(row)"
                             type="number" min="0" [max]="grid.maxTheory" step="0.5"
                             [value]="row.theory ?? ''"
                             [disabled]="!grid.canEdit"
                             (input)="onNumber(row, 'theory', $event)"
                             (keydown)="onKey($event, i, 'theory')" />
                    </td>
                    @if (showPractical) {
                      <td>
                        <input class="cell"
                               [class.invalid]="invalidPractical(row)"
                               type="number" min="0" [max]="grid.maxPractical" step="0.5"
                               [value]="row.practical ?? ''"
                               [disabled]="!grid.canEdit"
                               (input)="onNumber(row, 'practical', $event)"
                               (keydown)="onKey($event, i, 'practical')" />
                      </td>
                    }
                    <td>
                      <input class="cell"
                             [class.invalid]="invalidAssignment(row)"
                             type="number" min="0" [max]="grid.maxAssignment" step="0.5"
                             [value]="row.assignment ?? ''"
                             [disabled]="!grid.canEdit"
                             (input)="onNumber(row, 'assignment', $event)"
                             (keydown)="onKey($event, i, 'assignment')" />
                    </td>
                    <td>
                      <select class="cell"
                              [disabled]="!grid.canEdit"
                              [value]="row.attendanceStatus"
                              (change)="onAttendance(row, $event)"
                              (keydown)="onKey($event, i, 'attendanceStatus')">
                        @for (status of attendanceStatuses; track status) {
                          <option [value]="status">{{ attendanceLabel(status) | translate }}</option>
                        }
                      </select>
                    </td>
                    <td>
                      <input class="cell remarks"
                             type="text" maxlength="500"
                             [value]="row.remarks"
                             [disabled]="!grid.canEdit"
                             (input)="onRemarks(row, $event)"
                             (keydown)="onKey($event, i, 'remarks')" />
                    </td>
                    <td class="computed">{{ formatNum(row.total) }}</td>
                    <td class="computed">{{ formatNum(row.percentage) }}{{ row.percentage == null ? '' : '%' }}</td>
                    <td class="computed strong">{{ row.gradeLabel || '—' }}</td>
                  </tr>
                } @empty {
                  <tr><td [attr.colspan]="showPractical ? 11 : 10" class="center">{{ 'common.noData' | translate }}</td></tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      } @else if (loading) {
        <p class="muted">{{ 'common.loading' | translate }}</p>
      }
    </div>
  `,
  styles: `
    .page-header { margin-bottom: 16px; }
    h2 { font-size: 1.2rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); margin: 0; }
    .banner { padding: 10px 14px; border-radius: 8px; margin: 0 0 12px; font-size: .9rem; }
    .banner.success { background: #dcfce7; color: #15803d; }
    .banner.error { background: #fee2e2; color: #b91c1c; }
    .toolbar-card, .actions-card { margin-bottom: 12px; }
    .toolbar, .actions, .lock-row { display: flex; gap: 12px; flex-wrap: wrap; padding: 14px 16px; align-items: flex-end; }
    .lock-row { border-top: 1px solid var(--color-border); align-items: center; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field label, .lock-row label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    select, input[type="date"] {
      padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; min-width: 140px;
    }
    .meta { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin-bottom: 12px; }
    .chip { padding: 6px 12px; border-radius: 20px; background: #fff; border: 1px solid var(--color-border); font-size: .82rem; }
    .table-wrap { overflow-x: auto; }
    table.sheet { width: 100%; border-collapse: collapse; min-width: 980px; }
    th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid var(--color-border); font-size: .9rem; }
    th { color: var(--color-muted); font-weight: 600; font-size: .75rem; text-transform: uppercase; background: var(--color-bg); }
    .strong { font-weight: 600; }
    .center { text-align: center; color: var(--color-muted); padding: 28px; }
    .computed { font-variant-numeric: tabular-nums; }
    .cell {
      width: 88px; padding: 6px 8px; border: 1px solid var(--color-border); border-radius: 6px; font: inherit;
    }
    .cell.remarks { width: 160px; }
    select.cell { width: 118px; }
    .cell:focus { outline: 2px solid var(--color-primary); border-color: var(--color-primary); }
    .cell.invalid { border-color: var(--color-danger); background: #fef2f2; color: var(--color-danger); }
    .file-btn { cursor: pointer; }
    .badge { margin-left: 4px; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MarksEntryComponent implements OnInit {
  readonly attendanceStatuses = ATTENDANCE;
  readonly classControl = new FormControl('');
  readonly sectionControl = new FormControl('');
  readonly subjectControl = new FormControl('');
  readonly termControl = new FormControl('TERM');
  readonly deadlineControl = new FormControl('');

  options: MarksOptions | null = null;
  grid: MarksGrid | null = null;
  loading = false;
  busy = false;
  pageError = '';
  successMessage = '';

  @ViewChild('gridWrap') gridWrap?: ElementRef<HTMLElement>;
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  get selectedClass(): ClassOption | undefined {
    return this.options?.classes.find((klass) => klass.id === this.classControl.value);
  }

  get showPractical(): boolean {
    return !!this.grid && (this.grid.practicalEnabled || this.grid.maxPractical > 0);
  }

  get hasInvalid(): boolean {
    return (this.grid?.rows ?? []).some((row) =>
      this.invalidTheory(row) || this.invalidPractical(row) || this.invalidAssignment(row));
  }

  ngOnInit(): void {
    this.http.get<ApiResponse<MarksOptions>>('/api/exam-marks/options').subscribe({
      next: (res) => {
        this.options = res.data;
        const first = this.options.classes[0];
        if (first) {
          this.classControl.setValue(first.id, { emitEvent: false });
          this.sectionControl.setValue(first.sections[0]?.id ?? '', { emitEvent: false });
          this.subjectControl.setValue(first.subjects[0]?.id ?? '', { emitEvent: false });
          this.termControl.setValue(this.options.examTerms.includes('TERM') ? 'TERM' : this.options.examTerms[0], {
            emitEvent: false,
          });
          this.reloadGrid();
        }
        this.cdr.markForCheck();
      },
      error: (err) => this.fail(err),
    });
  }

  onClassChange(): void {
    const klass = this.selectedClass;
    this.sectionControl.setValue(klass?.sections[0]?.id ?? '', { emitEvent: false });
    this.subjectControl.setValue(klass?.subjects[0]?.id ?? '', { emitEvent: false });
    this.reloadGrid();
  }

  reloadGrid(): void {
    const params = this.filterParams();
    if (!params) {
      return;
    }
    this.loading = true;
    this.pageError = '';
    this.http.get<ApiResponse<MarksGrid>>('/api/exam-marks', { params }).subscribe({
      next: (res) => {
        this.applyGrid(res.data);
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loading = false;
        this.fail(err);
      },
    });
  }

  onNumber(row: MarkRow, field: 'theory' | 'practical' | 'assignment', event: Event): void {
    const raw = (event.target as HTMLInputElement).value;
    row[field] = raw === '' ? null : Number(raw);
    this.recompute(row);
  }

  onAttendance(row: MarkRow, event: Event): void {
    row.attendanceStatus = (event.target as HTMLSelectElement).value as MarkRow['attendanceStatus'];
  }

  onRemarks(row: MarkRow, event: Event): void {
    row.remarks = (event.target as HTMLInputElement).value;
  }

  invalidTheory(row: MarkRow): boolean {
    return this.outOfRange(row.theory, this.grid?.maxTheory ?? 0);
  }

  invalidPractical(row: MarkRow): boolean {
    return this.outOfRange(row.practical, this.grid?.maxPractical ?? 0);
  }

  invalidAssignment(row: MarkRow): boolean {
    return this.outOfRange(row.assignment, this.grid?.maxAssignment ?? 0);
  }

  formatNum(value: number | null): string {
    return value == null ? '—' : value.toFixed(2).replace(/\.00$/, '');
  }

  termLabel(term: string): string {
    return `academics.exam${term.charAt(0)}${term.slice(1).toLowerCase()}`;
  }

  attendanceLabel(status: string): string {
    return `examinations.marksPage.${status.toLowerCase()}`;
  }

  onKey(event: KeyboardEvent, rowIndex: number, field: EditableField): void {
    const fields = this.visibleFields();
    const col = fields.indexOf(field);
    if (col < 0) {
      return;
    }
    let nextRow = rowIndex;
    let nextCol = col;
    if (event.key === 'Tab') {
      event.preventDefault();
      nextCol = event.shiftKey ? col - 1 : col + 1;
      if (nextCol < 0) {
        nextCol = fields.length - 1;
        nextRow = rowIndex - 1;
      } else if (nextCol >= fields.length) {
        nextCol = 0;
        nextRow = rowIndex + 1;
      }
    } else if (event.key === 'Enter') {
      event.preventDefault();
      nextRow = event.shiftKey ? rowIndex - 1 : rowIndex + 1;
    } else if (event.key === 'ArrowDown') {
      event.preventDefault();
      nextRow = rowIndex + 1;
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      nextRow = rowIndex - 1;
    } else if (event.key === 'ArrowRight' && (event.target as HTMLInputElement).type !== 'text') {
      event.preventDefault();
      nextCol = col + 1;
    } else if (event.key === 'ArrowLeft' && (event.target as HTMLInputElement).type !== 'text') {
      event.preventDefault();
      nextCol = col - 1;
    } else {
      return;
    }
    const rows = this.grid?.rows.length ?? 0;
    if (nextRow < 0 || nextRow >= rows || nextCol < 0 || nextCol >= fields.length) {
      return;
    }
    this.focusCell(nextRow, fields[nextCol]);
  }

  save(submit: boolean): void {
    if (!this.grid || this.hasInvalid) {
      return;
    }
    this.busy = true;
    this.pageError = '';
    this.successMessage = '';
    const body = {
      academicYearId: this.grid.academicYearId,
      classId: this.grid.classId,
      sectionId: this.grid.sectionId,
      subjectId: this.grid.subjectId,
      examTerm: this.grid.examTerm,
      submit,
      rows: this.grid.rows.map((row) => ({
        studentId: row.studentId,
        theory: row.theory,
        practical: row.practical,
        assignment: row.assignment,
        attendanceStatus: row.attendanceStatus,
        remarks: row.remarks,
      })),
    };
    this.http.post<ApiResponse<MarksGrid>>('/api/exam-marks', body).subscribe({
      next: (res) => {
        this.applyGrid(res.data);
        this.busy = false;
        this.successMessage = submit ? 'examinations.marksPage.submitted' : 'examinations.marksPage.draftSaved';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.busy = false;
        this.fail(err);
      },
    });
  }

  setLock(locked: boolean): void {
    if (!this.grid) {
      return;
    }
    this.busy = true;
    this.pageError = '';
    this.http.put<ApiResponse<MarksGrid>>('/api/exam-marks/lock', {
      academicYearId: this.grid.academicYearId,
      classId: this.grid.classId,
      sectionId: this.grid.sectionId,
      subjectId: this.grid.subjectId,
      examTerm: this.grid.examTerm,
      locked,
      entryDeadline: this.deadlineControl.value || null,
    }).subscribe({
      next: (res) => {
        this.applyGrid(res.data);
        this.busy = false;
        this.successMessage = locked ? 'examinations.marksPage.lockSuccess' : 'examinations.marksPage.unlockSuccess';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.busy = false;
        this.fail(err);
      },
    });
  }

  downloadTemplate(format: 'xlsx' | 'csv'): void {
    const params = this.filterParams();
    if (!params) {
      return;
    }
    this.busy = true;
    this.http.get(`/api/exam-marks/template`, {
      params: params.set('format', format),
      responseType: 'blob',
    }).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = format === 'csv' ? 'marks-template.csv' : 'marks-template.xlsx';
        link.click();
        URL.revokeObjectURL(url);
        this.busy = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.busy = false;
        this.fail(err);
      },
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    const params = this.filterParams();
    if (!file || !params) {
      return;
    }
    const body = new FormData();
    body.append('file', file);
    this.busy = true;
    this.pageError = '';
    this.successMessage = '';
    this.http.post<ApiResponse<ImportResult>>('/api/exam-marks/import', body, { params }).subscribe({
      next: (res) => {
        this.applyGrid(res.data.grid);
        this.busy = false;
        if (res.data.errors.length > 0) {
          this.pageError = res.data.errors.join(' | ');
        } else {
          this.successMessage = 'examinations.marksPage.imported';
        }
        if (this.fileInput) {
          this.fileInput.nativeElement.value = '';
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.busy = false;
        if (this.fileInput) {
          this.fileInput.nativeElement.value = '';
        }
        this.fail(err);
      },
    });
  }

  private applyGrid(grid: MarksGrid): void {
    this.grid = {
      ...grid,
      rows: grid.rows.map((row) => {
        const next = { ...row, remarks: row.remarks ?? '' };
        this.recompute(next);
        return next;
      }),
    };
    this.deadlineControl.setValue(grid.entryDeadline ?? '', { emitEvent: false });
  }

  private recompute(row: MarkRow): void {
    const theory = row.theory ?? 0;
    const practical = row.practical ?? 0;
    const assignment = row.assignment ?? 0;
    const entered = row.theory != null || row.practical != null || row.assignment != null;
    if (!entered) {
      row.total = null;
      row.percentage = null;
      row.gradeLabel = null;
      return;
    }
    const total = theory + practical + assignment;
    const maxTotal = this.grid?.maxTotal || 0;
    const percent = maxTotal === 0 ? 0 : Math.round((total * 10000) / maxTotal) / 100;
    row.total = Math.round(total * 100) / 100;
    row.percentage = percent;
    row.gradeLabel = this.letterGrade(percent);
  }

  private letterGrade(percent: number): string | null {
    const boundaries = this.grid?.boundaries ?? [];
    for (const boundary of boundaries) {
      if (percent >= boundary.minPercent && percent <= boundary.maxPercent) {
        return boundary.label;
      }
    }
    return boundaries.length ? boundaries[boundaries.length - 1].label : null;
  }

  private outOfRange(value: number | null, max: number): boolean {
    return value != null && (Number.isNaN(value) || value < 0 || value > max);
  }

  private visibleFields(): EditableField[] {
    return this.showPractical
      ? FIELD_ORDER
      : FIELD_ORDER.filter((field) => field !== 'practical');
  }

  private focusCell(rowIndex: number, field: EditableField): void {
    const root = this.gridWrap?.nativeElement;
    if (!root) {
      return;
    }
    const rows = root.querySelectorAll('tbody tr');
    const row = rows.item(rowIndex);
    if (!row) {
      return;
    }
    const offset = this.showPractical ? 0 : field === 'assignment' || field === 'attendanceStatus' || field === 'remarks' ? -1 : 0;
    const indexMap: Record<EditableField, number> = {
      theory: 3,
      practical: 4,
      assignment: 5 + offset,
      attendanceStatus: 6 + offset,
      remarks: 7 + offset,
    };
    const cell = row.querySelectorAll('td').item(indexMap[field]);
    const input = cell?.querySelector('input,select') as HTMLElement | null;
    input?.focus();
    if (input instanceof HTMLInputElement) {
      input.select();
    }
  }

  private filterParams(): HttpParams | null {
    const yearId = this.options?.academicYears.find((year) => year.current)?.id
      ?? this.options?.academicYears[0]?.id;
    const classId = this.classControl.value;
    const sectionId = this.sectionControl.value;
    const subjectId = this.subjectControl.value;
    const examTerm = this.termControl.value;
    if (!yearId || !classId || !sectionId || !subjectId || !examTerm) {
      return null;
    }
    return new HttpParams()
      .set('academicYearId', yearId)
      .set('classId', classId)
      .set('sectionId', sectionId)
      .set('subjectId', subjectId)
      .set('examTerm', examTerm);
  }

  private fail(err: HttpErrorResponse): void {
    const body = err.error as ApiError | undefined;
    this.pageError = body?.message || err.message || 'Error';
    this.cdr.markForCheck();
  }
}
