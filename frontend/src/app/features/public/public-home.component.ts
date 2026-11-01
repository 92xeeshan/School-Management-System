import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth/auth.service';
import { LocaleService } from '../../i18n/locale.service';
import { LOCALE_META, SUPPORTED_LOCALES } from '../../i18n/i18n.config';
import { PublicEvent, PublicSite } from './public.model';
import { PublicService } from './public.service';

@Component({
  selector: 'app-public-home',
  imports: [ReactiveFormsModule, TranslateModule, RouterLink],
  template: `
    <div class="public-page">
      <header class="top">
        <div class="brand">
          @if (site?.logoUrl) {
            <img [src]="site!.logoUrl!" alt="" class="logo" />
          }
          <div>
            <p class="school">{{ site?.name || ('app.name' | translate) }}</p>
            <p class="muted">{{ site?.copy?.tagline }}</p>
          </div>
        </div>
        <div class="top-actions">
          <div class="lang-switcher">
            @for (locale of supportedLocales; track locale) {
              <button
                class="lang-pill"
                [class.active]="locale === localeService.current"
                type="button"
                (click)="setLocale(locale)">
                {{ localeMeta[locale].label }}
              </button>
            }
          </div>
          @if (auth.isAuthenticated()) {
            <a class="btn ghost" routerLink="/dashboard">{{ 'public.dashboard' | translate }}</a>
          } @else {
            <a class="btn ghost" routerLink="/login">{{ 'public.staffLogin' | translate }}</a>
          }
        </div>
      </header>

      @if (error) {
        <p class="banner error">{{ error | translate }}</p>
      }

      @if (site) {
        <section class="hero" [class.has-image]="!!site.heroImageUrl">
          @if (site.heroImageUrl) {
            <img [src]="site.heroImageUrl" alt="" class="hero-img" />
          }
          <div class="hero-copy">
            <h1>{{ site.name }}</h1>
            <p>{{ site.copy.tagline }}</p>
            <a class="btn btn-primary" href="#enquire">{{ 'public.enquireCta' | translate }}</a>
          </div>
        </section>

        <main class="content">
          @if (site.copy.about) {
            <section class="card">
              <h2>{{ 'public.about' | translate }}</h2>
              <p class="prose">{{ site.copy.about }}</p>
            </section>
          }

          @if (site.copy.principalMessage) {
            <section class="card">
              <h2>{{ 'public.principal' | translate }}</h2>
              @if (site.principalName) {
                <p class="muted">{{ site.principalName }}</p>
              }
              <p class="prose">{{ site.copy.principalMessage }}</p>
            </section>
          }

          @if (facilityLines.length) {
            <section class="card">
              <h2>{{ 'public.facilities' | translate }}</h2>
              <ul class="list">
                @for (item of facilityLines; track item) {
                  <li>{{ item }}</li>
                }
              </ul>
            </section>
          }

          @if (site.notices.length) {
            <section class="card">
              <h2>{{ 'public.notices' | translate }}</h2>
              <ul class="feed">
                @for (notice of site.notices; track notice.id) {
                  <li>
                    <strong>{{ notice.title }}</strong>
                    <p>{{ notice.body }}</p>
                  </li>
                }
              </ul>
            </section>
          }

          @if (site.events.length) {
            <section class="card">
              <h2>{{ 'public.events' | translate }}</h2>
              <ul class="feed">
                @for (event of site.events; track event.id) {
                  <li>
                    <strong>{{ event.title }}</strong>
                    <p class="muted">{{ eventDate(event) }}@if (event.location) { · {{ event.location }} }</p>
                    @if (event.description) {
                      <p>{{ event.description }}</p>
                    }
                  </li>
                }
              </ul>
            </section>
          }

          <section class="card" id="enquire">
            <h2>{{ 'public.enquire' | translate }}</h2>
            <p class="muted">{{ 'public.enquireHint' | translate }}</p>
            @if (submitted) {
              <p class="banner success">{{ 'public.enquireThanks' | translate }}</p>
            } @else {
              <form class="form" [formGroup]="form" (ngSubmit)="submit()">
                <label>
                  {{ 'public.parentName' | translate }}
                  <input formControlName="parentName" />
                </label>
                <label>
                  {{ 'public.phone' | translate }}
                  <input formControlName="phone" />
                </label>
                <label>
                  {{ 'public.email' | translate }}
                  <input type="email" formControlName="email" />
                </label>
                <label>
                  {{ 'public.class' | translate }}
                  <select formControlName="classId">
                    <option value="">{{ 'public.classOptional' | translate }}</option>
                    @for (klass of site.classes; track klass.id) {
                      <option [value]="klass.id">{{ klass.name }}</option>
                    }
                  </select>
                </label>
                <label class="wide">
                  {{ 'public.message' | translate }}
                  <textarea formControlName="message" rows="4"></textarea>
                </label>
                @if (formError) {
                  <p class="error">{{ formError | translate }}</p>
                }
                <button class="btn btn-primary" type="submit" [disabled]="form.invalid || submitting">
                  {{ submitting ? ('common.loading' | translate) : ('public.submit' | translate) }}
                </button>
              </form>
            }
          </section>

          <section class="card contact">
            <h2>{{ 'public.contact' | translate }}</h2>
            @if (site.contact.address) {
              <p>{{ site.contact.address }}</p>
            }
            @if (site.contact.phone) {
              <p>{{ site.contact.phone }}</p>
            }
            @if (site.contact.email) {
              <p>{{ site.contact.email }}</p>
            }
          </section>
        </main>
      }
    </div>
  `,
  styles: `
    :host { display: block; min-height: 100vh; }
    .public-page { min-height: 100vh; background: #f4f4fb; color: var(--color-text); }
    .top {
      display: flex; justify-content: space-between; gap: 16px; align-items: center;
      padding: 16px 24px; background: var(--color-sidebar); color: #fff;
    }
    .brand { display: flex; gap: 12px; align-items: center; }
    .logo { width: 44px; height: 44px; border-radius: 8px; object-fit: cover; background: #fff; }
    .school { margin: 0; font-weight: 700; }
    .muted { color: var(--color-muted); margin: 0; }
    .top .muted { color: #c7d2fe; }
    .top-actions { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
    .lang-switcher { display: flex; gap: 6px; }
    .lang-pill {
      padding: 6px 10px; border-radius: 20px; border: 1px solid rgba(255,255,255,.35);
      background: transparent; color: #fff; cursor: pointer; font: inherit; font-size: .82rem;
    }
    .lang-pill.active { background: #fff; color: var(--color-sidebar); }
    .btn {
      display: inline-flex; align-items: center; justify-content: center;
      padding: 10px 16px; border-radius: 8px; text-decoration: none; font: inherit; cursor: pointer;
      border: 0;
    }
    .btn.ghost { border: 1px solid rgba(255,255,255,.5); color: #fff; background: transparent; }
    .hero {
      position: relative; min-height: 280px; display: flex; align-items: flex-end;
      background: linear-gradient(135deg, var(--color-sidebar) 0%, #33417a 100%); color: #fff;
    }
    .hero-img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; opacity: .35; }
    .hero-copy { position: relative; padding: 48px 24px; max-width: 760px; }
    .hero-copy h1 { margin: 0 0 8px; font-size: 2rem; }
    .content { max-width: 960px; margin: 0 auto; padding: 24px 16px 48px; display: grid; gap: 16px; }
    .card {
      background: var(--color-surface); border: 1px solid var(--color-border);
      border-radius: 12px; padding: 20px; display: grid; gap: 10px;
    }
    h2 { margin: 0; font-size: 1.15rem; }
    .prose, .list, .feed { margin: 0; white-space: pre-wrap; }
    .list { padding-inline-start: 18px; }
    .feed { list-style: none; padding: 0; display: grid; gap: 12px; }
    .form { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    label { display: grid; gap: 6px; font-size: .9rem; font-weight: 500; }
    .wide { grid-column: 1 / -1; }
    input, select, textarea {
      width: 100%; padding: 10px 12px; border: 1px solid var(--color-border);
      border-radius: 8px; font: inherit; background: var(--color-surface); color: var(--color-text);
    }
    .error { color: #b91c1c; grid-column: 1 / -1; }
    .banner { padding: 12px 16px; border-radius: 8px; }
    .banner.success { color: #166534; background: #dcfce7; }
    .banner.error { color: #b91c1c; background: #fee2e2; margin: 16px; }
    @media (max-width: 720px) {
      .top, .hero-copy { padding: 16px; }
      .form { grid-template-columns: 1fr; }
      .top { flex-direction: column; align-items: flex-start; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicHomeComponent implements OnInit {
  site: PublicSite | null = null;
  error = '';
  formError = '';
  submitting = false;
  submitted = false;
  readonly supportedLocales = SUPPORTED_LOCALES;
  readonly localeMeta = LOCALE_META;
  readonly form = new FormGroup({
    parentName: new FormControl('', Validators.required),
    phone: new FormControl('', Validators.required),
    email: new FormControl(''),
    classId: new FormControl(''),
    message: new FormControl(''),
  });

  constructor(
    readonly localeService: LocaleService,
    readonly auth: AuthService,
    private publicApi: PublicService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  get facilityLines(): string[] {
    return (this.site?.copy.facilities ?? '')
      .split('\n')
      .map((line) => line.trim())
      .filter(Boolean);
  }

  setLocale(locale: (typeof SUPPORTED_LOCALES)[number]): void {
    this.localeService.setLocale(locale);
    this.load();
  }

  eventDate(event: PublicEvent): string {
    return event.endDate && event.endDate !== event.startDate
      ? `${event.startDate} – ${event.endDate}`
      : event.startDate;
  }

  submit(): void {
    if (this.form.invalid || !this.site) {
      return;
    }
    this.submitting = true;
    this.formError = '';
    this.cdr.markForCheck();
    const value = this.form.getRawValue();
    const klass = this.site.classes.find((item) => item.id === value.classId);
    this.publicApi
      .submitEnquiry({
        parentName: value.parentName ?? '',
        phone: value.phone ?? '',
        email: value.email || null,
        classId: value.classId || null,
        className: klass?.name ?? null,
        message: value.message || null,
      })
      .subscribe({
        next: () => {
          this.submitting = false;
          this.submitted = true;
          this.cdr.markForCheck();
        },
        error: (error: HttpErrorResponse) => {
          this.submitting = false;
          this.formError = error.status === 429 ? 'public.rateLimited' : 'public.submitFailed';
          this.cdr.markForCheck();
        },
      });
  }

  private load(): void {
    this.error = '';
    this.publicApi.getSite(this.localeService.current).subscribe({
      next: (site) => {
        this.site = site;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'public.loadFailed';
        this.cdr.markForCheck();
      },
    });
  }
}
