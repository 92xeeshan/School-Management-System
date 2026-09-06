import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { ApiResponse } from '../../core/models/api.model';

interface DashboardStats {
  totalStudents: number;
  totalTeachers: number;
  presentToday: number;
  feesCollected: number;
}

interface Notice {
  id: string;
  title: string;
  publishedOn: string;
}

interface BackendDashboard {
  totalStudents: number;
  totalTeachers: number;
  presentToday: number;
  feesCollected: number;
  publishedNotices: number;
  unreadNotices: number;
}

interface BackendNotice {
  id: string;
  title: string;
  body: string;
  publishAt: string;
  status: string;
}

@Component({
  selector: 'app-dashboard',
  imports: [TranslateModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'dashboard.title' | translate }}</h1>
          <p class="muted">{{ 'dashboard.subtitle' | translate }}</p>
        </div>
      </div>

      <div class="welcome">
        <h2>{{ 'dashboard.welcomeMessage' | translate: { name: displayName } }}</h2>
      </div>

      <div class="cards">
        <div class="card stat">
          <div class="stat-icon">👩‍🎓</div>
          <div class="stat-body">
            <div class="stat-value">{{ stats?.totalStudents ?? '—' }}</div>
            <div class="stat-label">{{ 'dashboard.totalStudents' | translate }}</div>
          </div>
        </div>
        <div class="card stat">
          <div class="stat-icon">👨‍🏫</div>
          <div class="stat-body">
            <div class="stat-value">{{ stats?.totalTeachers ?? '—' }}</div>
            <div class="stat-label">{{ 'dashboard.totalTeachers' | translate }}</div>
          </div>
        </div>
        <div class="card stat">
          <div class="stat-icon">✅</div>
          <div class="stat-body">
            <div class="stat-value">{{ stats?.presentToday ?? '—' }}</div>
            <div class="stat-label">{{ 'dashboard.presentToday' | translate }}</div>
          </div>
        </div>
        <div class="card stat">
          <div class="stat-icon">💳</div>
          <div class="stat-body">
            <div class="stat-value">{{ formatMoney(stats?.feesCollected ?? 0) }}</div>
            <div class="stat-label">{{ 'dashboard.feesCollected' | translate }}</div>
          </div>
        </div>
      </div>

      <div class="card">
        <h3 class="card-title">{{ 'dashboard.recentNotices' | translate }}</h3>
        @if (notices.length === 0) {
          <p class="muted">{{ 'common.noData' | translate }}</p>
        } @else {
          <ul class="notice-list">
            @for (notice of notices; track notice.id) {
              <li>
                <span class="notice-dot"></span>
                <div>
                  <div class="notice-title">{{ notice.title }}</div>
                  <div class="notice-date">{{ notice.publishedOn }}</div>
                </div>
              </li>
            }
          </ul>
        }
      </div>
    </div>
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    h1 { font-size: 1.5rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); }
    .welcome h2 { font-size: 1.1rem; font-weight: 600; margin: 0 0 20px; }
    .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .stat { display: flex; align-items: center; gap: 16px; padding: 20px; }
    .stat-icon { font-size: 2rem; }
    .stat-value { font-size: 1.6rem; font-weight: 700; }
    .stat-label { color: var(--color-muted); font-size: .9rem; }
    .card-title { margin: 0 0 14px; font-size: 1.05rem; }
    .notice-list { list-style: none; margin: 0; padding: 0; }
    .notice-list li { display: flex; gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--color-border); }
    .notice-list li:last-child { border-bottom: none; }
    .notice-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--color-primary); margin-top: 8px; flex-shrink: 0; }
    .notice-title { font-weight: 500; }
    .notice-date { color: var(--color-muted); font-size: .82rem; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit {
  stats: DashboardStats | null = null;
  notices: Notice[] = [];

  constructor(private http: HttpClient, private auth: AuthService, private cdr: ChangeDetectorRef) {}

  get displayName(): string {
    return this.auth.currentUser?.displayName ?? this.auth.currentUser?.username ?? '';
  }

  ngOnInit(): void {
    this.http.get<ApiResponse<BackendDashboard>>('/api/dashboard').subscribe({
      next: (res) => {
        this.stats = {
          totalStudents: res.data.totalStudents,
          totalTeachers: res.data.totalTeachers,
          presentToday: res.data.presentToday,
          feesCollected: Number(res.data.feesCollected ?? 0),
        };
        this.cdr.markForCheck();
      },
      error: () => {
        this.stats = null;
        this.cdr.markForCheck();
      },
    });
    this.http.get<ApiResponse<BackendNotice[]>>('/api/notices/published').subscribe({
      next: (res) => {
        this.notices = res.data.map((n) => ({
          id: n.id,
          title: n.title,
          publishedOn: n.publishAt ? new Date(n.publishAt).toLocaleDateString() : '',
        }));
        this.cdr.markForCheck();
      },
      error: () => {
        this.notices = [];
        this.cdr.markForCheck();
      },
    });
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(value);
  }
}
