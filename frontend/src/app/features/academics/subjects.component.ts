import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AcademicsPlaceholderComponent } from './academics-placeholder.component';

@Component({
  selector: 'app-academics-subjects',
  imports: [AcademicsPlaceholderComponent],
  template: `
    <app-academics-placeholder
      titleKey="academics.subjectsPage.title"
      subtitleKey="academics.subjectsPage.subtitle"
      bodyKey="academics.subjectsPage.body" />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicsSubjectsComponent {}
