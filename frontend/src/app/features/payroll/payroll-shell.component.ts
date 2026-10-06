import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { PageHeaderComponent } from '../../layout/page-header/page-header.component';
import { PayrollTabsComponent } from './payroll-tabs.component';

@Component({
  selector: 'app-payroll-shell',
  imports: [RouterOutlet, TranslateModule, PageHeaderComponent, PayrollTabsComponent],
  template: `
    <div class="page">
      <app-page-header [title]="'payroll.title' | translate" [subtitle]="'payroll.subtitle' | translate" />
      <app-payroll-tabs />
      <router-outlet />
    </div>
  `,
  styles: ``,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PayrollShellComponent {}
