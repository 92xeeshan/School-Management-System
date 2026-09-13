import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ExaminationsPlaceholderComponent } from './examinations-placeholder.component';

@Component({
  selector: 'app-reevaluation',
  imports: [ExaminationsPlaceholderComponent],
  template: `
    <app-examinations-placeholder
      titleKey="examinations.reevaluationPage.title"
      subtitleKey="examinations.reevaluationPage.subtitle"
      bodyKey="examinations.reevaluationPage.body" />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReevaluationComponent {}
