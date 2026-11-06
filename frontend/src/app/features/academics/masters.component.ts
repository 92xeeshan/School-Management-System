import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ApiError, ApiResponse } from '../../core/models/api.model';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';

interface MasterRow {
  id: string;
  name: string;
  code: string | null;
  sortOrder: number | null;
  status: string;
}

type MasterKind = 'STREAM' | 'RELIGION' | 'CATEGORY' | 'HOUSE' | 'MEDIUM' | 'MOTHER_TONGUE';

@Component({
  selector: 'app-academics-masters',
  imports: [TranslateModule, ReactiveFormsModule, EmptyStateComponent],
  template: `
    <div class="tab-page">
      <div class="page-header">
        <div>
          <h2>{{ 'academics.mastersPage.title' | translate }}</h2>
          <p class="muted">{{ 'academics.mastersPage.subtitle' | translate }}</p>
        </div>
        @if (canCreate) {
          <button class="btn btn-primary" type="button" (click)="openAdd()">{{ 'common.add' | translate }}</button>
        }
      </div>

      @if (successMessage) {
        <p class="banner success">{{ successMessage | translate }}</p>
      }
      @if (pageError) {
        <p class="banner error">{{ pageError }}</p>
      }

      <div class="kind-tabs">
        @for (kind of kinds; track kind) {
          <button type="button" class="kind" [class.active]="selectedKind === kind" (click)="selectKind(kind)">
            {{ ('academics.mastersPage.kinds.' + kind) | translate }}
          </button>
        }
      </div>

      <div class="card">
        @if (rows.length === 0) {
          <app-empty-state icon="list" [title]="'common.noData' | translate" [hint]="'common.emptyHint' | translate">
            @if (canCreate) {
              <button class="btn btn-primary" type="button" (click)="openAdd()">{{ 'common.add' | translate }}</button>
            }
          </app-empty-state>
        } @else {
          <div class="data-table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>{{ 'academics.mastersPage.name' | translate }}</th>
                  <th>{{ 'academics.mastersPage.code' | translate }}</th>
                  <th>{{ 'common.status' | translate }}</th>
                  @if (canUpdate) {
                    <th>{{ 'common.actions' | translate }}</th>
                  }
                </tr>
              </thead>
              <tbody>
                @for (row of rows; track row.id) {
                  <tr>
                    <td class="strong">{{ row.name }}</td>
                    <td>{{ row.code || '—' }}</td>
                    <td>
                      <span class="badge status-pill" [class.badge-success]="row.status === 'ACTIVE'" [class.badge-muted]="row.status !== 'ACTIVE'">
                        {{ row.status }}
                      </span>
                    </td>
                    @if (canUpdate) {
                      <td>
                        <button class="btn btn-sm" type="button" (click)="openEdit(row)">{{ 'common.edit' | translate }}</button>
                      </td>
                    }
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    </div>

    @if (showModal) {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editingId ? ('common.edit' | translate) : ('common.add' | translate) }}</h2>
          @if (formError) {
            <p class="form-error">{{ formError }}</p>
          }
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="field">
              <label>{{ 'academics.mastersPage.name' | translate }} *</label>
              <input type="text" formControlName="name" />
            </div>
            <div class="field">
              <label>{{ 'academics.mastersPage.code' | translate }}</label>
              <input type="text" formControlName="code" />
            </div>
            <div class="field">
              <label>{{ 'common.status' | translate }}</label>
              <select formControlName="status">
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
              </select>
            </div>
            <div class="form-actions">
              <button class="btn" type="button" (click)="closeModal()">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving">
                {{ saving ? ('common.loading' | translate) : ('common.save' | translate) }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; gap: 12px; flex-wrap: wrap; }
    h2 { font-size: 1.2rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); }
    .strong { font-weight: 600; }
    .banner { padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
    .banner.success { background: #ecfdf5; border: 1px solid #a7f3d0; color: #047857; }
    .banner.error { background: #fef2f2; border: 1px solid #fecaca; color: #b91c1c; }
    .kind-tabs { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 14px; }
    .kind {
      padding: 6px 12px; border-radius: 16px; border: 1px solid var(--color-border);
      background: var(--color-surface); color: var(--color-muted); cursor: pointer; font: inherit;
    }
    .kind.active { border-color: var(--color-primary); color: var(--color-primary); background: var(--color-primary-soft); }
    .btn-sm { padding: 5px 10px; font-size: .82rem; }
    .modal-backdrop {
      position: fixed; inset: 0; z-index: 100; background: rgba(15, 23, 42, .5);
      display: flex; align-items: flex-start; justify-content: center; padding: 40px 16px; overflow-y: auto;
    }
    .modal { background: var(--color-surface); border-radius: var(--radius); padding: 24px; width: 420px; max-width: 100%; }
    .field { display: flex; flex-direction: column; gap: 6px; margin-bottom: 12px; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    input, select { padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; }
    .form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 12px; }
    .form-error { margin: 0 0 14px; color: #b91c1c; font-size: .9rem; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicsMastersComponent implements OnInit {
  readonly kinds: MasterKind[] = ['STREAM', 'RELIGION', 'CATEGORY', 'HOUSE', 'MEDIUM', 'MOTHER_TONGUE'];
  selectedKind: MasterKind = 'STREAM';
  rows: MasterRow[] = [];
  showModal = false;
  saving = false;
  editingId: string | null = null;
  formError = '';
  pageError = '';
  successMessage = '';

  readonly form = new FormGroup({
    name: new FormControl('', Validators.required),
    code: new FormControl(''),
    status: new FormControl('ACTIVE'),
  });

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef, private auth: AuthService) {}

  get canCreate(): boolean {
    return this.auth.hasPermission('CLASS_CREATE');
  }

  get canUpdate(): boolean {
    return this.auth.hasPermission('CLASS_UPDATE');
  }

  ngOnInit(): void {
    this.load();
  }

  selectKind(kind: MasterKind): void {
    this.selectedKind = kind;
    this.load();
  }

  openAdd(): void {
    this.editingId = null;
    this.showModal = true;
    this.formError = '';
    this.form.reset({ name: '', code: '', status: 'ACTIVE' });
    this.cdr.markForCheck();
  }

  openEdit(row: MasterRow): void {
    this.editingId = row.id;
    this.showModal = true;
    this.formError = '';
    this.form.reset({ name: row.name, code: row.code ?? '', status: row.status || 'ACTIVE' });
    this.cdr.markForCheck();
  }

  closeModal(): void {
    if (this.saving) {
      return;
    }
    this.showModal = false;
    this.editingId = null;
    this.cdr.markForCheck();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }
    this.saving = true;
    this.formError = '';
    const body = {
      name: this.form.value.name,
      code: this.form.value.code || null,
      sortOrder: 0,
      status: this.form.value.status,
    };
    const url = this.endpoint(this.editingId);
    const req$ = this.editingId
      ? this.http.put<ApiResponse<MasterRow>>(url, body)
      : this.http.post<ApiResponse<MasterRow>>(url, body);
    req$.subscribe({
      next: () => {
        this.saving = false;
        this.showModal = false;
        this.editingId = null;
        this.successMessage = 'academics.mastersPage.saved';
        this.pageError = '';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err: HttpErrorResponse) => {
        this.saving = false;
        const api = err.error as ApiError | undefined;
        this.formError = api?.message || 'Could not save.';
        this.cdr.markForCheck();
      },
    });
  }

  private endpoint(id: string | null): string {
    if (this.selectedKind === 'STREAM') {
      return id ? `/api/streams/${id}` : '/api/streams';
    }
    return id ? `/api/lookups/${id}` : `/api/lookups/${this.selectedKind}`;
  }

  private load(): void {
    const url = this.selectedKind === 'STREAM' ? '/api/streams' : `/api/lookups/${this.selectedKind}`;
    this.http.get<ApiResponse<MasterRow[]>>(url).subscribe({
      next: (res) => {
        this.rows = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.rows = [];
        this.cdr.markForCheck();
      },
    });
  }
}
