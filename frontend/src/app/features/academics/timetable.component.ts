import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AcademicsPlaceholderComponent } from './academics-placeholder.component';

@Component({
  selector: 'app-academics-timetable',
  imports: [AcademicsPlaceholderComponent],
  template: `
    <app-academics-placeholder
      titleKey="academics.timetablePage.title"
      subtitleKey="academics.timetablePage.subtitle"
      bodyKey="academics.timetablePage.body" />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicsTimetableComponent {}
