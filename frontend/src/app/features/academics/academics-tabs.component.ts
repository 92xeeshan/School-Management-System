import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

interface AcademicsTab {
  path: string;
  labelKey: string;
}

@Component({
  selector: 'app-academics-tabs',
  imports: [RouterLink, RouterLinkActive, TranslateModule],
  template: `
    <nav class="tabs" aria-label="Academics">
      @for (tab of tabs; track tab.path) {
        <a class="tab"
           [routerLink]="['/academics', tab.path]"
           routerLinkActive="active">
          {{ tab.labelKey | translate }}
        </a>
      }
    </nav>
  `,
  styles: `
    .tabs {
      display: flex;
      gap: 4px;
      overflow-x: auto;
      margin-bottom: 20px;
      border-bottom: 1px solid var(--color-border);
      padding-bottom: 0;
    }
    .tab {
      flex: 0 0 auto;
      padding: 10px 14px;
      text-decoration: none;
      color: var(--color-muted);
      font-weight: 600;
      font-size: .9rem;
      border-bottom: 2px solid transparent;
      margin-bottom: -1px;
      white-space: nowrap;
      border-radius: 8px 8px 0 0;
    }
    .tab:hover { color: var(--color-primary); background: var(--color-primary-soft); }
    .tab.active {
      color: var(--color-primary);
      border-bottom-color: var(--color-primary);
      background: transparent;
    }
    @media (max-width: 768px) {
      .tab { padding: 10px 12px; font-size: .82rem; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicsTabsComponent {
  readonly tabs: AcademicsTab[] = [
    { path: 'classes', labelKey: 'academics.tabs.classes' },
    { path: 'subjects', labelKey: 'academics.tabs.subjects' },
    { path: 'timetable', labelKey: 'academics.tabs.timetable' },
    { path: 'syllabus', labelKey: 'academics.tabs.syllabus' },
    { path: 'examinations', labelKey: 'academics.tabs.examinations' },
    { path: 'terms', labelKey: 'academics.tabs.terms' },
  ];
}
