import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';

interface FeesTab {
  path: string;
  labelKey: string;
  permissions: string[];
}

@Component({
  selector: 'app-fees-tabs',
  imports: [RouterLink, RouterLinkActive, TranslateModule],
  template: `
    <nav class="tabs" aria-label="Fees">
      @for (tab of visibleTabs; track tab.path) {
        <a class="tab" [routerLink]="['/fees', tab.path]" routerLinkActive="active">
          {{ tab.labelKey | translate }}
        </a>
      }
    </nav>
  `,
  styles: `
    .tabs { display: flex; gap: 4px; overflow-x: auto; margin-bottom: 20px;
            border-bottom: 1px solid var(--color-border); }
    .tab { flex: 0 0 auto; padding: 10px 14px; text-decoration: none; color: var(--color-muted);
           font-weight: 600; font-size: .9rem; border-bottom: 2px solid transparent;
           margin-bottom: -1px; white-space: nowrap; border-radius: 8px 8px 0 0; }
    .tab:hover { color: var(--color-primary); background: var(--color-primary-soft); }
    .tab.active { color: var(--color-primary); border-bottom-color: var(--color-primary); background: transparent; }
    @media (max-width: 768px) { .tab { padding: 10px 12px; font-size: .82rem; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeesTabsComponent {
  readonly tabs: FeesTab[] = [
    { path: 'heads', labelKey: 'fees.tabs.heads', permissions: ['FEE_STRUCTURE_MANAGE'] },
    { path: 'structures', labelKey: 'fees.tabs.structures', permissions: ['FEE_STRUCTURE_MANAGE'] },
    { path: 'discounts', labelKey: 'fees.tabs.discounts', permissions: ['FEE_STRUCTURE_MANAGE'] },
    { path: 'adhoc', labelKey: 'fees.tabs.adhoc', permissions: ['FEE_STRUCTURE_MANAGE'] },
    { path: 'collect', labelKey: 'fees.tabs.collect', permissions: ['FEE_READ', 'FEE_RECEIPT_VIEW', 'FEE_PAYMENT_RECORD'] },
  ];

  constructor(private auth: AuthService) {}

  get visibleTabs(): FeesTab[] {
    return this.tabs.filter((tab) => this.auth.hasAnyPermission(tab.permissions));
  }
}
