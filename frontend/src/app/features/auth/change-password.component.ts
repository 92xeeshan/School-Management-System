import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { PageHeaderComponent } from '../../layout/page-header/page-header.component';

@Component({
  selector: 'app-change-password',
  imports: [ReactiveFormsModule, TranslateModule, PageHeaderComponent],
  template: `
    <app-page-header [title]="'auth.changePassword.title' | translate" />
    <div class="card">
      <form [formGroup]="form" (ngSubmit)="onSubmit()">
        <div class="field">
          <label>{{ 'auth.changePassword.current' | translate }}</label>
          <input type="password" formControlName="currentPassword" autocomplete="current-password" />
        </div>
        <div class="field">
          <label>{{ 'auth.firstLogin.newPassword' | translate }}</label>
          <input type="password" formControlName="newPassword" autocomplete="new-password" />
        </div>
        <div class="field">
          <label>{{ 'auth.firstLogin.confirmPassword' | translate }}</label>
          <input type="password" formControlName="confirmPassword" autocomplete="new-password" />
        </div>
        <p class="hint">{{ 'auth.firstLogin.hint' | translate }}</p>
        @if (errorMessage) {
          <div class="alert alert-error">{{ errorMessage | translate }}</div>
        }
        <button class="btn btn-primary" type="submit" [disabled]="form.invalid || submitting">
          {{ submitting ? ('common.loading' | translate) : ('auth.changePassword.submit' | translate) }}
        </button>
      </form>
    </div>
  `,
  styles: `
    .card { max-width: 480px; background: var(--color-surface); border-radius: 12px; padding: 24px; }
    .field { margin-bottom: 14px; }
    label { display: block; margin-bottom: 6px; font-weight: 500; font-size: .9rem; }
    input {
      width: 100%; padding: 10px 12px; border: 1px solid var(--color-border); border-radius: 8px;
      background: var(--color-surface); color: var(--color-text); font: inherit;
    }
    .hint { color: var(--color-muted); font-size: .85rem; margin: 0 0 16px; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChangePasswordComponent {
  readonly form = new FormGroup({
    currentPassword: new FormControl('', Validators.required),
    newPassword: new FormControl('', [Validators.required, Validators.minLength(8)]),
    confirmPassword: new FormControl('', Validators.required),
  });
  submitting = false;
  errorMessage = '';

  constructor(private auth: AuthService, private router: Router, private cdr: ChangeDetectorRef) {}

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting = true;
    this.errorMessage = '';
    this.cdr.markForCheck();
    const { currentPassword, newPassword, confirmPassword } = this.form.value;
    this.auth.changePassword({
      currentPassword: currentPassword ?? '',
      newPassword: newPassword ?? '',
      confirmPassword: confirmPassword ?? '',
    }).subscribe({
      next: () => {
        this.auth.logout().subscribe(() => this.router.navigate(['/login']));
      },
      error: (error: HttpErrorResponse) => {
        this.submitting = false;
        this.errorMessage = (error.error && error.error.code) || 'common.error';
        this.cdr.markForCheck();
      },
    });
  }
}
