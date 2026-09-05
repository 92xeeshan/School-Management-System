import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ApiResponse } from '../../core/models/api.model';

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
  imports: [TranslateModule, ReactiveFormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'notices.title' | translate }}</h1>
          <p class="muted">{{ 'notices.subtitle' | translate }}</p>
        </div>
      </div>

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

      <div class="card">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'notices.titleField' | translate }}</th>
                <th>{{ 'notices.audience' | translate }}</th>
                <th>{{ 'notices.publishedOn' | translate }}</th>
                <th>{{ 'notices.expiresOn' | translate }}</th>
                <th>{{ 'common.status' | translate }}</th>
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
                    <span class="badge" [class.badge-success]="notice.status === 'PUBLISHED'" [class.badge-muted]="notice.status === 'DRAFT'">{{ notice.status }}</span>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="5" class="center">{{ 'common.noData' | translate }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: `
    .page-header { margin-bottom: 20px; }
    h1 { font-size: 1.5rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); }
    .composer { margin-bottom: 24px; padding: 20px; }
    .card-title { margin: 0 0 16px; font-size: 1.05rem; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .span-2 { grid-column: span 2; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    input, select, textarea {
      padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit;
    }
    textarea { resize: vertical; }
    .form-actions { margin-top: 16px; display: flex; justify-content: flex-end; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 12px 16px; border-bottom: 1px solid var(--color-border); font-size: .92rem; }
    th { color: var(--color-muted); font-weight: 600; font-size: .8rem; text-transform: uppercase; letter-spacing: .03em; background: var(--color-bg); }
    .strong { font-weight: 600; }
    .center { text-align: center; color: var(--color-muted); padding: 28px; }
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

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.http.get<ApiResponse<BackendNotice[]>>('/api/notices').subscribe({
      next: (res) => {
        this.notices = res.data.map(toNotice);
        this.cdr.markForCheck();
      },
      error: () => this.loadDemo(),
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
        this.notices = [toNotice(res.data), ...this.notices];
        this.cdr.markForCheck();
      },
      error: () => {
        const notice: Notice = {
          id: `n${Date.now()}`,
          title: payload.title,
          content: payload.body,
          audience: payload.visibilityScope,
          status,
          publishedOn: status === 'PUBLISHED' ? new Date().toISOString().slice(0, 10) : null,
          expiresOn: this.form.value.expiresOn || null,
        };
        this.notices = [notice, ...this.notices];
        this.cdr.markForCheck();
      },
    });
    this.form.reset({ audience: 'EVERYONE', status: 'PUBLISHED' });
  }

  private loadDemo(): void {
    this.notices = [
      { id: 'n1', title: 'School reopens on Monday', content: 'All students must report at 8 AM.', audience: 'EVERYONE', status: 'PUBLISHED', publishedOn: '2026-07-28', expiresOn: '2026-08-01' },
      { id: 'n2', title: 'PTA meeting scheduled', content: 'Parent-teacher meeting on Friday at 10 AM.', audience: 'PARENTS', status: 'PUBLISHED', publishedOn: '2026-07-25', expiresOn: null },
      { id: 'n3', title: 'Staff workshop draft', content: 'Upcoming teacher training details.', audience: 'TEACHERS', status: 'DRAFT', publishedOn: null, expiresOn: null },
    ];
    this.cdr.markForCheck();
  }
}
