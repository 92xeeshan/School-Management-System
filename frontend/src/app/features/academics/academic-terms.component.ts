import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ApiError, ApiResponse } from '../../core/models/api.model';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';

interface AcademicYear {
  id: string;
  name: string;
  startDate: string;
  endDate: string;
  current: boolean;
}

interface AcademicTerm {
  id: string;
  academicYearId: string;
  name: string;
  startDate: string;
  endDate: string;
  status: 'PLANNED' | 'ACTIVE' | 'COMPLETED';
  sortOrder: number;
}

interface SessionRow {
  year: AcademicYear;
  terms: AcademicTerm[];
}

interface SchoolClass {
  id: string;
  name: string;
  sections: SchoolSection[];
}

interface SchoolSection {
  id: string;
  name: string;
  classId?: string;
}

interface PromotionStudent {
  studentId: string;
  admissionNo: string;
  displayName: string;
  rollNumber: number | null;
  sourceClassName: string;
  sourceSectionName: string | null;
  outcome: 'PROMOTED' | 'SKIPPED';
  reason: string | null;
}

interface PromotionPreview {
  sourceYearId: string;
  sourceYearName: string;
  destinationYearId: string;
  destinationYearName: string;
  sourceClassName: string;
  sourceSectionName: string | null;
  destinationClassName: string;
  destinationSectionName: string;
  eligibleCount: number;
  skippedCount: number;
  destinationOccupied: number;
  destinationCapacity: number;
  students: PromotionStudent[];
}

interface PromotionBatch {
  id: string;
  sourceYearName: string;
  destinationYearName: string;
  sourceClassName: string;
  sourceSectionName: string | null;
  destinationClassName: string;
  destinationSectionName: string;
  promotedCount: number;
  skippedCount: number;
  notes: string | null;
  createdAt: string;
  students?: PromotionStudent[];
}

@Component({
  selector: 'app-academics-terms',
  imports: [TranslateModule, ReactiveFormsModule, EmptyStateComponent, DatePipe],
  template: `
    <div class="tab-page">
      <div class="page-header">
        <div>
          <h2>{{ 'academics.termsPage.title' | translate }}</h2>
          <p class="muted">{{ 'academics.termsPage.subtitle' | translate }}</p>
        </div>
        @if (canManage) {
          <div class="header-actions">
            <button class="btn" type="button" (click)="openPromote()">{{ 'academics.termsPage.promote' | translate }}</button>
            <button class="btn btn-primary" type="button" (click)="openYearModal()">{{ 'academics.termsPage.addYear' | translate }}</button>
          </div>
        }
      </div>

      @if (successMessage) {
        <p class="banner success">{{ successMessage | translate }}</p>
      }
      @if (pageError) {
        <p class="banner error">{{ pageError }}</p>
      }

      @if (currentYear) {
        <div class="current-card">
          <div>
            <p class="eyebrow">{{ 'academics.termsPage.currentSession' | translate }}</p>
            <h3>{{ currentYear.name }}</h3>
            <p class="muted">{{ currentYear.startDate | date:'mediumDate' }} – {{ currentYear.endDate | date:'mediumDate' }}</p>
          </div>
          <span class="badge current">{{ 'academics.termsPage.currentBadge' | translate }}</span>
        </div>
      } @else if (!loading) {
        <p class="banner">{{ 'academics.termsPage.noCurrent' | translate }}</p>
      }

      @if (loading) {
        <p class="muted">{{ 'common.loading' | translate }}</p>
      } @else if (sessions.length === 0) {
        <div class="card">
          <app-empty-state icon="event" [title]="'academics.termsPage.noYears' | translate" [hint]="'common.emptyHint' | translate">
            @if (canManage) {
              <button class="btn btn-primary" type="button" (click)="openYearModal()">{{ 'academics.termsPage.addYear' | translate }}</button>
            }
          </app-empty-state>
        </div>
      } @else {
        @for (session of sessions; track session.year.id) {
          <div class="card year-card">
            <div class="year-head">
              <div>
                <h3>
                  {{ session.year.name }}
                  @if (session.year.current) {
                    <span class="badge current">{{ 'academics.termsPage.currentBadge' | translate }}</span>
                  }
                </h3>
                <p class="muted">{{ session.year.startDate | date:'mediumDate' }} – {{ session.year.endDate | date:'mediumDate' }}</p>
              </div>
              @if (canManage) {
                <div class="row-actions">
                  @if (!session.year.current) {
                    <button class="btn btn-sm" type="button" (click)="askActivate(session.year)">{{ 'academics.termsPage.activate' | translate }}</button>
                  } @else {
                    <button class="btn btn-sm" type="button" (click)="deactivate(session.year)">{{ 'academics.termsPage.deactivate' | translate }}</button>
                  }
                  <button class="btn btn-sm" type="button" (click)="openYearModal(session.year)">{{ 'common.edit' | translate }}</button>
                  <button class="btn btn-sm" type="button" (click)="openTermModal(session.year)">{{ 'academics.termsPage.addTerm' | translate }}</button>
                </div>
              }
            </div>
            @if (session.terms.length === 0) {
              <p class="muted empty-terms">{{ 'academics.termsPage.noTerms' | translate }}</p>
            } @else {
              <div class="data-table-wrap">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th>{{ 'academics.termsPage.termName' | translate }}</th>
                      <th>{{ 'academics.termsPage.startDate' | translate }}</th>
                      <th>{{ 'academics.termsPage.endDate' | translate }}</th>
                      <th>{{ 'academics.termsPage.status' | translate }}</th>
                      @if (canManage) {
                        <th>{{ 'common.actions' | translate }}</th>
                      }
                    </tr>
                  </thead>
                  <tbody>
                    @for (term of session.terms; track term.id) {
                      <tr>
                        <td class="strong">{{ term.name }}</td>
                        <td>{{ term.startDate | date:'mediumDate' }}</td>
                        <td>{{ term.endDate | date:'mediumDate' }}</td>
                        <td><span class="badge" [class]="'st-' + term.status.toLowerCase()">{{ statusKey(term.status) | translate }}</span></td>
                        @if (canManage) {
                          <td>
                            <div class="row-actions">
                              <button class="btn btn-sm" type="button" (click)="openTermModal(session.year, term)">{{ 'common.edit' | translate }}</button>
                              <button class="btn btn-sm btn-danger" type="button" (click)="askDeleteTerm(term)">{{ 'common.delete' | translate }}</button>
                            </div>
                          </td>
                        }
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </div>
        }
      }

      <div class="card">
        <div class="year-head">
          <h3>{{ 'academics.termsPage.history' | translate }}</h3>
        </div>
        @if (history.length === 0) {
          <p class="muted empty-terms">{{ 'academics.termsPage.noHistory' | translate }}</p>
        } @else {
          <div class="data-table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>{{ 'academics.termsPage.sourceYear' | translate }}</th>
                  <th>{{ 'academics.termsPage.destYear' | translate }}</th>
                  <th>{{ 'academics.termsPage.sourceClass' | translate }}</th>
                  <th>{{ 'academics.termsPage.destClass' | translate }}</th>
                  <th>{{ 'academics.termsPage.promotedCount' | translate }}</th>
                  <th>{{ 'academics.termsPage.skippedCount' | translate }}</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (batch of history; track batch.id) {
                  <tr>
                    <td>{{ batch.sourceYearName }}</td>
                    <td>{{ batch.destinationYearName }}</td>
                    <td>{{ batch.sourceClassName }}{{ batch.sourceSectionName ? ' / ' + batch.sourceSectionName : '' }}</td>
                    <td>{{ batch.destinationClassName }} / {{ batch.destinationSectionName }}</td>
                    <td>{{ batch.promotedCount }}</td>
                    <td>{{ batch.skippedCount }}</td>
                    <td>
                      <button class="btn btn-sm" type="button" (click)="openBatch(batch.id)">{{ 'academics.termsPage.viewBatch' | translate }}</button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    </div>

    @if (yearModalOpen) {
      <div class="modal-backdrop" (click)="closeModals()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editingYear ? ('academics.termsPage.editYear' | translate) : ('academics.termsPage.addYear' | translate) }}</h2>
          @if (formError) { <p class="form-error">{{ formError }}</p> }
          <form [formGroup]="yearForm" (ngSubmit)="saveYear()">
            <div class="form-grid">
              <div class="field span-2">
                <label>{{ 'academics.termsPage.yearName' | translate }} *</label>
                <input type="text" formControlName="name" placeholder="2026-27" />
              </div>
              <div class="field">
                <label>{{ 'academics.termsPage.startDate' | translate }} *</label>
                <input type="date" formControlName="startDate" />
              </div>
              <div class="field">
                <label>{{ 'academics.termsPage.endDate' | translate }} *</label>
                <input type="date" formControlName="endDate" />
              </div>
              <label class="check span-2">
                <input type="checkbox" formControlName="current" />
                {{ 'academics.termsPage.currentBadge' | translate }}
              </label>
            </div>
            <div class="form-actions">
              <button class="btn" type="button" (click)="closeModals()">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="yearForm.invalid || saving">
                {{ saving ? ('common.loading' | translate) : ('common.save' | translate) }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    @if (termModalOpen) {
      <div class="modal-backdrop" (click)="closeModals()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editingTerm ? ('academics.termsPage.editTerm' | translate) : ('academics.termsPage.addTerm' | translate) }}</h2>
          @if (formError) { <p class="form-error">{{ formError }}</p> }
          <form [formGroup]="termForm" (ngSubmit)="saveTerm()">
            <div class="form-grid">
              <div class="field span-2">
                <label>{{ 'academics.termsPage.termName' | translate }} *</label>
                <input type="text" formControlName="name" placeholder="Term 1" />
              </div>
              <div class="field">
                <label>{{ 'academics.termsPage.startDate' | translate }} *</label>
                <input type="date" formControlName="startDate" />
              </div>
              <div class="field">
                <label>{{ 'academics.termsPage.endDate' | translate }} *</label>
                <input type="date" formControlName="endDate" />
              </div>
              <div class="field">
                <label>{{ 'academics.termsPage.status' | translate }}</label>
                <select formControlName="status">
                  <option value="PLANNED">{{ 'academics.termsPage.statusPlanned' | translate }}</option>
                  <option value="ACTIVE">{{ 'academics.termsPage.statusActive' | translate }}</option>
                  <option value="COMPLETED">{{ 'academics.termsPage.statusCompleted' | translate }}</option>
                </select>
              </div>
              <div class="field">
                <label>{{ 'academics.termsPage.sortOrder' | translate }}</label>
                <input type="number" formControlName="sortOrder" min="0" />
              </div>
            </div>
            <div class="form-actions">
              <button class="btn" type="button" (click)="closeModals()">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="termForm.invalid || saving">
                {{ saving ? ('common.loading' | translate) : ('common.save' | translate) }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    @if (promoteOpen) {
      <div class="modal-backdrop" (click)="closeModals()">
        <div class="modal modal-lg" (click)="$event.stopPropagation()">
          <h2>{{ preview ? ('academics.termsPage.previewTitle' | translate) : ('academics.termsPage.promoteTitle' | translate) }}</h2>
          <p class="muted">{{ 'academics.termsPage.promoteSubtitle' | translate }}</p>
          @if (formError) { <p class="form-error">{{ formError }}</p> }
          @if (!preview) {
            <form [formGroup]="promoteForm" (ngSubmit)="runPreview()">
              <div class="form-grid">
                <div class="field">
                  <label>{{ 'academics.termsPage.sourceYear' | translate }} *</label>
                  <select formControlName="sourceYearId">
                    <option value="">{{ 'academics.termsPage.selectYear' | translate }}</option>
                    @for (year of years; track year.id) {
                      <option [value]="year.id">{{ year.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label>{{ 'academics.termsPage.destYear' | translate }} *</label>
                  <select formControlName="destinationYearId">
                    <option value="">{{ 'academics.termsPage.selectYear' | translate }}</option>
                    @for (year of years; track year.id) {
                      <option [value]="year.id">{{ year.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label>{{ 'academics.termsPage.sourceClass' | translate }} *</label>
                  <select formControlName="sourceClassId" (change)="onSourceClassChange()">
                    <option value="">{{ 'academics.termsPage.selectClass' | translate }}</option>
                    @for (klass of classes; track klass.id) {
                      <option [value]="klass.id">{{ klass.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label>{{ 'academics.termsPage.sourceSection' | translate }}</label>
                  <select formControlName="sourceSectionId">
                    <option value="">{{ 'academics.termsPage.allSections' | translate }}</option>
                    @for (section of sourceSections; track section.id) {
                      <option [value]="section.id">{{ section.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label>{{ 'academics.termsPage.destClass' | translate }} *</label>
                  <select formControlName="destinationClassId" (change)="onDestClassChange()">
                    <option value="">{{ 'academics.termsPage.selectClass' | translate }}</option>
                    @for (klass of classes; track klass.id) {
                      <option [value]="klass.id">{{ klass.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label>{{ 'academics.termsPage.destSection' | translate }} *</label>
                  <select formControlName="destinationSectionId">
                    <option value="">{{ 'academics.termsPage.selectSection' | translate }}</option>
                    @for (section of destSections; track section.id) {
                      <option [value]="section.id">{{ section.name }}</option>
                    }
                  </select>
                </div>
                <div class="field span-2">
                  <label>{{ 'academics.termsPage.notes' | translate }}</label>
                  <input type="text" formControlName="notes" />
                </div>
              </div>
              <div class="form-actions">
                <button class="btn" type="button" (click)="closeModals()">{{ 'common.cancel' | translate }}</button>
                <button class="btn btn-primary" type="submit" [disabled]="promoteForm.invalid || saving">
                  {{ saving ? ('common.loading' | translate) : ('academics.termsPage.preview' | translate) }}
                </button>
              </div>
            </form>
          } @else {
            <div class="stats">
              <span>{{ 'academics.termsPage.eligible' | translate }}: <strong>{{ preview.eligibleCount }}</strong></span>
              <span>{{ 'academics.termsPage.skipped' | translate }}: <strong>{{ preview.skippedCount }}</strong></span>
              <span>{{ 'academics.termsPage.capacity' | translate }}: <strong>{{ preview.destinationOccupied }}/{{ preview.destinationCapacity }}</strong></span>
            </div>
            <div class="data-table-wrap preview-table">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>{{ 'academics.termsPage.admissionNo' | translate }}</th>
                    <th>{{ 'academics.termsPage.student' | translate }}</th>
                    <th>{{ 'academics.termsPage.roll' | translate }}</th>
                    <th>{{ 'academics.termsPage.outcome' | translate }}</th>
                    <th>{{ 'academics.termsPage.reason' | translate }}</th>
                  </tr>
                </thead>
                <tbody>
                  @for (row of preview.students; track row.studentId) {
                    <tr>
                      <td>{{ row.admissionNo }}</td>
                      <td>{{ row.displayName }}</td>
                      <td>{{ row.rollNumber ?? '—' }}</td>
                      <td>
                        <span class="badge" [class.st-active]="row.outcome === 'PROMOTED'" [class.st-planned]="row.outcome === 'SKIPPED'">
                          {{ (row.outcome === 'PROMOTED' ? 'academics.termsPage.outcomePromoted' : 'academics.termsPage.outcomeSkipped') | translate }}
                        </span>
                      </td>
                      <td>{{ reasonKey(row.reason) | translate }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
            <div class="form-actions">
              <button class="btn" type="button" [disabled]="saving" (click)="backToPromoteForm()">{{ 'common.back' | translate }}</button>
              <button class="btn btn-primary" type="button" [disabled]="saving || preview.eligibleCount === 0" (click)="askPromote()">
                {{ saving ? ('common.loading' | translate) : ('academics.termsPage.confirmPromote' | translate) }}
              </button>
            </div>
          }
        </div>
      </div>
    }

    @if (batchDetail) {
      <div class="modal-backdrop" (click)="closeModals()">
        <div class="modal modal-lg" (click)="$event.stopPropagation()">
          <h2>{{ 'academics.termsPage.history' | translate }}</h2>
          <p class="muted">{{ batchDetail.sourceClassName }} {{ batchDetail.sourceSectionName || '' }} -> {{ batchDetail.destinationClassName }} {{ batchDetail.destinationSectionName }}</p>
          <div class="data-table-wrap preview-table">
            <table class="data-table">
              <thead>
                <tr>
                  <th>{{ 'academics.termsPage.admissionNo' | translate }}</th>
                  <th>{{ 'academics.termsPage.student' | translate }}</th>
                  <th>{{ 'academics.termsPage.outcome' | translate }}</th>
                  <th>{{ 'academics.termsPage.reason' | translate }}</th>
                </tr>
              </thead>
              <tbody>
                @for (row of batchDetail.students ?? []; track row.studentId) {
                  <tr>
                    <td>{{ row.admissionNo }}</td>
                    <td>{{ row.displayName }}</td>
                    <td>{{ row.outcome }}</td>
                    <td>{{ reasonKey(row.reason) | translate }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <div class="form-actions">
            <button class="btn" type="button" (click)="closeModals()">{{ 'common.close' | translate }}</button>
          </div>
        </div>
      </div>
    }

    @if (confirmOpen) {
      <div class="modal-backdrop" (click)="closeConfirm()">
        <div class="modal modal-sm" (click)="$event.stopPropagation()">
          <h2>{{ confirmTitleKey | translate }}</h2>
          <p class="confirm-text">{{ confirmBodyKey | translate:confirmParams }}</p>
          <div class="form-actions">
            <button class="btn" type="button" [disabled]="saving" (click)="closeConfirm()">{{ 'common.cancel' | translate }}</button>
            <button class="btn btn-primary" type="button" [disabled]="saving" (click)="runConfirm()">
              {{ saving ? ('common.loading' | translate) : ('common.confirm' | translate) }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; gap: 12px; flex-wrap: wrap; }
    h2 { font-size: 1.2rem; margin: 0 0 4px; }
    h3 { margin: 0 0 4px; font-size: 1.05rem; }
    .muted { color: var(--color-muted); }
    .header-actions, .row-actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .banner { background: #eff6ff; border: 1px solid #bfdbfe; color: #1e40af; padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
    .banner.success { background: #ecfdf5; border-color: #a7f3d0; color: #047857; }
    .banner.error { background: #fef2f2; border-color: #fecaca; color: #b91c1c; }
    .current-card {
      display: flex; justify-content: space-between; align-items: center; gap: 12px;
      background: var(--color-primary-soft); border: 1px solid var(--color-primary); border-radius: var(--radius);
      padding: 16px 18px; margin-bottom: 16px;
    }
    .eyebrow { margin: 0; font-size: .75rem; font-weight: 700; letter-spacing: .04em; text-transform: uppercase; color: var(--color-primary); }
    .year-card { margin-bottom: 16px; }
    .year-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 12px; flex-wrap: wrap; }
    .empty-terms { margin: 0; }
    .badge { display: inline-block; padding: 3px 10px; border-radius: 20px; font-size: .78rem; font-weight: 700; background: var(--color-bg); border: 1px solid var(--color-border); }
    .badge.current { background: #ecfdf5; border-color: #a7f3d0; color: #047857; margin-left: 8px; }
    .st-planned { color: #92400e; background: #fffbeb; border-color: #fde68a; }
    .st-active { color: #047857; background: #ecfdf5; border-color: #a7f3d0; }
    .st-completed { color: #1e3a8a; background: #eff6ff; border-color: #bfdbfe; }
    .btn-sm { padding: 5px 10px; font-size: .82rem; }
    .btn-danger { background: #b91c1c; border-color: #b91c1c; color: #fff; }
    .modal-backdrop {
      position: fixed; inset: 0; z-index: 100; background: rgba(15, 23, 42, .5);
      display: flex; align-items: flex-start; justify-content: center; padding: 40px 16px; overflow-y: auto;
    }
    .modal { background: var(--color-surface); border-radius: var(--radius); padding: 24px; width: 560px; max-width: 100%; box-shadow: 0 20px 50px rgba(0,0,0,.25); }
    .modal-lg { width: 760px; }
    .modal-sm { width: 420px; }
    .modal h2 { margin: 0 0 8px; font-size: 1.2rem; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    input, select { padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; }
    .span-2 { grid-column: span 2; }
    .check { display: flex; align-items: center; gap: 8px; font-size: .9rem; }
    .form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
    .form-error { margin: 0 0 14px; color: #b91c1c; font-size: .9rem; }
    .confirm-text { margin: 8px 0 0; color: var(--color-muted); line-height: 1.45; }
    .stats { display: flex; gap: 16px; flex-wrap: wrap; margin: 12px 0; font-size: .9rem; }
    .preview-table { max-height: 320px; overflow: auto; }
    @media (max-width: 640px) { .form-grid { grid-template-columns: 1fr; } .span-2 { grid-column: span 1; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicsTermsComponent implements OnInit {
  sessions: SessionRow[] = [];
  years: AcademicYear[] = [];
  classes: SchoolClass[] = [];
  history: PromotionBatch[] = [];
  loading = false;
  saving = false;
  pageError = '';
  successMessage = '';
  formError = '';
  yearModalOpen = false;
  termModalOpen = false;
  promoteOpen = false;
  confirmOpen = false;
  editingYear: AcademicYear | null = null;
  editingTerm: AcademicTerm | null = null;
  termYear: AcademicYear | null = null;
  preview: PromotionPreview | null = null;
  batchDetail: PromotionBatch | null = null;
  confirmTitleKey = '';
  confirmBodyKey = '';
  confirmParams: Record<string, string | number> = {};
  confirmAction: (() => void) | null = null;

  readonly yearForm = new FormGroup({
    name: new FormControl('', Validators.required),
    startDate: new FormControl('', Validators.required),
    endDate: new FormControl('', Validators.required),
    current: new FormControl(false),
  });
  readonly termForm = new FormGroup({
    name: new FormControl('', Validators.required),
    startDate: new FormControl('', Validators.required),
    endDate: new FormControl('', Validators.required),
    status: new FormControl<'PLANNED' | 'ACTIVE' | 'COMPLETED'>('PLANNED', Validators.required),
    sortOrder: new FormControl(1),
  });
  readonly promoteForm = new FormGroup({
    sourceYearId: new FormControl('', Validators.required),
    destinationYearId: new FormControl('', Validators.required),
    sourceClassId: new FormControl('', Validators.required),
    sourceSectionId: new FormControl(''),
    destinationClassId: new FormControl('', Validators.required),
    destinationSectionId: new FormControl('', Validators.required),
    notes: new FormControl(''),
  });

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private auth: AuthService,
  ) {}

  get canManage(): boolean {
    return this.auth.hasAnyPermission(['TERM_MANAGE', 'CLASS_CREATE']);
  }

  get currentYear(): AcademicYear | null {
    return this.sessions.find((session) => session.year.current)?.year ?? null;
  }

  get sourceSections(): SchoolSection[] {
    const classId = this.promoteForm.value.sourceClassId;
    return this.classes.find((klass) => klass.id === classId)?.sections ?? [];
  }

  get destSections(): SchoolSection[] {
    const classId = this.promoteForm.value.destinationClassId;
    return this.classes.find((klass) => klass.id === classId)?.sections ?? [];
  }

  ngOnInit(): void {
    this.load();
    this.loadLookups();
  }

  reasonKey(reason: string | null): string {
    if (reason === 'promotion.student_inactive') {
      return 'academics.termsPage.reasonInactive';
    }
    if (reason === 'promotion.already_enrolled') {
      return 'academics.termsPage.reasonEnrolled';
    }
    if (reason === 'promotion.section_full') {
      return 'academics.termsPage.reasonFull';
    }
    return reason || '—';
  }

  statusKey(status: string): string {
    if (status === 'ACTIVE') {
      return 'academics.termsPage.statusActive';
    }
    if (status === 'COMPLETED') {
      return 'academics.termsPage.statusCompleted';
    }
    return 'academics.termsPage.statusPlanned';
  }

  openYearModal(year?: AcademicYear): void {
    this.editingYear = year ?? null;
    this.formError = '';
    this.yearForm.reset({
      name: year?.name ?? '',
      startDate: year?.startDate ?? '',
      endDate: year?.endDate ?? '',
      current: year?.current ?? false,
    });
    this.yearModalOpen = true;
    this.cdr.markForCheck();
  }

  saveYear(): void {
    if (this.yearForm.invalid || this.saving) {
      return;
    }
    this.saving = true;
    this.formError = '';
    const body = {
      name: this.yearForm.value.name,
      startDate: this.yearForm.value.startDate,
      endDate: this.yearForm.value.endDate,
      current: !!this.yearForm.value.current,
    };
    const request = this.editingYear
      ? this.http.put<ApiResponse<AcademicYear>>(`/api/academic-years/${this.editingYear.id}`, body)
      : this.http.post<ApiResponse<AcademicYear>>('/api/academic-years', body);
    request.subscribe({
      next: () => this.afterSave(this.editingYear ? 'academics.termsPage.yearUpdated' : 'academics.termsPage.yearCreated'),
      error: (err) => this.onFormError(err),
    });
  }

  openTermModal(year: AcademicYear, term?: AcademicTerm): void {
    this.termYear = year;
    this.editingTerm = term ?? null;
    this.formError = '';
    this.termForm.reset({
      name: term?.name ?? '',
      startDate: term?.startDate ?? year.startDate,
      endDate: term?.endDate ?? year.endDate,
      status: term?.status ?? 'PLANNED',
      sortOrder: term?.sortOrder ?? 1,
    });
    this.termModalOpen = true;
    this.cdr.markForCheck();
  }

  saveTerm(): void {
    if (this.termForm.invalid || this.saving || !this.termYear) {
      return;
    }
    this.saving = true;
    this.formError = '';
    const body = {
      name: this.termForm.value.name,
      startDate: this.termForm.value.startDate,
      endDate: this.termForm.value.endDate,
      status: this.termForm.value.status,
      sortOrder: this.termForm.value.sortOrder,
    };
    const request = this.editingTerm
      ? this.http.put<ApiResponse<AcademicTerm>>(`/api/academic-terms/${this.editingTerm.id}`, body)
      : this.http.post<ApiResponse<AcademicTerm>>(`/api/academic-years/${this.termYear.id}/terms`, body);
    request.subscribe({
      next: () => this.afterSave(this.editingTerm ? 'academics.termsPage.termUpdated' : 'academics.termsPage.termCreated'),
      error: (err) => this.onFormError(err),
    });
  }

  askDeleteTerm(term: AcademicTerm): void {
    this.openConfirm('academics.termsPage.deleteTerm', 'academics.termsPage.deleteTermConfirm', { name: term.name }, () => {
      this.saving = true;
      this.http.delete<ApiResponse<void>>(`/api/academic-terms/${term.id}`).subscribe({
        next: () => this.afterSave('academics.termsPage.termDeleted'),
        error: (err) => this.onPageError(err),
      });
    });
  }

  askActivate(year: AcademicYear): void {
    this.openConfirm('academics.termsPage.activate', 'academics.termsPage.activateConfirm', {}, () => {
      this.saving = true;
      this.http.post<ApiResponse<AcademicYear>>(`/api/academic-years/${year.id}/activate`, {}).subscribe({
        next: () => this.afterSave('academics.termsPage.yearActivated'),
        error: (err) => this.onPageError(err),
      });
    });
  }

  deactivate(year: AcademicYear): void {
    this.saving = true;
    this.http.post<ApiResponse<AcademicYear>>(`/api/academic-years/${year.id}/deactivate`, {}).subscribe({
      next: () => this.afterSave('academics.termsPage.yearDeactivated'),
      error: (err) => this.onPageError(err),
    });
  }

  openPromote(): void {
    this.preview = null;
    this.formError = '';
    const current = this.currentYear;
    const next = this.years.find((year) => year.id !== current?.id);
    this.promoteForm.reset({
      sourceYearId: current?.id ?? '',
      destinationYearId: next?.id ?? '',
      sourceClassId: '',
      sourceSectionId: '',
      destinationClassId: '',
      destinationSectionId: '',
      notes: '',
    });
    this.promoteOpen = true;
    this.cdr.markForCheck();
  }

  onSourceClassChange(): void {
    this.promoteForm.patchValue({ sourceSectionId: '' });
  }

  onDestClassChange(): void {
    this.promoteForm.patchValue({ destinationSectionId: '' });
  }

  backToPromoteForm(): void {
    this.preview = null;
    this.cdr.markForCheck();
  }

  runPreview(): void {
    if (this.promoteForm.invalid || this.saving) {
      return;
    }
    this.saving = true;
    this.formError = '';
    this.http.post<ApiResponse<PromotionPreview>>('/api/promotions/preview', this.promoteBody()).subscribe({
      next: (res) => {
        this.preview = res.data ?? null;
        this.saving = false;
        this.cdr.markForCheck();
      },
      error: (err) => this.onFormError(err),
    });
  }

  askPromote(): void {
    if (!this.preview) {
      return;
    }
    this.openConfirm(
      'academics.termsPage.confirmPromote',
      'academics.termsPage.promoteConfirm',
      {
        count: this.preview.eligibleCount,
        source: `${this.preview.sourceClassName} ${this.preview.sourceSectionName ?? ''}`.trim(),
        destination: `${this.preview.destinationClassName} ${this.preview.destinationSectionName}`.trim(),
      },
      () => {
        this.saving = true;
        this.http.post<ApiResponse<PromotionBatch>>('/api/promotions', this.promoteBody()).subscribe({
          next: (res) => {
            this.promoteOpen = false;
            this.preview = null;
            this.afterSave('academics.termsPage.promoteSuccess');
          },
          error: (err) => this.onFormError(err),
        });
      },
    );
  }

  openBatch(id: string): void {
    this.http.get<ApiResponse<PromotionBatch>>(`/api/promotions/${id}`).subscribe({
      next: (res) => {
        this.batchDetail = res.data ?? null;
        this.cdr.markForCheck();
      },
      error: (err) => this.onPageError(err),
    });
  }

  closeModals(): void {
    if (this.saving) {
      return;
    }
    this.yearModalOpen = false;
    this.termModalOpen = false;
    this.promoteOpen = false;
    this.preview = null;
    this.batchDetail = null;
    this.formError = '';
    this.cdr.markForCheck();
  }

  openConfirm(titleKey: string, bodyKey: string, params: Record<string, string | number>, action: () => void): void {
    this.confirmTitleKey = titleKey;
    this.confirmBodyKey = bodyKey;
    this.confirmParams = params;
    this.confirmAction = action;
    this.confirmOpen = true;
    this.cdr.markForCheck();
  }

  closeConfirm(): void {
    if (this.saving) {
      return;
    }
    this.confirmOpen = false;
    this.confirmAction = null;
    this.cdr.markForCheck();
  }

  runConfirm(): void {
    const action = this.confirmAction;
    this.confirmOpen = false;
    this.confirmAction = null;
    action?.();
  }

  private promoteBody() {
    const value = this.promoteForm.value;
    return {
      sourceYearId: value.sourceYearId,
      sourceClassId: value.sourceClassId,
      sourceSectionId: value.sourceSectionId || null,
      destinationYearId: value.destinationYearId,
      destinationClassId: value.destinationClassId,
      destinationSectionId: value.destinationSectionId,
      notes: value.notes || null,
    };
  }

  private afterSave(message: string): void {
    this.saving = false;
    this.yearModalOpen = false;
    this.termModalOpen = false;
    this.confirmOpen = false;
    this.successMessage = message;
    this.pageError = '';
    this.formError = '';
    this.cdr.markForCheck();
    this.load();
  }

  private onFormError(err: HttpErrorResponse): void {
    this.saving = false;
    const apiError = err.error as ApiError | undefined;
    this.formError = apiError?.message || 'Could not save. Please try again.';
    this.cdr.markForCheck();
  }

  private onPageError(err: HttpErrorResponse): void {
    this.saving = false;
    const apiError = err.error as ApiError | undefined;
    this.pageError = apiError?.message || 'Could not complete the request. Please try again.';
    this.cdr.markForCheck();
  }

  private load(): void {
    this.loading = true;
    this.http.get<ApiResponse<SessionRow[]>>('/api/academic-sessions').subscribe({
      next: (res) => {
        this.sessions = res.data ?? [];
        this.years = this.sessions.map((session) => session.year);
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.sessions = [];
        const apiError = err.error as ApiError | undefined;
        this.pageError = apiError?.message || 'Could not load academic sessions.';
        this.cdr.markForCheck();
      },
    });
    this.http.get<ApiResponse<PromotionBatch[]>>('/api/promotions').subscribe({
      next: (res) => {
        this.history = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  private loadLookups(): void {
    this.http.get<ApiResponse<SchoolClass[]>>('/api/classes').subscribe({
      next: (res) => {
        this.classes = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }
}
