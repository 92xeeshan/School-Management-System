import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { StaffTabsComponent } from './staff-tabs.component';

@Component({
  selector: 'app-staff-shell',
  imports: [RouterOutlet, TranslateModule, StaffTabsComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'staff.title' | translate }}</h1>
          <p class="muted">{{ 'staff.subtitle' | translate }}</p>
        </div>
      </div>
      <app-staff-tabs />
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
export class StaffShellComponent {}
