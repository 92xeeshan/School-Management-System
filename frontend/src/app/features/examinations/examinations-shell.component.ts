import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ExaminationsTabsComponent } from './examinations-tabs.component';
import { PageHeaderComponent } from '../../layout/page-header/page-header.component';

@Component({
  selector: 'app-examinations-shell',
  imports: [RouterOutlet, TranslateModule, ExaminationsTabsComponent, PageHeaderComponent],
  template: `
    <div class="page">
      <app-page-header [title]="'examinations.title' | translate" [subtitle]="'examinations.subtitle' | translate" />
      <app-examinations-tabs />
      <router-outlet />
    </div>
  `,
  styles: ``,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExaminationsShellComponent {}
