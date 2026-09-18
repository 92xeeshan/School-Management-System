import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { ApiError, ApiResponse } from '../../core/models/api.model';

interface YearOption { id: string; name: string; current: boolean }
interface SectionOption { id: string; name: string; room: string | null }
interface ClassOption { id: string; name: string; sections: SectionOption[] }
interface SubjectOption { id: string; name: string }
interface TeacherOption { id: string; displayName: string }
interface ScheduleOptions {
  academicYears: YearOption[];
  classes: ClassOption[];
  subjects: SubjectOption[];
  teachers: TeacherOption[];
  rooms: string[];
  examTerms: string[];
}
interface InvigilatorRef { id: string; displayName: string }
interface ExamSchedule {
  id: string;
  academicYearId: string;
  academicYearName: string;
  classId: string;
  className: string;
  sectionId: string;
  sectionName: string;
  subjectId: string;
  subjectName: string;
  examTerm: string;
  examDate: string;
  startTime: string;
  endTime: string;
  room: string | null;
  maxMarks: number;
  passMarks: number;
  status: 'DRAFT' | 'PUBLISHED';
  invigilators: InvigilatorRef[];
}
interface ScheduleConflict { field: string; code: string; conflictingScheduleId: string }
interface ValidateResponse { valid: boolean; conflicts: ScheduleConflict[] }

@Component({
  selector: 'app-exam-schedules',
  imports: [TranslateModule, ReactiveFormsModule],
  template: `
    <div class="tab-page">
      <div class="page-header">
        <div>
          <h2>{{ 'examinations.schedulesPage.title' | translate }}</h2>
          <p class="muted">{{ 'examinations.schedulesPage.subtitle' | translate }}</p>
        </div>
        <button class="btn btn-primary" type="button" (click)="openAddModal()">
          {{ 'examinations.schedulesPage.add' | translate }}
        </button>
      </div>

      @if (successMessage) {
        <p class="banner success">{{ successMessage | translate }}</p>
      }
      @if (pageError) {
        <p class="banner error">{{ pageError }}</p>
      }
      @if (conflictCount > 0) {
        <p class="banner error">{{ 'examinations.schedulesPage.conflictBanner' | translate:{ count: conflictCount } }}</p>
      }

      <div class="filters">
        <select [value]="yearId" (change)="onYearChange($event)">
          @for (year of options?.academicYears ?? []; track year.id) {
            <option [value]="year.id">{{ year.name }}</option>
          }
        </select>
        <select [value]="classId" (change)="onClassFilter($event)">
          <option value="">{{ 'examinations.schedulesPage.allClasses' | translate }}</option>
          @for (klass of options?.classes ?? []; track klass.id) {
            <option [value]="klass.id">{{ klass.name }}</option>
          }
        </select>
        <select [value]="sectionId" (change)="onSectionFilter($event)" [disabled]="!classId">
          <option value="">{{ 'examinations.schedulesPage.allSections' | translate }}</option>
          @for (section of filterSections; track section.id) {
            <option [value]="section.id">{{ section.name }}</option>
          }
        </select>
        <select [value]="room" (change)="onRoomFilter($event)">
          <option value="">{{ 'examinations.schedulesPage.allRooms' | translate }}</option>
          @for (item of rooms; track item) {
            <option [value]="item">{{ item }}</option>
          }
        </select>
      </div>

      <div class="week-bar">
        <button class="btn" type="button" (click)="shiftWeek(-7)">{{ 'examinations.schedulesPage.prevWeek' | translate }}</button>
        <strong>{{ weekLabel }}</strong>
        <button class="btn" type="button" (click)="shiftWeek(7)">{{ 'examinations.schedulesPage.nextWeek' | translate }}</button>
        <button class="btn" type="button" (click)="goThisWeek()">{{ 'examinations.schedulesPage.thisWeek' | translate }}</button>
      </div>

      <div class="card grid-card">
        <div class="table-wrap">
          <table class="grid">
            <thead>
              <tr>
                @for (day of weekDays; track day.iso) {
                  <th>{{ day.label }}</th>
                }
              </tr>
            </thead>
            <tbody>
              <tr>
                @for (day of weekDays; track day.iso) {
                  <td class="day-cell" [class.clickable]="true" (click)="openAddForDate(day.iso)">
                    @for (entry of entriesFor(day.iso); track entry.id) {
                      <button class="slot" type="button"
                              [class.draft]="entry.status === 'DRAFT'"
                              [class.conflict]="isConflict(entry)"
                              (click)="$event.stopPropagation(); openEditModal(entry)">
                        <div class="strong">{{ entry.subjectName }}</div>
                        <div>{{ entry.className }} {{ entry.sectionName }}</div>
                        <div class="muted">{{ formatTime(entry.startTime) }}–{{ formatTime(entry.endTime) }}</div>
                        <div class="muted">{{ entry.room || '—' }}</div>
                        <span class="badge" [class.badge-success]="entry.status === 'PUBLISHED'" [class.badge-muted]="entry.status !== 'PUBLISHED'">
                          {{ statusLabel(entry.status) | translate }}
                        </span>
                      </button>
                    } @empty {
                      <span class="muted empty">{{ 'examinations.schedulesPage.emptyWeek' | translate }}</span>
                    }
                  </td>
                }
              </tr>
            </tbody>
          </table>
        </div>
        @if (loading) {
          <p class="center muted">{{ 'common.loading' | translate }}</p>
        }
      </div>
    </div>

    @if (showModal) {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editingId ? ('examinations.schedulesPage.edit' | translate) : ('examinations.schedulesPage.add' | translate) }}</h2>
          @if (formError) {
            <p class="form-error">{{ formError }}</p>
          }
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="form-grid">
              <div class="field">
                <label>{{ 'examinations.schedulesPage.academicTerm' | translate }} *</label>
                <select formControlName="academicYearId">
                  @for (year of options?.academicYears ?? []; track year.id) {
                    <option [value]="year.id">{{ year.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'examinations.schedulesPage.class' | translate }} *</label>
                <select formControlName="classId" (change)="onFormClassChange()">
                  <option value="">{{ 'examinations.schedulesPage.selectClass' | translate }}</option>
                  @for (klass of options?.classes ?? []; track klass.id) {
                    <option [value]="klass.id">{{ klass.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'examinations.schedulesPage.section' | translate }} *</label>
                <select formControlName="sectionId">
                  <option value="">{{ 'examinations.schedulesPage.selectSection' | translate }}</option>
                  @for (section of formSections; track section.id) {
                    <option [value]="section.id">{{ section.name }}</option>
                  }
                </select>
                @if (fieldError('sectionId'); as sectionErr) {
                  <span class="field-error">{{ sectionErr | translate }}</span>
                }
              </div>
              <div class="field">
                <label>{{ 'examinations.schedulesPage.subject' | translate }} *</label>
                <select formControlName="subjectId">
                  <option value="">{{ 'examinations.schedulesPage.selectSubject' | translate }}</option>
                  @for (subject of options?.subjects ?? []; track subject.id) {
                    <option [value]="subject.id">{{ subject.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'examinations.marksPage.examTerm' | translate }} *</label>
                <select formControlName="examTerm">
                  @for (term of options?.examTerms ?? []; track term) {
                    <option [value]="term">{{ termLabel(term) | translate }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'examinations.schedulesPage.examDate' | translate }} *</label>
                <input type="date" formControlName="examDate" />
              </div>
              <div class="field">
                <label>{{ 'examinations.schedulesPage.startTime' | translate }} *</label>
                <input type="time" formControlName="startTime" />
              </div>
              <div class="field">
                <label>{{ 'examinations.schedulesPage.endTime' | translate }} *</label>
                <input type="time" formControlName="endTime" />
              </div>
              <div class="field">
                <label>{{ 'examinations.schedulesPage.room' | translate }}</label>
                <input type="text" formControlName="room" list="exam-rooms" />
                <datalist id="exam-rooms">
                  @for (item of rooms; track item) {
                    <option [value]="item"></option>
                  }
                </datalist>
                @if (fieldError('room'); as roomErr) {
                  <span class="field-error">{{ roomErr | translate }}</span>
                }
              </div>
              <div class="field">
                <label>{{ 'examinations.schedulesPage.maxMarks' | translate }} *</label>
                <input type="number" min="1" step="0.5" formControlName="maxMarks" />
              </div>
              <div class="field">
                <label>{{ 'examinations.schedulesPage.passMarks' | translate }} *</label>
                <input type="number" min="0" step="0.5" formControlName="passMarks" />
              </div>
              <div class="field">
                <label>{{ 'examinations.schedulesPage.status' | translate }} *</label>
                <select formControlName="status">
                  <option value="DRAFT">{{ 'examinations.schedulesPage.draft' | translate }}</option>
                  <option value="PUBLISHED">{{ 'examinations.schedulesPage.published' | translate }}</option>
                </select>
              </div>
              <div class="field field-span">
                <label>{{ 'examinations.schedulesPage.invigilators' | translate }}</label>
                <div class="chip-list">
                  @for (teacher of options?.teachers ?? []; track teacher.id) {
                    <label class="chip">
                      <input type="checkbox" [checked]="isInvigilatorSelected(teacher.id)"
                             (change)="toggleInvigilator(teacher.id, $event)" />
                      {{ teacher.displayName }}
                    </label>
                  }
                </div>
                @if (fieldError('invigilatorIds'); as invigilatorErr) {
                  <span class="field-error">{{ invigilatorErr | translate }}</span>
                }
              </div>
            </div>
            <div class="form-actions">
              @if (editingId) {
                <button class="btn btn-danger" type="button" (click)="askDelete()" [disabled]="saving">{{ 'common.delete' | translate }}</button>
                @if (form.value.status === 'DRAFT') {
                  <button class="btn" type="button" (click)="setStatus('PUBLISHED')" [disabled]="saving">
                    {{ 'examinations.schedulesPage.publish' | translate }}
                  </button>
                } @else {
                  <button class="btn" type="button" (click)="setStatus('DRAFT')" [disabled]="saving">
                    {{ 'examinations.schedulesPage.unpublish' | translate }}
                  </button>
                }
              }
              <span class="spacer"></span>
              <button class="btn" type="button" (click)="closeModal()" [disabled]="saving">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving || hasFieldConflicts">
                {{ saving ? ('common.loading' | translate) : ('common.save' | translate) }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    @if (pendingDelete) {
      <div class="modal-backdrop" (click)="pendingDelete = false; cdr.markForCheck()">
        <div class="modal modal-sm" (click)="$event.stopPropagation()">
          <h2>{{ 'common.delete' | translate }}</h2>
          <p class="confirm-text">{{ 'examinations.schedulesPage.deleteConfirm' | translate }}</p>
          <div class="form-actions">
            <button class="btn" type="button" (click)="pendingDelete = false; cdr.markForCheck()">{{ 'common.cancel' | translate }}</button>
            <button class="btn btn-danger" type="button" [disabled]="deleting" (click)="confirmDelete()">
              {{ deleting ? ('common.loading' | translate) : ('common.delete' | translate) }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; gap: 12px; flex-wrap: wrap; }
    h2 { font-size: 1.2rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); margin: 0; }
    .filters, .week-bar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; align-items: center; }
    .filters select { padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; background: #fff; min-width: 160px; }
    .table-wrap { overflow-x: auto; }
    table.grid { width: 100%; border-collapse: collapse; min-width: 840px; }
    th, td { text-align: left; padding: 10px; border: 1px solid var(--color-border); font-size: .85rem; vertical-align: top; }
    thead th { background: var(--color-bg); font-size: .75rem; letter-spacing: .03em; color: var(--color-muted); }
    td.day-cell { min-width: 140px; min-height: 180px; }
    td.clickable { cursor: pointer; }
    td.clickable:hover { background: #f8fafc; }
    .slot {
      display: flex; flex-direction: column; gap: 2px; width: 100%; text-align: left;
      margin-bottom: 8px; padding: 8px; border-radius: 8px; border: 1px solid var(--color-border);
      background: #ecfdf5; font: inherit; cursor: pointer;
    }
    .slot.draft { background: #fff7ed; }
    .slot.conflict { background: #fef2f2; box-shadow: inset 0 0 0 1px #fecaca; }
    .strong { font-weight: 600; }
    .empty { display: block; padding: 18px 0; text-align: center; }
    .banner { padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
    .banner.success { background: #ecfdf5; border: 1px solid #a7f3d0; color: #047857; }
    .banner.error { background: #fef2f2; border: 1px solid #fecaca; color: #b91c1c; }
    .badge { display: inline-block; margin-top: 4px; padding: 2px 8px; border-radius: 999px; font-size: .7rem; font-weight: 600; }
    .badge-success { background: #d1fae5; color: #047857; }
    .badge-muted { background: #e2e8f0; color: #475569; }
    .center { text-align: center; padding: 12px; }
    .modal-backdrop {
      position: fixed; inset: 0; z-index: 100;
      background: rgba(15, 23, 42, .5);
      display: flex; align-items: flex-start; justify-content: center;
      padding: 40px 16px; overflow-y: auto;
    }
    .modal { background: #fff; border-radius: var(--radius); padding: 24px; width: 720px; max-width: 100%; box-shadow: 0 20px 50px rgba(0,0,0,.25); }
    .modal-sm { width: 420px; }
    .modal h2 { margin: 0 0 18px; font-size: 1.2rem; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field-span { grid-column: 1 / -1; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    input, select { padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; }
    .chip-list { display: flex; flex-wrap: wrap; gap: 8px; }
    .chip { display: flex; align-items: center; gap: 6px; padding: 6px 10px; border: 1px solid var(--color-border); border-radius: 999px; font-size: .82rem; }
    .form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; align-items: center; flex-wrap: wrap; }
    .spacer { flex: 1; }
    .form-error, .field-error { color: #b91c1c; font-size: .85rem; }
    .confirm-text { margin: 0 0 8px; color: var(--color-muted); line-height: 1.45; }
    .btn-danger { background: #b91c1c; border-color: #b91c1c; color: #fff; }
    @media (max-width: 640px) { .form-grid { grid-template-columns: 1fr; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamSchedulesComponent implements OnInit, OnDestroy {
  options: ScheduleOptions | null = null;
  schedules: ExamSchedule[] = [];
  yearId = '';
  classId = '';
  sectionId = '';
  room = '';
  weekStart = this.startOfWeek(new Date());
  private weekSnapped = false;
  loading = false;
  pageError = '';
  successMessage = '';
  showModal = false;
  saving = false;
  formError = '';
  editingId: string | null = null;
  pendingDelete = false;
  deleting = false;
  fieldConflicts: Record<string, string> = {};
  overlapIds = new Set<string>();
  private readonly destroy$ = new Subject<void>();

  readonly form = new FormGroup({
    academicYearId: new FormControl('', Validators.required),
    classId: new FormControl('', Validators.required),
    sectionId: new FormControl('', Validators.required),
    subjectId: new FormControl('', Validators.required),
    examTerm: new FormControl('TERM', Validators.required),
    examDate: new FormControl('', Validators.required),
    startTime: new FormControl('09:00', Validators.required),
    endTime: new FormControl('11:00', Validators.required),
    room: new FormControl(''),
    maxMarks: new FormControl(100, Validators.required),
    passMarks: new FormControl(33, Validators.required),
    invigilatorIds: new FormControl<string[]>([]),
    status: new FormControl('DRAFT', Validators.required),
  });

  constructor(private http: HttpClient, public cdr: ChangeDetectorRef) {}

  get filterSections(): SectionOption[] {
    return this.options?.classes.find((klass) => klass.id === this.classId)?.sections ?? [];
  }

  get formSections(): SectionOption[] {
    return this.options?.classes.find((klass) => klass.id === this.form.value.classId)?.sections ?? [];
  }

  get rooms(): string[] {
    const values = new Set<string>(this.options?.rooms ?? []);
    for (const entry of this.schedules) {
      if (entry.room) {
        values.add(entry.room);
      }
    }
    return [...values].sort((a, b) => a.localeCompare(b));
  }

  get weekDays(): Array<{ iso: string; label: string }> {
    return Array.from({ length: 7 }, (_, index) => {
      const date = new Date(this.weekStart);
      date.setDate(this.weekStart.getDate() + index);
      return { iso: this.toIso(date), label: date.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' }) };
    });
  }

  get weekLabel(): string {
    const end = new Date(this.weekStart);
    end.setDate(this.weekStart.getDate() + 6);
    return `${this.weekStart.toLocaleDateString()} – ${end.toLocaleDateString()}`;
  }

  get conflictCount(): number {
    return this.overlapIds.size;
  }

  get hasFieldConflicts(): boolean {
    return Object.keys(this.fieldConflicts).length > 0;
  }

  ngOnInit(): void {
    this.form.valueChanges.pipe(
      debounceTime(250),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      takeUntil(this.destroy$),
    ).subscribe(() => this.validateLive());
    this.loadOptions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  termLabel(term: string): string {
    return `academics.exam${term.charAt(0)}${term.slice(1).toLowerCase()}`;
  }

  statusLabel(status: string): string {
    return status === 'PUBLISHED'
      ? 'examinations.schedulesPage.published'
      : 'examinations.schedulesPage.draft';
  }

  formatTime(value: string): string {
    return (value || '').slice(0, 5);
  }

  entriesFor(iso: string): ExamSchedule[] {
    return this.schedules
      .filter((entry) => entry.examDate === iso)
      .sort((a, b) => a.startTime.localeCompare(b.startTime));
  }

  isConflict(entry: ExamSchedule): boolean {
    return this.overlapIds.has(entry.id);
  }

  fieldError(field: string): string | null {
    return this.fieldConflicts[field] ?? null;
  }

  isInvigilatorSelected(id: string): boolean {
    return (this.form.value.invigilatorIds ?? []).includes(id);
  }

  toggleInvigilator(id: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const current = [...(this.form.value.invigilatorIds ?? [])];
    const next = checked ? [...current, id] : current.filter((item) => item !== id);
    this.form.patchValue({ invigilatorIds: next });
  }

  onYearChange(event: Event): void {
    this.yearId = (event.target as HTMLSelectElement).value;
    this.weekSnapped = false;
    this.reload();
  }

  onClassFilter(event: Event): void {
    this.classId = (event.target as HTMLSelectElement).value;
    this.sectionId = '';
    this.reload();
  }

  onSectionFilter(event: Event): void {
    this.sectionId = (event.target as HTMLSelectElement).value;
    this.reload();
  }

  onRoomFilter(event: Event): void {
    this.room = (event.target as HTMLSelectElement).value;
    this.reload();
  }

  shiftWeek(days: number): void {
    const next = new Date(this.weekStart);
    next.setDate(this.weekStart.getDate() + days);
    this.weekStart = this.startOfWeek(next);
    this.weekSnapped = true;
    this.cdr.markForCheck();
  }

  goThisWeek(): void {
    this.weekStart = this.startOfWeek(new Date());
    this.weekSnapped = true;
    this.cdr.markForCheck();
  }

  onFormClassChange(): void {
    this.form.patchValue({ sectionId: '' });
  }

  openAddModal(): void {
    this.openAddForDate(this.toIso(new Date()));
  }

  openAddForDate(iso: string): void {
    this.editingId = null;
    this.formError = '';
    this.fieldConflicts = {};
    this.form.reset({
      academicYearId: this.yearId,
      classId: this.classId,
      sectionId: this.sectionId,
      subjectId: '',
      examTerm: 'TERM',
      examDate: iso,
      startTime: '09:00',
      endTime: '11:00',
      room: this.room,
      maxMarks: 100,
      passMarks: 33,
      invigilatorIds: [],
      status: 'DRAFT',
    });
    this.showModal = true;
    this.cdr.markForCheck();
  }

  openEditModal(entry: ExamSchedule): void {
    this.editingId = entry.id;
    this.formError = '';
    this.fieldConflicts = {};
    this.form.reset({
      academicYearId: entry.academicYearId,
      classId: entry.classId,
      sectionId: entry.sectionId,
      subjectId: entry.subjectId,
      examTerm: entry.examTerm,
      examDate: entry.examDate,
      startTime: this.formatTime(entry.startTime),
      endTime: this.formatTime(entry.endTime),
      room: entry.room ?? '',
      maxMarks: entry.maxMarks,
      passMarks: entry.passMarks,
      invigilatorIds: entry.invigilators.map((item) => item.id),
      status: entry.status,
    });
    this.showModal = true;
    this.cdr.markForCheck();
  }

  closeModal(): void {
    this.showModal = false;
    this.formError = '';
    this.fieldConflicts = {};
    this.cdr.markForCheck();
  }

  onSubmit(): void {
    if (this.form.invalid || this.saving || this.hasFieldConflicts) {
      return;
    }
    this.saving = true;
    this.formError = '';
    const req$ = this.editingId
      ? this.http.put<ApiResponse<ExamSchedule>>(`/api/exam-schedules/${this.editingId}`, this.payload())
      : this.http.post<ApiResponse<ExamSchedule>>('/api/exam-schedules', this.payload());
    req$.subscribe({
      next: () => {
        this.saving = false;
        this.showModal = false;
        this.successMessage = this.editingId
          ? 'examinations.schedulesPage.updated'
          : 'examinations.schedulesPage.created';
        this.reload();
      },
      error: (err) => this.failForm(err),
    });
  }

  setStatus(status: 'DRAFT' | 'PUBLISHED'): void {
    if (!this.editingId || this.saving) {
      return;
    }
    this.saving = true;
    this.http.patch<ApiResponse<ExamSchedule>>(`/api/exam-schedules/${this.editingId}/status`, { status })
      .subscribe({
        next: (res) => {
          this.saving = false;
          this.form.patchValue({ status: res.data.status });
          this.successMessage = status === 'PUBLISHED'
            ? 'examinations.schedulesPage.publishedSuccess'
            : 'examinations.schedulesPage.unpublishedSuccess';
          this.reload();
        },
        error: (err) => this.failForm(err),
      });
  }

  askDelete(): void {
    this.pendingDelete = true;
    this.cdr.markForCheck();
  }

  confirmDelete(): void {
    if (!this.editingId) {
      return;
    }
    this.deleting = true;
    this.http.delete<ApiResponse<void>>(`/api/exam-schedules/${this.editingId}`).subscribe({
      next: () => {
        this.deleting = false;
        this.pendingDelete = false;
        this.showModal = false;
        this.successMessage = 'examinations.schedulesPage.deleted';
        this.reload();
      },
      error: (err) => {
        this.deleting = false;
        this.failForm(err);
      },
    });
  }

  private loadOptions(): void {
    this.http.get<ApiResponse<ScheduleOptions>>('/api/exam-schedules/options').subscribe({
      next: (res) => {
        this.options = res.data;
        this.yearId = res.data.academicYears.find((year) => year.current)?.id ?? res.data.academicYears[0]?.id ?? '';
        this.reload();
      },
      error: (err) => this.failPage(err),
    });
  }

  private reload(): void {
    if (!this.yearId) {
      return;
    }
    this.loading = true;
    this.pageError = '';
    let params = new HttpParams().set('academicYearId', this.yearId);
    if (this.classId) {
      params = params.set('classId', this.classId);
    }
    if (this.sectionId) {
      params = params.set('sectionId', this.sectionId);
    }
    if (this.room) {
      params = params.set('room', this.room);
    }
    this.http.get<ApiResponse<ExamSchedule[]>>('/api/exam-schedules', { params }).subscribe({
      next: (res) => {
        this.schedules = res.data ?? [];
        this.loading = false;
        this.snapWeekToSchedules();
        this.recomputeOverlaps();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loading = false;
        this.failPage(err);
      },
    });
  }

  private validateLive(): void {
    if (!this.showModal || this.form.invalid) {
      this.fieldConflicts = {};
      this.cdr.markForCheck();
      return;
    }
    let params = new HttpParams();
    if (this.editingId) {
      params = params.set('excludeId', this.editingId);
    }
    this.http.post<ApiResponse<ValidateResponse>>('/api/exam-schedules/validate', this.payload(), { params })
      .subscribe({
        next: (res) => {
          const next: Record<string, string> = {};
          for (const conflict of res.data.conflicts ?? []) {
            next[conflict.field] = this.conflictKey(conflict.code);
          }
          this.fieldConflicts = next;
          this.cdr.markForCheck();
        },
        error: () => undefined,
      });
  }

  private payload() {
    const value = this.form.getRawValue();
    return {
      academicYearId: value.academicYearId,
      classId: value.classId,
      sectionId: value.sectionId,
      subjectId: value.subjectId,
      examTerm: value.examTerm,
      examDate: value.examDate,
      startTime: this.withSeconds(value.startTime ?? '09:00'),
      endTime: this.withSeconds(value.endTime ?? '11:00'),
      room: value.room || null,
      maxMarks: Number(value.maxMarks),
      passMarks: Number(value.passMarks),
      invigilatorIds: value.invigilatorIds ?? [],
      status: value.status,
    };
  }

  private snapWeekToSchedules(): void {
    if (this.weekSnapped || !this.schedules.length) {
      return;
    }
    const inWeek = this.schedules.some((entry) => {
      const date = new Date(`${entry.examDate}T00:00:00`);
      const end = new Date(this.weekStart);
      end.setDate(this.weekStart.getDate() + 6);
      return date >= this.weekStart && date <= end;
    });
    if (!inWeek) {
      this.weekStart = this.startOfWeek(new Date(`${this.schedules[0].examDate}T00:00:00`));
    }
    this.weekSnapped = true;
  }

  private recomputeOverlaps(): void {
    const ids = new Set<string>();
    for (let i = 0; i < this.schedules.length; i += 1) {
      for (let j = i + 1; j < this.schedules.length; j += 1) {
        const a = this.schedules[i];
        const b = this.schedules[j];
        if (a.examDate !== b.examDate || !this.overlaps(a.startTime, a.endTime, b.startTime, b.endTime)) {
          continue;
        }
        if (a.sectionId === b.sectionId
            || (a.room && b.room && a.room.toLowerCase() === b.room.toLowerCase())
            || a.invigilators.some((left) => b.invigilators.some((right) => left.id === right.id))) {
          ids.add(a.id);
          ids.add(b.id);
        }
      }
    }
    this.overlapIds = ids;
  }

  private overlaps(startA: string, endA: string, startB: string, endB: string): boolean {
    return startA < endB && startB < endA;
  }

  private conflictKey(code: string): string {
    if (code === 'exam_schedule.room_overlap') {
      return 'examinations.schedulesPage.fieldRoomOverlap';
    }
    if (code === 'exam_schedule.section_overlap') {
      return 'examinations.schedulesPage.fieldSectionOverlap';
    }
    if (code === 'exam_schedule.invigilator_overlap') {
      return 'examinations.schedulesPage.fieldInvigilatorOverlap';
    }
    return 'examinations.schedulesPage.fieldSectionOverlap';
  }

  private failPage(err: HttpErrorResponse): void {
    const body = err.error as ApiError | undefined;
    this.pageError = body?.message || err.message || 'Error';
    this.cdr.markForCheck();
  }

  private failForm(err: HttpErrorResponse): void {
    this.saving = false;
    const body = err.error as ApiError | undefined;
    this.formError = body?.message || err.message || 'Error';
    if (body?.code) {
      const field = body.code.includes('room') ? 'room'
        : body.code.includes('invigilator') ? 'invigilatorIds'
          : body.code.includes('section') ? 'sectionId' : '';
      if (field) {
        this.fieldConflicts = { ...this.fieldConflicts, [field]: this.conflictKey(body.code) };
      }
    }
    this.cdr.markForCheck();
  }

  private startOfWeek(date: Date): Date {
    const next = new Date(date);
    const day = next.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    next.setDate(next.getDate() + diff);
    next.setHours(0, 0, 0, 0);
    return next;
  }

  private toIso(date: Date): string {
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }

  private withSeconds(value: string): string {
    return value.length === 5 ? `${value}:00` : value;
  }
}
