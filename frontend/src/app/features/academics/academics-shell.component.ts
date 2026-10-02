import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AcademicsTabsComponent } from './academics-tabs.component';
import { PageHeaderComponent } from '../../layout/page-header/page-header.component';

@Component({
  selector: 'app-academics-shell',
  imports: [RouterOutlet, TranslateModule, AcademicsTabsComponent, PageHeaderComponent],
  template: `
    <div class="page">
      <app-page-header [title]="'academics.title' | translate" [subtitle]="'academics.subtitle' | translate" />
      <app-academics-tabs />
      <router-outlet />
    </div>
  `,
  styles: ``,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicsShellComponent {}
