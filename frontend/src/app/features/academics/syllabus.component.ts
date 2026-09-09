import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AcademicsPlaceholderComponent } from './academics-placeholder.component';

@Component({
  selector: 'app-academics-syllabus',
  imports: [AcademicsPlaceholderComponent],
  template: `
    <app-academics-placeholder
      titleKey="academics.syllabusPage.title"
      subtitleKey="academics.syllabusPage.subtitle"
      bodyKey="academics.syllabusPage.body" />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicsSyllabusComponent {}
