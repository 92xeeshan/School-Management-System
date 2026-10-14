import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { PageHeaderComponent } from '../../layout/page-header/page-header.component';
import { InventoryTabsComponent } from './inventory-tabs.component';

@Component({
  selector: 'app-inventory-shell',
  imports: [RouterOutlet, TranslateModule, PageHeaderComponent, InventoryTabsComponent],
  template: `
    <div class="page">
      <app-page-header [title]="'inventory.title' | translate" [subtitle]="'inventory.subtitle' | translate" />
      <app-inventory-tabs />
      <router-outlet />
    </div>
  `,
  styles: ``,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InventoryShellComponent {}
