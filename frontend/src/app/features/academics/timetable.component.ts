import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ApiError, ApiResponse } from '../../core/models/api.model';
import { AuthService } from '../../core/auth/auth.service';

type TimetableView = 'class' | 'teacher' | 'room';

interface AcademicYear {
  id: string;
  name: string;
  current: boolean;
}

interface SchoolSection {
  id: string;
  name: string;
  room?: string | null;
}

interface SchoolClass {
  id: string;
  name: string;
  code: string;
  sections: SchoolSection[];
}

interface SubjectOption {
  id: string;
  name: string;
  code: string;
}

interface TeacherOption {
  id: string;
  firstName: string;
  lastName: string;
  displayName: string;
  status: string;
}

interface TimetableEntry {
  id: string;
  sectionId: string;
  sectionName: string;
  className: string;
  academicYearId: string;
  dayOfWeek: number;
  periodNumber: number;
  startTime: string;
  endTime: string;
  subjectId: string | null;
  subjectName: string | null;
  teacherId: string | null;
  teacherName: string | null;
  room: string | null;
}

interface PeriodDef {
  number: number;
  start: string;
  end: string;
}

const PERIODS: PeriodDef[] = [
  { number: 1, start: '09:00', end: '09:45' },
  { number: 2, start: '09:45', end: '10:30' },
  { number: 3, start: '10:45', end: '11:30' },
  { number: 4, start: '11:30', end: '12:15' },
  { number: 5, start: '13:00', end: '13:45' },
  { number: 6, start: '13:45', end: '14:30' },
  { number: 7, start: '14:45', end: '15:30' },
  { number: 8, start: '15:30', end: '16:15' },
];

const DAYS = [1, 2, 3, 4, 5, 6];

@Component({
  selector: 'app-academics-timetable',
  imports: [TranslateModule, ReactiveFormsModule],
  template: `
    <div class="tab-page">
      <div class="page-header">
        <div>
          <h2>{{ 'academics.timetablePage.title' | translate }}</h2>
          <p class="muted">{{ 'academics.timetablePage.subtitle' | translate }}</p>
        </div>
        @if (canManage) {
          <div class="header-actions">
            <button class="btn btn-primary" type="button" (click)="openAddModal()">{{ 'academics.addPeriod' | translate }}</button>
          </div>
        }
      </div>

      @if (successMessage) {
        <p class="banner success">{{ successMessage | translate }}</p>
      }
      @if (pageError) {
        <p class="banner error">{{ pageError }}</p>
      }
      @if (conflictCount > 0) {
        <p class="banner error">{{ 'academics.conflictBanner' | translate:{ count: conflictCount } }}</p>
      }

      <div class="view-tabs">
        <button type="button" class="view-tab" [class.active]="view === 'class'" (click)="setView('class')">{{ 'academics.viewClass' | translate }}</button>
        <button type="button" class="view-tab" [class.active]="view === 'teacher'" (click)="setView('teacher')">{{ 'academics.viewTeacher' | translate }}</button>
        <button type="button" class="view-tab" [class.active]="view === 'room'" (click)="setView('room')">{{ 'academics.viewRoom' | translate }}</button>
      </div>

      <div class="filters">
        @if (view === 'class') {
          <select [value]="classId" (change)="onClassChange($event)">
            <option value="">{{ 'academics.selectClass' | translate }}</option>
            @for (klass of classes; track klass.id) {
              <option [value]="klass.id">{{ klass.name }}</option>
            }
          </select>
          <select [value]="sectionId" (change)="onSectionChange($event)" [disabled]="!classId">
            <option value="">{{ 'academics.selectSection' | translate }}</option>
            @for (section of classSections; track section.id) {
              <option [value]="section.id">{{ section.name }}</option>
            }
          </select>
        }
        @if (view === 'teacher') {
          <select [value]="teacherId" (change)="onTeacherChange($event)">
            <option value="">{{ 'academics.selectSubjectTeacher' | translate }}</option>
            @for (teacher of teachers; track teacher.id) {
              <option [value]="teacher.id">{{ teacherLabel(teacher) }}</option>
            }
          </select>
        }
        @if (view === 'room') {
          <select [value]="room" (change)="onRoomChange($event)">
            <option value="">{{ 'academics.selectRoom' | translate }}</option>
            @for (item of rooms; track item) {
              <option [value]="item">{{ item }}</option>
            }
          </select>
        }
      </div>

      @if (!hasSelection) {
        <div class="card empty-card">{{ promptKey | translate }}</div>
      } @else {
        <div class="card grid-card">
          <div class="table-wrap">
            <table class="grid">
              <thead>
                <tr>
                  <th>{{ 'academics.period' | translate }}</th>
                  @for (day of days; track day) {
                    <th>{{ dayKey(day) | translate }}</th>
                  }
                </tr>
              </thead>
              <tbody>
                @for (period of periods; track period.number) {
                  <tr>
                    <th>
                      <div>{{ 'academics.periodN' | translate:{ n: period.number } }}</div>
                      <div class="muted time">{{ period.start }}–{{ period.end }}</div>
                    </th>
                    @for (day of days; track day) {
                      <td [class.conflict]="isConflict(day, period.number)" [class.clickable]="canManage" (click)="onCellClick(day, period)">
                        @if (cell(day, period.number); as entry) {
                          <div class="slot">
                            <div class="strong">{{ entry.subjectName || ('academics.noSubject' | translate) }}</div>
                            @if (view !== 'class') {
                              <div>{{ entry.className }} {{ entry.sectionName }}</div>
                            }
                            @if (view !== 'teacher') {
                              <div class="muted">{{ entry.teacherName || '—' }}</div>
                            }
                            @if (view !== 'room') {
                              <div class="muted">{{ entry.room || '—' }}</div>
                            }
                          </div>
                        } @else if (view === 'room') {
                          <span class="available">{{ 'academics.available' | translate }}</span>
                        } @else {
                          <span class="muted">—</span>
                        }
                      </td>
                    }
                  </tr>
                }
              </tbody>
            </table>
          </div>
          @if (loading) {
            <p class="center muted">{{ 'common.loading' | translate }}</p>
          }
        </div>
      }
    </div>

    @if (showModal) {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editingEntry ? ('academics.editPeriod' | translate) : ('academics.addPeriod' | translate) }}</h2>
          @if (formError) {
            <p class="form-error">{{ formError }}</p>
          }
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="form-grid">
              <div class="field">
                <label>{{ 'academics.className' | translate }} *</label>
                <select formControlName="classId" (change)="onFormClassChange()">
                  <option value="">{{ 'academics.selectClass' | translate }}</option>
                  @for (klass of classes; track klass.id) {
                    <option [value]="klass.id">{{ klass.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'academics.sections' | translate }} *</label>
                <select formControlName="sectionId">
                  <option value="">{{ 'academics.selectSection' | translate }}</option>
                  @for (section of formSections; track section.id) {
                    <option [value]="section.id">{{ section.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'academics.day' | translate }} *</label>
                <select formControlName="dayOfWeek">
                  @for (day of days; track day) {
                    <option [value]="day">{{ dayKey(day) | translate }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'academics.period' | translate }} *</label>
                <select formControlName="periodNumber" (change)="onPeriodChange()">
                  @for (period of periods; track period.number) {
                    <option [value]="period.number">{{ 'academics.periodN' | translate:{ n: period.number } }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'academics.subjectName' | translate }}</label>
                <select formControlName="subjectId">
                  <option value="">{{ 'academics.selectOneSubject' | translate }}</option>
                  @for (subject of subjects; track subject.id) {
                    <option [value]="subject.id">{{ subject.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'academics.subjectTeacher' | translate }}</label>
                <select formControlName="teacherId">
                  <option value="">{{ 'academics.selectSubjectTeacher' | translate }}</option>
                  @for (teacher of teachers; track teacher.id) {
                    <option [value]="teacher.id">{{ teacherLabel(teacher) }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'academics.room' | translate }}</label>
                <input type="text" formControlName="room" />
              </div>
              <div class="field">
                <label>{{ 'academics.startTime' | translate }} *</label>
                <input type="time" formControlName="startTime" />
              </div>
              <div class="field">
                <label>{{ 'academics.endTime' | translate }} *</label>
                <input type="time" formControlName="endTime" />
              </div>
            </div>
            <div class="form-actions">
              @if (editingEntry && canManage) {
                <button class="btn btn-danger" type="button" (click)="askDelete()" [disabled]="saving">{{ 'common.delete' | translate }}</button>
              }
              <span class="spacer"></span>
              <button class="btn" type="button" (click)="closeModal()" [disabled]="saving">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving">
                {{ saving ? ('common.loading' | translate) : ('common.save' | translate) }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    @if (pendingDelete) {
      <div class="modal-backdrop" (click)="pendingDelete = null; cdr.markForCheck()">
        <div class="modal modal-sm" (click)="$event.stopPropagation()">
          <h2>{{ 'academics.deletePeriod' | translate }}</h2>
          <p class="confirm-text">{{ 'academics.deletePeriodConfirm' | translate }}</p>
          <div class="form-actions">
            <button class="btn" type="button" (click)="pendingDelete = null; cdr.markForCheck()">{{ 'common.cancel' | translate }}</button>
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
    .muted { color: var(--color-muted); }
    .view-tabs { display: flex; gap: 8px; margin-bottom: 14px; flex-wrap: wrap; }
    .view-tab {
      padding: 8px 14px; border-radius: 20px; border: 1px solid var(--color-border);
      background: #fff; font: inherit; cursor: pointer;
    }
    .view-tab.active { background: var(--color-primary-soft); color: var(--color-primary); border-color: var(--color-primary); font-weight: 600; }
    .filters { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
    .filters select { padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; background: #fff; min-width: 180px; }
    .empty-card { padding: 28px; text-align: center; color: var(--color-muted); }
    .table-wrap { overflow-x: auto; }
    table.grid { width: 100%; border-collapse: collapse; min-width: 760px; }
    th, td { text-align: left; padding: 10px 12px; border: 1px solid var(--color-border); font-size: .85rem; vertical-align: top; }
    thead th { background: var(--color-bg); text-transform: uppercase; font-size: .75rem; letter-spacing: .03em; color: var(--color-muted); }
    tbody th { background: var(--color-bg); width: 110px; }
    .time { font-size: .75rem; font-weight: 400; margin-top: 2px; }
    td.clickable { cursor: pointer; }
    td.clickable:hover { background: #f8fafc; }
    td.conflict { background: #fef2f2; box-shadow: inset 0 0 0 1px #fecaca; }
    .slot { display: flex; flex-direction: column; gap: 2px; }
    .strong { font-weight: 600; }
    .available { color: #047857; font-weight: 600; font-size: .8rem; }
    .banner { padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
    .banner.success { background: #ecfdf5; border: 1px solid #a7f3d0; color: #047857; }
    .banner.error { background: #fef2f2; border: 1px solid #fecaca; color: #b91c1c; }
    .center { text-align: center; padding: 12px; }
    .modal-backdrop {
      position: fixed; inset: 0; z-index: 100;
      background: rgba(15, 23, 42, .5);
      display: flex; align-items: flex-start; justify-content: center;
      padding: 40px 16px; overflow-y: auto;
    }
    .modal { background: #fff; border-radius: var(--radius); padding: 24px; width: 560px; max-width: 100%; box-shadow: 0 20px 50px rgba(0,0,0,.25); }
    .modal-sm { width: 420px; }
    .modal h2 { margin: 0 0 18px; font-size: 1.2rem; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    input, select { padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; }
    .form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; align-items: center; }
    .spacer { flex: 1; }
    .form-error { margin: 0 0 14px; color: #b91c1c; font-size: .9rem; }
    .confirm-text { margin: 0 0 8px; color: var(--color-muted); line-height: 1.45; }
    .btn-danger { background: #b91c1c; border-color: #b91c1c; color: #fff; }
    .btn-danger:hover { background: #991b1b; color: #fff; border-color: #991b1b; }
    @media (max-width: 640px) { .form-grid { grid-template-columns: 1fr; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicsTimetableComponent implements OnInit {
  readonly days = DAYS;
  readonly periods = PERIODS;
  view: TimetableView = 'class';
  years: AcademicYear[] = [];
  yearId = '';
  classes: SchoolClass[] = [];
  subjects: SubjectOption[] = [];
  teachers: TeacherOption[] = [];
  entries: TimetableEntry[] = [];
  classId = '';
  sectionId = '';
  teacherId = '';
  room = '';
  loading = false;
  pageError = '';
  successMessage = '';
  showModal = false;
  saving = false;
  formError = '';
  editingEntry: TimetableEntry | null = null;
  pendingDelete: TimetableEntry | null = null;
  deleting = false;
  conflictKeys = new Set<string>();

  readonly form = new FormGroup({
    classId: new FormControl('', Validators.required),
    sectionId: new FormControl('', Validators.required),
    dayOfWeek: new FormControl(1, Validators.required),
    periodNumber: new FormControl(1, Validators.required),
    subjectId: new FormControl(''),
    teacherId: new FormControl(''),
    room: new FormControl(''),
    startTime: new FormControl('09:00', Validators.required),
    endTime: new FormControl('09:45', Validators.required),
  });

  constructor(
    private http: HttpClient,
    public cdr: ChangeDetectorRef,
    private auth: AuthService,
  ) {}

  get canManage(): boolean {
    return this.auth.hasPermission('TIMETABLE_MANAGE');
  }

  get classSections(): SchoolSection[] {
    return this.classes.find((klass) => klass.id === this.classId)?.sections ?? [];
  }

  get formSections(): SchoolSection[] {
    return this.classes.find((klass) => klass.id === this.form.value.classId)?.sections ?? [];
  }

  get rooms(): string[] {
    const values = new Set<string>();
    for (const klass of this.classes) {
      for (const section of klass.sections || []) {
        if (section.room) {
          values.add(section.room);
        }
      }
    }
    for (const entry of this.entries) {
      if (entry.room) {
        values.add(entry.room);
      }
    }
    return [...values].sort((a, b) => a.localeCompare(b));
  }

  get hasSelection(): boolean {
    if (this.view === 'class') {
      return !!this.sectionId;
    }
    if (this.view === 'teacher') {
      return !!this.teacherId;
    }
    return !!this.room;
  }

  get promptKey(): string {
    if (this.view === 'teacher') {
      return 'academics.selectTeacherPrompt';
    }
    if (this.view === 'room') {
      return 'academics.selectRoomPrompt';
    }
    return 'academics.selectClassPrompt';
  }

  get visibleEntries(): TimetableEntry[] {
    return this.entries.filter((entry) => {
      if (this.view === 'class') {
        return entry.sectionId === this.sectionId;
      }
      if (this.view === 'teacher') {
        return entry.teacherId === this.teacherId;
      }
      return (entry.room || '').toLowerCase() === this.room.toLowerCase();
    });
  }

  get conflictCount(): number {
    return this.conflictKeys.size;
  }

  ngOnInit(): void {
    this.loadLookups();
  }

  dayKey(day: number): string {
    return `academics.day${day}`;
  }

  teacherLabel(teacher: TeacherOption): string {
    return teacher.displayName || [teacher.firstName, teacher.lastName].filter(Boolean).join(' ');
  }

  cell(day: number, period: number): TimetableEntry | undefined {
    return this.visibleEntries.find((entry) => Number(entry.dayOfWeek) === day && Number(entry.periodNumber) === period);
  }

  isConflict(day: number, period: number): boolean {
    const entry = this.cell(day, period);
    if (!entry) {
      return false;
    }
    return this.conflictKeys.has(`teacher:${entry.teacherId}:${day}:${period}`)
      || this.conflictKeys.has(`room:${(entry.room || '').toLowerCase()}:${day}:${period}`);
  }

  setView(view: TimetableView): void {
    this.view = view;
    this.successMessage = '';
    this.cdr.markForCheck();
  }

  onClassChange(event: Event): void {
    this.classId = (event.target as HTMLSelectElement).value;
    this.sectionId = this.classSections[0]?.id || '';
    this.cdr.markForCheck();
  }

  onSectionChange(event: Event): void {
    this.sectionId = (event.target as HTMLSelectElement).value;
    this.cdr.markForCheck();
  }

  onTeacherChange(event: Event): void {
    this.teacherId = (event.target as HTMLSelectElement).value;
    this.cdr.markForCheck();
  }

  onRoomChange(event: Event): void {
    this.room = (event.target as HTMLSelectElement).value;
    this.cdr.markForCheck();
  }

  onCellClick(day: number, period: PeriodDef): void {
    if (!this.canManage) {
      return;
    }
    const entry = this.cell(day, period.number);
    if (entry) {
      this.openEditModal(entry);
      return;
    }
    this.openAddModal(day, period);
  }

  openAddModal(day?: number, period?: PeriodDef): void {
    this.editingEntry = null;
    this.formError = '';
    this.successMessage = '';
    const classId = this.view === 'class' ? this.classId : '';
    const sectionId = this.view === 'class' ? this.sectionId : '';
    const teacherId = this.view === 'teacher' ? this.teacherId : '';
    const room = this.view === 'room' ? this.room : '';
    const selected = period || PERIODS[0];
    this.form.reset({
      classId,
      sectionId,
      dayOfWeek: day || 1,
      periodNumber: selected.number,
      subjectId: '',
      teacherId,
      room,
      startTime: selected.start,
      endTime: selected.end,
    });
    this.showModal = true;
    this.cdr.markForCheck();
  }

  openEditModal(entry: TimetableEntry): void {
    this.editingEntry = entry;
    this.formError = '';
    this.successMessage = '';
    this.form.reset({
      classId: this.classIdFor(entry.sectionId),
      sectionId: entry.sectionId,
      dayOfWeek: Number(entry.dayOfWeek),
      periodNumber: Number(entry.periodNumber),
      subjectId: entry.subjectId || '',
      teacherId: entry.teacherId || '',
      room: entry.room || '',
      startTime: this.toTime(entry.startTime),
      endTime: this.toTime(entry.endTime),
    });
    this.showModal = true;
    this.cdr.markForCheck();
  }

  closeModal(): void {
    if (this.saving) {
      return;
    }
    this.showModal = false;
    this.editingEntry = null;
    this.formError = '';
    this.cdr.markForCheck();
  }

  onFormClassChange(): void {
    const sections = this.formSections;
    const current = this.form.value.sectionId;
    if (!sections.some((section) => section.id === current)) {
      this.form.patchValue({ sectionId: sections[0]?.id || '' });
    }
    this.cdr.markForCheck();
  }

  onPeriodChange(): void {
    const number = Number(this.form.value.periodNumber);
    const period = PERIODS.find((item) => item.number === number);
    if (period) {
      this.form.patchValue({ startTime: period.start, endTime: period.end });
    }
  }

  onSubmit(): void {
    if (this.form.invalid || this.saving || !this.yearId) {
      return;
    }
    const value = this.form.getRawValue();
    const payload = {
      sectionId: value.sectionId,
      academicYearId: this.yearId,
      dayOfWeek: Number(value.dayOfWeek),
      periodNumber: Number(value.periodNumber),
      startTime: value.startTime,
      endTime: value.endTime,
      subjectId: value.subjectId || null,
      teacherId: value.teacherId || null,
      room: (value.room || '').trim() || null,
    };
    this.saving = true;
    this.formError = '';
    const request$ = this.editingEntry
      ? this.http.put<ApiResponse<TimetableEntry>>(`/api/timetable/${this.editingEntry.id}`, payload)
      : this.http.post<ApiResponse<TimetableEntry>>('/api/timetable', payload);
    const wasEdit = !!this.editingEntry;
    request$.subscribe({
      next: () => {
        this.saving = false;
        this.showModal = false;
        this.editingEntry = null;
        this.successMessage = wasEdit ? 'academics.periodUpdateSuccess' : 'academics.periodCreateSuccess';
        this.loadEntries();
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        const apiError = err.error as ApiError | undefined;
        this.formError = apiError?.message || 'Could not save timetable entry.';
        this.saving = false;
        this.cdr.markForCheck();
      },
    });
  }

  askDelete(): void {
    this.pendingDelete = this.editingEntry;
    this.cdr.markForCheck();
  }

  confirmDelete(): void {
    if (!this.pendingDelete) {
      return;
    }
    this.deleting = true;
    this.http.delete(`/api/timetable/${this.pendingDelete.id}`).subscribe({
      next: () => {
        this.deleting = false;
        this.pendingDelete = null;
        this.showModal = false;
        this.editingEntry = null;
        this.successMessage = 'academics.periodDeleteSuccess';
        this.loadEntries();
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        const apiError = err.error as ApiError | undefined;
        this.pageError = apiError?.message || 'Could not delete timetable entry.';
        this.deleting = false;
        this.pendingDelete = null;
        this.cdr.markForCheck();
      },
    });
  }

  private classIdFor(sectionId: string): string {
    for (const klass of this.classes) {
      if ((klass.sections || []).some((section) => section.id === sectionId)) {
        return klass.id;
      }
    }
    return '';
  }

  private toTime(value: string): string {
    return (value || '').slice(0, 5);
  }

  private loadLookups(): void {
    this.http.get<ApiResponse<AcademicYear[]>>('/api/academic-years').subscribe({
      next: (res) => {
        this.years = res.data ?? [];
        this.yearId = this.years.find((year) => year.current)?.id || this.years[0]?.id || '';
        if (this.yearId) {
          this.loadEntries();
        }
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
    this.http.get<ApiResponse<SchoolClass[]>>('/api/classes').subscribe({
      next: (res) => {
        this.classes = res.data ?? [];
        if (!this.classId && this.classes.length) {
          this.classId = this.classes[0].id;
          this.sectionId = this.classes[0].sections?.[0]?.id || '';
        }
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
    this.http.get<ApiResponse<SubjectOption[]>>('/api/subjects').subscribe({
      next: (res) => {
        this.subjects = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
    this.http.get<ApiResponse<TeacherOption[]>>('/api/teachers').subscribe({
      next: (res) => {
        this.teachers = (res.data ?? []).filter((teacher) => teacher.status !== 'INACTIVE');
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  private loadEntries(): void {
    if (!this.yearId) {
      return;
    }
    this.loading = true;
    this.pageError = '';
    this.http.get<ApiResponse<TimetableEntry[]>>('/api/timetable', { params: { academicYearId: this.yearId } }).subscribe({
      next: (res) => {
        this.entries = res.data ?? [];
        this.conflictKeys = this.detectConflicts(this.entries);
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        const apiError = err.error as ApiError | undefined;
        this.pageError = apiError?.message || 'Could not load timetable.';
        this.entries = [];
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  private detectConflicts(entries: TimetableEntry[]): Set<string> {
    const keys = new Set<string>();
    const teachers = new Map<string, string[]>();
    const rooms = new Map<string, string[]>();
    for (const entry of entries) {
      const slot = `${entry.dayOfWeek}:${entry.periodNumber}`;
      if (entry.teacherId) {
        const key = `${entry.teacherId}:${slot}`;
        const list = teachers.get(key) || [];
        list.push(entry.id);
        teachers.set(key, list);
      }
      if (entry.room) {
        const key = `${entry.room.toLowerCase()}:${slot}`;
        const list = rooms.get(key) || [];
        list.push(entry.id);
        rooms.set(key, list);
      }
    }
    for (const [key, list] of teachers) {
      if (list.length > 1) {
        const [teacherId, day, period] = key.split(':');
        keys.add(`teacher:${teacherId}:${day}:${period}`);
      }
    }
    for (const [key, list] of rooms) {
      if (list.length > 1) {
        const [room, day, period] = key.split(':');
        keys.add(`room:${room}:${day}:${period}`);
      }
    }
    return keys;
  }
}
