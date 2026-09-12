import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ExaminationsPlaceholderComponent } from './examinations-placeholder.component';

@Component({
  selector: 'app-report-cards',
  imports: [ExaminationsPlaceholderComponent],
  template: `
    <app-examinations-placeholder
      titleKey="examinations.reportCardsPage.title"
      subtitleKey="examinations.reportCardsPage.subtitle"
      bodyKey="examinations.reportCardsPage.body" />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportCardsComponent {}
