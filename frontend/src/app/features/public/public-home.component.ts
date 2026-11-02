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
    <div class="madarsa">
      <div class="arabesque" aria-hidden="true"></div>
      <svg class="vine left" viewBox="0 0 280 420" aria-hidden="true">
        <path class="stem" d="M40 0 C70 70 10 120 48 180 C90 250 20 300 56 380 C70 410 80 420 90 430" />
        <path class="leaf" d="M48 70 C78 58 108 92 72 108 C58 96 46 84 48 70Z" />
        <path class="leaf" d="M28 150 C8 168 28 206 52 184 C46 168 36 154 28 150Z" />
        <path class="leaf" d="M70 220 C104 208 122 252 86 262 C74 244 68 228 70 220Z" />
        <path class="leaf" d="M30 300 C6 318 22 360 52 338 C44 318 34 304 30 300Z" />
        <circle class="bloom white" cx="86" cy="96" r="7" />
        <circle class="bloom gold" cx="42" cy="188" r="6" />
        <circle class="bloom white" cx="98" cy="248" r="7" />
        <circle class="bloom gold" cx="38" cy="332" r="6" />
      </svg>
      <svg class="vine right" viewBox="0 0 280 420" aria-hidden="true">
        <path class="stem" d="M240 0 C210 70 270 120 232 180 C190 250 260 300 224 380 C210 410 200 420 190 430" />
        <path class="leaf" d="M232 70 C202 58 172 92 208 108 C222 96 234 84 232 70Z" />
        <path class="leaf" d="M252 150 C272 168 252 206 228 184 C234 168 244 154 252 150Z" />
        <path class="leaf" d="M210 220 C176 208 158 252 194 262 C206 244 212 228 210 220Z" />
        <path class="leaf" d="M250 300 C274 318 258 360 228 338 C236 318 246 304 250 300Z" />
        <circle class="bloom white" cx="194" cy="96" r="7" />
        <circle class="bloom gold" cx="238" cy="188" r="6" />
        <circle class="bloom white" cx="182" cy="248" r="7" />
        <circle class="bloom gold" cx="242" cy="332" r="6" />
      </svg>

      <header class="nav">
        <div class="nav-row">
          <a class="brand" href="#top">
            <svg class="madina" viewBox="0 0 88 88" aria-hidden="true">
              <circle cx="44" cy="44" r="42" fill="#fff" stroke="#C5A059" stroke-width="2" />
              <path fill="#9BB89A" d="M10 70h68v6H10z" />
              <rect x="16" y="48" width="12" height="22" rx="1" fill="#0A5C36" />
              <rect x="60" y="48" width="12" height="22" rx="1" fill="#0A5C36" />
              <rect x="30" y="52" width="28" height="18" rx="1" fill="#0A5C36" />
              <path fill="#2F7A4A" d="M28 52h32v4H28z" />
              <circle cx="44" cy="38" r="14" fill="#2F7A4A" />
              <path fill="#1F6B40" d="M44 18c8 6 14 14 14 20a14 14 0 0 1-28 0c0-6 6-14 14-20z" />
              <circle cx="44" cy="22" r="3.2" fill="#C5A059" />
              <rect x="42.6" y="8" width="2.8" height="12" rx="1" fill="#C5A059" />
              <rect x="20.6" y="28" width="2.8" height="20" rx="1" fill="#C5A059" />
              <rect x="64.6" y="28" width="2.8" height="20" rx="1" fill="#C5A059" />
              <circle cx="22" cy="26" r="2.4" fill="#E8D7A8" />
              <circle cx="66" cy="26" r="2.4" fill="#E8D7A8" />
              <rect x="41" y="58" width="6" height="12" rx="1" fill="#E8D7A8" />
            </svg>
            <span class="brand-copy">
              <span class="arabic" dir="rtl">{{ 'public.collegeArabic' | translate }}</span>
              <span class="wordmark">{{ 'public.collegeName' | translate }}</span>
            </span>
          </a>
          <p class="bismillah" dir="rtl">بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ</p>
          <div class="nav-actions">
            <div class="lang-switcher">
              @for (locale of supportedLocales; track locale) {
                <button
                  class="lang-pill"
                  [attr.lang]="locale"
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
        </div>
        <nav class="links" aria-label="primary">
          <a href="#programs">{{ 'public.navPrograms' | translate }}</a>
          <a href="#about">{{ 'public.navAbout' | translate }}</a>
          <a href="#faculty">{{ 'public.navFaculty' | translate }}</a>
          <a href="#enquire">{{ 'public.navAdmissions' | translate }}</a>
        </nav>
      </header>

      @if (error) {
        <p class="banner error">{{ error | translate }}</p>
      }

      <section class="hero" id="top">
        <p class="arabic hero-arabic" dir="rtl">{{ 'public.collegeArabic' | translate }}</p>
        <p class="eyebrow">{{ 'public.collegeSuffix' | translate }}</p>
        <h1>{{ 'public.heroHeadline' | translate }}</h1>
        <p class="lead">{{ site?.copy?.tagline || ('public.heroSub' | translate) }}</p>
        <div class="hero-ctas">
          <a class="btn gold" href="#programs">{{ 'public.explorePrograms' | translate }}</a>
          <a class="btn outline" href="#enquire">{{ 'public.admissionsOpen' | translate }}</a>
        </div>
      </section>

      <section class="stats">
        <div class="wrap stats-row">
          <div>
            <strong>K–12</strong>
            <span>{{ 'public.statFaculties' | translate }}</span>
          </div>
          <div>
            <strong>480+</strong>
            <span>{{ 'public.statScholars' | translate }}</span>
          </div>
          <div>
            <strong>36</strong>
            <span>{{ 'public.statPapers' | translate }}</span>
          </div>
          <div>
            <strong>25</strong>
            <span>{{ 'public.statAlumni' | translate }}</span>
          </div>
        </div>
      </section>

      <section class="about" id="about">
        <div class="wrap split">
          <article>
            <p class="kicker">{{ 'public.welcome' | translate }}</p>
            <h2>{{ 'public.about' | translate }}</h2>
            <p class="prose">{{ site?.copy?.about || ('public.aboutLead' | translate) }}</p>
            @if (site?.classes?.length) {
              <div class="class-row">
                <span class="class-label">{{ 'public.classesOffered' | translate }}</span>
                @for (klass of site!.classes; track klass.id) {
                  <span class="chip">{{ klass.name }}</span>
                }
              </div>
            }
          </article>
          <blockquote class="note">
            <p class="kicker">{{ 'public.principal' | translate }}</p>
            <p class="quote">{{ site?.copy?.principalMessage || ('public.heroSub' | translate) }}</p>
            <footer>
              <span class="who">{{ site?.principalName || displayName }}</span>
              <span class="role">{{ 'public.principalRole' | translate }}</span>
            </footer>
          </blockquote>
        </div>
      </section>

      <section class="programs" id="programs">
        <div class="wrap">
          <header class="section-head">
            <p class="kicker">{{ 'public.programsKicker' | translate }}</p>
            <h2>{{ 'public.programsTitle' | translate }}</h2>
          </header>
          <ul class="card-grid four">
            <li>
              <span class="mark">01</span>
              <h3>{{ 'public.programFiqh' | translate }}</h3>
              <p>{{ 'public.programFiqhDesc' | translate }}</p>
            </li>
            <li>
              <span class="mark">02</span>
              <h3>{{ 'public.programArabic' | translate }}</h3>
              <p>{{ 'public.programArabicDesc' | translate }}</p>
            </li>
            <li>
              <span class="mark">03</span>
              <h3>{{ 'public.programQuran' | translate }}</h3>
              <p>{{ 'public.programQuranDesc' | translate }}</p>
            </li>
            <li>
              <span class="mark">04</span>
              <h3>{{ 'public.programEthics' | translate }}</h3>
              <p>{{ 'public.programEthicsDesc' | translate }}</p>
            </li>
          </ul>
        </div>
      </section>

      <section class="faculty" id="faculty">
        <div class="wrap">
          <header class="section-head">
            <p class="kicker">{{ 'public.facultyKicker' | translate }}</p>
            <h2>{{ 'public.facultyTitle' | translate }}</h2>
          </header>
          <ul class="card-grid three">
            <li class="teacher">
              <span class="avatar">ق</span>
              <strong>{{ 'public.facultyOne' | translate }}</strong>
            </li>
            <li class="teacher">
              <span class="avatar">ع</span>
              <strong>{{ 'public.facultyTwo' | translate }}</strong>
            </li>
            <li class="teacher">
              <span class="avatar">ع</span>
              <strong>{{ 'public.facultyThree' | translate }}</strong>
            </li>
          </ul>
        </div>
      </section>

      <section class="wisdom">
        <div class="wrap wisdom-inner">
          <p class="kicker">{{ 'public.wisdomKicker' | translate }}</p>
          <span class="ornament" aria-hidden="true"></span>
          <blockquote>{{ 'public.wisdomQuote' | translate }}</blockquote>
          <cite>{{ 'public.wisdomSource' | translate }}</cite>
        </div>
      </section>

      @if (facilityLines.length) {
        <section class="campus" id="campus">
          <div class="wrap">
            <header class="section-head">
              <p class="kicker">{{ 'public.navCampus' | translate }}</p>
              <h2>{{ 'public.facilities' | translate }}</h2>
            </header>
            <ul class="card-grid five">
              @for (item of facilityLines; track item) {
                <li>{{ item }}</li>
              }
            </ul>
          </div>
        </section>
      }

      @if (site?.notices?.length || site?.events?.length) {
        <section class="news" id="news">
          <div class="wrap news-grid">
            @if (site!.notices.length) {
              <div>
                <header class="section-head">
                  <p class="kicker">{{ 'public.navNews' | translate }}</p>
                  <h2>{{ 'public.notices' | translate }}</h2>
                </header>
                <ul class="feed">
                  @for (notice of site!.notices; track notice.id) {
                    <li>
                      <strong>{{ notice.title }}</strong>
                      <p>{{ notice.body }}</p>
                    </li>
                  }
                </ul>
              </div>
            }
            @if (site!.events.length) {
              <div>
                <header class="section-head">
                  <p class="kicker">{{ 'public.events' | translate }}</p>
                  <h2>{{ 'public.events' | translate }}</h2>
                </header>
                <ul class="feed events">
                  @for (event of site!.events; track event.id) {
                    <li>
                      <time>
                        <span>{{ eventDay(event.startDate) }}</span>
                        <small>{{ eventMonth(event.startDate) }}</small>
                      </time>
                      <div>
                        <strong>{{ event.title }}</strong>
                        <p>{{ eventDate(event) }}@if (event.location) { · {{ event.location }} }</p>
                      </div>
                    </li>
                  }
                </ul>
              </div>
            }
          </div>
        </section>
      }

      <section class="enquire" id="enquire">
        <div class="wrap enquire-grid">
          <div id="contact">
            <p class="kicker">{{ 'public.navAdmissions' | translate }}</p>
            <h2>{{ 'public.enquire' | translate }}</h2>
            <p class="lead-ink">{{ 'public.enquireHint' | translate }}</p>
            <dl>
              @if (site?.contact?.address) {
                <div>
                  <dt>{{ 'public.addressLabel' | translate }}</dt>
                  <dd>{{ site!.contact.address }}</dd>
                </div>
              }
              @if (site?.contact?.phone) {
                <div>
                  <dt>{{ 'public.phoneLabel' | translate }}</dt>
                  <dd>{{ site!.contact.phone }}</dd>
                </div>
              }
              @if (site?.contact?.email) {
                <div>
                  <dt>{{ 'public.emailLabel' | translate }}</dt>
                  <dd>{{ site!.contact.email }}</dd>
                </div>
              }
            </dl>
          </div>
          <div class="form-card">
            @if (submitted) {
              <p class="banner success">{{ 'public.enquireThanks' | translate }}</p>
            } @else {
              <form [formGroup]="form" (ngSubmit)="submit()">
                <label>
                  {{ 'public.parentName' | translate }}
                  <input formControlName="parentName" autocomplete="name" />
                </label>
                <label>
                  {{ 'public.phone' | translate }}
                  <input formControlName="phone" autocomplete="tel" />
                </label>
                <label>
                  {{ 'public.email' | translate }}
                  <input type="email" formControlName="email" autocomplete="email" />
                </label>
                <label>
                  {{ 'public.class' | translate }}
                  <select formControlName="classId">
                    <option value="">{{ 'public.classOptional' | translate }}</option>
                    @for (klass of site?.classes ?? []; track klass.id) {
                      <option [value]="klass.id">{{ klass.name }}</option>
                    }
                  </select>
                </label>
                <label class="wide">
                  {{ 'public.message' | translate }}
                  <textarea formControlName="message" rows="4"></textarea>
                </label>
                @if (formError) {
                  <p class="form-error">{{ formError | translate }}</p>
                }
                <button class="btn gold wide" type="submit" [disabled]="form.invalid || submitting">
                  {{ submitting ? ('common.loading' | translate) : ('public.submit' | translate) }}
                </button>
              </form>
            }
          </div>
        </div>
      </section>

      <footer class="foot">
        <div class="wrap foot-grid">
          <div>
            <span class="arabic" dir="rtl">{{ 'public.collegeArabic' | translate }}</span>
            <p class="wordmark">{{ 'public.collegeName' | translate }}</p>
            <p class="muted">{{ 'public.footerAccreditation' | translate }}</p>
          </div>
          <div>
            <strong>{{ 'public.footerQuickLinks' | translate }}</strong>
            <a href="#programs">{{ 'public.navPrograms' | translate }}</a>
            <a href="#about">{{ 'public.navAbout' | translate }}</a>
            <a href="#enquire">{{ 'public.navAdmissions' | translate }}</a>
            <a routerLink="/login">{{ 'public.navPortal' | translate }}</a>
          </div>
          <div>
            <strong>{{ 'public.contact' | translate }}</strong>
            @if (site?.contact?.address) { <p>{{ site!.contact.address }}</p> }
            @if (site?.contact?.phone) { <p>{{ site!.contact.phone }}</p> }
            @if (site?.contact?.email) { <p>{{ site!.contact.email }}</p> }
          </div>
        </div>
        <p class="legal">{{ displayName }} · {{ 'public.footerRights' | translate }}</p>
      </footer>
    </div>
  `,
  styles: `
    :host { display: block; min-height: 100vh; }
    .madarsa {
      --ivory: #FBFBFA;
      --linen: #F7F3EA;
      --emerald: #0A5C36;
      --emerald-deep: #064e3b;
      --gold: #C5A059;
      --gold-soft: #E8D7A8;
      --ink: #1F2937;
      --muted: #5B6470;
      position: relative;
      min-height: 100vh;
      overflow: hidden;
      background: var(--ivory);
      color: var(--ink);
      font-family: Inter, "Noto Nastaliq Urdu", sans-serif;
    }
    .arabesque {
      pointer-events: none;
      position: absolute; inset: 0;
      opacity: .055;
      background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='120' height='104' viewBox='0 0 120 104'%3E%3Cpath fill='none' stroke='%230A5C36' stroke-width='1.2' d='M60 2 L90 18 L90 52 L60 68 L30 52 L30 18 Z M60 18 L75 26 L75 44 L60 52 L45 44 L45 26 Z M60 68 L90 84 L60 102 L30 84 Z'/%3E%3C/svg%3E");
      background-size: 120px 104px;
    }
    .vine {
      position: absolute; top: 0; width: min(280px, 32vw); height: 420px;
      z-index: 2; pointer-events: none;
    }
    .vine.left { left: 0; }
    .vine.right { right: 0; }
    .stem { fill: none; stroke: #7A9B7E; stroke-width: 2.2; }
    .leaf { fill: #9BB89A; opacity: .85; }
    .bloom.white { fill: #fff; stroke: #E8D7A8; stroke-width: 1.4; }
    .bloom.gold { fill: #E8D7A8; }
    .wrap { width: min(1080px, calc(100% - 40px)); margin-inline: auto; position: relative; }
    .nav {
      position: sticky; top: 0; z-index: 20;
      display: grid; gap: 8px;
      padding: 10px 28px 12px;
      background: rgba(251,251,250,.86);
      backdrop-filter: blur(16px);
      border-bottom: 1px solid rgba(197,160,89,.28);
    }
    .nav-row {
      display: grid; grid-template-columns: 1fr auto 1fr;
      align-items: center; gap: 16px;
    }
    .brand { display: flex; gap: 12px; align-items: center; text-decoration: none; color: inherit; min-width: 0; justify-self: start; }
    .madina { width: 52px; height: 52px; flex: none; }
    .brand-copy { display: grid; min-width: 0; line-height: 1.15; }
    .arabic { font-family: Amiri, serif; color: var(--emerald); }
    .wordmark {
      font-family: "Playfair Display", Georgia, serif; font-weight: 700;
      background: linear-gradient(120deg, #0A5C36, #C5A059);
      -webkit-background-clip: text; background-clip: text; color: transparent;
    }
    .bismillah {
      margin: 0; text-align: center; justify-self: center;
      font-family: Amiri, serif; font-size: clamp(1.15rem, 2.2vw, 1.7rem);
      color: var(--emerald-deep); font-weight: 700; line-height: 1.4;
      letter-spacing: .02em;
    }
    .links { display: flex; gap: 18px; font-size: .9rem; justify-content: center; }
    .links a { color: var(--emerald-deep); text-decoration: none; font-weight: 500; }
    .links a:hover { color: var(--gold); }
    .nav-actions { justify-self: end; }
    .nav-actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
    .lang-switcher { display: flex; gap: 6px; align-items: center; }
    .lang-pill {
      min-width: 72px; height: 32px; padding: 0 12px; border-radius: 999px;
      border: 1px solid rgba(10,92,54,.2); background: transparent; color: var(--emerald);
      cursor: pointer; font-size: .78rem; font-weight: 600; line-height: 1;
      display: inline-flex; align-items: center; justify-content: center;
      font-family: Inter, Amiri, "Noto Naskh Arabic", sans-serif;
      overflow: visible;
    }
    .lang-pill[lang="ur"] {
      font-family: Amiri, "Noto Naskh Arabic", serif;
      font-size: .9rem;
    }
    .lang-pill.active { background: var(--emerald); color: #fff; border-color: var(--emerald); }
    .btn {
      display: inline-flex; align-items: center; justify-content: center;
      padding: 11px 22px; border-radius: 999px; text-decoration: none; font: inherit;
      cursor: pointer; border: 0; font-weight: 650; transition: transform .2s ease, box-shadow .2s ease;
    }
    .btn:hover { transform: translateY(-1px); }
    .btn.gold { background: var(--gold); color: var(--emerald-deep); }
    .btn.gold:disabled { opacity: .55; cursor: not-allowed; transform: none; }
    .btn.outline { background: transparent; color: var(--emerald); box-shadow: inset 0 0 0 1.5px var(--emerald); }
    .btn.ghost { background: transparent; color: var(--emerald); box-shadow: inset 0 0 0 1px var(--gold); padding: 8px 14px; font-size: .85rem; }
    .hero {
      position: relative; z-index: 1; text-align: center;
      padding: 92px 24px 72px; max-width: 820px; margin: 0 auto;
    }
    .hero-arabic { display: block; font-size: 1.8rem; margin: 0 0 8px; }
    .eyebrow {
      margin: 0 0 16px; letter-spacing: .22em; text-transform: uppercase;
      font-size: .72rem; color: var(--gold); font-weight: 700;
    }
    .hero h1 {
      margin: 0 0 18px; font-family: "Playfair Display", Georgia, serif;
      font-size: clamp(2.2rem, 5vw, 3.8rem); line-height: 1.18; color: var(--emerald-deep); font-weight: 700;
    }
    .lead { margin: 0 auto 28px; max-width: 38rem; color: var(--muted); line-height: 1.8; }
    .hero-ctas { display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; }
    .stats { position: relative; z-index: 1; padding: 8px 0 36px; }
    .stats-row {
      display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px;
      background: rgba(255,255,255,.72); border: 1px solid var(--gold-soft);
      border-radius: 20px; padding: 22px 12px; text-align: center;
    }
    .stats-row strong { display: block; font-family: "Playfair Display", serif; font-size: 1.6rem; color: var(--emerald); }
    .stats-row span { font-size: .72rem; letter-spacing: .12em; text-transform: uppercase; color: var(--muted); }
    .about, .programs, .faculty, .campus, .news { position: relative; z-index: 1; padding: 64px 0; }
    .split { display: grid; grid-template-columns: 1.15fr .85fr; gap: 32px; align-items: stretch; }
    .kicker {
      margin: 0 0 8px; letter-spacing: .2em; text-transform: uppercase;
      font-size: .7rem; font-weight: 700; color: var(--gold);
    }
    h2 {
      margin: 0 0 16px; font-family: "Playfair Display", Georgia, serif;
      font-size: clamp(1.6rem, 3vw, 2.2rem); color: var(--emerald-deep);
    }
    .prose { margin: 0; white-space: pre-wrap; line-height: 1.85; color: var(--muted); }
    .class-row { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 20px; align-items: center; }
    .class-label { font-size: .8rem; font-weight: 700; color: var(--muted); }
    .chip { padding: 6px 12px; border-radius: 999px; background: #fff; border: 1px solid var(--gold-soft); color: var(--emerald); font-size: .8rem; }
    .note {
      margin: 0; padding: 28px; background: #fff; border: 1px solid var(--gold-soft);
      border-radius: 24px; display: grid; gap: 12px;
    }
    .quote { margin: 0; font-family: "Playfair Display", serif; font-style: italic; line-height: 1.8; color: var(--emerald-deep); white-space: pre-wrap; }
    .who { font-weight: 700; color: var(--emerald); }
    .role { color: var(--gold); font-size: .85rem; }
    .section-head { text-align: center; margin-bottom: 28px; }
    .card-grid { list-style: none; margin: 0; padding: 0; display: grid; gap: 16px; }
    .card-grid.four { grid-template-columns: repeat(4, 1fr); }
    .card-grid.three { grid-template-columns: repeat(3, 1fr); }
    .card-grid.five { grid-template-columns: repeat(5, 1fr); }
    .card-grid li {
      background: rgba(255,255,255,.82); border: 1px solid var(--gold-soft); border-radius: 20px;
      padding: 24px 20px; transition: transform .25s ease, box-shadow .25s ease;
    }
    .card-grid li:hover { transform: translateY(-4px); box-shadow: 0 16px 30px rgba(10,92,54,.08); }
    .mark { color: var(--gold); font-family: "Playfair Display", serif; font-weight: 700; }
    .card-grid h3 { margin: 10px 0 8px; font-size: 1.05rem; color: var(--emerald-deep); }
    .card-grid p { margin: 0; color: var(--muted); line-height: 1.65; }
    .teacher { display: grid; justify-items: center; text-align: center; gap: 12px; }
    .avatar {
      width: 72px; height: 72px; border-radius: 50%; display: grid; place-items: center;
      background: #fff; color: var(--emerald); font-family: Amiri, serif; font-size: 1.8rem;
      border: 1px solid var(--gold);
    }
    .wisdom { position: relative; z-index: 1; padding: 64px 0; text-align: center; }
    .wisdom-inner {
      max-width: 720px; padding: 36px 24px; background: rgba(255,255,255,.7);
      border: 1px solid var(--gold-soft); border-radius: 28px;
    }
    .ornament {
      display: block; width: 72px; height: 2px; margin: 10px auto 16px;
      background: linear-gradient(90deg, transparent, var(--gold), transparent);
    }
    .wisdom blockquote {
      margin: 0; font-family: "Playfair Display", serif; font-style: italic;
      font-size: clamp(1.25rem, 2.4vw, 1.8rem); line-height: 1.55; color: var(--emerald-deep);
    }
    .wisdom cite { display: block; margin-top: 12px; color: var(--gold); font-style: normal; letter-spacing: .12em; text-transform: uppercase; font-size: .72rem; }
    .news-grid { display: grid; grid-template-columns: 1.2fr .8fr; gap: 28px; }
    .feed { list-style: none; margin: 0; padding: 0; display: grid; gap: 12px; }
    .feed li { background: rgba(255,255,255,.85); border: 1px solid var(--gold-soft); border-radius: 16px; padding: 16px; }
    .feed strong { display: block; color: var(--emerald-deep); margin-bottom: 6px; }
    .feed p { margin: 0; color: var(--muted); }
    .events li { display: grid; grid-template-columns: 58px 1fr; gap: 12px; }
    time {
      display: grid; place-items: center; background: var(--linen); color: var(--emerald);
      border: 1px solid var(--gold-soft); border-radius: 12px; text-align: center; padding: 8px 0;
    }
    time span { font-size: 1.1rem; font-weight: 700; line-height: 1; }
    time small { font-size: .65rem; letter-spacing: .08em; text-transform: uppercase; }
    .enquire { position: relative; z-index: 1; padding: 72px 0 80px; }
    .enquire-grid { display: grid; grid-template-columns: .9fr 1.1fr; gap: 32px; }
    .lead-ink { color: var(--muted); line-height: 1.75; }
    dl { display: grid; gap: 14px; margin-top: 22px; }
    dt { font-size: .72rem; letter-spacing: .14em; text-transform: uppercase; color: var(--gold); }
    dd { margin: 0; color: var(--emerald-deep); }
    .form-card { background: #fff; border: 1px solid var(--gold-soft); border-radius: 24px; padding: 28px; }
    form { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    label { display: grid; gap: 6px; font-size: .85rem; font-weight: 650; color: var(--emerald-deep); }
    .wide { grid-column: 1 / -1; }
    input, select, textarea {
      width: 100%; padding: 11px 12px; border: 1px solid #E5E0D4; border-radius: 12px;
      font: inherit; background: var(--ivory); color: var(--ink);
    }
    input:focus, select:focus, textarea:focus { outline: 2px solid var(--gold); border-color: var(--gold); }
    .form-error { color: #991B1B; grid-column: 1 / -1; margin: 0; }
    .banner { padding: 12px 16px; border-radius: 12px; }
    .banner.success { color: var(--emerald-deep); background: #E8F5EE; }
    .banner.error { color: #991B1B; background: #FEE2E2; margin: 16px; }
    .foot { position: relative; z-index: 1; padding: 40px 0 24px; border-top: 1px solid var(--gold-soft); }
    .foot-grid { display: grid; grid-template-columns: 1.3fr .8fr .9fr; gap: 24px; }
    .foot .wordmark { margin: 6px 0 10px; font-size: 1.3rem; }
    .muted { color: var(--muted); line-height: 1.65; }
    .foot a, .foot p { display: block; color: var(--muted); text-decoration: none; margin: 6px 0; }
    .foot a:hover { color: var(--emerald); }
    .legal { text-align: center; margin: 24px 0 0; font-size: .8rem; color: var(--muted); }
    @media (max-width: 980px) {
      .links, .vine { display: none; }
      .split, .card-grid.four, .card-grid.three, .card-grid.five, .news-grid, .enquire-grid, .stats-row, .foot-grid {
        grid-template-columns: 1fr 1fr;
      }
      .nav-row { grid-template-columns: 1fr; justify-items: center; text-align: center; }
      .brand, .nav-actions { justify-self: center; }
      .bismillah { order: -1; }
    }
    @media (max-width: 700px) {
      .split, .card-grid.four, .card-grid.three, .card-grid.five, .news-grid, .enquire-grid, .stats-row, .foot-grid, form {
        grid-template-columns: 1fr;
      }
      .hero { padding-top: 56px; }
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

  get displayName(): string {
    return this.site?.name?.trim() || 'Madarsa Gulshan-e-Auliya';
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

  eventDay(iso: string): string {
    const date = new Date(`${iso}T00:00:00`);
    return Number.isNaN(date.getTime()) ? iso : String(date.getDate());
  }

  eventMonth(iso: string): string {
    const date = new Date(`${iso}T00:00:00`);
    return Number.isNaN(date.getTime())
      ? ''
      : date.toLocaleString(this.localeService.current, { month: 'short' });
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting = true;
    this.formError = '';
    this.cdr.markForCheck();
    const value = this.form.getRawValue();
    const klass = this.site?.classes.find((item) => item.id === value.classId);
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
