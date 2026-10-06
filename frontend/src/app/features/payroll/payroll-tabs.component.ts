import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';

interface PayrollTab {
  path: string;
  labelKey: string;
  permissions: string[];
}

@Component({
  selector: 'app-payroll-tabs',
  imports: [RouterLink, RouterLinkActive, TranslateModule],
  template: `
    <nav class="tabs" aria-label="Payroll">
      @for (tab of visibleTabs; track tab.path) {
        <a class="tab" [routerLink]="['/payroll', tab.path]" routerLinkActive="active">
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
export class PayrollTabsComponent {
  readonly tabs: PayrollTab[] = [
    { path: 'expenses', labelKey: 'payroll.tabs.expenses', permissions: ['EXPENSE_READ'] },
    { path: 'runs', labelKey: 'payroll.tabs.runs', permissions: ['PAYROLL_READ'] },
    { path: 'my-payslips', labelKey: 'payroll.tabs.myPayslips', permissions: ['PAYSLIP_READ'] },
  ];

  constructor(private auth: AuthService) {}

  get visibleTabs(): PayrollTab[] {
    return this.tabs.filter((tab) => this.auth.hasAnyPermission(tab.permissions));
  }
}
