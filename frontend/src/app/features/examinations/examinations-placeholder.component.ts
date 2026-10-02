import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';

@Component({
  selector: 'app-examinations-placeholder',
  imports: [TranslateModule, EmptyStateComponent],
  template: `
    <div class="tab-page">
      <div class="page-header">
        <div>
          <h2>{{ titleKey | translate }}</h2>
          <p class="muted">{{ subtitleKey | translate }}</p>
        </div>
      </div>
      <div class="card">
        <app-empty-state icon="construction" [title]="'examinations.comingSoon' | translate" [hint]="bodyKey | translate" />
      </div>
    </div>
  `,
  styles: `
    h2 { font-size: 1.2rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); margin: 0; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExaminationsPlaceholderComponent {
  @Input({ required: true }) titleKey = '';
  @Input({ required: true }) subtitleKey = '';
  @Input({ required: true }) bodyKey = '';
}
