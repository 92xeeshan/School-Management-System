import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { SecurityQuestion } from '../../core/auth/auth.model';

@Component({
  selector: 'app-first-time-setup',
  imports: [ReactiveFormsModule, TranslateModule],
  template: `
    <div class="login-page">
      <div class="login-card wide">
        <div class="login-brand">
          <h1>{{ 'auth.firstLogin.title' | translate }}</h1>
          <p class="muted">{{ 'auth.firstLogin.subtitle' | translate }}</p>
        </div>
        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="field">
            <label>{{ 'auth.firstLogin.newPassword' | translate }}</label>
            <input type="password" formControlName="newPassword" autocomplete="new-password" />
          </div>
          <div class="field">
            <label>{{ 'auth.firstLogin.confirmPassword' | translate }}</label>
            <input type="password" formControlName="confirmPassword" autocomplete="new-password" />
          </div>
          <p class="hint">{{ 'auth.firstLogin.hint' | translate }}</p>
          @for (slot of slots; track slot) {
            <div class="qa">
              <div class="field">
                <label>{{ 'auth.firstLogin.question' | translate:{ n: slot } }}</label>
                <select [formControlName]="'question' + slot">
                  <option value="">{{ 'auth.firstLogin.selectQuestion' | translate }}</option>
                  @for (question of questions; track question.id) {
                    <option [value]="question.id">{{ ('auth.questions.' + question.code) | translate }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'auth.firstLogin.answer' | translate }}</label>
                <input type="text" [formControlName]="'answer' + slot" autocomplete="off" />
              </div>
            </div>
          }
          @if (errorMessage) {
            <div class="alert alert-error">{{ errorMessage | translate }}</div>
          }
          <button class="btn btn-primary btn-block" type="submit" [disabled]="form.invalid || submitting">
            @if (submitting) { {{ 'common.loading' | translate }} } @else { {{ 'auth.firstLogin.submit' | translate }} }
          </button>
        </form>
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
      width: 480px; max-width: 100%; background: var(--color-surface); border-radius: 16px;
      color: var(--color-text); padding: 36px 32px; box-shadow: 0 20px 50px rgba(0,0,0,.25);
    }
    .login-brand { text-align: center; margin-bottom: 20px; }
    h1 { margin: 0 0 8px; font-size: 1.4rem; }
    .muted { color: var(--color-muted); font-size: .9rem; }
    .field { margin-bottom: 12px; }
    label { display: block; margin-bottom: 6px; font-weight: 500; font-size: .9rem; }
    input, select {
      width: 100%; padding: 10px 12px; border: 1px solid var(--color-border); border-radius: 8px;
      background: var(--color-surface); color: var(--color-text); font: inherit;
    }
    .hint { color: var(--color-muted); font-size: .82rem; margin: 0 0 16px; }
    .qa { border-top: 1px solid var(--color-border); padding-top: 12px; margin-top: 4px; }
    .alert-error { margin-bottom: 16px; }
    .btn-block { width: 100%; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FirstTimeSetupComponent implements OnInit {
  readonly slots = [1, 2, 3];
  readonly form = new FormGroup({
    newPassword: new FormControl('', [Validators.required, Validators.minLength(8)]),
    confirmPassword: new FormControl('', Validators.required),
    question1: new FormControl('', Validators.required),
    answer1: new FormControl('', [Validators.required, Validators.minLength(2)]),
    question2: new FormControl('', Validators.required),
    answer2: new FormControl('', [Validators.required, Validators.minLength(2)]),
    question3: new FormControl(''),
    answer3: new FormControl(''),
  });
  questions: SecurityQuestion[] = [];
  submitting = false;
  errorMessage = '';

  constructor(private auth: AuthService, private router: Router, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.auth.listQuestions().subscribe({
      next: (questions) => {
        this.questions = questions;
        this.cdr.markForCheck();
      },
      error: () => {
        this.errorMessage = 'common.error';
        this.cdr.markForCheck();
      },
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }
    const value = this.form.getRawValue();
    const answers = [
      { questionId: value.question1, answer: value.answer1 },
      { questionId: value.question2, answer: value.answer2 },
      { questionId: value.question3, answer: value.answer3 },
    ]
      .filter((row) => !!row.questionId && !!row.answer?.trim())
      .map((row) => ({ questionId: String(row.questionId), answer: String(row.answer).trim() }));
    if (answers.length < 2 || answers.length > 3) {
      this.errorMessage = 'auth.questionsCount';
      this.cdr.markForCheck();
      return;
    }
    this.submitting = true;
    this.errorMessage = '';
    this.cdr.markForCheck();
    this.auth.completeFirstLogin({
      newPassword: value.newPassword ?? '',
      confirmPassword: value.confirmPassword ?? '',
      answers,
    }).subscribe({
      next: () => {
        this.auth.logout().subscribe(() => {
          this.router.navigate(['/login']);
        });
      },
      error: (error: HttpErrorResponse) => {
        this.submitting = false;
        this.errorMessage = (error.error && error.error.code) || 'common.error';
        this.cdr.markForCheck();
      },
    });
  }
}
