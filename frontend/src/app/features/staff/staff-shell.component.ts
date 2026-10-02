import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { StaffTabsComponent } from './staff-tabs.component';
import { PageHeaderComponent } from '../../layout/page-header/page-header.component';

@Component({
  selector: 'app-staff-shell',
  imports: [RouterOutlet, TranslateModule, StaffTabsComponent, PageHeaderComponent],
  template: `
    <div class="page">
      <app-page-header [title]="'staff.title' | translate" [subtitle]="'staff.subtitle' | translate" />
      <app-staff-tabs />
      <router-outlet />
    </div>
  `,
  styles: ``,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StaffShellComponent {}
