import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth/auth.service';
import { LocaleService } from '../../i18n/locale.service';
import { LOCALE_META, SUPPORTED_LOCALES } from '../../i18n/i18n.config';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, TranslateModule],
  template: `
    <div class="login-page">
      <div class="login-card">
        <div class="login-brand">
          <span class="brand-logo">🏫</span>
          <h1>{{ 'app.name' | translate }}</h1>
          <p class="muted">{{ 'app.tagline' | translate }}</p>
        </div>

        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="field">
            <label for="username">{{ 'login.username' | translate }}</label>
            <input id="username" type="text" formControlName="username" autocomplete="username" />
          </div>

          <div class="field">
            <label for="password">{{ 'login.password' | translate }}</label>
            <input id="password" type="password" formControlName="password" autocomplete="current-password" />
          </div>

          @if (errorMessage) {
            <div class="alert alert-error">{{ errorMessage | translate }}</div>
          }

          <button class="btn btn-primary btn-block" type="submit" [disabled]="form.invalid || submitting">
            @if (submitting) { {{ 'common.loading' | translate }} } @else { {{ 'login.submit' | translate }} }
          </button>
        </form>

        <div class="lang-switcher">
          @for (locale of supportedLocales; track locale) {
            <button
              class="lang-pill"
              [class.active]="locale === localeService.current"
              type="button"
              (click)="localeService.setLocale(locale)">
              {{ localeMeta[locale].label }}
            </button>
          }
        </div>
      </div>
    </div>
  `,
  styles: `
    :host { display: block; height: 100vh; }
    .login-page {
      height: 100%;
      display: flex; align-items: center; justify-content: center;
      background: linear-gradient(135deg, var(--color-sidebar) 0%, #33417a 100%);
      padding: 16px;
    }
    .login-card {
      width: 380px; max-width: 100%;
      background: #fff; border-radius: 16px;
      padding: 36px 32px;
      box-shadow: 0 20px 50px rgba(0,0,0,.25);
    }
    .login-brand { text-align: center; margin-bottom: 24px; }
    .brand-logo { font-size: 2.4rem; }
    h1 { margin: 8px 0 4px; font-size: 1.5rem; }
    .muted { color: var(--color-muted); font-size: .9rem; }
    .field { margin-bottom: 16px; }
    label { display: block; margin-bottom: 6px; font-weight: 500; font-size: .9rem; }
    input {
      width: 100%; padding: 10px 12px;
      border: 1px solid var(--color-border); border-radius: 8px;
      font: inherit;
    }
    input:focus { outline: none; border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(79,70,229,.12); }
    .alert-error { margin-bottom: 16px; }
    .lang-switcher { display: flex; justify-content: center; gap: 8px; margin-top: 20px; }
    .lang-pill {
      padding: 6px 12px; border-radius: 20px;
      border: 1px solid var(--color-border); background: #fff;
      cursor: pointer; font: inherit; font-size: .85rem;
    }
    .lang-pill.active { background: var(--color-primary); color: #fff; border-color: var(--color-primary); }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent implements OnInit {
  readonly form = new FormGroup({
    username: new FormControl('', Validators.required),
    password: new FormControl('', Validators.required),
  });

  submitting = false;
  errorMessage = '';

  readonly supportedLocales = SUPPORTED_LOCALES;
  readonly localeMeta = LOCALE_META;

  constructor(
    readonly localeService: LocaleService,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting = true;
    this.errorMessage = '';
    const { username, password } = this.form.value;
    this.auth.login(username ?? '', password ?? '').subscribe({
      next: () => {
        this.submitting = false;
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigate([returnUrl ?? '/dashboard']);
      },
      error: (error: HttpErrorResponse) => {
        this.submitting = false;
        this.errorMessage =
          error.status === 401 ? 'login.invalidCredentials' : 'login.serverError';
      },
    });
  }
}
