import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-change-password',
  imports: [ReactiveFormsModule, TranslateModule],
  template: `
    @if (open) {
      <div class="modal-backdrop" (click)="cancel()">
        <div class="modal" role="dialog" aria-modal="true" [attr.aria-label]="'auth.changePassword.title' | translate" (click)="$event.stopPropagation()">
          <h2>{{ 'auth.changePassword.title' | translate }}</h2>
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
              <p class="error">{{ errorMessage | translate }}</p>
            }
            <div class="form-actions">
              <button class="btn" type="button" [disabled]="submitting" (click)="cancel()">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="form.invalid || submitting">
                {{ submitting ? ('common.loading' | translate) : ('auth.changePassword.submit' | translate) }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: `
    .modal-backdrop { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 60; }
    .modal { width: 420px; max-width: 94vw; background: var(--color-surface); border-radius: 12px; padding: 24px; }
    h2 { margin: 0 0 16px; font-size: 1.15rem; }
    .field { margin-bottom: 14px; }
    label { display: block; margin-bottom: 6px; font-weight: 500; font-size: .9rem; }
    input {
      width: 100%; padding: 10px 12px; border: 1px solid var(--color-border); border-radius: 8px;
      background: var(--color-surface); color: var(--color-text); font: inherit;
    }
    .hint { color: var(--color-muted); font-size: .85rem; margin: 0 0 16px; }
    .error { color: #b91c1c; font-size: .85rem; margin: 0 0 12px; }
    .form-actions { display: flex; justify-content: flex-end; gap: 10px; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChangePasswordComponent implements OnChanges {
  @Input() open = false;
  @Output() closed = new EventEmitter<void>();

  readonly form = new FormGroup({
    currentPassword: new FormControl('', Validators.required),
    newPassword: new FormControl('', [Validators.required, Validators.minLength(8)]),
    confirmPassword: new FormControl('', Validators.required),
  });
  submitting = false;
  errorMessage = '';

  constructor(private auth: AuthService, private cdr: ChangeDetectorRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open']?.currentValue) {
      this.form.reset({ currentPassword: '', newPassword: '', confirmPassword: '' });
      this.errorMessage = '';
      this.submitting = false;
    }
  }

  onSubmit(): void {
    if (this.form.invalid || this.submitting) {
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
        this.auth.logout().subscribe(() => {
          window.location.href = '/login';
        });
      },
      error: (error: HttpErrorResponse) => {
        this.submitting = false;
        this.errorMessage = (error.error && error.error.code) || 'common.error';
        this.cdr.markForCheck();
      },
    });
  }

  cancel(): void {
    if (this.submitting) {
      return;
    }
    this.closed.emit();
  }
}
