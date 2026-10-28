import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { PageHeaderComponent } from '../../layout/page-header/page-header.component';
import { LeaveTabsComponent } from './leave-tabs.component';

@Component({
  selector: 'app-leave-shell',
  imports: [RouterOutlet, TranslateModule, PageHeaderComponent, LeaveTabsComponent],
  template: `
    <div class="page">
      <app-page-header [title]="'leave.title' | translate" [subtitle]="'leave.subtitle' | translate" />
      <app-leave-tabs />
      <router-outlet />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeaveShellComponent {}
