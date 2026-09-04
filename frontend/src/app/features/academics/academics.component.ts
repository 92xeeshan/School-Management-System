import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ApiResponse } from '../../core/models/api.model';

interface SchoolClass {
  id: string;
  name: string;
  code: string;
  sections: SchoolSection[];
  subjects: string[];
}

interface SchoolSection {
  id: string;
  name: string;
  capacity: number;
}

interface BackendClass {
  id: string;
  name: string;
  code: string;
}

interface BackendSection {
  id: string;
  classId: string;
  className: string;
  name: string;
  capacity: number;
}

interface BackendSubject {
  id: string;
  name: string;
  code: string;
}

@Component({
  selector: 'app-academics',
  imports: [TranslateModule, ReactiveFormsModule],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>{{ 'academics.title' | translate }}</h1>
          <p class="muted">{{ 'academics.subtitle' | translate }}</p>
        </div>
        <div class="header-actions">
          <button class="btn btn-primary" (click)="openAddModal()">{{ 'academics.addClass' | translate }}</button>
        </div>
      </div>

      <div class="card">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'academics.className' | translate }}</th>
                <th>{{ 'academics.sections' | translate }}</th>
                <th>{{ 'academics.subjects' | translate }}</th>
              </tr>
            </thead>
            <tbody>
              @for (klass of classes; track klass.id) {
                <tr>
                  <td class="strong">{{ klass.name }} <span class="muted code">{{ klass.code }}</span></td>
                  <td>
                    <div class="sections">
                      @for (section of klass.sections; track section.id) {
                        <div class="section-chip">
                          {{ section.name }} · {{ section.capacity }}
                        </div>
                      } @empty {
                        <span class="muted">—</span>
                      }
                    </div>
                  </td>
                  <td>
                    <div class="subject-tags">
                      @for (subject of klass.subjects; track subject) {
                        <span class="tag">{{ subject }}</span>
                      }
                    </div>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="3" class="center">{{ 'common.noData' | translate }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>

    @if (showModal) {
      <div class="modal-backdrop" (click)="closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ 'academics.addClass' | translate }}</h2>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="form-grid">
              <div class="field">
                <label>{{ 'academics.className' | translate }} *</label>
                <input type="text" formControlName="name" placeholder="IX" />
              </div>
              <div class="field">
                <label>Code</label>
                <input type="text" formControlName="code" placeholder="IX" />
              </div>
            </div>
            <div class="form-actions">
              <button class="btn" type="button" (click)="closeModal()">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving">
                {{ saving ? ('common.loading' | translate) : ('common.save' | translate) }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    h1 { font-size: 1.5rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); }
    .code { font-size: .8rem; font-weight: 400; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 14px 16px; border-bottom: 1px solid var(--color-border); font-size: .92rem; vertical-align: top; }
    th { color: var(--color-muted); font-weight: 600; font-size: .8rem; text-transform: uppercase; letter-spacing: .03em; background: var(--color-bg); }
    .strong { font-weight: 600; }
    .center { text-align: center; color: var(--color-muted); padding: 28px; }
    .sections { display: flex; flex-wrap: wrap; gap: 8px; }
    .section-chip {
      padding: 4px 10px; border-radius: 20px; font-size: .82rem;
      background: var(--color-primary-soft); color: var(--color-primary); font-weight: 600;
    }
    .subject-tags { display: flex; flex-wrap: wrap; gap: 6px; }
    .tag { padding: 3px 10px; border-radius: 6px; background: var(--color-bg); border: 1px solid var(--color-border); font-size: .82rem; }

    .modal-backdrop {
      position: fixed; inset: 0; z-index: 100;
      background: rgba(15, 23, 42, .5);
      display: flex; align-items: flex-start; justify-content: center;
      padding: 40px 16px; overflow-y: auto;
    }
    .modal {
      background: #fff; border-radius: var(--radius);
      padding: 24px; width: 440px; max-width: 100%;
      box-shadow: 0 20px 50px rgba(0,0,0,.25);
    }
    .modal h2 { margin: 0 0 18px; font-size: 1.2rem; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    input {
      padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit;
    }
    .form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicsComponent implements OnInit {
  classes: SchoolClass[] = [];
  showModal = false;
  saving = false;

  readonly form = new FormGroup({
    name: new FormControl('', Validators.required),
    code: new FormControl(''),
  });

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadReal();
  }

  openAddModal(): void {
    this.showModal = true;
    this.form.reset();
  }

  closeModal(): void {
    if (!this.saving) {
      this.showModal = false;
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }
    this.saving = true;
    const body = {
      name: this.form.value.name,
      code: this.form.value.code || this.form.value.name,
      sortOrder: 0,
    };
    this.http.post<ApiResponse<unknown>>('/api/classes', body).subscribe({
      next: () => {
        this.saving = false;
        this.showModal = false;
        this.cdr.markForCheck();
        this.loadReal();
      },
      error: () => {
        this.saving = false;
        this.showModal = false;
        this.cdr.markForCheck();
        alert('Class added (demo mode — backend not reachable)');
      },
    });
  }

  private loadReal(): void {
    this.http.get<ApiResponse<BackendClass[]>>('/api/classes').subscribe({
      next: (res) => {
        const classes = res.data.map((c) => ({ id: c.id, name: c.name, code: c.code, sections: [], subjects: [] }));
        this.loadSectionsAndSubjects(classes);
      },
      error: () => this.loadDemo(),
    });
  }

  private loadSectionsAndSubjects(classes: SchoolClass[]): void {
    this.http.get<ApiResponse<BackendSubject[]>>('/api/subjects').subscribe({
      next: (subjectsRes) => {
        const subjectNames = subjectsRes.data.map((s) => s.name);
        for (const klass of classes) {
          klass.subjects = subjectNames;
        }
      },
      error: () => undefined,
    });

    for (const klass of classes) {
      this.http.get<ApiResponse<BackendSection[]>>('/api/sections', { params: { classId: klass.id } }).subscribe({
        next: (res) => {
          klass.sections = res.data.map((s) => ({ id: s.id, name: s.name, capacity: s.capacity }));
          this.cdr.markForCheck();
        },
        error: () => undefined,
      });
    }
    this.classes = [...classes];
    this.cdr.markForCheck();
  }

  private loadDemo(): void {
    this.classes = [
      { id: 'c1', name: 'VI', code: 'VI', sections: [
        { id: 's1', name: 'A', capacity: 40 },
        { id: 's2', name: 'B', capacity: 40 },
      ], subjects: ['English', 'Hindi', 'Mathematics', 'Science', 'Social Studies'] },
      { id: 'c2', name: 'VII', code: 'VII', sections: [
        { id: 's3', name: 'A', capacity: 40 },
      ], subjects: ['English', 'Hindi', 'Mathematics', 'Science', 'Social Studies', 'Urdu'] },
      { id: 'c3', name: 'VIII', code: 'VIII', sections: [
        { id: 's4', name: 'A', capacity: 40 },
        { id: 's5', name: 'B', capacity: 40 },
      ], subjects: ['English', 'Mathematics', 'Science', 'Social Studies', 'Computer Science'] },
    ];
    this.cdr.markForCheck();
  }
}
