import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

export interface BreadcrumbItem {
  labelKey: string;
  route?: string[];
}

/**
 * Consistent page-header pattern used across feature modules:
 * an optional breadcrumb trail, a title, an optional subtitle,
 * and a right-aligned slot for page-level actions (buttons, filters, etc.)
 * passed in via content projection.
 *
 * Usage:
 * <app-page-header titleKey="students.title" subtitleKey="students.subtitle"
 *   [breadcrumbs]="[{ labelKey: 'nav.dashboard', route: ['/dashboard'] }, { labelKey: 'nav.students' }]">
 *   <button mat-flat-button color="primary">Add student</button>
 * </app-page-header>
 */
@Component({
  selector: 'app-page-header',
  imports: [RouterLink, TranslateModule],
  template: `
    @if (breadcrumbs?.length) {
      <nav class="page-header__breadcrumbs" aria-label="Breadcrumb">
        @for (crumb of breadcrumbs; track crumb.labelKey; let last = $last) {
          @if (crumb.route && !last) {
            <a [routerLink]="crumb.route">{{ crumb.labelKey | translate }}</a>
            <span class="page-header__crumb-sep">/</span>
          } @else {
            <span class="page-header__crumb-current">{{ crumb.labelKey | translate }}</span>
          }
        }
      </nav>
    }
    <div class="page-header__row">
      <div class="page-header__text">
        <h1 class="page-header__title">{{ titleKey | translate }}</h1>
        @if (subtitleKey) {
          <p class="page-header__subtitle">{{ subtitleKey | translate }}</p>
        }
      </div>
      <div class="page-header__actions">
        <ng-content />
      </div>
    </div>
  `,
  styles: `
    :host {
      display: block;
      margin-bottom: 20px;
    }
    .page-header__breadcrumbs {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 0.8125rem;
      color: var(--color-muted);
      margin-bottom: 6px;
    }
    .page-header__breadcrumbs a {
      color: var(--color-muted);
      text-decoration: none;
    }
    .page-header__breadcrumbs a:hover {
      color: var(--color-primary);
    }
    .page-header__crumb-sep {
      opacity: 0.6;
    }
    .page-header__crumb-current {
      color: var(--color-text);
      font-weight: 500;
    }
    .page-header__row {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
      flex-wrap: wrap;
    }
    .page-header__title {
      font-size: 1.375rem;
      font-weight: 700;
      color: var(--color-text);
      margin: 0;
      line-height: 1.3;
    }
    .page-header__subtitle {
      margin: 4px 0 0;
      color: var(--color-muted);
      font-size: 0.875rem;
    }
    .page-header__actions {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }

    @media (max-width: 640px) {
      .page-header__row { flex-direction: column; align-items: stretch; }
      .page-header__actions { justify-content: flex-start; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageHeaderComponent {
  @Input({ required: true }) titleKey!: string;
  @Input() subtitleKey?: string;
  @Input() breadcrumbs?: BreadcrumbItem[];
}
