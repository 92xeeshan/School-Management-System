import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { ThemeService } from '../../core/theme/theme.service';
import { AttendanceTrendComponent } from './attendance-trend.component';
import { DashboardService } from './dashboard.service';
import { GenderDonutComponent } from './gender-donut.component';
import {
  DashboardNotice,
  DashboardSummary,
  SchoolEvent,
  WidgetId,
} from './dashboard.model';
import { MiniCalendarComponent } from './mini-calendar.component';
import { StarStudentsComponent } from './star-students.component';

interface KpiCard {
  key: string;
  value: string;
  icon: string;
  tone: string;
  hintKey?: string;
}

interface QuickAction {
  key: string;
  labelKey: string;
  icon: string;
  route?: string;
  permissions: string[];
}

interface SearchResult {
  label: string;
  sublabel: string;
  kind: 'event' | 'student' | 'nav';
  route?: string[];
  date?: string;
}

const STAFF_ROLES = ['SUPER_ADMIN', 'ADMIN', 'TEACHER'];
const WIDGET_STORAGE_KEY = 'schoolms.dashboard.widgets';
const DEFAULT_WIDGETS: Record<WidgetId, boolean> = {
  gender: true,
  attendanceTrend: true,
  starStudents: true,
  roleInsights: true,
  calendar: true,
  agenda: true,
  upcoming: true,
  notices: true,
};

@Component({
  selector: 'app-dashboard',
  imports: [
    TranslateModule,
    RouterLink,
    ReactiveFormsModule,
    DatePipe,
    GenderDonutComponent,
    AttendanceTrendComponent,
    StarStudentsComponent,
    MiniCalendarComponent,
  ],
  template: `
    <div class="dashboard">
      <header class="page-header">
        <div class="greeting">
          <h1>{{ 'dashboard.title' | translate }}</h1>
          <p class="muted">{{ 'dashboard.welcomeMessage' | translate: { name: displayName } }}</p>
        </div>

        <div class="header-tools">
          <div class="search">
            <input type="search"
                   [placeholder]="'dashboard.search.placeholder' | translate"
                   [value]="searchTerm"
                   (input)="onSearch($event)"
                   (focus)="searchOpen = true"
                   (blur)="onSearchBlur()"
                   [attr.aria-label]="'dashboard.search.placeholder' | translate" />
            @if (searchOpen && searchTerm.length >= 2) {
              <div class="search-panel">
                @if (searchResults.length === 0) {
                  <div class="search-empty">{{ 'dashboard.search.noResults' | translate }}</div>
                } @else {
                  @for (result of searchResults; track result.kind + result.label) {
                    <button type="button" class="search-item" (mousedown)="onResultClick(result)">
                      <span class="search-kind">{{ result.kind }}</span>
                      <span class="search-body">
                        <span class="search-label">{{ result.label }}</span>
                        <span class="search-sub">{{ result.sublabel }}</span>
                      </span>
                    </button>
                  }
                }
              </div>
            }
          </div>

          <button type="button" class="icon-btn"
                  (click)="toggleTheme()"
                  [attr.aria-label]="(theme.current === 'dark' ? 'dashboard.theme.light' : 'dashboard.theme.dark') | translate"
                  [title]="(theme.current === 'dark' ? 'dashboard.theme.light' : 'dashboard.theme.dark') | translate">
            {{ theme.current === 'dark' ? '☀' : '☾' }}
          </button>

          <button type="button" class="icon-btn"
                  (click)="showCustomize = !showCustomize"
                  [attr.aria-label]="'dashboard.customize.title' | translate"
                  [title]="'dashboard.customize.title' | translate">
            ⚙
          </button>

          @if (canManageEvents) {
            <button type="button" class="btn btn-primary" (click)="openEventDialog()">
              + {{ 'dashboard.quickActions.addEvent' | translate }}
            </button>
          }
        </div>
      </header>

      @if (showCustomize) {
        <div class="card customize">
          <div class="customize-head">
            <h3>{{ 'dashboard.customize.title' | translate }}</h3>
            <button type="button" class="btn btn-ghost" (click)="resetWidgets()">
              {{ 'dashboard.customize.reset' | translate }}
            </button>
          </div>
          <div class="customize-grid">
            @for (widget of widgetOptions; track widget.id) {
              <label class="toggle">
                <input type="checkbox" [checked]="widgetVisible(widget.id)"
                       (change)="toggleWidget(widget.id)" />
                <span>{{ widget.labelKey | translate }}</span>
              </label>
            }
          </div>
        </div>
      }

      @if (quickActions.length > 0) {
        <div class="quick-actions">
          @for (action of quickActions; track action.key) {
            @if (action.route) {
              <a class="action" [routerLink]="action.route">
                <span class="action-icon">{{ action.icon }}</span>
                <span>{{ action.labelKey | translate }}</span>
              </a>
            } @else {
              <button type="button" class="action" (click)="openEventDialog()">
                <span class="action-icon">{{ action.icon }}</span>
                <span>{{ action.labelKey | translate }}</span>
              </button>
            }
          }
        </div>
      }

      @if (urgentNotices.length > 0) {
        <div class="alert-banner">
          <span class="alert-icon">⚠</span>
          <div class="alert-body">
            <strong>{{ 'dashboard.alerts.urgent' | translate }}</strong>
            <span>{{ urgentNotices[0].title }}</span>
          </div>
          <a class="alert-link" routerLink="/notices">{{ 'dashboard.notices.viewAll' | translate }}</a>
        </div>
      }

      @if (summaryError) {
        <div class="card error-card">
          <p>{{ 'common.error' | translate }}</p>
          <button type="button" class="btn" (click)="loadSummary()">{{ 'common.retry' | translate }}</button>
        </div>
      }

      <div class="grid">
        <div class="col-main">
          <section class="kpi-grid">
            @if (loadingSummary) {
              @for (placeholder of [1, 2, 3, 4]; track placeholder) {
                <div class="card kpi skeleton"></div>
              }
            } @else {
              @for (kpi of kpis; track kpi.key) {
                <div class="card kpi" [class]="'tone-' + kpi.tone">
                  <span class="kpi-icon">{{ kpi.icon }}</span>
                  <div class="kpi-body">
                    <span class="kpi-value">{{ kpi.value }}</span>
                    <span class="kpi-label">{{ kpi.key | translate }}</span>
                  </div>
                </div>
              }
            }
          </section>

          @if (widgetVisible('gender')) {
            <div class="card widget">
              <h3 class="card-title">{{ 'dashboard.gender.title' | translate }}</h3>
              @if (loadingSummary) {
                <div class="skeleton block"></div>
              } @else {
                <app-gender-donut [breakdown]="summary?.gender ?? null" />
              }
            </div>
          }

          @if (showTrend && widgetVisible('attendanceTrend')) {
            <div class="card widget">
              <div class="card-head">
                <h3 class="card-title">{{ 'dashboard.attendanceTrend.title' | translate }}</h3>
                @if (summary) {
                  <div class="head-stats">
                    <span class="pill present">{{ summary.presentToday }} {{ 'dashboard.attendanceTrend.present' | translate }}</span>
                    <span class="pill absent">{{ summary.absentToday }} {{ 'dashboard.attendanceTrend.absent' | translate }}</span>
                  </div>
                }
              </div>
              @if (loadingSummary) {
                <div class="skeleton block"></div>
              } @else {
                <app-attendance-trend [points]="summary?.attendanceTrend ?? []" />
              }
            </div>
          }

          @if (widgetVisible('starStudents')) {
            <div class="card widget">
              <h3 class="card-title">{{ 'dashboard.starStudents.title' | translate }}</h3>
              @if (loadingSummary) {
                <div class="skeleton block"></div>
              } @else {
                <app-star-students [students]="summary?.starStudents ?? []" />
              }
            </div>
          }

          @if (widgetVisible('roleInsights') && summary && hasRoleInsights) {
            <div class="card widget">
              <h3 class="card-title">{{ 'dashboard.roleInsights.title' | translate }}</h3>

              @if (showFinance) {
                <div class="insight-metrics">
                  <div class="metric">
                    <span class="metric-label">{{ 'dashboard.roleInsights.feesCollected' | translate }}</span>
                    <span class="metric-value success">{{ formatMoney(summary.feesCollected) }}</span>
                  </div>
                  <div class="metric">
                    <span class="metric-label">{{ 'dashboard.roleInsights.feesOverdue' | translate }}</span>
                    <span class="metric-value danger">{{ formatMoney(summary.feesOverdue) }}</span>
                  </div>
                  <div class="metric">
                    <span class="metric-label">{{ 'dashboard.roleInsights.collectionRate' | translate }}</span>
                    <span class="metric-value">{{ collectionRate }}%</span>
                  </div>
                </div>
              }

              @if (isTeacher && summary.mySections.length > 0) {
                <div class="insight-block">
                  <div class="insight-head">
                    <span>{{ 'dashboard.roleInsights.mySections' | translate }}</span>
                    <span class="pill warn">{{ summary.pendingGrading }} {{ 'dashboard.roleInsights.pendingGrading' | translate }}</span>
                  </div>
                  <ul class="section-list">
                    @for (section of summary.mySections; track section.id) {
                      <li>
                        <span class="section-name">{{ section.className }} · {{ section.name }}</span>
                        <span class="section-meta">
                          {{ section.studentCount }} {{ 'dashboard.roleInsights.students' | translate }}
                          @if (section.averagePercentage !== null) {
                            <span> · {{ 'dashboard.roleInsights.average' | translate }} {{ section.averagePercentage }}%</span>
                          }
                        </span>
                      </li>
                    }
                  </ul>
                </div>
              }

              @if (isParentOrStudent) {
                <div class="insight-metrics">
                  @if (summary.myAttendance) {
                    <div class="metric">
                      <span class="metric-label">{{ 'dashboard.roleInsights.attendanceRate' | translate }}</span>
                      <span class="metric-value">{{ summary.myAttendance.percentage }}%</span>
                    </div>
                  }
                  @if (summary.feeBalance !== null) {
                    <div class="metric">
                      <span class="metric-label">{{ 'dashboard.roleInsights.feeBalance' | translate }}</span>
                      <span class="metric-value danger">{{ formatMoney(summary.feeBalance) }}</span>
                    </div>
                  }
                  <div class="metric">
                    <span class="metric-label">{{ 'dashboard.roleInsights.upcomingExams' | translate }}</span>
                    <span class="metric-value">{{ upcomingExams.length }}</span>
                  </div>
                </div>
                @if (summary.myChildren.length > 0) {
                  <ul class="section-list">
                    @for (child of summary.myChildren; track child.id) {
                      <li>
                        <span class="section-name">{{ child.name }}</span>
                        <span class="section-meta">
                          {{ child.className || '—' }}@if (child.sectionName) {<span> · {{ child.sectionName }}</span>}
                          · {{ child.admissionNo }}
                        </span>
                      </li>
                    }
                  </ul>
                }
              }
            </div>
          }
        </div>

        <aside class="col-side">
          @if (widgetVisible('calendar')) {
            <div class="card widget">
              <h3 class="card-title">{{ 'dashboard.calendar.title' | translate }}</h3>
              <app-mini-calendar
                [eventsInput]="events"
                [selectedDate]="selectedDate"
                (selectedDateChange)="onDateSelected($event)" />
            </div>
          }

          @if (widgetVisible('upcoming')) {
            <div class="card widget">
              <div class="card-head">
                <h3 class="card-title">{{ 'dashboard.upcoming.title' | translate }}</h3>
                <a class="link" routerLink="/calendar">{{ 'dashboard.upcoming.viewAll' | translate }}</a>
              </div>
              @if (loadingUpcoming) {
                <div class="skeleton block"></div>
              } @else if (upcomingEvents.length === 0) {
                <p class="muted empty">{{ 'dashboard.upcoming.empty' | translate }}</p>
              } @else {
                <ul class="agenda">
                  @for (event of upcomingEvents; track event.id) {
                    <li>
                      <span class="tag" [class]="'tag-' + event.eventType.toLowerCase()">
                        {{ 'dashboard.agenda.types.' + event.eventType | translate }}
                      </span>
                      <div class="agenda-body">
                        <span class="agenda-title">{{ event.title }}</span>
                        <span class="agenda-meta">
                          <span>{{ event.startDate }}</span>
                          @if (!event.allDay && event.startTime) {
                            <span> · {{ formatTime(event.startTime) }}</span>
                          }
                          @if (event.location) {
                            <span> · {{ event.location }}</span>
                          }
                        </span>
                      </div>
                    </li>
                  }
                </ul>
              }
            </div>
          }

          @if (widgetVisible('agenda')) {
            <div class="card widget">
              <h3 class="card-title">{{ 'dashboard.agenda.title' | translate }}</h3>
              @if (loadingEvents) {
                <div class="skeleton block"></div>
              } @else if (agendaEvents.length === 0) {
                <p class="muted empty">{{ 'dashboard.agenda.noEvents' | translate }}</p>
              } @else {
                <ul class="agenda">
                  @for (event of agendaEvents; track event.id) {
                    <li>
                      <span class="tag" [class]="'tag-' + event.eventType.toLowerCase()">
                        {{ 'dashboard.agenda.types.' + event.eventType | translate }}
                      </span>
                      <div class="agenda-body">
                        <span class="agenda-title">{{ event.title }}</span>
                        <span class="agenda-meta">
                          @if (!event.allDay && event.startTime) {
                            <span>{{ formatTime(event.startTime) }}@if (event.endTime) {–{{ formatTime(event.endTime) }}}</span>
                          } @else {
                            <span>{{ 'dashboard.agenda.allDay' | translate }}</span>
                          }
                          @if (event.location) {
                            <span> · {{ event.location }}</span>
                          }
                        </span>
                        @if (event.description) {
                          <span class="agenda-desc">{{ event.description }}</span>
                        }
                      </div>
                    </li>
                  }
                </ul>
              }
            </div>
          }

          @if (widgetVisible('notices')) {
            <div class="card widget">
              <div class="card-head">
                <h3 class="card-title">{{ 'dashboard.notices.title' | translate }}</h3>
                <a class="link" routerLink="/notices">{{ 'dashboard.notices.viewAll' | translate }}</a>
              </div>
              @if (loadingNotices) {
                <div class="skeleton block"></div>
              } @else if (notices.length === 0) {
                <p class="muted empty">{{ 'common.noData' | translate }}</p>
              } @else {
                <ul class="notice-list">
                  @for (notice of notices; track notice.id) {
                    <li>
                      <span class="priority-dot" [class]="'prio-' + notice.priority.toLowerCase()"></span>
                      <div class="notice-body">
                        <span class="notice-title">{{ notice.title }}</span>
                        <span class="notice-meta">
                          <span class="badge" [class]="'badge-' + priorityClass(notice.priority)">
                            {{ 'dashboard.notices.priorities.' + notice.priority | translate }}
                          </span>
                          @if (notice.publishAt) {
                            <span>{{ notice.publishAt | date: 'short' }}</span>
                          }
                        </span>
                      </div>
                    </li>
                  }
                </ul>
              }
            </div>
          }
        </aside>
      </div>
    </div>

    @if (eventDialogOpen) {
      <div class="modal-backdrop" (click)="closeEventDialog()">
        <div class="modal card" (click)="$event.stopPropagation()">
          <h3 class="card-title">{{ 'dashboard.eventDialog.title' | translate }}</h3>
          <form [formGroup]="eventForm" (ngSubmit)="submitEvent()">
            <div class="field">
              <label>{{ 'dashboard.eventDialog.eventTitle' | translate }}</label>
              <input type="text" formControlName="title" />
            </div>
            <div class="field-row">
              <div class="field">
                <label>{{ 'dashboard.eventDialog.type' | translate }}</label>
                <select formControlName="eventType">
                  <option value="EVENT">{{ 'dashboard.agenda.types.EVENT' | translate }}</option>
                  <option value="EXAM">{{ 'dashboard.agenda.types.EXAM' | translate }}</option>
                  <option value="MEETING">{{ 'dashboard.agenda.types.MEETING' | translate }}</option>
                  <option value="HOLIDAY">{{ 'dashboard.agenda.types.HOLIDAY' | translate }}</option>
                  <option value="ACTIVITY">{{ 'dashboard.agenda.types.ACTIVITY' | translate }}</option>
                  <option value="PTM">{{ 'dashboard.agenda.types.PTM' | translate }}</option>
                  <option value="SPORTS">{{ 'dashboard.agenda.types.SPORTS' | translate }}</option>
                  <option value="NOTICE">{{ 'dashboard.agenda.types.NOTICE' | translate }}</option>
                  <option value="OTHER">{{ 'dashboard.agenda.types.OTHER' | translate }}</option>
                </select>
              </div>
              <div class="field">
                <label>{{ 'dashboard.eventDialog.location' | translate }}</label>
                <input type="text" formControlName="location" />
              </div>
            </div>
            <div class="field-row">
              <div class="field">
                <label>{{ 'dashboard.eventDialog.startDate' | translate }}</label>
                <input type="date" formControlName="startDate" />
              </div>
              <div class="field">
                <label>{{ 'dashboard.eventDialog.endDate' | translate }}</label>
                <input type="date" formControlName="endDate" />
              </div>
            </div>
            <div class="field">
              <label>{{ 'dashboard.eventDialog.description' | translate }}</label>
              <textarea rows="2" formControlName="description"></textarea>
            </div>
            @if (eventError) {
              <div class="alert alert-error">{{ 'common.error' | translate }}</div>
            }
            <div class="modal-actions">
              <button type="button" class="btn" (click)="closeEventDialog()">
                {{ 'common.cancel' | translate }}
              </button>
              <button type="submit" class="btn btn-primary" [disabled]="eventForm.invalid || savingEvent">
                {{ 'common.save' | translate }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: `
    .dashboard { display: flex; flex-direction: column; gap: 18px; }
    .page-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; flex-wrap: wrap; }
    h1 { font-size: 1.5rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); }
    .header-tools { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }

    .search { position: relative; }
    .search input {
      width: 240px;
      padding: 9px 12px;
      border: 1px solid var(--color-border);
      border-radius: 8px;
      background: var(--color-surface);
      color: var(--color-text);
      font: inherit;
    }
    .search input:focus { outline: none; border-color: var(--color-primary); }
    .search-panel {
      position: absolute;
      z-index: 30;
      top: calc(100% + 6px);
      right: 0;
      width: 300px;
      max-height: 320px;
      overflow-y: auto;
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: 10px;
      box-shadow: 0 12px 28px rgba(15, 23, 42, .18);
      padding: 6px;
    }
    .search-item {
      display: flex;
      align-items: center;
      gap: 10px;
      width: 100%;
      padding: 8px 10px;
      border: none;
      background: transparent;
      border-radius: 8px;
      cursor: pointer;
      text-align: left;
      color: var(--color-text);
      font: inherit;
    }
    .search-item:hover { background: var(--color-primary-soft); }
    .search-kind {
      font-size: .62rem;
      text-transform: uppercase;
      letter-spacing: .05em;
      color: var(--color-muted);
      min-width: 46px;
    }
    .search-body { display: flex; flex-direction: column; min-width: 0; }
    .search-label { font-weight: 600; }
    .search-sub { font-size: .75rem; color: var(--color-muted); }
    .search-empty { padding: 12px; color: var(--color-muted); text-align: center; font-size: .85rem; }

    .icon-btn {
      width: 38px; height: 38px;
      display: inline-flex; align-items: center; justify-content: center;
      border: 1px solid var(--color-border);
      border-radius: 8px;
      background: var(--color-surface);
      color: var(--color-text);
      font-size: 1.05rem;
      cursor: pointer;
    }
    .icon-btn:hover { border-color: var(--color-primary); color: var(--color-primary); }

    .customize { padding: 16px; }
    .customize-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
    .customize-head h3 { margin: 0; font-size: 1rem; }
    .customize-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; }
    .toggle { display: flex; align-items: center; gap: 8px; font-size: .9rem; cursor: pointer; }
    .toggle input { width: 16px; height: 16px; accent-color: var(--color-primary); }

    .quick-actions { display: flex; gap: 10px; flex-wrap: wrap; }
    .action {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 9px 14px;
      border-radius: 10px;
      border: 1px solid var(--color-border);
      background: var(--color-surface);
      color: var(--color-text);
      text-decoration: none;
      font-weight: 500;
      font-size: .9rem;
      cursor: pointer;
      transition: all .15s ease;
    }
    .action:hover { border-color: var(--color-primary); color: var(--color-primary); transform: translateY(-1px); }
    .action-icon { font-size: 1rem; }

    .alert-banner {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      border-radius: 10px;
      background: #fee2e2;
      border: 1px solid #fecaca;
      color: #b91c1c;
    }
    html.theme-dark .alert-banner { background: #401b1b; border-color: #7f1d1d; color: #fca5a5; }
    .alert-icon { font-size: 1.2rem; }
    .alert-body { flex: 1; display: flex; gap: 8px; flex-wrap: wrap; align-items: baseline; }
    .alert-link { color: inherit; font-weight: 600; }

    .error-card { padding: 20px; display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }

    .grid { display: grid; grid-template-columns: minmax(0, 2fr) minmax(0, 1fr); gap: 18px; align-items: start; }
    .col-main, .col-side { display: flex; flex-direction: column; gap: 18px; min-width: 0; }

    .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; }
    .kpi { display: flex; align-items: center; gap: 14px; padding: 18px; }
    .kpi-icon {
      width: 46px; height: 46px;
      display: inline-flex; align-items: center; justify-content: center;
      border-radius: 12px;
      font-size: 1.4rem;
      background: var(--color-primary-soft);
      flex-shrink: 0;
    }
    .kpi.tone-emerald .kpi-icon { background: #d1fae5; }
    .kpi.tone-amber .kpi-icon { background: #fef3c7; }
    .kpi.tone-rose .kpi-icon { background: #ffe4e6; }
    .kpi.tone-sky .kpi-icon { background: #e0f2fe; }
    .kpi-body { display: flex; flex-direction: column; min-width: 0; }
    .kpi-value { font-size: 1.5rem; font-weight: 700; line-height: 1.2; }
    .kpi-label { font-size: .82rem; color: var(--color-muted); }
    .kpi-hint { font-size: .72rem; color: var(--color-muted); }

    .widget { padding: 18px; }
    .card-title { margin: 0 0 14px; font-size: 1.02rem; }
    .card-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 14px; }
    .card-head .card-title { margin: 0; }
    .head-stats { display: flex; gap: 6px; }
    .pill { font-size: .72rem; padding: 3px 9px; border-radius: 20px; background: var(--color-bg); color: var(--color-muted); }
    .pill.present { background: #dcfce7; color: #15803d; }
    .pill.absent { background: #fee2e2; color: #b91c1c; }
    .pill.warn { background: #fef3c7; color: #b45309; }
    .link { font-size: .82rem; text-decoration: none; font-weight: 600; }

    .insight-metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 12px; }
    .metric {
      display: flex; flex-direction: column; gap: 4px;
      padding: 12px;
      border-radius: 10px;
      background: var(--color-bg);
    }
    .metric-label { font-size: .76rem; color: var(--color-muted); }
    .metric-value { font-size: 1.15rem; font-weight: 700; }
    .metric-value.success { color: var(--color-success); }
    .metric-value.danger { color: var(--color-danger); }
    .insight-block { margin-top: 14px; }
    .insight-head { display: flex; align-items: center; justify-content: space-between; font-size: .85rem; color: var(--color-muted); margin-bottom: 8px; }
    .section-list { list-style: none; margin: 0; padding: 0; }
    .section-list li {
      display: flex; align-items: center; justify-content: space-between; gap: 12px;
      padding: 9px 0;
      border-bottom: 1px solid var(--color-border);
      font-size: .9rem;
    }
    .section-list li:last-child { border-bottom: none; }
    .section-name { font-weight: 600; }
    .section-meta { color: var(--color-muted); font-size: .8rem; text-align: right; }

    .agenda { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 12px; }
    .agenda li { display: flex; gap: 10px; }
    .tag {
      align-self: flex-start;
      font-size: .66rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: .04em;
      padding: 3px 8px;
      border-radius: 6px;
      background: var(--color-bg);
      color: var(--color-muted);
      white-space: nowrap;
    }
    .tag-holiday { background: #fee2e2; color: #b91c1c; }
    .tag-exam { background: #fef3c7; color: #b45309; }
    .tag-ptm { background: #dbeafe; color: #1d4ed8; }
    .tag-sports { background: #dcfce7; color: #15803d; }
    .tag-notice { background: #f3e8ff; color: #7e22ce; }
    .tag-meeting { background: #dbeafe; color: #1d4ed8; }
    .tag-activity { background: #d1fae5; color: #047857; }
    .tag-event { background: #ede9fe; color: #6d28d9; }
    .agenda-body { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
    .agenda-title { font-weight: 600; }
    .agenda-meta { font-size: .78rem; color: var(--color-muted); }
    .agenda-desc { font-size: .8rem; color: var(--color-muted); }

    .notice-list { list-style: none; margin: 0; padding: 0; }
    .notice-list li { display: flex; gap: 10px; padding: 10px 0; border-bottom: 1px solid var(--color-border); }
    .notice-list li:last-child { border-bottom: none; }
    .priority-dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 7px; flex-shrink: 0; background: var(--color-muted); }
    .prio-urgent { background: var(--color-danger); }
    .prio-important { background: var(--color-warning); }
    .notice-body { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
    .notice-title { font-weight: 500; }
    .notice-meta { display: flex; align-items: center; gap: 8px; font-size: .75rem; color: var(--color-muted); }

    .empty { padding: 12px 0; text-align: center; font-size: .88rem; }

    .skeleton { position: relative; overflow: hidden; background: var(--color-bg); }
    .kpi.skeleton { height: 82px; }
    .skeleton.block { height: 150px; border-radius: 10px; }
    .skeleton::after {
      content: '';
      position: absolute; inset: 0;
      background: linear-gradient(90deg, transparent, rgba(148, 163, 184, .18), transparent);
      animation: shimmer 1.4s infinite;
    }
    @keyframes shimmer { 0% { transform: translateX(-100%); } 100% { transform: translateX(100%); } }

    .modal-backdrop {
      position: fixed; inset: 0; z-index: 60;
      background: rgba(15, 23, 42, .5);
      display: flex; align-items: center; justify-content: center;
      padding: 20px;
    }
    .modal { width: 100%; max-width: 460px; padding: 22px; }
    .modal .field { display: flex; flex-direction: column; gap: 6px; margin-bottom: 12px; }
    .modal label { font-size: .82rem; color: var(--color-muted); }
    .modal input, .modal select, .modal textarea {
      padding: 9px 11px;
      border: 1px solid var(--color-border);
      border-radius: 8px;
      background: var(--color-surface);
      color: var(--color-text);
      font: inherit;
    }
    .field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 6px; }

    @media (max-width: 1100px) {
      .grid { grid-template-columns: 1fr; }
    }
    @media (max-width: 640px) {
      .search input { width: 160px; }
      .field-row { grid-template-columns: 1fr; }
      .kpi-value { font-size: 1.3rem; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit {
  summary: DashboardSummary | null = null;
  events: SchoolEvent[] = [];
  upcomingEvents: SchoolEvent[] = [];
  notices: DashboardNotice[] = [];
  selectedDate = this.todayIso();

  loadingSummary = true;
  loadingEvents = true;
  loadingUpcoming = true;
  loadingNotices = true;
  summaryError = false;

  showCustomize = false;
  searchOpen = false;
  searchTerm = '';

  eventDialogOpen = false;
  savingEvent = false;
  eventError = false;

  readonly widgetOptions: Array<{ id: WidgetId; labelKey: string }> = [
    { id: 'gender', labelKey: 'dashboard.customize.widgets.gender' },
    { id: 'attendanceTrend', labelKey: 'dashboard.customize.widgets.attendanceTrend' },
    { id: 'starStudents', labelKey: 'dashboard.customize.widgets.starStudents' },
    { id: 'roleInsights', labelKey: 'dashboard.customize.widgets.roleInsights' },
    { id: 'calendar', labelKey: 'dashboard.customize.widgets.calendar' },
    { id: 'agenda', labelKey: 'dashboard.customize.widgets.agenda' },
    { id: 'upcoming', labelKey: 'dashboard.customize.widgets.upcoming' },
    { id: 'notices', labelKey: 'dashboard.customize.widgets.notices' },
  ];

  eventForm;

  private widgets: Record<WidgetId, boolean> = this.storedWidgets();

  constructor(
    private dashboardService: DashboardService,
    private auth: AuthService,
    readonly theme: ThemeService,
    private fb: FormBuilder,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.eventForm = this.fb.nonNullable.group({
      title: ['', Validators.required],
      eventType: ['EVENT'],
      location: [''],
      startDate: [this.todayIso(), Validators.required],
      endDate: [this.todayIso()],
      description: [''],
    });
  }

  ngOnInit(): void {
    this.loadSummary();
    this.loadEvents();
    this.loadUpcoming();
    this.loadNotices();
  }

  get displayName(): string {
    return this.auth.currentUser?.displayName ?? this.auth.currentUser?.username ?? '';
  }

  get isAdmin(): boolean {
    return this.auth.hasRole('ADMIN') || this.auth.hasRole('SUPER_ADMIN');
  }

  get isTeacher(): boolean {
    return this.auth.hasRole('TEACHER');
  }

  get showStaff(): boolean {
    return this.auth.hasAnyRole(STAFF_ROLES);
  }

  get showTrend(): boolean {
    return this.showStaff;
  }

  get showFinance(): boolean {
    return this.auth.hasAnyPermission(['FEE_STRUCTURE_MANAGE', 'FEE_PAYMENT_RECORD']) || this.isAdmin;
  }

  get isParentOrStudent(): boolean {
    return this.auth.hasRole('PARENT') || this.auth.hasRole('STUDENT');
  }

  get canManageEvents(): boolean {
    return this.auth.hasPermission('EVENT_MANAGE');
  }

  get hasRoleInsights(): boolean {
    return this.showFinance || this.isTeacher || this.isParentOrStudent;
  }

  get kpis(): KpiCard[] {
    const data = this.summary;
    if (!data) {
      return [];
    }
    const cards: KpiCard[] = [
      { key: 'dashboard.totalStudents', value: String(data.totalStudents), icon: '👩‍🎓', tone: 'indigo' },
      { key: 'dashboard.totalTeachers', value: String(data.totalTeachers), icon: '👨‍🏫', tone: 'emerald' },
    ];
    if (this.showStaff) {
      cards.push({ key: 'dashboard.totalStaff', value: String(data.totalStaff), icon: '🧑‍💼', tone: 'amber' });
    }
    cards.push({ key: 'dashboard.totalAwards', value: String(data.totalAwards), icon: '🏆', tone: 'rose' });
    if (!this.showStaff) {
      if (data.myAttendance) {
        cards.push({ key: 'dashboard.roleInsights.attendanceRate', value: data.myAttendance.percentage + '%', icon: '✅', tone: 'sky' });
      }
      if (data.feeBalance !== null) {
        cards.push({ key: 'dashboard.feeBalance', value: this.formatMoney(data.feeBalance), icon: '💳', tone: 'sky' });
      }
    }
    return cards;
  }

  get quickActions(): QuickAction[] {
    const actions: QuickAction[] = [
      { key: 'markAttendance', labelKey: 'dashboard.quickActions.markAttendance', icon: '✅', route: '/attendance', permissions: ['ATTENDANCE_MARK'] },
      { key: 'addStudent', labelKey: 'dashboard.quickActions.addStudent', icon: '👩‍🎓', route: '/students', permissions: ['STUDENT_CREATE'] },
      { key: 'publishNotice', labelKey: 'dashboard.quickActions.publishNotice', icon: '📢', route: '/notices', permissions: ['NOTICE_CREATE'] },
      { key: 'collectFee', labelKey: 'dashboard.quickActions.collectFee', icon: '💳', route: '/fees', permissions: ['FEE_PAYMENT_RECORD'] },
      { key: 'addEvent', labelKey: 'dashboard.quickActions.addEvent', icon: '📅', route: '/calendar', permissions: ['EVENT_MANAGE'] },
      { key: 'admitCards', labelKey: 'examinations.tabs.admitCards', icon: '🎫', route: '/examinations/admit-cards', permissions: ['ADMIT_CARD_READ'] },
      { key: 'reportCards', labelKey: 'examinations.tabs.reportCards', icon: '📄', route: '/examinations/report-cards', permissions: ['REPORT_CARD_READ'] },
      { key: 'marksheets', labelKey: 'downloads.tabs.marksheet', icon: '📥', route: '/downloads/marksheet', permissions: ['MARKSHEET_READ'] },
    ];
    return actions.filter((action) => this.auth.hasAnyPermission(action.permissions));
  }

  get urgentNotices(): DashboardNotice[] {
    return this.notices.filter((n) => n.priority === 'URGENT');
  }

  get agendaEvents(): SchoolEvent[] {
    return this.events.filter((event) => {
      const end = event.endDate ?? event.startDate;
      return this.selectedDate >= event.startDate && this.selectedDate <= end;
    });
  }

  get upcomingExams(): SchoolEvent[] {
    return this.events.filter((event) => event.eventType === 'EXAM' && (event.endDate ?? event.startDate) >= this.todayIso());
  }

  get collectionRate(): number {
    const data = this.summary;
    if (!data) {
      return 0;
    }
    const target = data.feesCollected + data.feesOverdue;
    return target <= 0 ? 100 : Math.round((data.feesCollected / target) * 100);
  }

  get searchResults(): SearchResult[] {
    const term = this.searchTerm.trim().toLowerCase();
    if (term.length < 2) {
      return [];
    }
    const results: SearchResult[] = [];
    for (const event of this.events) {
      if (event.title.toLowerCase().includes(term)) {
        results.push({ label: event.title, sublabel: event.startDate, kind: 'event', date: event.startDate });
      }
    }
    for (const student of this.summary?.starStudents ?? []) {
      if (student.name.toLowerCase().includes(term)) {
        results.push({
          label: student.name,
          sublabel: student.className ?? '',
          kind: 'student',
          route: ['/students', student.studentId],
        });
      }
    }
    return results.slice(0, 8);
  }

  loadSummary(): void {
    this.loadingSummary = true;
    this.summaryError = false;
    this.cdr.markForCheck();
    this.dashboardService.summary().subscribe((data) => {
      this.summary = data;
      this.summaryError = data === null;
      this.loadingSummary = false;
      this.cdr.markForCheck();
    });
  }

  loadEvents(): void {
    this.loadingEvents = true;
    this.cdr.markForCheck();
    const from = this.addDays(this.todayIso(), -30);
    const to = this.addDays(this.todayIso(), 90);
    this.dashboardService.events(from, to).subscribe((events) => {
      this.events = events;
      this.loadingEvents = false;
      this.cdr.markForCheck();
    });
  }

  loadUpcoming(): void {
    this.loadingUpcoming = true;
    this.cdr.markForCheck();
    this.dashboardService.upcoming(30).subscribe((events) => {
      this.upcomingEvents = events.slice(0, 5);
      this.loadingUpcoming = false;
      this.cdr.markForCheck();
    });
  }

  loadNotices(): void {
    this.loadingNotices = true;
    this.cdr.markForCheck();
    this.dashboardService.notices().subscribe((notices) => {
      this.notices = notices.slice(0, 6);
      this.loadingNotices = false;
      this.cdr.markForCheck();
    });
  }

  onDateSelected(date: string): void {
    this.selectedDate = date;
    this.cdr.markForCheck();
  }

  toggleTheme(): void {
    this.theme.toggle();
  }

  widgetVisible(id: WidgetId): boolean {
    return this.widgets[id] !== false;
  }

  toggleWidget(id: WidgetId): void {
    this.widgets = { ...this.widgets, [id]: !this.widgetVisible(id) };
    localStorage.setItem(WIDGET_STORAGE_KEY, JSON.stringify(this.widgets));
    this.cdr.markForCheck();
  }

  resetWidgets(): void {
    this.widgets = { ...DEFAULT_WIDGETS };
    localStorage.removeItem(WIDGET_STORAGE_KEY);
    this.cdr.markForCheck();
  }

  onSearch(event: Event): void {
    this.searchTerm = (event.target as HTMLInputElement).value;
    this.searchOpen = true;
    this.cdr.markForCheck();
  }

  onSearchBlur(): void {
    setTimeout(() => {
      this.searchOpen = false;
      this.cdr.markForCheck();
    }, 150);
  }

  onResultClick(result: SearchResult): void {
    this.searchTerm = '';
    this.searchOpen = false;
    if (result.kind === 'event' && result.date) {
      this.selectedDate = result.date;
    } else if (result.route) {
      void this.router.navigate(result.route);
    }
    this.cdr.markForCheck();
  }

  openEventDialog(): void {
    this.eventForm.reset({
      title: '',
      eventType: 'EVENT',
      location: '',
      startDate: this.selectedDate,
      endDate: this.selectedDate,
      description: '',
    });
    this.eventError = false;
    this.eventDialogOpen = true;
    this.cdr.markForCheck();
  }

  closeEventDialog(): void {
    this.eventDialogOpen = false;
    this.cdr.markForCheck();
  }

  submitEvent(): void {
    if (this.eventForm.invalid || this.savingEvent) {
      return;
    }
    this.savingEvent = true;
    this.eventError = false;
    const value = this.eventForm.getRawValue();
    this.dashboardService
      .createEvent({
        title: value.title,
        description: value.description || null,
        eventType: value.eventType as SchoolEvent['eventType'],
        startDate: value.startDate,
        endDate: value.endDate || value.startDate,
        allDay: true,
        startTime: null,
        endTime: null,
        location: value.location || null,
        visibilityScope: 'SCHOOL_WIDE',
      })
      .subscribe((created) => {
        this.savingEvent = false;
        if (created) {
          this.eventDialogOpen = false;
          this.loadEvents();
          this.loadUpcoming();
        } else {
          this.eventError = true;
        }
        this.cdr.markForCheck();
      });
  }

  priorityClass(priority: string): string {
    switch (priority) {
      case 'URGENT':
        return 'danger';
      case 'IMPORTANT':
        return 'warning';
      default:
        return 'muted';
    }
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: 'USD',
      maximumFractionDigits: 0,
    }).format(value);
  }

  formatTime(time: string): string {
    const [hours, minutes] = time.split(':').map(Number);
    const date = new Date();
    date.setHours(hours, minutes ?? 0, 0, 0);
    return date.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
  }

  private storedWidgets(): Record<WidgetId, boolean> {
    try {
      const raw = localStorage.getItem(WIDGET_STORAGE_KEY);
      if (raw) {
        return { ...DEFAULT_WIDGETS, ...(JSON.parse(raw) as Partial<Record<WidgetId, boolean>>) };
      }
    } catch {
      // ignore malformed preferences
    }
    return { ...DEFAULT_WIDGETS };
  }

  private todayIso(): string {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${now.getFullYear()}-${month}-${day}`;
  }

  private addDays(iso: string, days: number): string {
    const [year, month, day] = iso.split('-').map(Number);
    const date = new Date(year, month - 1, day + days);
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${m}-${d}`;
  }
}
