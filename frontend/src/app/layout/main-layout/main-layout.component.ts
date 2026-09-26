import { AsyncPipe, SlicePipe } from '@angular/common';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Observable, Subject, takeUntil } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { AuthUser } from '../../core/auth/auth.model';
import { ThemeService } from '../../core/theme/theme.service';
import { LocaleService } from '../../i18n/locale.service';
import { LOCALE_META, SUPPORTED_LOCALES } from '../../i18n/i18n.config';

interface NavItem {
  route: string;
  labelKey: string;
  icon: string;
  permissions: string[];
}

const NAV_ITEMS: NavItem[] = [
  { route: '/dashboard', labelKey: 'nav.dashboard', icon: 'space_dashboard', permissions: ['DASHBOARD_VIEW'] },
  { route: '/students', labelKey: 'nav.students', icon: 'school', permissions: ['STUDENT_READ'] },
  { route: '/staff', labelKey: 'nav.staff', icon: 'badge', permissions: ['STAFF_READ'] },
  { route: '/academics', labelKey: 'nav.academics', icon: 'menu_book', permissions: ['CLASS_READ'] },
  {
    route: '/examinations',
    labelKey: 'nav.examinations',
    icon: 'assignment',
    permissions: ['EXAM_READ', 'EXAM_MANAGE', 'ADMIT_CARD_READ', 'REPORT_CARD_READ'],
  },
  { route: '/attendance', labelKey: 'nav.attendance', icon: 'fact_check', permissions: ['ATTENDANCE_READ', 'ATTENDANCE_MARK'] },
  { route: '/fees', labelKey: 'nav.fees', icon: 'payments', permissions: ['FEE_READ', 'FEE_RECEIPT_VIEW'] },
  { route: '/calendar', labelKey: 'nav.calendar', icon: 'calendar_month', permissions: ['EVENT_READ'] },
  { route: '/downloads', labelKey: 'nav.downloads', icon: 'download', permissions: ['MARKSHEET_READ'] },
  { route: '/notices', labelKey: 'nav.notices', icon: 'campaign', permissions: ['NOTICE_READ'] },
];

const COLLAPSE_STORAGE_KEY = 'schoolms.sidenav.collapsed';

@Component({
  selector: 'app-main-layout',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    TranslateModule,
    AsyncPipe,
    SlicePipe,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule,
    MatTooltipModule,
  ],
  template: `
    <mat-sidenav-container class="shell">
      <mat-sidenav
        #sidenav
        class="shell__sidenav"
        [class.shell__sidenav--collapsed]="collapsed && !isMobile"
        [mode]="isMobile ? 'over' : 'side'"
        [opened]="!isMobile || mobileOpen"
        (closedStart)="mobileOpen = false"
      >
        <div class="brand">
          <span class="brand-logo">🏫</span>
          @if (!collapsed || isMobile) {
            <span class="brand-name">{{ 'app.name' | translate }}</span>
          }
        </div>

        <mat-nav-list class="nav">
          @for (item of visibleNavItems; track item.route) {
            <a
              mat-list-item
              class="nav-link"
              [matTooltip]="collapsed && !isMobile ? (item.labelKey | translate) : ''"
              matTooltipPosition="right"
              [routerLink]="item.route"
              routerLinkActive="nav-link--active"
              [routerLinkActiveOptions]="{ exact: item.route === '/dashboard' }"
              (click)="onNavClick()"
            >
              <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
              @if (!collapsed || isMobile) {
                <span matListItemTitle>{{ item.labelKey | translate }}</span>
              }
            </a>
          }
        </mat-nav-list>

        @if (!isMobile) {
          <div class="sidenav-footer">
            <button
              type="button"
              class="collapse-toggle"
              [matTooltip]="(collapsed ? 'common.expand' : '') | translate"
              matTooltipPosition="right"
              (click)="collapsed = !collapsed; persistCollapsed()"
            >
              <mat-icon>{{ collapsed ? 'chevron_right' : 'chevron_left' }}</mat-icon>
              @if (!collapsed) {
                <span class="collapse-label">{{ (collapsed ? 'common.expand' : 'common.collapse') | translate }}</span>
              }
            </button>
          </div>
        }
      </mat-sidenav>

      <mat-sidenav-content class="shell__content">
        <mat-toolbar class="topbar">
          @if (isMobile) {
            <button type="button" mat-icon-button (click)="mobileOpen = !mobileOpen">
              <mat-icon>menu</mat-icon>
            </button>
          }
          <span class="topbar-title">{{ 'app.tagline' | translate }}</span>
          <span class="topbar-spacer"></span>

          <button
            type="button"
            mat-icon-button
            class="theme-toggle"
            [matTooltip]="(themeService.current === 'dark' ? 'common.lightMode' : 'common.darkMode') | translate"
            (click)="themeService.toggle()"
          >
            <mat-icon>{{ themeService.current === 'dark' ? 'light_mode' : 'dark_mode' }}</mat-icon>
          </button>

          <button type="button" mat-button class="lang-trigger" [matMenuTriggerFor]="langMenu">
            <mat-icon>translate</mat-icon>
            <span class="lang-trigger__label">{{ localeMeta[localeService.current].label }}</span>
          </button>
          <mat-menu #langMenu="matMenu">
            @for (locale of supportedLocales; track locale) {
              <button type="button" mat-menu-item (click)="localeService.setLocale(locale)">
                {{ localeMeta[locale].label }}
              </button>
            }
          </mat-menu>

          <button type="button" class="user-chip" [matMenuTriggerFor]="profileMenu">
            <span class="avatar">{{ (user$ | async)?.displayName | slice: 0 : 1 }}</span>
            <span class="user-name">{{ (user$ | async)?.displayName || (user$ | async)?.username }}</span>
            <mat-icon class="user-caret">expand_more</mat-icon>
          </button>
          <mat-menu #profileMenu="matMenu" xPosition="before">
            <div class="profile-summary" (click)="$event.stopPropagation()">
              <span class="avatar avatar--lg">{{ (user$ | async)?.displayName | slice: 0 : 1 }}</span>
              <div>
                <div class="profile-name">{{ (user$ | async)?.displayName || (user$ | async)?.username }}</div>
                @if ((user$ | async)?.roles?.length) {
                  <div class="profile-role">{{ (user$ | async)?.roles?.[0] }}</div>
                }
              </div>
            </div>
            <mat-divider />
            <button type="button" mat-menu-item (click)="onLogout()">
              <mat-icon>logout</mat-icon>
              <span>{{ 'common.logout' | translate }}</span>
            </button>
          </mat-menu>
        </mat-toolbar>

        <main class="content">
          <router-outlet />
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: `
:host { display: block; height: 100%; }

    .shell { height: 100vh; }

    .shell__sidenav {
      width: 240px;
      background: var(--color-sidebar);
      color: var(--color-sidebar-text);
      border-right: none;
      transition: width .2s ease;
      overflow-x: hidden;
    }
    .shell__sidenav--collapsed { width: 72px; }

    /* Force Angular Material's inner container to act as a flex column */
    .shell__sidenav ::ng-deep .mat-drawer-inner-container {
      display: flex;
      flex-direction: column;
      height: 100%;
      overflow-y: hidden;
    }

    .brand {
      display: flex; align-items: center; gap: 10px;
      padding: 18px;
      min-height: 64px;
      font-weight: 700; font-size: 1.05rem;
      color: var(--color-sidebar-text);
      border-bottom: 1px solid rgba(255,255,255,.08);
      white-space: nowrap;
      flex-shrink: 0;
    }
    .brand-logo { font-size: 1.4rem; flex-shrink: 0; }

    /* Nav list takes remaining height and scrolls if items overflow */
    .nav { padding: 10px 8px; flex: 1; overflow-y: auto; }
    .nav-link {
      --mdc-list-list-item-label-text-color: var(--color-sidebar-text);
      --mdc-list-list-item-leading-icon-color: var(--color-sidebar-text);
      --mdc-list-list-item-hover-state-layer-color: rgba(255,255,255,.1);
      border-radius: 8px;
      margin-bottom: 4px;
    }
    .nav-link--active {
      --mdc-list-list-item-label-text-color: #fff;
      --mdc-list-list-item-leading-icon-color: #fff;
      background: var(--color-primary);
    }

    /* Pinned strictly to the bottom of the flex container */
    .sidenav-footer {
      padding: 12px 14px;
      border-top: 1px solid rgba(255, 255, 255, 0.08);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      background: var(--color-sidebar);
    }
    .shell__sidenav--collapsed .sidenav-footer {
      padding: 12px 8px;
    }

    .collapse-toggle {
      background: rgba(255, 255, 255, 0.08);
      color: var(--color-sidebar-text);
      border-radius: 8px;
      width: 100%;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      border: none;
      cursor: pointer;
      font: inherit;
      font-size: 0.85rem;
      font-weight: 500;
      transition: background 0.15s ease, color 0.15s ease;
    }
    .shell__sidenav--collapsed .collapse-toggle {
      width: 40px;
      padding: 0;
    }
    .collapse-toggle:hover {
      background: rgba(255, 255, 255, 0.15);
      color: #fff;
    }
    .collapse-label {
      white-space: nowrap;
    }

    .shell__content { display: flex; flex-direction: column; }
    
    .topbar {
      position: sticky; top: 0; z-index: 10;
      display: flex; align-items: center; gap: 8px;
      background: var(--color-surface);
      color: var(--color-text);
      border-bottom: 1px solid var(--color-border);
    }
    .topbar-title { font-weight: 600; color: var(--color-muted); font-size: .95rem; }
    .topbar-spacer { flex: 1; }

    .theme-toggle { color: var(--color-muted); }

    .lang-trigger {
      display: inline-flex; align-items: center; gap: 6px;
      color: var(--color-muted);
    }
    .lang-trigger__label { font-weight: 500; }

    .user-chip {
      display: flex; align-items: center; gap: 8px;
      background: transparent; border: none; cursor: pointer;
      padding: 6px 10px 6px 6px;
      border-radius: 100px;
      color: var(--color-text);
      font: inherit;
      transition: background .15s ease;
    }
    .user-chip:hover { background: var(--color-surface-container); }
    .user-caret { font-size: 18px; width: 18px; height: 18px; color: var(--color-muted); }

    .avatar {
      width: 32px; height: 32px;
      display: inline-flex; align-items: center; justify-content: center;
      border-radius: 50%;
      background: var(--color-primary);
      color: #fff;
      font-weight: 700; font-size: .85rem;
      flex-shrink: 0;
    }
    .avatar--lg { width: 40px; height: 40px; font-size: 1rem; }
    .user-name { font-weight: 500; font-size: .9rem; }

    .profile-summary {
      display: flex; align-items: center; gap: 10px;
      padding: 10px 16px 12px;
      cursor: default;
    }
    .profile-name { font-weight: 600; font-size: .9rem; color: var(--color-text); }
    .profile-role { font-size: .75rem; color: var(--color-muted); text-transform: capitalize; }

    .content { padding: 24px; flex: 1; overflow: auto; }

    @media (max-width: 768px) {
      .user-name { display: none; }
      .lang-trigger__label { display: none; }
      .content { padding: 16px; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})

export class MainLayoutComponent implements OnDestroy {
  private readonly destroyed$ = new Subject<void>();

  readonly navItems = NAV_ITEMS;
  readonly supportedLocales = SUPPORTED_LOCALES;
  readonly localeMeta = LOCALE_META;

  user$: Observable<AuthUser | null>;

  isMobile = false;
  mobileOpen = false;
  collapsed = localStorage.getItem(COLLAPSE_STORAGE_KEY) === 'true';

  constructor(
    readonly localeService: LocaleService,
    readonly themeService: ThemeService,
    private auth: AuthService,
    private breakpointObserver: BreakpointObserver
  ) {
    this.user$ = this.auth.user$;
    this.breakpointObserver
      .observe(Breakpoints.Handset)
      .pipe(takeUntil(this.destroyed$))
      .subscribe((state) => {
        this.isMobile = state.matches;
        if (this.isMobile) {
          this.mobileOpen = false;
        }
      });
  }

  get visibleNavItems(): NavItem[] {
    return this.navItems.filter((item) => this.auth.hasAnyPermission(item.permissions));
  }

  onNavClick(): void {
    if (this.isMobile) {
      this.mobileOpen = false;
    }
  }

  persistCollapsed(): void {
    localStorage.setItem(COLLAPSE_STORAGE_KEY, String(this.collapsed));
  }

  onLogout(): void {
    this.auth.logout().subscribe(() => {
      window.location.href = '/login';
    });
  }

  ngOnDestroy(): void {
    this.destroyed$.next();
    this.destroyed$.complete();
  }
}
