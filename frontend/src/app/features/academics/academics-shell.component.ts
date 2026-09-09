import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AcademicsTabsComponent } from './academics-tabs.component';

@Component({
  selector: 'app-academics-shell',
  imports: [RouterOutlet, TranslateModule, AcademicsTabsComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'academics.title' | translate }}</h1>
          <p class="muted">{{ 'academics.subtitle' | translate }}</p>
        </div>
      </div>
      <app-academics-tabs />
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
export class AcademicsShellComponent {}
