import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { AsyncPipe, SlicePipe } from '@angular/common';
import { AuthService } from '../../core/auth/auth.service';
import { AuthUser } from '../../core/auth/auth.model';
import { LocaleService } from '../../i18n/locale.service';
import { LOCALE_META, SUPPORTED_LOCALES } from '../../i18n/i18n.config';

interface NavItem {
  route: string;
  labelKey: string;
  icon: string;
}

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslateModule, AsyncPipe, SlicePipe],
  template: `
    <div class="layout">
      <aside class="sidebar">
        <div class="brand">
          <span class="brand-logo">🏫</span>
          <span class="brand-name">{{ 'app.name' | translate }}</span>
        </div>
        <nav class="nav">
          @for (item of navItems; track item.route) {
            <a class="nav-link"
               [routerLink]="item.route"
               routerLinkActive="active"
               [routerLinkActiveOptions]="{ exact: item.route === '/dashboard' }">
              <span class="nav-icon">{{ item.icon }}</span>
              <span>{{ item.labelKey | translate }}</span>
            </a>
          }
        </nav>
      </aside>

      <div class="main">
        <header class="topbar">
          <div class="topbar-title">{{ 'app.tagline' | translate }}</div>
          <div class="topbar-actions">
            <div class="lang-switcher">
              <select [value]="localeService.current" (change)="onLanguageChange($event)">
                @for (locale of supportedLocales; track locale) {
                  <option [value]="locale">{{ localeMeta[locale].label }}</option>
                }
              </select>
            </div>
            <div class="user-chip">
              <span class="avatar">{{ (user$ | async)?.displayName | slice: 0 : 1 }}</span>
              <span class="user-name">{{ (user$ | async)?.displayName || (user$ | async)?.username }}</span>
            </div>
            <button class="btn btn-ghost" (click)="onLogout()">{{ 'common.logout' | translate }}</button>
          </div>
        </header>

        <main class="content">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: `
    :host { display: block; height: 100%; }
    .layout { display: flex; min-height: 100vh; }

    .sidebar {
      width: 240px;
      background: var(--color-sidebar);
      color: var(--color-sidebar-text);
      display: flex;
      flex-direction: column;
      flex-shrink: 0;
    }
    .brand {
      display: flex; align-items: center; gap: 10px;
      padding: 20px 18px;
      font-weight: 700; font-size: 1.1rem;
      border-bottom: 1px solid rgba(255,255,255,.08);
    }
    .brand-logo { font-size: 1.4rem; }
    .nav { padding: 12px 10px; display: flex; flex-direction: column; gap: 4px; }
    .nav-link {
      display: flex; align-items: center; gap: 12px;
      padding: 11px 14px;
      border-radius: 8px;
      color: var(--color-sidebar-text);
      text-decoration: none;
      transition: background .15s ease;
    }
    .nav-link:hover { background: rgba(255,255,255,.08); }
    .nav-link.active { background: var(--color-primary); color: #fff; }
    .nav-icon { width: 20px; text-align: center; }

    .main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
    .topbar {
      display: flex; align-items: center; justify-content: space-between;
      padding: 12px 24px;
      background: #fff;
      border-bottom: 1px solid var(--color-border);
      position: sticky; top: 0; z-index: 10;
    }
    .topbar-title { font-weight: 600; color: var(--color-muted); }
    .topbar-actions { display: flex; align-items: center; gap: 14px; }
    .lang-switcher select {
      padding: 7px 10px;
      border: 1px solid var(--color-border);
      border-radius: 8px;
      background: #fff;
      font: inherit;
    }
    .user-chip { display: flex; align-items: center; gap: 8px; }
    .avatar {
      width: 32px; height: 32px;
      display: inline-flex; align-items: center; justify-content: center;
      border-radius: 50%;
      background: var(--color-primary);
      color: #fff;
      font-weight: 700; font-size: .85rem;
    }
    .user-name { font-weight: 500; }

    .content { padding: 24px; flex: 1; }

    @media (max-width: 768px) {
      .sidebar { width: 200px; }
      .user-name { display: none; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayoutComponent {
  readonly navItems: NavItem[] = [
    { route: '/dashboard', labelKey: 'nav.dashboard', icon: '📊' },
    { route: '/students', labelKey: 'nav.students', icon: '👩‍🎓' },
    { route: '/academics', labelKey: 'nav.academics', icon: '📚' },
    { route: '/attendance', labelKey: 'nav.attendance', icon: '✅' },
    { route: '/fees', labelKey: 'nav.fees', icon: '💳' },
    { route: '/notices', labelKey: 'nav.notices', icon: '📢' },
  ];

  readonly supportedLocales = SUPPORTED_LOCALES;
  readonly localeMeta = LOCALE_META;

  user$: Observable<AuthUser | null>;

  constructor(
    readonly localeService: LocaleService,
    private auth: AuthService
  ) {
    this.user$ = this.auth.user$;
  }

  onLanguageChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.localeService.setLocale(value as (typeof SUPPORTED_LOCALES)[number]);
  }

  onLogout(): void {
    this.auth.logout().subscribe(() => {
      window.location.href = '/login';
    });
  }
}
