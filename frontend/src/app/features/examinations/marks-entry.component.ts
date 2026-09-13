import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ExaminationsPlaceholderComponent } from './examinations-placeholder.component';

@Component({
  selector: 'app-marks-entry',
  imports: [ExaminationsPlaceholderComponent],
  template: `
    <app-examinations-placeholder
      titleKey="examinations.marksPage.title"
      subtitleKey="examinations.marksPage.subtitle"
      bodyKey="examinations.marksPage.body" />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MarksEntryComponent {}
