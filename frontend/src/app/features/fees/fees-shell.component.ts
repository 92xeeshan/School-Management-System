import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { PageHeaderComponent } from '../../layout/page-header/page-header.component';
import { FeesTabsComponent } from './fees-tabs.component';

@Component({
  selector: 'app-fees-shell',
  imports: [RouterOutlet, TranslateModule, PageHeaderComponent, FeesTabsComponent],
  template: `
    <div class="page">
      <app-page-header [title]="'fees.title' | translate" [subtitle]="'fees.subtitle' | translate" />
      <app-fees-tabs />
      <router-outlet />
    </div>
  `,
  styles: ``,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeesShellComponent {}
