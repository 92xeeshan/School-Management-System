import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AcademicsPlaceholderComponent } from './academics-placeholder.component';

@Component({
  selector: 'app-academics-terms',
  imports: [AcademicsPlaceholderComponent],
  template: `
    <app-academics-placeholder
      titleKey="academics.termsPage.title"
      subtitleKey="academics.termsPage.subtitle"
      bodyKey="academics.termsPage.body" />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicsTermsComponent {}
