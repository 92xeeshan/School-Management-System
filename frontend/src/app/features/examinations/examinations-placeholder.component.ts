import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-examinations-placeholder',
  imports: [TranslateModule],
  template: `
    <div class="tab-page">
      <div class="page-header">
        <div>
          <h2>{{ titleKey | translate }}</h2>
          <p class="muted">{{ subtitleKey | translate }}</p>
        </div>
      </div>
      <div class="card placeholder">
        <p class="placeholder-title">{{ 'examinations.comingSoon' | translate }}</p>
        <p class="muted">{{ bodyKey | translate }}</p>
      </div>
    </div>
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    h2 { font-size: 1.2rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); margin: 0; }
    .placeholder { padding: 28px 24px; }
    .placeholder-title { font-weight: 600; margin: 0 0 8px; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExaminationsPlaceholderComponent {
  @Input({ required: true }) titleKey = '';
  @Input({ required: true }) subtitleKey = '';
  @Input({ required: true }) bodyKey = '';
}
