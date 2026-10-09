import { AsyncPipe } from '@angular/common';
import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Observable, map } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { AuthUser } from '../../core/auth/auth.model';
import { ThemeMode, ThemeService } from '../../core/theme/theme.service';
import { LocaleService } from '../../i18n/locale.service';
import { LOCALE_META, SUPPORTED_LOCALES, SupportedLocale } from '../../i18n/i18n.config';
import { NotificationBellComponent } from '../notifications/notification-bell.component';

interface NavItem {
  route: string;
  labelKey: string;
  icon: string;
  permissions: string[];
}

const SIDENAV_STORAGE_KEY = 'schoolms.sidenav.collapsed';

@Component({
  selector: 'app-main-layout',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    TranslateModule,
    AsyncPipe,
    NotificationBellComponent,
    MatButtonModule,
    MatDividerModule,
    MatFormFieldModule,
    MatIconModule,
    MatMenuModule,
    MatSelectModule,
    MatSidenavModule,
    MatToolbarModule,
    MatTooltipModule,
  ],
  template: `
    <mat-sidenav-container class="layout">
      <mat-sidenav
        class="sidebar"
        [class.collapsed]="collapsed && !(isMobile$ | async)"
        [mode]="(isMobile$ | async) ? 'over' : 'side'"
        [opened]="(isMobile$ | async) ? mobileOpen : true"
        (closedStart)="mobileOpen = false">
        <div class="brand">
          <span class="brand-logo" aria-hidden="true">🏫</span>
          @if (!collapsed || (isMobile$ | async)) {
            <span class="brand-name">{{ 'app.name' | translate }}</span>
          }
        </div>
        <nav class="nav">
          @for (item of visibleNavItems; track item.route) {
            <a class="nav-link"
               [routerLink]="item.route"
               routerLinkActive="active"
               [routerLinkActiveOptions]="{ exact: item.route === '/dashboard' }"
               [matTooltip]="item.labelKey | translate"
               [matTooltipDisabled]="!collapsed"
               matTooltipShowDelay="200"
               (click)="onNavClick()">
              <mat-icon class="nav-icon">{{ item.icon }}</mat-icon>
              @if (!collapsed || (isMobile$ | async)) {
                <span class="nav-label">{{ item.labelKey | translate }}</span>
              }
            </a>
          }
        </nav>
        @if (!(isMobile$ | async)) {
          <div class="sidebar-footer">
            <button
              mat-icon-button
              type="button"
              class="collapse-btn"
              (click)="toggleCollapsed()"
              [attr.aria-label]="(collapsed ? 'common.expandNav' : 'common.collapseNav') | translate"
              [matTooltip]="(collapsed ? 'common.expandNav' : 'common.collapseNav') | translate">
              <mat-icon>{{ collapsed ? 'chevron_right' : 'chevron_left' }}</mat-icon>
            </button>
          </div>
        }
      </mat-sidenav>

      <mat-sidenav-content class="main">
        <mat-toolbar class="topbar">
          @if (isMobile$ | async) {
            <button
              mat-icon-button
              type="button"
              (click)="mobileOpen = !mobileOpen"
              [attr.aria-label]="'nav.menu' | translate">
              <mat-icon>menu</mat-icon>
            </button>
          }
          <span class="topbar-title">{{ 'app.tagline' | translate }}</span>
          <span class="topbar-spacer"></span>
          <div class="topbar-actions">
            <mat-form-field appearance="outline" class="lang-field" subscriptSizing="dynamic">
              <mat-label>{{ 'common.language' | translate }}</mat-label>
              <mat-select
                [value]="localeService.current"
                (selectionChange)="onLanguageChange($event.value)">
                @for (locale of supportedLocales; track locale) {
                  <mat-option [value]="locale">{{ localeMeta[locale].label }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            @if (theme.theme$ | async; as currentTheme) {
              <button
                mat-icon-button
                type="button"
                class="theme-toggle"
                (click)="theme.toggle()"
                [attr.aria-label]="'common.themeToggle' | translate"
                [matTooltip]="themeTooltipKey(currentTheme) | translate">
                <mat-icon fontSet="material-symbols-outlined">{{ currentTheme === 'dark' ? 'light_mode' : 'dark_mode' }}</mat-icon>
              </button>
            }
            <app-notification-bell />
            @if (user$ | async; as user) {
              <button mat-button type="button" class="profile-btn" [matMenuTriggerFor]="profileMenu">
                <span class="avatar">{{ userInitial(user) }}</span>
                <span class="user-meta">
                  <span class="user-name">{{ user.displayName || user.username }}</span>
                  <span class="user-role">{{ roleLabel(user) | translate }}</span>
                </span>
                <mat-icon>expand_more</mat-icon>
              </button>
              <mat-menu #profileMenu="matMenu" xPosition="before">
                <div class="profile-menu-head" (click)="$event.stopPropagation()">
                  <span class="avatar avatar-lg">{{ userInitial(user) }}</span>
                  <div>
                    <div class="user-name">{{ user.displayName || user.username }}</div>
                    <div class="user-role">{{ roleLabel(user) | translate }}</div>
                  </div>
                </div>
                <mat-divider />
                <button mat-menu-item type="button" (click)="onLogout()">
                  <mat-icon>logout</mat-icon>
                  <span>{{ 'common.logout' | translate }}</span>
                </button>
              </mat-menu>
            }
          </div>
        </mat-toolbar>

        <main class="content">
          <router-outlet />
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: `
    :host { display: block; height: 100%; }
    .layout { height: 100vh; }

    .sidebar {
      width: 256px;
      background: var(--color-sidebar);
      color: var(--color-sidebar-text);
      display: flex;
      flex-direction: column;
      border: 0;
      transition: width .2s ease;
    }
    .sidebar.collapsed { width: 72px; overflow-x: hidden; }
    :host-context(html.rtl) .collapse-btn mat-icon { transform: scaleX(-1); }
    .brand {
      display: flex; align-items: center; gap: 10px;
      padding: 16px 18px;
      min-height: 64px;
      font-weight: 700; font-size: 1.1rem;
      border-bottom: 1px solid rgba(255,255,255,.08);
    }
    .sidebar.collapsed .brand { justify-content: center; padding-inline: 8px; }
    .brand-logo { font-size: 1.4rem; line-height: 1; }
    .nav { padding: 12px 10px; display: flex; flex-direction: column; gap: 4px; flex: 1; overflow: auto; }
    .nav-link {
      display: flex; align-items: center; gap: 12px;
      padding: 10px 12px;
      border-radius: 8px;
      color: var(--color-sidebar-text);
      text-decoration: none;
      transition: background .15s ease, color .15s ease;
    }
    .sidebar.collapsed .nav-link { justify-content: center; padding-inline: 8px; }
    .nav-link:hover { background: rgba(255,255,255,.08); }
    .nav-link.active { background: var(--color-primary); color: #fff; }
    .nav-icon { width: 22px; height: 22px; font-size: 22px; flex-shrink: 0; }
    .nav-label { white-space: nowrap; overflow: hidden; }
    .sidebar-footer {
      padding: 8px;
      border-top: 1px solid rgba(255,255,255,.08);
      display: flex;
      justify-content: flex-end;
    }
    .sidebar.collapsed .sidebar-footer { justify-content: center; }
    .collapse-btn { color: var(--color-sidebar-text); }

    .main { display: flex; flex-direction: column; min-width: 0; background: var(--color-bg); }
    .topbar {
      position: sticky; top: 0; z-index: 10;
      background: var(--color-surface);
      color: var(--color-text);
      border-bottom: 1px solid var(--color-border);
      min-height: 64px;
      padding-inline: 8px 16px;
      gap: 8px;
    }
    .topbar-title { font-weight: 600; color: var(--color-muted); font-size: 0.95rem; }
    .topbar-spacer { flex: 1 1 auto; }
    .topbar-actions { display: flex; align-items: center; gap: 8px; }
    .lang-field { width: 132px; font-size: 14px; }
    .lang-field .mat-mdc-form-field-infix { min-height: 40px; padding-top: 8px; padding-bottom: 8px; }
    .theme-toggle mat-icon { font-size: 22px; width: 22px; height: 22px; color: var(--color-text); }

    .profile-btn {
      display: inline-flex; align-items: center; gap: 8px;
      height: 44px; padding-inline: 8px 4px;
      border-radius: 24px;
    }
    .avatar {
      width: 32px; height: 32px;
      display: inline-flex; align-items: center; justify-content: center;
      border-radius: 50%;
      background: var(--color-primary);
      color: var(--mat-sys-on-primary, #fff);
      font-weight: 700; font-size: .85rem;
      flex-shrink: 0;
    }
    .avatar-lg { width: 40px; height: 40px; font-size: 1rem; }
    .user-meta { display: flex; flex-direction: column; align-items: flex-start; line-height: 1.2; text-align: start; }
    .user-name { font-weight: 600; font-size: .88rem; }
    .user-role { font-size: .72rem; color: var(--color-muted); font-weight: 400; }
    .profile-menu-head {
      display: flex; align-items: center; gap: 12px;
      padding: 12px 16px 8px;
      min-width: 220px;
    }

    .content { padding: 24px; flex: 1; }

    @media (max-width: 768px) {
      .user-meta { display: none; }
      .topbar-title { display: none; }
      .lang-field { width: 108px; }
      .content { padding: 16px; }
      .sidebar { width: min(280px, 86vw); }
    }
    @media (max-width: 480px) {
      .topbar { padding-inline: 4px 8px; }
      .topbar-actions { gap: 4px; }
      .content { padding: 12px; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayoutComponent {
  collapsed = localStorage.getItem(SIDENAV_STORAGE_KEY) === 'true';
  mobileOpen = false;

  readonly isMobile$: Observable<boolean>;
  readonly user$: Observable<AuthUser | null>;
  readonly supportedLocales = SUPPORTED_LOCALES;
  readonly localeMeta = LOCALE_META;

  readonly navItems: NavItem[] = [
    { route: '/dashboard', labelKey: 'nav.dashboard', icon: 'dashboard', permissions: ['DASHBOARD_VIEW'] },
    { route: '/students', labelKey: 'nav.students', icon: 'school', permissions: ['STUDENT_READ'] },
    { route: '/staff', labelKey: 'nav.staff', icon: 'groups', permissions: ['STAFF_READ'] },
    { route: '/academics', labelKey: 'nav.academics', icon: 'menu_book', permissions: ['CLASS_READ'] },
    { route: '/examinations', labelKey: 'nav.examinations', icon: 'assignment', permissions: ['EXAM_READ', 'EXAM_MANAGE', 'ADMIT_CARD_READ', 'REPORT_CARD_READ'] },
    { route: '/attendance', labelKey: 'nav.attendance', icon: 'fact_check', permissions: ['ATTENDANCE_READ', 'ATTENDANCE_MARK'] },
    { route: '/fees', labelKey: 'nav.fees', icon: 'payments', permissions: ['FEE_READ', 'FEE_RECEIPT_VIEW', 'FEE_STRUCTURE_MANAGE'] },
    { route: '/payroll', labelKey: 'nav.payroll', icon: 'account_balance_wallet', permissions: ['EXPENSE_READ', 'PAYROLL_READ', 'PAYSLIP_READ'] },
    { route: '/calendar', labelKey: 'nav.calendar', icon: 'calendar_month', permissions: ['EVENT_READ'] },
    { route: '/certificates', labelKey: 'nav.certificates', icon: 'workspace_premium', permissions: ['CERTIFICATE_GENERATE', 'CERTIFICATE_APPROVE'] },
    { route: '/downloads', labelKey: 'nav.downloads', icon: 'download', permissions: ['MARKSHEET_READ', 'CERTIFICATE_READ'] },
    { route: '/notices', labelKey: 'nav.notices', icon: 'campaign', permissions: ['NOTICE_READ'] },
  ];

  constructor(
    readonly localeService: LocaleService,
    readonly theme: ThemeService,
    private auth: AuthService,
    breakpoints: BreakpointObserver
  ) {
    this.user$ = this.auth.user$;
    this.isMobile$ = breakpoints.observe('(max-width: 768px)').pipe(map((state) => state.matches));
  }

  get visibleNavItems(): NavItem[] {
    return this.navItems.filter((item) => this.auth.hasAnyPermission(item.permissions));
  }

  toggleCollapsed(): void {
    this.collapsed = !this.collapsed;
    localStorage.setItem(SIDENAV_STORAGE_KEY, String(this.collapsed));
  }

  onNavClick(): void {
    this.mobileOpen = false;
  }

  onLanguageChange(locale: SupportedLocale): void {
    this.localeService.setLocale(locale);
  }

  themeTooltipKey(theme: ThemeMode): string {
    return theme === 'dark' ? 'common.themeLight' : 'common.themeDark';
  }

  userInitial(user: AuthUser): string {
    const name = user.displayName || user.username || '?';
    return name.slice(0, 1).toUpperCase();
  }

  roleLabel(user: AuthUser): string {
    const role = user.roles[0];
    return role ? `roles.${role}` : 'roles.STUDENT';
  }

  onLogout(): void {
    this.auth.logout().subscribe(() => {
      window.location.href = '/login';
    });
  }
}
