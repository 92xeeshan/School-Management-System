import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ApiResponse } from '../../core/models/api.model';
import { AppUser, CredentialAudit } from '../../core/auth/auth.model';
import { AuthService } from '../../core/auth/auth.service';
import { PageHeaderComponent } from '../../layout/page-header/page-header.component';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';
import { CredentialHandoverComponent, IssuedCredentials } from '../../layout/credential-handover/credential-handover.component';

@Component({
  selector: 'app-users',
  imports: [ReactiveFormsModule, TranslateModule, PageHeaderComponent, EmptyStateComponent, CredentialHandoverComponent],
  template: `
    <app-page-header [title]="'users.title' | translate">
      @if (canCreate) {
        <button class="btn btn-primary" type="button" (click)="openAdd()">{{ 'users.add' | translate }}</button>
      }
    </app-page-header>

    <app-credential-handover [credentials]="issued" (closed)="issued = null" />

    @if (pageError) {
      <p class="banner error">{{ pageError | translate }}</p>
    }

    <div class="card">
      @if (users.length === 0) {
        <app-empty-state icon="manage_accounts" [title]="'common.noData' | translate" [hint]="'common.emptyHint' | translate" />
      } @else {
        <div class="data-table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>{{ 'users.username' | translate }}</th>
                <th>{{ 'users.name' | translate }}</th>
                <th>{{ 'users.roles' | translate }}</th>
                <th>{{ 'common.status' | translate }}</th>
                <th>{{ 'users.mustChange' | translate }}</th>
                <th>{{ 'users.lastLogin' | translate }}</th>
                @if (canUpdate) {
                  <th>{{ 'common.actions' | translate }}</th>
                }
              </tr>
            </thead>
            <tbody>
              @for (user of users; track user.id) {
                <tr>
                  <td class="strong">{{ user.username }}</td>
                  <td>{{ user.displayName }}</td>
                  <td>{{ user.roles.join(', ') }}</td>
                  <td>{{ user.status }}</td>
                  <td>{{ user.mustChangePassword ? ('common.yes' | translate) : ('common.no' | translate) }}</td>
                  <td>{{ user.lastLoginAt || '—' }}</td>
                  @if (canUpdate) {
                    <td>
                      <button class="btn btn-sm" type="button" [disabled]="resettingId === user.id" (click)="reset(user)">
                        {{ 'users.resetPassword' | translate }}
                      </button>
                    </td>
                  }
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>

    <div class="card audit">
      <h2>{{ 'users.audit' | translate }}</h2>
      @if (audit.length === 0) {
        <p class="muted">{{ 'common.noData' | translate }}</p>
      } @else {
        <ul>
          @for (row of audit; track row.id) {
            <li>
              <span class="strong">{{ row.action }}</span>
              <span class="muted">{{ row.createdAt }}</span>
              @if (row.detail) { <span>{{ row.detail }}</span> }
            </li>
          }
        </ul>
      }
    </div>

    @if (showModal) {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ 'users.add' | translate }}</h2>
          <form [formGroup]="form" (ngSubmit)="create()">
            <div class="field">
              <label>{{ 'users.name' | translate }} *</label>
              <input type="text" formControlName="firstName" />
            </div>
            <div class="field">
              <label>{{ 'users.lastName' | translate }}</label>
              <input type="text" formControlName="lastName" />
            </div>
            <div class="field">
              <label>{{ 'users.email' | translate }}</label>
              <input type="email" formControlName="email" />
            </div>
            <div class="field">
              <label>{{ 'users.role' | translate }} *</label>
              <select formControlName="role">
                <option value="ADMIN">{{ 'roles.ADMIN' | translate }}</option>
                <option value="TEACHER">{{ 'roles.TEACHER' | translate }}</option>
                <option value="PARENT">{{ 'roles.PARENT' | translate }}</option>
                <option value="STUDENT">{{ 'roles.STUDENT' | translate }}</option>
              </select>
            </div>
            @if (formError) {
              <p class="banner error">{{ formError | translate }}</p>
            }
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
    .banner { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 12px; padding: 16px; margin-bottom: 16px; }
    .banner.error { color: #b91c1c; }
    .card { background: var(--color-surface); border-radius: 12px; padding: 16px; margin-bottom: 16px; }
    .audit ul { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
    .muted { color: var(--color-muted); }
    .modal-backdrop { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 40; }
    .modal { width: 420px; max-width: 94vw; background: var(--color-surface); border-radius: 12px; padding: 24px; }
    .field { margin-bottom: 12px; }
    label { display: block; margin-bottom: 6px; font-size: .85rem; color: var(--color-muted); }
    input, select { width: 100%; padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; background: var(--color-surface); color: var(--color-text); }
    .form-actions { display: flex; justify-content: flex-end; gap: 10px; }
    .btn-sm { padding: 5px 10px; font-size: .82rem; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsersComponent implements OnInit {
  users: AppUser[] = [];
  audit: CredentialAudit[] = [];
  pageError = '';
  showModal = false;
  saving = false;
  formError = '';
  resettingId: string | null = null;
  issued: IssuedCredentials | null = null;

  readonly form = new FormGroup({
    firstName: new FormControl('', Validators.required),
    lastName: new FormControl(''),
    email: new FormControl(''),
    role: new FormControl('TEACHER', Validators.required),
  });

  constructor(private http: HttpClient, private auth: AuthService, private cdr: ChangeDetectorRef) {}

  get canCreate(): boolean {
    return this.auth.hasPermission('USER_CREATE');
  }

  get canUpdate(): boolean {
    return this.auth.hasPermission('USER_UPDATE');
  }

  ngOnInit(): void {
    this.load();
  }

  openAdd(): void {
    this.form.reset({ firstName: '', lastName: '', email: '', role: 'TEACHER' });
    this.formError = '';
    this.showModal = true;
    this.cdr.markForCheck();
  }

  closeModal(): void {
    this.showModal = false;
    this.cdr.markForCheck();
  }

  create(): void {
    if (this.form.invalid) {
      return;
    }
    this.saving = true;
    this.formError = '';
    this.cdr.markForCheck();
    const { firstName, lastName, email, role } = this.form.value;
    this.http.post<ApiResponse<AppUser>>('/api/users', {
      firstName,
      lastName: lastName || null,
      email: email || null,
      roleCodes: [role],
    }).subscribe({
      next: (res) => {
        this.saving = false;
        this.showModal = false;
        this.captureIssued(res.data);
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.saving = false;
        this.formError = (error.error && error.error.code) || 'common.error';
        this.cdr.markForCheck();
      },
    });
  }

  reset(user: AppUser): void {
    this.resettingId = user.id;
    this.cdr.markForCheck();
    this.http.post<ApiResponse<AppUser>>(`/api/users/${user.id}/reset-password`, {}).subscribe({
      next: (res) => {
        this.resettingId = null;
        this.captureIssued(res.data);
        this.load();
      },
      error: () => {
        this.resettingId = null;
        this.pageError = 'common.error';
        this.cdr.markForCheck();
      },
    });
  }

  private captureIssued(user: AppUser | undefined): void {
    if (user?.username && user.temporaryPassword) {
      this.issued = { username: user.username, temporaryPassword: user.temporaryPassword };
    }
  }

  private load(): void {
    this.http.get<ApiResponse<AppUser[]>>('/api/users').subscribe({
      next: (res) => {
        this.users = res.data ?? [];
        this.pageError = '';
        this.cdr.markForCheck();
      },
      error: () => {
        this.pageError = 'common.error';
        this.cdr.markForCheck();
      },
    });
    this.http.get<ApiResponse<CredentialAudit[]>>('/api/users/credential-audit').subscribe({
      next: (res) => {
        this.audit = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }
}
