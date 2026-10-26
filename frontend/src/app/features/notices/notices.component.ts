import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ApiResponse, PagedResponse } from '../../core/models/api.model';
import { AuthService } from '../../core/auth/auth.service';
import { PageHeaderComponent } from '../../layout/page-header/page-header.component';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';
import { DirectoryPagerComponent, SortDirection, sortIndicator, toggleSort } from '../../layout/directory-pager/directory-pager.component';

interface Notice {
  id: string;
  title: string;
  content: string;
  audience: string;
  status: 'PUBLISHED' | 'DRAFT';
  publishedOn: string | null;
  expiresOn: string | null;
}

interface BackendNotice {
  id: string;
  title: string;
  body: string;
  visibilityScope: string;
  publishAt: string | null;
  expiresAt: string | null;
  status: string;
}

function toNotice(n: BackendNotice): Notice {
  return {
    id: n.id,
    title: n.title,
    content: n.body,
    audience: n.visibilityScope,
    status: (n.status === 'PUBLISHED' ? 'PUBLISHED' : 'DRAFT'),
    publishedOn: n.publishAt ? new Date(n.publishAt).toISOString().slice(0, 10) : null,
    expiresOn: n.expiresAt ? new Date(n.expiresAt).toISOString().slice(0, 10) : null,
  };
}

@Component({
  selector: 'app-notices',
  imports: [TranslateModule, ReactiveFormsModule, PageHeaderComponent, EmptyStateComponent, DirectoryPagerComponent],
  template: `
    <div class="page">
      <app-page-header [title]="'notices.title' | translate" [subtitle]="'notices.subtitle' | translate" />

      @if (canCreate) {
      <div class="card composer">
        <h3 class="card-title">{{ 'notices.addNotice' | translate }}</h3>
        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="form-grid">
            <div class="field span-2">
              <label>{{ 'notices.titleField' | translate }}</label>
              <input type="text" formControlName="title" />
            </div>
            <div class="field span-2">
              <label>{{ 'notices.content' | translate }}</label>
              <textarea rows="3" formControlName="content"></textarea>
            </div>
            <div class="field">
              <label>{{ 'notices.audience' | translate }}</label>
              <select formControlName="audience">
                <option value="EVERYONE">{{ 'notices.everyone' | translate }}</option>
                <option value="TEACHERS">{{ 'notices.teachers' | translate }}</option>
                <option value="PARENTS">{{ 'notices.parents' | translate }}</option>
              </select>
            </div>
            <div class="field">
              <label>{{ 'common.status' | translate }}</label>
              <select formControlName="status">
                <option value="PUBLISHED">{{ 'notices.publish' | translate }}</option>
                <option value="DRAFT">{{ 'notices.draft' | translate }}</option>
              </select>
            </div>
            <div class="field">
              <label>{{ 'notices.expiresOn' | translate }}</label>
              <input type="date" formControlName="expiresOn" />
            </div>
          </div>
          <div class="form-actions">
            <button class="btn btn-primary" type="submit" [disabled]="form.invalid">{{ 'notices.publish' | translate }}</button>
          </div>
        </form>
      </div>
      }

      <div class="card">
        <div class="card-toolbar">
          <input class="search" type="search" [value]="query" [placeholder]="'common.search' | translate"
                 (input)="onSearch($any($event.target).value)" />
        </div>
        @if (notices.length === 0) {
          <app-empty-state icon="campaign" [title]="'common.noData' | translate" [hint]="'common.emptyHint' | translate" />
        } @else {
        <div class="data-table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th class="sortable" (click)="sortBy('title')">{{ 'notices.titleField' | translate }}<span class="sort-ind">{{ indicator('title') }}</span></th>
                <th>{{ 'notices.audience' | translate }}</th>
                <th class="sortable" (click)="sortBy('publishAt')">{{ 'notices.publishedOn' | translate }}<span class="sort-ind">{{ indicator('publishAt') }}</span></th>
                <th class="sortable" (click)="sortBy('expiresAt')">{{ 'notices.expiresOn' | translate }}<span class="sort-ind">{{ indicator('expiresAt') }}</span></th>
                <th class="sortable" (click)="sortBy('status')">{{ 'common.status' | translate }}<span class="sort-ind">{{ indicator('status') }}</span></th>
              </tr>
            </thead>
            <tbody>
              @for (notice of notices; track notice.id) {
                <tr>
                  <td class="strong">{{ notice.title }}</td>
                  <td>{{ notice.audience }}</td>
                  <td>{{ notice.publishedOn ?? '—' }}</td>
                  <td>{{ notice.expiresOn ?? '—' }}</td>
                  <td>
                    <span class="badge status-pill" [class.badge-success]="notice.status === 'PUBLISHED'" [class.badge-muted]="notice.status === 'DRAFT'">{{ notice.status }}</span>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <app-directory-pager
          [pageIndex]="pageIndex" [pageSize]="pageSize"
          [totalElements]="totalElements" [totalPages]="totalPages"
          (pageChange)="onPage($event)" (pageSizeChange)="onPageSize($event)" />
        }
      </div>
    </div>
  `,
  styles: `
    .muted { color: var(--color-muted); }
    .composer { margin-bottom: 24px; padding: 20px; }
    .card-title { margin: 0 0 16px; font-size: 1.05rem; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .span-2 { grid-column: span 2; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    input, select, textarea {
      padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit;
      background: var(--color-surface); color: var(--color-text);
    }
    textarea { resize: vertical; }
    .form-actions { margin-top: 16px; display: flex; justify-content: flex-end; }
    .strong { font-weight: 600; }
    @media (max-width: 640px) { .form-grid { grid-template-columns: 1fr; } .span-2 { grid-column: span 1; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NoticesComponent implements OnInit {
  readonly form = new FormGroup({
    title: new FormControl('', Validators.required),
    content: new FormControl('', Validators.required),
    audience: new FormControl('EVERYONE'),
    status: new FormControl('PUBLISHED'),
    expiresOn: new FormControl(''),
  });

  notices: Notice[] = [];
  query = '';
  pageIndex = 0;
  pageSize = 25;
  totalElements = 0;
  totalPages = 0;
  sortKey = 'publishAt';
  sortDirection: SortDirection = 'desc';
  private searchTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef, private auth: AuthService) {}

  get canCreate(): boolean {
    return this.auth.hasPermission('NOTICE_CREATE');
  }

  ngOnInit(): void {
    this.load();
  }

  onSearch(value: string): void {
    this.query = value;
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
    this.searchTimer = setTimeout(() => {
      this.pageIndex = 0;
      this.load();
    }, 300);
  }

  sortBy(key: string): void {
    const next = toggleSort(this.sortKey, key, this.sortDirection);
    this.sortKey = next.key;
    this.sortDirection = next.direction;
    this.pageIndex = 0;
    this.load();
  }

  indicator(key: string): string {
    return sortIndicator(this.sortKey, key, this.sortDirection);
  }

  onPage(page: number): void {
    this.pageIndex = page;
    this.load();
  }

  onPageSize(size: number): void {
    this.pageSize = size;
    this.pageIndex = 0;
    this.load();
  }

  private load(): void {
    const params: Record<string, string> = {
      page: String(this.pageIndex),
      size: String(this.pageSize),
      sort: this.sortKey,
      direction: this.sortDirection,
    };
    if (this.query.trim()) {
      params['query'] = this.query.trim();
    }
    this.http.get<ApiResponse<PagedResponse<BackendNotice>>>('/api/notices', { params }).subscribe({
      next: (res) => {
        this.notices = (res.data?.content ?? []).map(toNotice);
        this.totalElements = res.data?.totalElements ?? 0;
        this.totalPages = res.data?.totalPages ?? 0;
        this.cdr.markForCheck();
      },
      error: () => {
        this.notices = [];
        this.cdr.markForCheck();
      },
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }
    const status = this.form.value.status === 'DRAFT' ? 'DRAFT' : 'PUBLISHED';
    const payload = {
      title: this.form.value.title ?? '',
      body: this.form.value.content ?? '',
      visibilityScope: this.form.value.audience ?? 'EVERYONE',
      priority: 'NORMAL',
      publishAt: status === 'PUBLISHED' ? new Date().toISOString() : null,
      expiresAt: this.form.value.expiresOn ? new Date(this.form.value.expiresOn).toISOString() : null,
      status,
    };
    this.http.post<ApiResponse<BackendNotice>>('/api/notices', payload).subscribe({
      next: (res) => {
        this.pageIndex = 0;
        this.load();
      },
      error: () => {
        this.cdr.markForCheck();
        alert('Could not save notice. Please try again.');
      },
    });
    this.form.reset({ audience: 'EVERYONE', status: 'PUBLISHED' });
  }
}
