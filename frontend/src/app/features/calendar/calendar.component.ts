import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { ApiResponse } from '../../core/models/api.model';

interface EventItem {
  id: string;
  title: string;
  description: string | null;
  type: string;
  startDateTime: string;
  endDateTime: string | null;
  allDay: boolean;
  audienceScope: string;
  source: string;
  syncStatus: string;
}

interface BackendEvent {
  id: string;
  title: string;
  description: string | null;
  type: string;
  startDateTime: string;
  endDateTime: string | null;
  allDay: boolean;
  audienceScope: string;
  source: string;
  syncStatus: string;
}

@Component({
  selector: 'app-calendar',
  imports: [TranslateModule, ReactiveFormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'calendar.title' | translate }}</h1>
          <p class="muted">{{ 'calendar.subtitle' | translate }}</p>
        </div>
        @if (canManage) {
          <button class="btn btn-primary" (click)="onCreate()">{{ 'calendar.addEvent' | translate }}</button>
        }
      </div>

      @if (canManage && showForm) {
        <div class="card composer">
          <h3 class="card-title">{{ 'calendar.newEvent' | translate }}</h3>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="form-grid">
              <div class="field span-2">
                <label>{{ 'calendar.title' | translate }}</label>
                <input type="text" formControlName="title" />
              </div>
              <div class="field span-2">
                <label>{{ 'calendar.description' | translate }}</label>
                <textarea rows="3" formControlName="description"></textarea>
              </div>
              <div class="field">
                <label>{{ 'calendar.type' | translate }}</label>
                <select formControlName="type">
                  <option value="HOLIDAY">Holiday</option>
                  <option value="EXAM">Exam</option>
                  <option value="PTM">PTM</option>
                  <option value="SPORTS_CULTURAL_EVENT">Sports/Cultural Event</option>
                  <option value="NOTICE_LINKED_EVENT">Notice-linked Event</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div class="field">
                <label>{{ 'calendar.audience' | translate }}</label>
                <select formControlName="audienceScope">
                  <option value="ALL">All</option>
                  <option value="CLASS_SECTION">Class Section</option>
                  <option value="ROLE">Role</option>
                </select>
              </div>
              <div class="field">
                <label>{{ 'calendar.start' | translate }}</label>
                <input type="datetime-local" formControlName="startDateTime" />
              </div>
              <div class="field">
                <label>{{ 'calendar.end' | translate }}</label>
                <input type="datetime-local" formControlName="endDateTime" />
              </div>
            </div>
            <div class="form-actions">
              <button class="btn btn-secondary" type="button" (click)="showForm = false">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="form.invalid">{{ 'common.save' | translate }}</button>
            </div>
          </form>
        </div>
      }

      <div class="card">
        <div class="timeline">
          @for (event of events; track event.id) {
            <div class="event-item" [class.event-holiday]="event.type === 'HOLIDAY'" [class.event-exam]="event.type === 'EXAM'">
              <div class="event-date">
                <span>{{ toDateString(event.startDateTime) }}</span>
              </div>
              <div class="event-body">
                <div class="event-header">
                  <strong>{{ event.title }}</strong>
                  <span class="badge">{{ event.type }}</span>
                </div>
                <div class="event-meta">{{ event.description || '—' }}</div>
                <div class="event-meta">{{ event.audienceScope }} · {{ event.source }}</div>
              </div>
              @if (canManage) {
                <div class="event-actions">
                  <button class="btn btn-ghost" (click)="onDelete(event.id)">{{ 'common.delete' | translate }}</button>
                </div>
              }
            </div>
          } @empty {
            <p class="muted empty">{{ 'common.noData' | translate }}</p>
          }
        </div>
      </div>
    </div>
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    h1 { font-size: 1.5rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); }
    .composer { padding: 20px; margin-bottom: 24px; }
    .card-title { margin: 0 0 16px; font-size: 1.05rem; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .span-2 { grid-column: span 2; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field label { font-weight: 500; color: var(--color-muted); }
    input, select, textarea { padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; }
    .form-actions { margin-top: 16px; display: flex; justify-content: flex-end; gap: 12px; }
    .timeline { display: flex; flex-direction: column; gap: 14px; }
    .event-item { display: grid; grid-template-columns: 140px 1fr auto; gap: 14px; border: 1px solid var(--color-border); border-radius: 12px; padding: 16px; background: #fff; }
    .event-date { font-weight: 600; color: var(--color-secondary); }
    .event-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
    .event-meta { color: var(--color-muted); margin-top: 4px; }
    .badge { display: inline-flex; padding: 4px 8px; border-radius: 999px; font-size: .72rem; background: #eef2ff; color: #3730a3; }
    .event-holiday .badge { background: #ecfdf5; color: #065f46; }
    .event-exam .badge { background: #fff7ed; color: #c2410c; }
    .empty { padding: 18px 8px; }
    @media (max-width: 640px) { .page-header { display: block; } .form-grid { grid-template-columns: 1fr; } .span-2 { grid-column: span 1; } .event-item { grid-template-columns: 1fr; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CalendarComponent implements OnInit {
  events: EventItem[] = [];
  showForm = false;
  readonly form = new FormGroup({
    title: new FormControl('', Validators.required),
    description: new FormControl(''),
    type: new FormControl('HOLIDAY', Validators.required),
    audienceScope: new FormControl('ALL'),
    startDateTime: new FormControl('', Validators.required),
    endDateTime: new FormControl(''),
  });

  constructor(private http: HttpClient, private auth: AuthService, private cdr: ChangeDetectorRef) {}

  get canManage(): boolean {
    return this.auth.hasPermission('EVENT_MANAGE');
  }

  ngOnInit(): void {
    this.loadEvents();
  }

  onCreate(): void {
    this.showForm = true;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }

    const payload = {
      title: this.form.value.title,
      description: this.form.value.description ?? '',
      type: this.form.value.type ?? 'HOLIDAY',
      startDateTime: this.toIso(this.form.value.startDateTime),
      endDateTime: this.form.value.endDateTime ? this.toIso(this.form.value.endDateTime) : null,
      allDay: false,
      audienceScope: this.form.value.audienceScope ?? 'ALL',
      source: 'MANUAL',
      syncStatus: 'NA',
    };

    this.http.post<ApiResponse<BackendEvent>>('/api/events', payload).subscribe({
      next: () => {
        this.showForm = false;
        this.form.reset({ type: 'HOLIDAY', audienceScope: 'ALL' });
        this.loadEvents();
      },
      error: () => alert('Unable to save event.')
    });
  }

  onDelete(id: string): void {
    if (!confirm('Delete this event?')) {
      return;
    }
    this.http.delete('/api/events/' + id).subscribe({
      next: () => this.loadEvents(),
      error: () => alert('Unable to delete event.')
    });
  }

  private loadEvents(): void {
    this.http.get<ApiResponse<BackendEvent[]>>('/api/events').subscribe({
      next: (res) => {
        this.events = (res.data ?? []).map((event) => ({
          id: event.id,
          title: event.title,
          description: event.description,
          type: event.type,
          startDateTime: event.startDateTime,
          endDateTime: event.endDateTime,
          allDay: event.allDay,
          audienceScope: event.audienceScope,
          source: event.source,
          syncStatus: event.syncStatus,
        }));
        this.cdr.markForCheck();
      },
      error: () => {
        this.events = [];
        this.cdr.markForCheck();
      }
    });
  }

  toDateString(value: string): string {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString();
  }

  private toIso(value: string | null): string {
    if (!value) {
      return new Date().toISOString();
    }
    const date = new Date(value);
    return date.toISOString();
  }
}
