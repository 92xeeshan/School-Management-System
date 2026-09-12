import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ExaminationsPlaceholderComponent } from './examinations-placeholder.component';

@Component({
  selector: 'app-exam-schedules',
  imports: [ExaminationsPlaceholderComponent],
  template: `
    <app-examinations-placeholder
      titleKey="examinations.schedulesPage.title"
      subtitleKey="examinations.schedulesPage.subtitle"
      bodyKey="examinations.schedulesPage.body" />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamSchedulesComponent {}
