import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AcademicsPlaceholderComponent } from './academics-placeholder.component';

@Component({
  selector: 'app-academics-examinations',
  imports: [AcademicsPlaceholderComponent],
  template: `
    <app-academics-placeholder
      titleKey="academics.examinationsPage.title"
      subtitleKey="academics.examinationsPage.subtitle"
      bodyKey="academics.examinationsPage.body" />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicsExaminationsComponent {}
