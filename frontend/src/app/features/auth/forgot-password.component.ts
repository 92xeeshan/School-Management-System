import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { SecurityQuestion } from '../../core/auth/auth.model';

@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, TranslateModule, RouterLink],
  template: `
    <div class="login-page">
      <div class="login-card">
        <div class="login-brand">
          <h1>{{ 'auth.forgot.title' | translate }}</h1>
          <p class="muted">{{ 'auth.forgot.subtitle' | translate }}</p>
        </div>

        @if (step === 1) {
          <form [formGroup]="startForm" (ngSubmit)="start()">
            <div class="field">
              <label>{{ 'login.username' | translate }}</label>
              <input type="text" formControlName="username" autocomplete="username" />
            </div>
            @if (errorMessage) {
              <div class="alert alert-error">{{ errorMessage | translate }}</div>
            }
            <button class="btn btn-primary btn-block" type="submit" [disabled]="startForm.invalid || submitting">
              {{ submitting ? ('common.loading' | translate) : ('auth.forgot.start' | translate) }}
            </button>
          </form>
        }

        @if (step === 2) {
          <form [formGroup]="answerForm" (ngSubmit)="verify()">
            @for (question of questions; track question.id; let i = $index) {
              <div class="field">
                <label>{{ ('auth.questions.' + question.code) | translate }}</label>
                <input type="text" [formControlName]="'answer' + i" autocomplete="off" />
              </div>
            }
            @if (errorMessage) {
              <div class="alert alert-error">{{ errorMessage | translate }}</div>
            }
            <button class="btn btn-primary btn-block" type="submit" [disabled]="answerForm.invalid || submitting">
              {{ submitting ? ('common.loading' | translate) : ('auth.forgot.verify' | translate) }}
            </button>
          </form>
        }

        @if (step === 3) {
          <form [formGroup]="resetForm" (ngSubmit)="complete()">
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
            @if (success) {
              <div class="alert">{{ 'auth.forgot.success' | translate }}</div>
            }
            <button class="btn btn-primary btn-block" type="submit" [disabled]="resetForm.invalid || submitting || success">
              {{ submitting ? ('common.loading' | translate) : ('auth.forgot.complete' | translate) }}
            </button>
          </form>
        }

        <a class="back" routerLink="/login">{{ 'auth.forgot.backToLogin' | translate }}</a>
      </div>
    </div>
  `,
  styles: `
    :host { display: block; height: 100vh; }
    .login-page {
      height: 100%; display: flex; align-items: center; justify-content: center;
      background: linear-gradient(135deg, var(--color-sidebar) 0%, #33417a 100%);
      padding: 16px;
    }
    .login-card {
      width: 400px; max-width: 100%; background: var(--color-surface); border-radius: 16px;
      color: var(--color-text); padding: 36px 32px; box-shadow: 0 20px 50px rgba(0,0,0,.25);
    }
    .login-brand { text-align: center; margin-bottom: 20px; }
    h1 { margin: 0 0 8px; font-size: 1.4rem; }
    .muted, .hint { color: var(--color-muted); font-size: .9rem; }
    .field { margin-bottom: 14px; }
    label { display: block; margin-bottom: 6px; font-weight: 500; font-size: .9rem; }
    input {
      width: 100%; padding: 10px 12px; border: 1px solid var(--color-border); border-radius: 8px;
      background: var(--color-surface); color: var(--color-text); font: inherit;
    }
    .alert-error { margin-bottom: 16px; }
    .btn-block { width: 100%; }
    .back { display: block; text-align: center; margin-top: 16px; color: var(--color-primary); }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForgotPasswordComponent {
  step = 1;
  submitting = false;
  success = false;
  errorMessage = '';
  challengeId = '';
  resetToken = '';
  questions: SecurityQuestion[] = [];

  readonly startForm = new FormGroup({
    username: new FormControl('', Validators.required),
  });
  readonly answerForm = new FormGroup({});
  readonly resetForm = new FormGroup({
    newPassword: new FormControl('', [Validators.required, Validators.minLength(8)]),
    confirmPassword: new FormControl('', Validators.required),
  });

  constructor(private auth: AuthService, private cdr: ChangeDetectorRef) {}

  start(): void {
    if (this.startForm.invalid) {
      return;
    }
    this.submitting = true;
    this.errorMessage = '';
    this.cdr.markForCheck();
    this.auth.startForgot(this.startForm.value.username ?? '').subscribe({
      next: (res) => {
        this.challengeId = res.challengeId;
        this.questions = res.questions ?? [];
        this.rebuildAnswerForm();
        this.step = 2;
        this.submitting = false;
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  verify(): void {
    if (this.answerForm.invalid) {
      return;
    }
    this.submitting = true;
    this.errorMessage = '';
    this.cdr.markForCheck();
    const answers = this.questions.map((question, index) => ({
      questionId: question.id,
      answer: String(this.answerForm.get('answer' + index)?.value ?? ''),
    }));
    this.auth.verifyForgot(this.challengeId, answers).subscribe({
      next: (token) => {
        this.resetToken = token;
        this.step = 3;
        this.submitting = false;
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  complete(): void {
    if (this.resetForm.invalid) {
      return;
    }
    this.submitting = true;
    this.errorMessage = '';
    this.cdr.markForCheck();
    const { newPassword, confirmPassword } = this.resetForm.value;
    this.auth.completeForgot(this.resetToken, newPassword ?? '', confirmPassword ?? '').subscribe({
      next: () => {
        this.submitting = false;
        this.success = true;
        this.cdr.markForCheck();
      },
      error: (error: HttpErrorResponse) => this.fail(error),
    });
  }

  private rebuildAnswerForm(): void {
    const group: Record<string, FormControl<string | null>> = {};
    this.questions.forEach((_, index) => {
      group['answer' + index] = new FormControl('', [Validators.required, Validators.minLength(2)]);
    });
    Object.keys(this.answerForm.controls).forEach((key) => this.answerForm.removeControl(key));
    Object.entries(group).forEach(([key, control]) => this.answerForm.addControl(key, control));
  }

  private fail(error: HttpErrorResponse): void {
    this.submitting = false;
    this.errorMessage = (error.error && error.error.code) || 'common.error';
    this.cdr.markForCheck();
  }
}
