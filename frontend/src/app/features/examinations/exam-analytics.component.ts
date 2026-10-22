import { ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ApiError, ApiResponse } from '../../core/models/api.model';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';

interface YearOption { id: string; name: string; current: boolean }
interface SectionOption { id: string; name: string }
interface ClassOption { id: string; name: string; sections: SectionOption[] }
interface AnalyticsOptions {
  academicYears: YearOption[];
  classes: ClassOption[];
  examTerms: string[];
  studentView: boolean;
  defaultStudentId: string | null;
  defaultClassId: string | null;
  defaultSectionId: string | null;
}
interface SubjectDistribution {
  subjectId: string;
  subjectName: string;
  studentCount: number;
  averagePercent: number;
}
interface TopPerformer {
  rank: number;
  studentId: string;
  studentName: string;
  admissionNo: string;
  rollNumber: number | null;
  percentage: number;
  overallGrade: string | null;
  result: string;
}
interface ClassAnalytics {
  academicYearId: string;
  academicYearName: string;
  classId: string;
  className: string;
  sectionId: string;
  sectionName: string;
  examTerm: string;
  studentCount: number;
  publishedCount: number;
  classAverage: number;
  passPercent: number;
  subjectDistribution: SubjectDistribution[];
  topPerformers: TopPerformer[];
}
interface TrendPoint {
  examTerm: string;
  published: boolean;
  percentage: number;
  overallGrade: string | null;
  result: string;
}
interface StudentTrend {
  studentId: string;
  studentName: string;
  admissionNo: string;
  academicYearName: string;
  className: string;
  sectionName: string;
  points: TrendPoint[];
}

@Component({
  selector: 'app-exam-analytics',
  imports: [TranslateModule, RouterLink, EmptyStateComponent],
  template: `
    <div class="tab-page">
      <div class="page-header">
        <div>
          <h2>{{ 'examinations.analyticsPage.title' | translate }}</h2>
          <p class="muted">{{ 'examinations.analyticsPage.subtitle' | translate }}</p>
        </div>
      </div>

      @if (pageError) {
        <p class="banner error">{{ pageError }}</p>
      }

      @if (studentView) {
        <div class="portal">
          <a class="portal-card" routerLink="/examinations/admit-cards">
            <strong>{{ 'examinations.analyticsPage.portalSchedules' | translate }}</strong>
            <span>{{ 'examinations.analyticsPage.portalSchedulesHint' | translate }}</span>
          </a>
          <a class="portal-card" routerLink="/examinations/report-cards">
            <strong>{{ 'examinations.analyticsPage.portalReports' | translate }}</strong>
            <span>{{ 'examinations.analyticsPage.portalReportsHint' | translate }}</span>
          </a>
        </div>
      }

      <div class="filters">
        <select [value]="yearId" (change)="onYearChange($event)">
          @for (year of options?.academicYears ?? []; track year.id) {
            <option [value]="year.id">{{ year.name }}</option>
          }
        </select>
        @if (!studentView) {
          <select [value]="examTerm" (change)="onTermChange($event)">
            @for (term of options?.examTerms ?? []; track term) {
              <option [value]="term">{{ termLabel(term) | translate }}</option>
            }
          </select>
          <select [value]="classId" (change)="onClassChange($event)">
            <option value="">{{ 'examinations.analyticsPage.selectClass' | translate }}</option>
            @for (klass of options?.classes ?? []; track klass.id) {
              <option [value]="klass.id">{{ klass.name }}</option>
            }
          </select>
          <select [value]="sectionId" (change)="onSectionChange($event)" [disabled]="!classId">
            <option value="">{{ 'examinations.analyticsPage.selectSection' | translate }}</option>
            @for (section of filterSections; track section.id) {
              <option [value]="section.id">{{ section.name }}</option>
            }
          </select>
        }
      </div>

      @if (!studentView && classAnalytics) {
        <section class="kpi-grid">
          <div class="card kpi">
            <span class="kpi-value">{{ formatPercent(classAnalytics.classAverage) }}</span>
            <span class="kpi-label">{{ 'examinations.analyticsPage.classAverage' | translate }}</span>
          </div>
          <div class="card kpi">
            <span class="kpi-value">{{ formatPercent(classAnalytics.passPercent) }}</span>
            <span class="kpi-label">{{ 'examinations.analyticsPage.passPercent' | translate }}</span>
          </div>
          <div class="card kpi">
            <span class="kpi-value">{{ classAnalytics.publishedCount }} / {{ classAnalytics.studentCount }}</span>
            <span class="kpi-label">{{ 'examinations.analyticsPage.publishedCount' | translate }}</span>
          </div>
        </section>

        <div class="grid">
          <div class="card">
            <h3>{{ 'examinations.analyticsPage.subjectDistribution' | translate }}</h3>
            @if (classAnalytics.subjectDistribution.length === 0) {
              <app-empty-state icon="bar_chart" [title]="'examinations.analyticsPage.noPublished' | translate" [hint]="'examinations.analyticsPage.noPublishedHint' | translate" />
            } @else {
              <div class="bars">
                @for (subject of classAnalytics.subjectDistribution; track subject.subjectId) {
                  <div class="bar-row">
                    <span class="bar-label">{{ subject.subjectName }}</span>
                    <div class="bar-track">
                      <div class="bar-fill" [style.width.%]="subject.averagePercent"></div>
                    </div>
                    <span class="bar-value">{{ formatPercent(subject.averagePercent) }}</span>
                  </div>
                }
              </div>
            }
          </div>
          <div class="card">
            <h3>{{ 'examinations.analyticsPage.topPerformers' | translate }}</h3>
            @if (classAnalytics.topPerformers.length === 0) {
              <p class="muted">{{ 'common.noData' | translate }}</p>
            } @else {
              <ul class="rank-list">
                @for (row of classAnalytics.topPerformers; track row.studentId) {
                  <li>
                    <button type="button" class="rank-btn" (click)="loadTrend(row.studentId)">
                      <span class="rank" [class.top]="row.rank === 1">{{ row.rank }}</span>
                      <span class="rank-body">
                        <strong>{{ row.studentName }}</strong>
                        <span class="muted">{{ row.admissionNo }}</span>
                      </span>
                      <span class="rank-score">{{ formatPercent(row.percentage) }} · {{ row.overallGrade || '—' }}</span>
                    </button>
                  </li>
                }
              </ul>
            }
          </div>
        </div>
      }

      @if (!studentView && !sectionId) {
        <div class="card">
          <app-empty-state icon="analytics" [title]="'examinations.analyticsPage.selectSectionHint' | translate" [hint]="'examinations.analyticsPage.body' | translate" />
        </div>
      }

      <div class="card">
        <h3>{{ 'examinations.analyticsPage.trendTitle' | translate }}</h3>
        @if (trend && trend.points.length > 0) {
          <p class="muted">{{ trend.studentName }} · {{ trend.className }} {{ trend.sectionName }}</p>
          <div class="chart">
            @for (point of trend.points; track point.examTerm) {
              <div class="col">
                <div class="bar-wrap">
                  <div class="bar"
                       [class.good]="point.percentage >= 80"
                       [class.ok]="point.percentage >= 33 && point.percentage < 80"
                       [class.low]="point.percentage < 33"
                       [style.height.%]="point.percentage"></div>
                </div>
                <span class="value">{{ formatPercent(point.percentage) }}</span>
                <span class="x-label">{{ termLabel(point.examTerm) | translate }}</span>
              </div>
            }
          </div>
        } @else if (!loading) {
          <app-empty-state icon="show_chart" [title]="'examinations.analyticsPage.noTrend' | translate" [hint]="'examinations.analyticsPage.noPublishedHint' | translate" />
        }
        @if (loading) {
          <p class="center muted">{{ 'common.loading' | translate }}</p>
        }
      </div>
    </div>
  `,
  styles: `
    h2 { font-size: 1.2rem; margin: 0 0 4px; }
    h3 { margin: 0 0 12px; font-size: 1rem; }
    .muted { color: var(--color-muted); margin: 0; }
    .banner.error { background: #fee2e2; color: #b91c1c; padding: 10px 12px; border-radius: 8px; }
    .filters { display: flex; flex-wrap: wrap; gap: 8px; margin: 16px 0; }
    .filters select { padding: 8px 10px; border-radius: 8px; border: 1px solid var(--color-border); background: var(--color-surface); color: var(--color-text); }
    .portal { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px; }
    .portal-card {
      display: flex; flex-direction: column; gap: 4px;
      padding: 16px; border-radius: 12px; text-decoration: none;
      background: var(--color-surface); border: 1px solid var(--color-border); color: var(--color-text);
    }
    .portal-card span { color: var(--color-muted); font-size: .85rem; }
    .kpi-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
    .kpi { padding: 16px; }
    .kpi-value { display: block; font-size: 1.4rem; font-weight: 700; }
    .kpi-label { color: var(--color-muted); font-size: .85rem; }
    .grid { display: grid; grid-template-columns: 1.2fr 1fr; gap: 12px; margin-bottom: 16px; }
    .card { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 12px; padding: 16px; margin-bottom: 16px; }
    .bars { display: flex; flex-direction: column; gap: 10px; }
    .bar-row { display: grid; grid-template-columns: 120px 1fr 64px; gap: 8px; align-items: center; }
    .bar-label { font-size: .85rem; }
    .bar-track { height: 10px; background: var(--color-primary-soft); border-radius: 999px; overflow: hidden; }
    .bar-fill { height: 100%; background: var(--color-primary); border-radius: 999px; }
    .bar-value { font-size: .8rem; font-weight: 600; text-align: right; }
    .rank-list { list-style: none; margin: 0; padding: 0; }
    .rank-btn {
      width: 100%; display: flex; align-items: center; gap: 10px;
      padding: 8px 0; border: 0; background: transparent; color: inherit; cursor: pointer; text-align: left;
    }
    .rank { width: 22px; height: 22px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; font-size: .72rem; font-weight: 700; background: var(--color-bg); color: var(--color-muted); }
    .rank.top { background: #fef3c7; color: #b45309; }
    .rank-body { flex: 1; display: flex; flex-direction: column; }
    .rank-score { font-weight: 600; font-size: .85rem; }
    .chart { display: flex; align-items: flex-end; gap: 10px; height: 180px; padding-top: 8px; }
    .col { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; height: 100%; }
    .bar-wrap { width: 100%; max-width: 46px; flex: 1; display: flex; align-items: flex-end; background: var(--color-primary-soft); border-radius: 8px 8px 4px 4px; overflow: hidden; }
    .bar { width: 100%; min-height: 3px; border-radius: 8px 8px 4px 4px; }
    .bar.good { background: linear-gradient(180deg, #34d399, #16a34a); }
    .bar.ok { background: linear-gradient(180deg, #fbbf24, #d97706); }
    .bar.low { background: linear-gradient(180deg, #f87171, #dc2626); }
    .value { font-size: .72rem; font-weight: 600; }
    .x-label { font-size: .72rem; color: var(--color-muted); text-align: center; }
    .center { text-align: center; }
    @media (max-width: 768px) {
      .portal, .kpi-grid, .grid { grid-template-columns: 1fr; }
      .bar-row { grid-template-columns: 1fr; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamAnalyticsComponent implements OnInit {
  options: AnalyticsOptions | null = null;
  classAnalytics: ClassAnalytics | null = null;
  trend: StudentTrend | null = null;
  yearId = '';
  classId = '';
  sectionId = '';
  examTerm = 'TERM';
  studentView = false;
  loading = false;
  pageError = '';

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private auth: AuthService,
    private destroyRef: DestroyRef,
  ) {}

  get filterSections(): SectionOption[] {
    return this.options?.classes.find((klass) => klass.id === this.classId)?.sections ?? [];
  }

  ngOnInit(): void {
    this.loadOptions();
    this.auth.activeChild$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((child) => {
      if (this.studentView && this.yearId) {
        this.fetchTrend(child?.studentId);
      }
    });
  }

  termLabel(term: string): string {
    return `academics.exam${term.charAt(0)}${term.slice(1).toLowerCase()}`;
  }

  formatPercent(value: number | null | undefined): string {
    if (value == null) {
      return '—';
    }
    return `${value}%`;
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
    this.classAnalytics = null;
    this.trend = null;
    this.cdr.markForCheck();
  }

  onSectionChange(event: Event): void {
    this.sectionId = (event.target as HTMLSelectElement).value;
    this.reload();
  }

  loadTrend(studentId: string): void {
    this.fetchTrend(studentId);
  }

  private loadOptions(): void {
    this.http.get<ApiResponse<AnalyticsOptions>>('/api/exam-analytics/options', { params: this.childParams() })
      .subscribe({
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
      this.fetchTrend();
      return;
    }
    if (!this.sectionId) {
      this.classAnalytics = null;
      this.cdr.markForCheck();
      return;
    }
    this.loading = true;
    let params = new HttpParams()
      .set('academicYearId', this.yearId)
      .set('examTerm', this.examTerm)
      .set('classId', this.classId)
      .set('sectionId', this.sectionId);
    this.http.get<ApiResponse<ClassAnalytics>>('/api/exam-analytics/class', { params }).subscribe({
      next: (res) => {
        this.classAnalytics = res.data;
        this.loading = false;
        const first = res.data.topPerformers[0];
        if (first) {
          this.fetchTrend(first.studentId);
        } else {
          this.trend = null;
          this.cdr.markForCheck();
        }
      },
      error: (err) => {
        this.loading = false;
        this.fail(err);
      },
    });
  }

  private fetchTrend(studentId?: string): void {
    let params = new HttpParams().set('academicYearId', this.yearId);
    const childId = studentId || this.auth.activeChild?.studentId || undefined;
    if (childId) {
      params = params.set('studentId', childId);
    }
    this.loading = true;
    this.http.get<ApiResponse<StudentTrend>>('/api/exam-analytics/trend', { params }).subscribe({
      next: (res) => {
        this.trend = res.data;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loading = false;
        this.fail(err);
      },
    });
  }

  private childParams(): HttpParams {
    let params = new HttpParams();
    const childId = this.auth.activeChild?.studentId;
    if (this.auth.hasRole('PARENT') && childId) {
      params = params.set('studentId', childId);
    }
    return params;
  }

  private fail(err: HttpErrorResponse): void {
    const body = err.error as ApiError | undefined;
    this.pageError = body?.message || err.message || 'Error';
    this.cdr.markForCheck();
  }
}
