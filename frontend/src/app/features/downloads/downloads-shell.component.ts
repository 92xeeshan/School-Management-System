import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { DownloadsTabsComponent } from './downloads-tabs.component';

@Component({
  selector: 'app-downloads-shell',
  imports: [RouterOutlet, TranslateModule, DownloadsTabsComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'downloads.title' | translate }}</h1>
          <p class="muted">{{ 'downloads.subtitle' | translate }}</p>
        </div>
      </div>
      <app-downloads-tabs />
      <router-outlet />
    </div>
  `,
  styles: `
    .page-header { margin-bottom: 16px; }
    h1 { font-size: 1.5rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); margin: 0; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DownloadsShellComponent {}
