import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/models/api.model';
import {
  CalendarClassOption,
  CalendarEvent,
  CalendarEventType,
  CalendarOptions,
  CalendarSectionOption,
  CalendarView,
  EVENT_TYPES,
} from './calendar.model';
import { CalendarService } from './calendar.service';

interface MonthCell {
  iso: string;
  day: number;
  inMonth: boolean;
  isToday: boolean;
  events: CalendarEvent[];
}

@Component({
  selector: 'app-calendar',
  imports: [TranslateModule, ReactiveFormsModule, DatePipe],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'calendar.title' | translate }}</h1>
          <p class="muted">{{ 'calendar.subtitle' | translate }}</p>
        </div>
        <div class="header-actions">
          @if (canManage) {
            <button class="btn" type="button" [disabled]="exporting" (click)="exportHolidays()">
              {{ 'calendar.exportHolidays' | translate }}
            </button>
            <button class="btn btn-primary" type="button" (click)="openCreate()">
              + {{ 'calendar.addEvent' | translate }}
            </button>
          }
        </div>
      </div>

      @if (error) {
        <p class="banner error">{{ error }}</p>
      }

      <div class="toolbar">
        <div class="views">
          @for (view of views; track view) {
            <button type="button" class="btn" [class.active]="currentView === view" (click)="setView(view)">
              {{ 'calendar.views.' + view | translate }}
            </button>
          }
        </div>
        <div class="nav">
          <button type="button" class="btn" (click)="shift(-1)">‹</button>
          <button type="button" class="btn" (click)="goToday()">{{ 'calendar.today' | translate }}</button>
          <button type="button" class="btn" (click)="shift(1)">›</button>
          <strong>{{ rangeLabel }}</strong>
        </div>
        <label class="filter">
          <span>{{ 'calendar.filterType' | translate }}</span>
          <select [value]="typeFilter" (change)="onTypeFilter($event)">
            <option value="">{{ 'common.all' | translate }}</option>
            @for (type of eventTypes; track type) {
              <option [value]="type">{{ 'calendar.types.' + type | translate }}</option>
            }
          </select>
        </label>
      </div>

      @if (loading) {
        <p class="muted">{{ 'common.loading' | translate }}</p>
      }

      @if (currentView === 'month') {
        <div class="card month">
          <div class="weekdays">
            @for (label of weekdayLabels; track label) {
              <span>{{ label }}</span>
            }
          </div>
          <div class="month-grid">
            @for (cell of monthCells; track cell.iso) {
              <button type="button" class="cell" [class.out]="!cell.inMonth" [class.today]="cell.isToday" (click)="openDay(cell.iso)">
                <span class="num">{{ cell.day }}</span>
                @for (event of cell.events.slice(0, 3); track event.id) {
                  <span class="chip" [class]="'type-' + event.eventType.toLowerCase()" (click)="openView(event); $event.stopPropagation()">{{ event.title }}</span>
                }
                @if (cell.events.length > 3) {
                  <span class="more">+{{ cell.events.length - 3 }}</span>
                }
              </button>
            }
          </div>
        </div>
      }

      @if (currentView === 'week') {
        <div class="card week">
          <div class="week-grid">
            @for (cell of weekCells; track cell.iso) {
              <div class="week-col" [class.today]="cell.isToday">
                <div class="week-head">{{ cell.iso | date: 'EEE d' }}</div>
                @for (event of cell.events; track event.id) {
                  <button type="button" class="chip block" [class]="'type-' + event.eventType.toLowerCase()" (click)="openView(event)">
                    <strong>{{ event.title }}</strong>
                    <span>{{ timeLabel(event) }}</span>
                  </button>
                }
                @if (cell.events.length === 0) {
                  <p class="muted empty">{{ 'calendar.noEvents' | translate }}</p>
                }
              </div>
            }
          </div>
        </div>
      }

      @if (currentView === 'agenda') {
        <div class="card agenda">
          @if (agendaEvents.length === 0 && !loading) {
            <p class="muted empty">{{ 'calendar.noEvents' | translate }}</p>
          }
          @for (event of agendaEvents; track event.id) {
            <article class="agenda-row">
              <span class="chip" [class]="'type-' + event.eventType.toLowerCase()">
                {{ 'calendar.types.' + event.eventType | translate }}
              </span>
              <div class="agenda-body">
                <button type="button" class="linkish" (click)="openView(event)">{{ event.title }}</button>
                <p class="muted">
                  {{ event.startDate | date: 'mediumDate' }}
                  @if (event.endDate && event.endDate !== event.startDate) {
                    <span> – {{ event.endDate | date: 'mediumDate' }}</span>
                  }
                  · {{ timeLabel(event) }}
                  @if (event.location) { <span> · {{ event.location }}</span> }
                  @if (event.className) { <span> · {{ event.className }} {{ event.sectionName || '' }}</span> }
                </p>
                @if (event.updatedAt) {
                  <p class="muted tiny">{{ 'calendar.updatedAt' | translate }}: {{ event.updatedAt | date: 'short' }}</p>
                }
              </div>
              @if (canManage) {
                <div class="row-actions">
                  <button class="btn" type="button" (click)="openEdit(event)">{{ 'common.edit' | translate }}</button>
                  <button class="btn btn-danger" type="button" (click)="confirmDelete(event)">{{ 'common.delete' | translate }}</button>
                </div>
              }
            </article>
          }
        </div>
      }
    </div>

    @if (detailEvent) {
      <div class="modal-backdrop" (click)="closeDetail()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ detailEvent.title }}</h2>
          <p class="chip" [class]="'type-' + detailEvent.eventType.toLowerCase()">
            {{ 'calendar.types.' + detailEvent.eventType | translate }}
          </p>
          <dl class="meta">
            <div><dt>{{ 'calendar.when' | translate }}</dt><dd>{{ dateRange(detailEvent) }} · {{ timeLabel(detailEvent) }}</dd></div>
            <div><dt>{{ 'calendar.audience' | translate }}</dt><dd>{{ audienceLabel(detailEvent) }}</dd></div>
            @if (detailEvent.location) {
              <div><dt>{{ 'calendar.location' | translate }}</dt><dd>{{ detailEvent.location }}</dd></div>
            }
            @if (detailEvent.updatedAt) {
              <div><dt>{{ 'calendar.updatedAt' | translate }}</dt><dd>{{ detailEvent.updatedAt | date: 'short' }}</dd></div>
            }
          </dl>
          @if (detailEvent.description) {
            <p class="desc">{{ detailEvent.description }}</p>
          }
          <div class="modal-actions">
            @if (canManage) {
              <button class="btn" type="button" (click)="openEdit(detailEvent)">{{ 'common.edit' | translate }}</button>
              <button class="btn btn-danger" type="button" (click)="confirmDelete(detailEvent)">{{ 'common.delete' | translate }}</button>
            }
            <button class="btn" type="button" (click)="closeDetail()">{{ 'common.cancel' | translate }}</button>
          </div>
        </div>
      </div>
    }

    @if (formOpen) {
      <div class="modal-backdrop" (click)="closeForm()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editingId ? ('calendar.editEvent' | translate) : ('calendar.addEvent' | translate) }}</h2>
          <form [formGroup]="form" (ngSubmit)="save()">
            <div class="field">
              <label>{{ 'calendar.eventTitle' | translate }}</label>
              <input type="text" formControlName="title" />
            </div>
            <div class="field-row">
              <div class="field">
                <label>{{ 'calendar.type' | translate }}</label>
                <select formControlName="eventType">
                  @for (type of eventTypes; track type) {
                    <option [value]="type">{{ 'calendar.types.' + type | translate }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'calendar.audience' | translate }}</label>
                <select formControlName="visibilityScope">
                  <option value="SCHOOL_WIDE">{{ 'calendar.scopes.SCHOOL_WIDE' | translate }}</option>
                  <option value="CLASS_WIDE">{{ 'calendar.scopes.CLASS_WIDE' | translate }}</option>
                  <option value="STAFF">{{ 'calendar.scopes.STAFF' | translate }}</option>
                  <option value="ROLE">{{ 'calendar.scopes.ROLE' | translate }}</option>
                </select>
              </div>
            </div>
            @if (form.controls.visibilityScope.value === 'CLASS_WIDE') {
              <div class="field-row">
                <div class="field">
                  <label>{{ 'calendar.class' | translate }}</label>
                  <select formControlName="classId">
                    <option value="">{{ 'calendar.selectClass' | translate }}</option>
                    @for (klass of classes; track klass.id) {
                      <option [value]="klass.id">{{ klass.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label>{{ 'calendar.section' | translate }}</label>
                  <select formControlName="sectionId">
                    <option value="">{{ 'common.all' | translate }}</option>
                    @for (section of formSections; track section.id) {
                      <option [value]="section.id">{{ section.name }}</option>
                    }
                  </select>
                </div>
              </div>
            }
            @if (form.controls.visibilityScope.value === 'ROLE') {
              <div class="field">
                <label>{{ 'calendar.role' | translate }}</label>
                <select formControlName="audienceRole">
                  <option value="TEACHER">{{ 'calendar.roles.TEACHER' | translate }}</option>
                  <option value="STUDENT">{{ 'calendar.roles.STUDENT' | translate }}</option>
                  <option value="PARENT">{{ 'calendar.roles.PARENT' | translate }}</option>
                </select>
              </div>
            }
            <div class="field-row">
              <div class="field">
                <label>{{ 'calendar.startDate' | translate }}</label>
                <input type="date" formControlName="startDate" />
              </div>
              <div class="field">
                <label>{{ 'calendar.endDate' | translate }}</label>
                <input type="date" formControlName="endDate" />
              </div>
            </div>
            <label class="check">
              <input type="checkbox" formControlName="allDay" />
              {{ 'calendar.allDay' | translate }}
            </label>
            @if (!form.controls.allDay.value) {
              <div class="field-row">
                <div class="field">
                  <label>{{ 'calendar.startTime' | translate }}</label>
                  <input type="time" formControlName="startTime" />
                </div>
                <div class="field">
                  <label>{{ 'calendar.endTime' | translate }}</label>
                  <input type="time" formControlName="endTime" />
                </div>
              </div>
            }
            <div class="field">
              <label>{{ 'calendar.location' | translate }}</label>
              <input type="text" formControlName="location" />
            </div>
            <div class="field">
              <label>{{ 'calendar.description' | translate }}</label>
              <textarea rows="3" formControlName="description"></textarea>
            </div>
            @if (formError) {
              <p class="banner error">{{ formError }}</p>
            }
            <div class="modal-actions">
              <button class="btn" type="button" (click)="closeForm()">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving">{{ 'common.save' | translate }}</button>
            </div>
          </form>
        </div>
      </div>
    }

    @if (pendingDelete) {
      <div class="modal-backdrop" (click)="pendingDelete = null">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ 'common.delete' | translate }}</h2>
          <p>{{ 'calendar.deleteConfirm' | translate: { title: pendingDelete.title } }}</p>
          <div class="modal-actions">
            <button class="btn" type="button" (click)="pendingDelete = null">{{ 'common.cancel' | translate }}</button>
            <button class="btn btn-danger" type="button" [disabled]="deleting" (click)="deleteEvent()">{{ 'common.delete' | translate }}</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 18px; }
    .header-actions, .toolbar, .views, .nav, .modal-actions, .row-actions { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
    .muted { color: var(--color-muted); }
    .toolbar { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
    .btn.active, .btn-primary { background: var(--color-primary); color: #fff; border-color: var(--color-primary); }
    .btn-danger { color: #b91c1c; }
    .filter { display: flex; align-items: center; gap: 8px; font-size: .9rem; }
    .filter select, .field input, .field select, .field textarea {
      padding: 8px 10px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; width: 100%;
    }
    .card { padding: 12px; }
    .weekdays, .month-grid { display: grid; grid-template-columns: repeat(7, 1fr); }
    .weekdays { text-align: center; font-size: .75rem; color: var(--color-muted); margin-bottom: 6px; }
    .month-grid { gap: 4px; }
    .cell {
      min-height: 96px; border: 1px solid var(--color-border); border-radius: 8px; background: var(--color-surface);
      text-align: left; padding: 6px; display: flex; flex-direction: column; gap: 3px; cursor: pointer; color: inherit; font: inherit;
    }
    .cell.out { opacity: .45; }
    .cell.today { border-color: var(--color-primary); }
    .num { font-weight: 700; font-size: .85rem; }
    .chip { display: inline-block; border-radius: 999px; padding: 2px 8px; font-size: .72rem; font-weight: 600; background: #eef2ff; color: #3730a3; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100%; }
    .chip.block { display: flex; flex-direction: column; width: 100%; text-align: left; border: 0; cursor: pointer; margin-bottom: 6px; }
    .type-holiday { background: #fee2e2; color: #b91c1c; }
    .type-exam { background: #ffedd5; color: #c2410c; }
    .type-ptm, .type-meeting { background: #dbeafe; color: #1d4ed8; }
    .type-sports, .type-activity { background: #dcfce7; color: #15803d; }
    .type-notice { background: #f3e8ff; color: #7e22ce; }
    .type-event, .type-other { background: #e0e7ff; color: #4338ca; }
    .week-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; }
    .week-col { border: 1px solid var(--color-border); border-radius: 8px; min-height: 220px; padding: 8px; }
    .week-col.today { border-color: var(--color-primary); }
    .week-head { font-weight: 700; margin-bottom: 8px; }
    .agenda-row { display: flex; gap: 12px; align-items: flex-start; padding: 12px 4px; border-bottom: 1px solid var(--color-border); }
    .agenda-body { flex: 1; }
    .linkish { background: none; border: 0; padding: 0; font: inherit; font-weight: 700; cursor: pointer; color: inherit; }
    .tiny { font-size: .75rem; }
    .empty { margin: 8px 0 0; }
    .modal-backdrop { position: fixed; inset: 0; background: rgba(15,23,42,.45); display: flex; align-items: center; justify-content: center; z-index: 20; padding: 16px; }
    .modal { width: 560px; max-width: 100%; background: var(--color-surface); border-radius: 12px; padding: 20px; }
    .field { margin-bottom: 10px; }
    .field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .check { display: flex; gap: 8px; align-items: center; margin: 8px 0; }
    .banner { padding: 10px 14px; border-radius: 8px; }
    .banner.error { background: #fef2f2; color: #b91c1c; }
    .meta { display: grid; gap: 8px; }
    dt { font-size: .75rem; color: var(--color-muted); }
    dd { margin: 0; font-weight: 600; }
    @media (max-width: 900px) {
      .week-grid, .month-grid, .weekdays { grid-template-columns: 1fr; }
      .field-row { grid-template-columns: 1fr; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CalendarComponent implements OnInit {
  readonly views: CalendarView[] = ['month', 'week', 'agenda'];
  readonly weekdayLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  readonly eventTypes = EVENT_TYPES;

  currentView: CalendarView = 'month';
  typeFilter = '';
  loading = false;
  exporting = false;
  saving = false;
  deleting = false;
  error = '';
  formError = '';
  events: CalendarEvent[] = [];
  options: CalendarOptions | null = null;
  formOpen = false;
  editingId: string | null = null;
  detailEvent: CalendarEvent | null = null;
  pendingDelete: CalendarEvent | null = null;
  form;
  private cursor = this.startOfMonth(new Date());

  constructor(
    private calendar: CalendarService,
    private auth: AuthService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.nonNullable.group({
      title: ['', Validators.required],
      eventType: ['EVENT' as CalendarEventType],
      visibilityScope: ['SCHOOL_WIDE'],
      audienceRole: ['PARENT'],
      classId: [''],
      sectionId: [''],
      startDate: [this.toIso(new Date()), Validators.required],
      endDate: [this.toIso(new Date())],
      allDay: [true],
      startTime: [''],
      endTime: [''],
      location: [''],
      description: [''],
    });
  }

  ngOnInit(): void {
    this.calendar.options().subscribe({
      next: (options) => {
        this.options = options;
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
    this.reload();
  }

  get canManage(): boolean {
    return this.auth.hasPermission('EVENT_MANAGE');
  }

  get classes(): CalendarClassOption[] {
    return this.options?.classes ?? [];
  }

  get formSections(): CalendarSectionOption[] {
    const classId = this.form.controls.classId.value;
    return this.classes.find((klass) => klass.id === classId)?.sections ?? [];
  }

  get rangeLabel(): string {
    if (this.currentView === 'week') {
      const start = this.startOfWeek(this.cursor);
      const end = this.addDays(start, 6);
      return `${this.toIso(start)} – ${this.toIso(end)}`;
    }
    return this.cursor.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
  }

  get monthCells(): MonthCell[] {
    return this.buildGrid(this.startOfMonth(this.cursor), 42);
  }

  get weekCells(): MonthCell[] {
    return this.buildGrid(this.startOfWeek(this.cursor), 7);
  }

  get agendaEvents(): CalendarEvent[] {
    return [...this.events].sort((a, b) => a.startDate.localeCompare(b.startDate));
  }

  setView(view: CalendarView): void {
    this.currentView = view;
    this.reload();
  }

  shift(delta: number): void {
    if (this.currentView === 'week') {
      this.cursor = this.addDays(this.cursor, delta * 7);
    } else {
      this.cursor = new Date(this.cursor.getFullYear(), this.cursor.getMonth() + delta, 1);
    }
    this.reload();
  }

  goToday(): void {
    this.cursor = new Date();
    this.reload();
  }

  onTypeFilter(event: Event): void {
    this.typeFilter = (event.target as HTMLSelectElement).value;
    this.reload();
  }

  openDay(iso: string): void {
    const matches = this.eventsOn(iso);
    if (matches.length === 1) {
      this.openView(matches[0]);
      return;
    }
    this.currentView = 'agenda';
    this.cursor = this.parse(iso);
    this.reload();
  }

  openView(event: CalendarEvent): void {
    this.detailEvent = event;
    this.cdr.markForCheck();
  }

  closeDetail(): void {
    this.detailEvent = null;
    this.cdr.markForCheck();
  }

  openCreate(): void {
    this.editingId = null;
    this.formError = '';
    this.form.reset({
      title: '',
      eventType: 'EVENT',
      visibilityScope: 'SCHOOL_WIDE',
      audienceRole: 'PARENT',
      classId: '',
      sectionId: '',
      startDate: this.toIso(this.cursor),
      endDate: this.toIso(this.cursor),
      allDay: true,
      startTime: '',
      endTime: '',
      location: '',
      description: '',
    });
    this.formOpen = true;
    this.cdr.markForCheck();
  }

  openEdit(event: CalendarEvent): void {
    this.detailEvent = null;
    this.editingId = event.id;
    this.formError = '';
    this.form.reset({
      title: event.title,
      eventType: event.eventType,
      visibilityScope: event.visibilityScope,
      audienceRole: event.audienceRole || 'PARENT',
      classId: event.classId || '',
      sectionId: event.sectionId || '',
      startDate: event.startDate,
      endDate: event.endDate || event.startDate,
      allDay: event.allDay,
      startTime: event.startTime?.slice(0, 5) || '',
      endTime: event.endTime?.slice(0, 5) || '',
      location: event.location || '',
      description: event.description || '',
    });
    this.formOpen = true;
    this.cdr.markForCheck();
  }

  closeForm(): void {
    this.formOpen = false;
    this.cdr.markForCheck();
  }

  save(): void {
    if (this.form.invalid || this.saving) {
      return;
    }
    const value = this.form.getRawValue();
    if (value.visibilityScope === 'CLASS_WIDE' && !value.classId) {
      this.formError = 'Select a class for class-scoped events.';
      this.cdr.markForCheck();
      return;
    }
    const payload = {
      title: value.title,
      description: value.description || null,
      eventType: value.eventType,
      startDate: value.startDate,
      endDate: value.endDate || value.startDate,
      allDay: value.allDay,
      startTime: value.allDay ? null : value.startTime || null,
      endTime: value.allDay ? null : value.endTime || null,
      location: value.location || null,
      visibilityScope: value.visibilityScope,
      audienceRole: value.visibilityScope === 'ROLE' ? value.audienceRole : null,
      classId: value.visibilityScope === 'CLASS_WIDE' ? value.classId || null : null,
      sectionId: value.visibilityScope === 'CLASS_WIDE' ? value.sectionId || null : null,
    };
    this.saving = true;
    this.formError = '';
    const request = this.editingId
      ? this.calendar.update(this.editingId, payload)
      : this.calendar.create(payload);
    request.subscribe({
      next: () => {
        this.saving = false;
        this.formOpen = false;
        this.reload();
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        this.formError = this.message(err);
        this.cdr.markForCheck();
      },
    });
  }

  confirmDelete(event: CalendarEvent): void {
    this.detailEvent = null;
    this.pendingDelete = event;
    this.cdr.markForCheck();
  }

  deleteEvent(): void {
    if (!this.pendingDelete || this.deleting) {
      return;
    }
    this.deleting = true;
    this.calendar.delete(this.pendingDelete.id).subscribe({
      next: () => {
        this.deleting = false;
        this.pendingDelete = null;
        this.reload();
      },
      error: (err: HttpErrorResponse) => {
        this.deleting = false;
        this.error = this.message(err);
        this.cdr.markForCheck();
      },
    });
  }

  exportHolidays(): void {
    this.exporting = true;
    this.calendar.exportHolidays(this.cursor.getFullYear()).subscribe({
      next: (blob) => {
        this.exporting = false;
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `holiday-calendar-${this.cursor.getFullYear()}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.exporting = false;
        this.error = this.message(err);
        this.cdr.markForCheck();
      },
    });
  }

  timeLabel(event: CalendarEvent): string {
    if (event.allDay || !event.startTime) {
      return 'All day';
    }
    return event.endTime ? `${event.startTime.slice(0, 5)}–${event.endTime.slice(0, 5)}` : event.startTime.slice(0, 5);
  }

  dateRange(event: CalendarEvent): string {
    return event.endDate && event.endDate !== event.startDate
      ? `${event.startDate} – ${event.endDate}`
      : event.startDate;
  }

  audienceLabel(event: CalendarEvent): string {
    if (event.visibilityScope === 'CLASS_WIDE') {
      return [event.className, event.sectionName].filter(Boolean).join(' ') || 'Class';
    }
    if (event.visibilityScope === 'ROLE') {
      return event.audienceRole || 'Role';
    }
    return event.visibilityScope.replace(/_/g, ' ');
  }

  private reload(): void {
    this.loading = true;
    this.error = '';
    const { from, to } = this.range();
    this.calendar.list(from, to, this.typeFilter || undefined).subscribe({
      next: (events) => {
        this.events = events;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = this.message(err);
        this.cdr.markForCheck();
      },
    });
  }

  private range(): { from: string; to: string } {
    if (this.currentView === 'week') {
      const start = this.startOfWeek(this.cursor);
      return { from: this.toIso(start), to: this.toIso(this.addDays(start, 6)) };
    }
    const start = this.startOfMonth(this.cursor);
    const gridStart = this.startOfWeek(start);
    return { from: this.toIso(gridStart), to: this.toIso(this.addDays(gridStart, 41)) };
  }

  private buildGrid(start: Date, count: number): MonthCell[] {
    const today = this.toIso(new Date());
    const month = this.cursor.getMonth();
    const cells: MonthCell[] = [];
    for (let i = 0; i < count; i++) {
      const date = this.addDays(start, i);
      const iso = this.toIso(date);
      cells.push({
        iso,
        day: date.getDate(),
        inMonth: date.getMonth() === month,
        isToday: iso === today,
        events: this.eventsOn(iso),
      });
    }
    return cells;
  }

  private eventsOn(iso: string): CalendarEvent[] {
    return this.events.filter((event) => {
      const end = event.endDate || event.startDate;
      return iso >= event.startDate && iso <= end;
    });
  }

  private message(err: HttpErrorResponse): string {
    const body = err.error as ApiError | undefined;
    return body?.message || err.message || 'Error';
  }

  private startOfMonth(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), 1);
  }

  private startOfWeek(date: Date): Date {
    const offset = (date.getDay() + 6) % 7;
    return this.addDays(date, -offset);
  }

  private addDays(date: Date, days: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
  }

  private parse(iso: string): Date {
    const [year, month, day] = iso.split('-').map(Number);
    return new Date(year, (month ?? 1) - 1, day ?? 1);
  }

  private toIso(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }
}
