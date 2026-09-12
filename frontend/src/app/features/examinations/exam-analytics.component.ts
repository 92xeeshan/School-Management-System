import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ExaminationsPlaceholderComponent } from './examinations-placeholder.component';

@Component({
  selector: 'app-exam-analytics',
  imports: [ExaminationsPlaceholderComponent],
  template: `
    <app-examinations-placeholder
      titleKey="examinations.analyticsPage.title"
      subtitleKey="examinations.analyticsPage.subtitle"
      bodyKey="examinations.analyticsPage.body" />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamAnalyticsComponent {}
