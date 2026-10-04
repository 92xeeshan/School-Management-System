import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { DownloadsTabsComponent } from './downloads-tabs.component';
import { PageHeaderComponent } from '../../layout/page-header/page-header.component';

@Component({
  selector: 'app-downloads-shell',
  imports: [RouterOutlet, TranslateModule, DownloadsTabsComponent, PageHeaderComponent],
  template: `
    <div class="page">
      <app-page-header [title]="'downloads.title' | translate" [subtitle]="'downloads.subtitle' | translate" />
      <app-downloads-tabs />
      <router-outlet />
    </div>
  `,
  styles: ``,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DownloadsShellComponent {}
