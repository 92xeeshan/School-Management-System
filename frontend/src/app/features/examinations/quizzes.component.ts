import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ApiError, ApiResponse, PagedResponse } from '../../core/models/api.model';
import { AuthService } from '../../core/auth/auth.service';
import { DirectoryPagerComponent, SortDirection, sortIndicator, toggleSort } from '../../layout/directory-pager/directory-pager.component';

interface YearOption { id: string; name: string; current: boolean }
interface SectionOption { id: string; name: string }
interface SubjectOption { id: string; name: string }
interface ClassOption { id: string; name: string; sections: SectionOption[]; subjects: SubjectOption[] }
interface QuizOptions {
  academicYears: YearOption[];
  classes: ClassOption[];
  questionTypes: string[];
  statuses: string[];
}
interface QuestionOption { key: string; text: string }
interface QuizQuestion {
  id: string;
  subjectId: string | null;
  subjectName: string | null;
  classId: string | null;
  className: string | null;
  questionType: string;
  prompt: string;
  options: QuestionOption[] | null;
  correctAnswer: string | null;
  marks: number;
  explanation: string | null;
  createdAt: string;
}
interface QuizItem {
  id: string;
  questionId: string;
  sortOrder: number;
  marks: number;
  question: QuizQuestion | null;
}
interface QuizRow {
  id: string;
  academicYearId: string;
  academicYearName: string;
  classId: string;
  className: string;
  sectionId: string | null;
  sectionName: string | null;
  subjectId: string;
  subjectName: string;
  title: string;
  instructions: string | null;
  durationMinutes: number;
  maxAttempts: number;
  startAt: string | null;
  endAt: string | null;
  passMarks: number | null;
  shuffleQuestions: boolean;
  showResults: boolean;
  status: string;
  questionCount: number;
  maxScore: number;
  attemptCount: number;
  createdAt: string;
  items: QuizItem[] | null;
}
interface TakeQuestion {
  questionId: string;
  questionType: string;
  prompt: string;
  options: QuestionOption[] | null;
  marks: number;
  response: string | null;
}
interface QuizTake {
  attemptId: string;
  quizId: string;
  title: string;
  instructions: string | null;
  durationMinutes: number;
  startedAt: string;
  expiresAt: string;
  status: string;
  questions: TakeQuestion[];
}
interface QuizAnswer {
  questionId: string;
  questionType: string;
  prompt: string;
  options: QuestionOption[] | null;
  correctAnswer: string | null;
  response: string | null;
  autoCorrect: boolean | null;
  autoMarks: number | null;
  awardedMarks: number | null;
  maxMarks: number;
  graderComment: string | null;
}
interface QuizAttempt {
  id: string;
  quizId: string;
  quizTitle: string;
  studentId: string;
  studentName: string;
  admissionNo: string;
  rollNumber: number | null;
  attemptNo: number;
  status: string;
  startedAt: string;
  submittedAt: string | null;
  expiresAt: string | null;
  autoScore: number | null;
  manualScore: number | null;
  totalScore: number | null;
  maxScore: number;
  percentage: number | null;
  passed: boolean;
  answers: QuizAnswer[] | null;
}
interface OfflineRow {
  studentId: string;
  admissionNo: string;
  studentName: string;
  rollNumber: number | null;
  marks: number | null;
  remarks: string | null;
}
interface OfflineGrid {
  quizId: string;
  quizTitle: string;
  classId: string;
  className: string;
  sectionId: string | null;
  sectionName: string | null;
  subjectId: string;
  subjectName: string;
  maxScore: number;
  canEdit: boolean;
  rows: OfflineRow[];
}
interface ResultRow {
  studentId: string;
  admissionNo: string;
  studentName: string;
  rollNumber: number | null;
  attemptId: string | null;
  attemptNo: number;
  source: string;
  status: string;
  submittedAt: string | null;
  score: number | null;
  maxScore: number;
  percentage: number | null;
  passed: boolean;
}

type PageTab = 'quizzes' | 'bank' | 'offline' | 'results';

@Component({
  selector: 'app-quizzes',
  imports: [TranslateModule, ReactiveFormsModule, DirectoryPagerComponent],
  template: `
    <div class="tab-page">
      <div class="page-header">
        <div>
          <h2>{{ 'examinations.quizzesPage.title' | translate }}</h2>
          <p class="muted">{{ 'examinations.quizzesPage.subtitle' | translate }}</p>
        </div>
        @if (canManage && pageTab === 'quizzes') {
          <button class="btn btn-primary" type="button" (click)="openQuizModal()">
            {{ 'examinations.quizzesPage.addQuiz' | translate }}
          </button>
        }
        @if (canManage && pageTab === 'bank') {
          <button class="btn btn-primary" type="button" (click)="openQuestionModal()">
            {{ 'examinations.quizzesPage.addQuestion' | translate }}
          </button>
        }
      </div>

      @if (successMessage) {
        <p class="banner success">{{ successMessage | translate }}</p>
      }
      @if (pageError) {
        <p class="banner error">{{ pageError }}</p>
      }

      <div class="subtabs">
        <button type="button" class="subtab" [class.active]="pageTab === 'quizzes'" (click)="setTab('quizzes')">
          {{ 'examinations.quizzesPage.quizzes' | translate }}
        </button>
        @if (canManage) {
          <button type="button" class="subtab" [class.active]="pageTab === 'bank'" (click)="setTab('bank')">
            {{ 'examinations.quizzesPage.questionBank' | translate }}
          </button>
          <button type="button" class="subtab" [class.active]="pageTab === 'offline'" (click)="setTab('offline')">
            {{ 'examinations.quizzesPage.offline' | translate }}
          </button>
        }
        <button type="button" class="subtab" [class.active]="pageTab === 'results'" (click)="setTab('results')">
          {{ 'examinations.quizzesPage.results' | translate }}
        </button>
      </div>

      @if (pageTab === 'quizzes' || pageTab === 'bank') {
        <div class="filters">
          @if (pageTab === 'quizzes') {
            <select [value]="yearId" (change)="onYearChange($event)">
              @for (year of options?.academicYears ?? []; track year.id) {
                <option [value]="year.id">{{ year.name }}</option>
              }
            </select>
            <select [value]="classId" (change)="onClassFilter($event)">
              <option value="">{{ 'examinations.schedulesPage.allClasses' | translate }}</option>
              @for (klass of options?.classes ?? []; track klass.id) {
                <option [value]="klass.id">{{ klass.name }}</option>
              }
            </select>
            <select [value]="sectionId" (change)="onSectionFilter($event)" [disabled]="!classId">
              <option value="">{{ 'examinations.schedulesPage.allSections' | translate }}</option>
              @for (section of filterSections; track section.id) {
                <option [value]="section.id">{{ section.name }}</option>
              }
            </select>
            <select [value]="subjectId" (change)="onSubjectFilter($event)">
              <option value="">{{ 'common.all' | translate }}</option>
              @for (subject of filterSubjects; track subject.id) {
                <option [value]="subject.id">{{ subject.name }}</option>
              }
            </select>
            <select [value]="statusFilter" (change)="onStatusFilter($event)">
              <option value="">{{ 'examinations.quizzesPage.allStatuses' | translate }}</option>
              @for (status of options?.statuses ?? []; track status) {
                <option [value]="status">{{ statusLabel(status) | translate }}</option>
              }
            </select>
            <input type="search" [value]="quizQuery" [placeholder]="'common.search' | translate"
                   (input)="onQuizSearch($any($event.target).value)" />
          } @else {
            <select [value]="bankClassId" (change)="onBankClass($event)">
              <option value="">{{ 'examinations.schedulesPage.allClasses' | translate }}</option>
              @for (klass of options?.classes ?? []; track klass.id) {
                <option [value]="klass.id">{{ klass.name }}</option>
              }
            </select>
            <select [value]="bankSubjectId" (change)="onBankSubject($event)">
              <option value="">{{ 'common.all' | translate }}</option>
              @for (subject of bankSubjects; track subject.id) {
                <option [value]="subject.id">{{ subject.name }}</option>
              }
            </select>
            <select [value]="bankType" (change)="onBankType($event)">
              <option value="">{{ 'examinations.quizzesPage.allTypes' | translate }}</option>
              @for (type of options?.questionTypes ?? []; track type) {
                <option [value]="type">{{ typeLabel(type) | translate }}</option>
              }
            </select>
            <input type="search" [value]="bankQuery" [placeholder]="'common.search' | translate"
                   (input)="onBankSearch($any($event.target).value)" />
          }
        </div>
      }

      @if (pageTab === 'quizzes') {
        <div class="card">
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th class="sortable" (click)="sortQuizzesBy('title')">{{ 'examinations.quizzesPage.titleField' | translate }}<span class="sort-ind">{{ quizIndicator('title') }}</span></th>
                  <th class="sortable" (click)="sortQuizzesBy('class')">{{ 'examinations.quizzesPage.class' | translate }}<span class="sort-ind">{{ quizIndicator('class') }}</span></th>
                  <th class="sortable" (click)="sortQuizzesBy('subject')">{{ 'examinations.quizzesPage.subject' | translate }}<span class="sort-ind">{{ quizIndicator('subject') }}</span></th>
                  <th>{{ 'examinations.quizzesPage.duration' | translate }}</th>
                  <th>{{ 'examinations.quizzesPage.questionCount' | translate }}</th>
                  <th>{{ 'examinations.quizzesPage.maxScore' | translate }}</th>
                  <th class="sortable" (click)="sortQuizzesBy('status')">{{ 'examinations.quizzesPage.status' | translate }}<span class="sort-ind">{{ quizIndicator('status') }}</span></th>
                  <th>{{ 'common.actions' | translate }}</th>
                </tr>
              </thead>
              <tbody>
                @for (quiz of quizzes; track quiz.id) {
                  <tr>
                    <td class="strong">{{ quiz.title }}</td>
                    <td>{{ quiz.className }} {{ quiz.sectionName || '' }}</td>
                    <td>{{ quiz.subjectName }}</td>
                    <td>{{ quiz.durationMinutes }}</td>
                    <td>{{ quiz.questionCount }}</td>
                    <td>{{ formatNum(quiz.maxScore) }}</td>
                    <td>
                      <span class="badge" [class]="statusClass(quiz.status)">
                        {{ statusLabel(quiz.status) | translate }}
                      </span>
                    </td>
                    <td class="actions-cell">
                      @if (canTake(quiz)) {
                        <button class="btn" type="button" (click)="startQuiz(quiz)">
                          {{ 'examinations.quizzesPage.take' | translate }}
                        </button>
                      }
                      @if (canManage) {
                        <button class="btn" type="button" (click)="openQuizModal(quiz)">
                          {{ 'common.edit' | translate }}
                        </button>
                        @if (quiz.status === 'DRAFT') {
                          <button class="btn" type="button" (click)="askPublish(quiz)">
                            {{ 'examinations.quizzesPage.publish' | translate }}
                          </button>
                        }
                        @if (quiz.status === 'PUBLISHED') {
                          <button class="btn" type="button" (click)="setQuizStatus(quiz, 'CLOSED')">
                            {{ 'examinations.quizzesPage.closeQuiz' | translate }}
                          </button>
                        }
                        @if (quiz.status === 'CLOSED') {
                          <button class="btn" type="button" (click)="setQuizStatus(quiz, 'ARCHIVED')">
                            {{ 'examinations.quizzesPage.archive' | translate }}
                          </button>
                        }
                        @if (quiz.status !== 'PUBLISHED') {
                          <button class="btn btn-danger" type="button" (click)="askDeleteQuiz(quiz)">
                            {{ 'common.delete' | translate }}
                          </button>
                        }
                      }
                      <button class="btn" type="button" (click)="selectQuiz(quiz, 'results')">
                        {{ 'examinations.quizzesPage.results' | translate }}
                      </button>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="8" class="center">
                      {{ loading ? ('common.loading' | translate) : ('examinations.quizzesPage.emptyQuizzes' | translate) }}
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <app-directory-pager
            [pageIndex]="quizPageIndex" [pageSize]="quizPageSize"
            [totalElements]="quizTotalElements" [totalPages]="quizTotalPages"
            (pageChange)="onQuizPage($event)" (pageSizeChange)="onQuizPageSize($event)" />
        </div>
      }

      @if (pageTab === 'bank') {
        <div class="card">
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th class="sortable" (click)="sortQuestionsBy('prompt')">{{ 'examinations.quizzesPage.prompt' | translate }}<span class="sort-ind">{{ bankIndicator('prompt') }}</span></th>
                  <th class="sortable" (click)="sortQuestionsBy('questionType')">{{ 'examinations.quizzesPage.questionType' | translate }}<span class="sort-ind">{{ bankIndicator('questionType') }}</span></th>
                  <th class="sortable" (click)="sortQuestionsBy('subject')">{{ 'examinations.quizzesPage.subject' | translate }}<span class="sort-ind">{{ bankIndicator('subject') }}</span></th>
                  <th class="sortable" (click)="sortQuestionsBy('class')">{{ 'examinations.quizzesPage.class' | translate }}<span class="sort-ind">{{ bankIndicator('class') }}</span></th>
                  <th class="sortable" (click)="sortQuestionsBy('marks')">{{ 'examinations.quizzesPage.marks' | translate }}<span class="sort-ind">{{ bankIndicator('marks') }}</span></th>
                  <th>{{ 'common.actions' | translate }}</th>
                </tr>
              </thead>
              <tbody>
                @for (question of questions; track question.id) {
                  <tr>
                    <td class="strong">{{ question.prompt }}</td>
                    <td>{{ typeLabel(question.questionType) | translate }}</td>
                    <td>{{ question.subjectName || '—' }}</td>
                    <td>{{ question.className || '—' }}</td>
                    <td>{{ formatNum(question.marks) }}</td>
                    <td class="actions-cell">
                      <button class="btn" type="button" (click)="openQuestionModal(question)">
                        {{ 'common.edit' | translate }}
                      </button>
                      <button class="btn btn-danger" type="button" (click)="askDeleteQuestion(question)">
                        {{ 'common.delete' | translate }}
                      </button>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="6" class="center">
                      {{ loading ? ('common.loading' | translate) : ('examinations.quizzesPage.emptyQuestions' | translate) }}
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      }

      @if (pageTab === 'offline' || pageTab === 'results') {
        <div class="filters">
          <select [value]="selectedQuizId" (change)="onSelectedQuiz($event)">
            <option value="">{{ 'examinations.quizzesPage.quizzes' | translate }}</option>
            @for (quiz of quizChoices; track quiz.id) {
              <option [value]="quiz.id">{{ quiz.title }} — {{ quiz.className }}</option>
            }
          </select>
          @if (pageTab === 'results' && selectedQuizId) {
            <button class="btn" type="button" (click)="downloadResultsPdf()" [disabled]="busy">
              {{ 'examinations.quizzesPage.downloadPdf' | translate }}
            </button>
          }
        </div>
      }

      @if (pageTab === 'offline') {
        @if (offlineGrid) {
          <div class="meta">
            <span class="chip">{{ offlineGrid.quizTitle }}</span>
            <span class="chip">{{ offlineGrid.className }} {{ offlineGrid.sectionName || '' }}</span>
            <span class="chip">{{ 'examinations.quizzesPage.maxScore' | translate }}: {{ formatNum(offlineGrid.maxScore) }}</span>
          </div>
          <div class="card">
            <div class="table-wrap">
              <table class="sheet">
                <thead>
                  <tr>
                    <th>{{ 'students.rollNumber' | translate }}</th>
                    <th>{{ 'students.studentId' | translate }}</th>
                    <th>{{ 'students.name' | translate }}</th>
                    <th>{{ 'examinations.quizzesPage.marks' | translate }}</th>
                    <th>{{ 'examinations.quizzesPage.remarks' | translate }}</th>
                  </tr>
                </thead>
                <tbody>
                  @for (row of offlineGrid.rows; track row.studentId) {
                    <tr>
                      <td>{{ row.rollNumber || '' }}</td>
                      <td>{{ row.admissionNo }}</td>
                      <td class="strong">{{ row.studentName }}</td>
                      <td>
                        <input class="cell" type="number" min="0" [max]="offlineGrid.maxScore" step="0.5"
                               [value]="row.marks ?? ''"
                               [disabled]="!offlineGrid.canEdit"
                               (input)="onOfflineMarks(row, $event)" />
                      </td>
                      <td>
                        <input class="cell remarks" type="text" maxlength="500"
                               [value]="row.remarks || ''"
                               [disabled]="!offlineGrid.canEdit"
                               (input)="onOfflineRemarks(row, $event)" />
                      </td>
                    </tr>
                  } @empty {
                    <tr><td colspan="5" class="center">{{ 'common.noData' | translate }}</td></tr>
                  }
              </tbody>
            </table>
          </div>
          <app-directory-pager
            [pageIndex]="bankPageIndex" [pageSize]="bankPageSize"
            [totalElements]="bankTotalElements" [totalPages]="bankTotalPages"
            (pageChange)="onBankPage($event)" (pageSizeChange)="onBankPageSize($event)" />
        </div>
          @if (offlineGrid.canEdit) {
            <div class="form-actions inline">
              <button class="btn btn-primary" type="button" (click)="saveOffline()" [disabled]="busy">
                {{ 'examinations.quizzesPage.saveOffline' | translate }}
              </button>
            </div>
          }
        } @else if (loading) {
          <p class="muted">{{ 'common.loading' | translate }}</p>
        }
      }

      @if (pageTab === 'results') {
        @if (resultStats) {
          <div class="meta">
            <span class="chip">{{ 'examinations.quizzesPage.scored' | translate }}: {{ resultStats.count }}</span>
            <span class="chip">{{ 'examinations.quizzesPage.average' | translate }}: {{ formatNum(resultStats.average) }}</span>
            <span class="chip">{{ 'examinations.quizzesPage.highest' | translate }}: {{ formatNum(resultStats.highest) }}</span>
            <span class="chip">{{ 'examinations.quizzesPage.lowest' | translate }}: {{ formatNum(resultStats.lowest) }}</span>
            <span class="chip">{{ 'examinations.quizzesPage.passCount' | translate }}: {{ resultStats.passed }}</span>
            <span class="chip">{{ 'examinations.quizzesPage.failCount' | translate }}: {{ resultStats.failed }}</span>
          </div>
        }
        <div class="card">
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{{ 'students.rollNumber' | translate }}</th>
                  <th>{{ 'students.name' | translate }}</th>
                  <th>{{ 'examinations.quizzesPage.source' | translate }}</th>
                  <th>{{ 'examinations.quizzesPage.score' | translate }}</th>
                  <th>{{ 'examinations.quizzesPage.percentage' | translate }}</th>
                  <th>{{ 'examinations.quizzesPage.status' | translate }}</th>
                  <th>{{ 'common.actions' | translate }}</th>
                </tr>
              </thead>
              <tbody>
                @for (row of results; track row.studentId) {
                  <tr>
                    <td>{{ row.rollNumber || '' }}</td>
                    <td class="strong">{{ row.studentName }}</td>
                    <td>{{ sourceLabel(row.source) | translate }}</td>
                    <td>{{ formatNum(row.score) }} / {{ formatNum(row.maxScore) }}</td>
                    <td>{{ formatNum(row.percentage) }}{{ row.percentage == null ? '' : '%' }}</td>
                    <td>
                      @if (row.source === 'NONE') {
                        {{ 'examinations.quizzesPage.none' | translate }}
                      } @else {
                        <span class="badge" [class.badge-success]="row.passed" [class.badge-danger]="!row.passed">
                          {{ (row.passed ? 'examinations.quizzesPage.passed' : 'examinations.quizzesPage.failed') | translate }}
                        </span>
                      }
                    </td>
                    <td class="actions-cell">
                      @if (row.attemptId) {
                        @if (canManage) {
                          <button class="btn" type="button" (click)="openAttempt(row.attemptId)">
                            {{ 'examinations.quizzesPage.grade' | translate }}
                          </button>
                        }
                        <button class="btn" type="button" (click)="downloadAttemptPdf(row.attemptId)" [disabled]="busy">
                          {{ 'examinations.quizzesPage.downloadAttempt' | translate }}
                        </button>
                      }
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td colspan="7" class="center">
                      {{ loading ? ('common.loading' | translate) : ('examinations.quizzesPage.emptyResults' | translate) }}
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      }
    </div>

    @if (showQuizModal) {
      <div class="modal-backdrop" (click)="closeQuizModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editingQuizId ? ('examinations.quizzesPage.editQuiz' | translate) : ('examinations.quizzesPage.addQuiz' | translate) }}</h2>
          @if (formError) {
            <p class="form-error">{{ formError }}</p>
          }
          <form [formGroup]="quizForm" (ngSubmit)="saveQuiz()">
            <div class="form-grid">
              <div class="field">
                <label>{{ 'examinations.quizzesPage.year' | translate }} *</label>
                <select formControlName="academicYearId">
                  @for (year of options?.academicYears ?? []; track year.id) {
                    <option [value]="year.id">{{ year.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'examinations.quizzesPage.class' | translate }} *</label>
                <select formControlName="classId" (change)="onQuizClassChange()">
                  <option value="">{{ 'examinations.schedulesPage.selectClass' | translate }}</option>
                  @for (klass of options?.classes ?? []; track klass.id) {
                    <option [value]="klass.id">{{ klass.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'examinations.quizzesPage.section' | translate }}</label>
                <select formControlName="sectionId">
                  <option value="">{{ 'examinations.schedulesPage.selectSection' | translate }}</option>
                  @for (section of quizFormSections; track section.id) {
                    <option [value]="section.id">{{ section.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'examinations.quizzesPage.subject' | translate }} *</label>
                <select formControlName="subjectId">
                  <option value="">{{ 'examinations.schedulesPage.selectSubject' | translate }}</option>
                  @for (subject of quizFormSubjects; track subject.id) {
                    <option [value]="subject.id">{{ subject.name }}</option>
                  }
                </select>
              </div>
              <div class="field field-span">
                <label>{{ 'examinations.quizzesPage.titleField' | translate }} *</label>
                <input type="text" formControlName="title" />
              </div>
              <div class="field field-span">
                <label>{{ 'examinations.quizzesPage.instructions' | translate }}</label>
                <textarea formControlName="instructions" rows="3"></textarea>
              </div>
              <div class="field">
                <label>{{ 'examinations.quizzesPage.duration' | translate }} *</label>
                <input type="number" min="1" formControlName="durationMinutes" />
              </div>
              <div class="field">
                <label>{{ 'examinations.quizzesPage.maxAttempts' | translate }}</label>
                <input type="number" min="1" formControlName="maxAttempts" />
              </div>
              <div class="field">
                <label>{{ 'examinations.quizzesPage.startAt' | translate }}</label>
                <input type="datetime-local" formControlName="startAt" />
              </div>
              <div class="field">
                <label>{{ 'examinations.quizzesPage.endAt' | translate }}</label>
                <input type="datetime-local" formControlName="endAt" />
              </div>
              <div class="field">
                <label>{{ 'examinations.quizzesPage.passMarks' | translate }}</label>
                <input type="number" min="0" step="0.5" formControlName="passMarks" />
              </div>
              <div class="field checks">
                <label class="check">
                  <input type="checkbox" formControlName="shuffleQuestions" />
                  {{ 'examinations.quizzesPage.shuffle' | translate }}
                </label>
                <label class="check">
                  <input type="checkbox" formControlName="showResults" />
                  {{ 'examinations.quizzesPage.showResults' | translate }}
                </label>
              </div>
              <div class="field field-span">
                <label>{{ 'examinations.quizzesPage.selectQuestions' | translate }}</label>
                <div class="chip-list">
                  @for (question of questions; track question.id) {
                    <label class="chip">
                      <input type="checkbox" [checked]="selectedQuestionIds.has(question.id)"
                             (change)="toggleQuestion(question.id, $event)" />
                      {{ question.prompt }} ({{ formatNum(question.marks) }})
                    </label>
                  }
                </div>
              </div>
            </div>
            <div class="form-actions">
              <button class="btn" type="button" (click)="closeQuizModal()" [disabled]="saving">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="quizForm.invalid || saving">
                {{ saving ? ('common.loading' | translate) : ('common.save' | translate) }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    @if (showQuestionModal) {
      <div class="modal-backdrop" (click)="closeQuestionModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editingQuestionId ? ('examinations.quizzesPage.editQuestion' | translate) : ('examinations.quizzesPage.addQuestion' | translate) }}</h2>
          @if (formError) {
            <p class="form-error">{{ formError }}</p>
          }
          <form [formGroup]="questionForm" (ngSubmit)="saveQuestion()">
            <div class="form-grid">
              <div class="field">
                <label>{{ 'examinations.quizzesPage.questionType' | translate }} *</label>
                <select formControlName="questionType" (change)="onQuestionTypeChange()">
                  @for (type of options?.questionTypes ?? []; track type) {
                    <option [value]="type">{{ typeLabel(type) | translate }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'examinations.quizzesPage.marks' | translate }} *</label>
                <input type="number" min="0.5" step="0.5" formControlName="marks" />
              </div>
              <div class="field">
                <label>{{ 'examinations.quizzesPage.class' | translate }}</label>
                <select formControlName="classId">
                  <option value="">{{ 'examinations.schedulesPage.selectClass' | translate }}</option>
                  @for (klass of options?.classes ?? []; track klass.id) {
                    <option [value]="klass.id">{{ klass.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'examinations.quizzesPage.subject' | translate }}</label>
                <select formControlName="subjectId">
                  <option value="">{{ 'examinations.schedulesPage.selectSubject' | translate }}</option>
                  @for (subject of questionSubjects; track subject.id) {
                    <option [value]="subject.id">{{ subject.name }}</option>
                  }
                </select>
              </div>
              <div class="field field-span">
                <label>{{ 'examinations.quizzesPage.prompt' | translate }} *</label>
                <textarea formControlName="prompt" rows="3"></textarea>
              </div>
              @if (questionForm.value.questionType !== 'SHORT_ANSWER') {
                <div class="field field-span">
                  <label>{{ 'examinations.quizzesPage.options' | translate }}</label>
                  @for (option of questionOptions; track $index; let i = $index) {
                    <div class="option-row">
                      <input class="key" type="text" [value]="option.key" (input)="onOptionKey(i, $event)" />
                      <input type="text" [value]="option.text" (input)="onOptionText(i, $event)" />
                      <label class="check">
                        <input type="radio" name="correct" [checked]="questionForm.value.correctAnswer === option.key"
                               (change)="setCorrectAnswer(option.key)" />
                        {{ 'examinations.quizzesPage.correctAnswer' | translate }}
                      </label>
                    </div>
                  }
                  <button class="btn" type="button" (click)="addOption()">{{ 'examinations.quizzesPage.addOption' | translate }}</button>
                </div>
              } @else {
                <div class="field field-span">
                  <label>{{ 'examinations.quizzesPage.correctAnswer' | translate }}</label>
                  <input type="text" formControlName="correctAnswer" />
                </div>
              }
              <div class="field field-span">
                <label>{{ 'examinations.quizzesPage.explanation' | translate }}</label>
                <textarea formControlName="explanation" rows="2"></textarea>
              </div>
            </div>
            <div class="form-actions">
              <button class="btn" type="button" (click)="closeQuestionModal()" [disabled]="saving">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="questionForm.invalid || saving">
                {{ saving ? ('common.loading' | translate) : ('common.save' | translate) }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    @if (take) {
      <div class="modal-backdrop">
        <div class="modal modal-lg" (click)="$event.stopPropagation()">
          <h2>{{ take.title }}</h2>
          @if (take.instructions) {
            <p class="muted">{{ take.instructions }}</p>
          }
          <p class="timer">{{ 'examinations.quizzesPage.timeLeft' | translate }}: {{ timeLeft }}</p>
          @if (formError) {
            <p class="form-error">{{ formError }}</p>
          }
          @for (question of take.questions; track question.questionId; let i = $index) {
            <div class="take-q">
              <p class="strong">{{ i + 1 }}. {{ question.prompt }} <span class="muted">({{ formatNum(question.marks) }})</span></p>
              @if (question.questionType === 'SHORT_ANSWER') {
                <textarea rows="2" [value]="responses[question.questionId] || ''"
                          (input)="onResponse(question.questionId, $event)"></textarea>
              } @else {
                @for (option of question.options ?? []; track option.key) {
                  <label class="check">
                    <input type="radio" [name]="'q-' + question.questionId"
                           [checked]="responses[question.questionId] === option.key"
                           (change)="setResponse(question.questionId, option.key)" />
                    {{ option.text }}
                  </label>
                }
              }
            </div>
          }
          <div class="form-actions">
            <button class="btn" type="button" (click)="saveProgress()" [disabled]="saving">
              {{ 'examinations.quizzesPage.saveProgress' | translate }}
            </button>
            <button class="btn btn-primary" type="button" (click)="submitQuiz()" [disabled]="saving">
              {{ 'examinations.quizzesPage.submit' | translate }}
            </button>
          </div>
        </div>
      </div>
    }

    @if (gradeAttempt) {
      <div class="modal-backdrop" (click)="closeGrade()">
        <div class="modal modal-lg" (click)="$event.stopPropagation()">
          <h2>{{ gradeAttempt.quizTitle }} — {{ gradeAttempt.studentName }}</h2>
          <p class="muted">
            {{ formatNum(gradeAttempt.totalScore) }} / {{ formatNum(gradeAttempt.maxScore) }}
            ({{ formatNum(gradeAttempt.percentage) }}%)
          </p>
          @if (formError) {
            <p class="form-error">{{ formError }}</p>
          }
          @for (answer of gradeAttempt.answers ?? []; track answer.questionId) {
            <div class="take-q">
              <p class="strong">{{ answer.prompt }}</p>
              <p>{{ 'examinations.quizzesPage.yourAnswer' | translate }}: {{ answer.response || '—' }}</p>
              @if (answer.correctAnswer) {
                <p>{{ 'examinations.quizzesPage.correctAnswer' | translate }}: {{ answer.correctAnswer }}</p>
              }
              @if (canManage && answer.questionType === 'SHORT_ANSWER') {
                <div class="option-row">
                  <input class="cell" type="number" min="0" [max]="answer.maxMarks" step="0.5"
                         [value]="answer.awardedMarks ?? ''"
                         (input)="onAwarded(answer, $event)" />
                  <input type="text" [value]="answer.graderComment || ''"
                         (input)="onComment(answer, $event)" />
                </div>
              } @else {
                <p>{{ 'examinations.quizzesPage.awarded' | translate }}: {{ formatNum(answer.awardedMarks) }}</p>
              }
            </div>
          }
          <div class="form-actions">
            <button class="btn" type="button" (click)="closeGrade()">{{ 'common.close' | translate }}</button>
            @if (canManage) {
              <button class="btn btn-primary" type="button" (click)="saveGrade()" [disabled]="saving">
                {{ 'examinations.quizzesPage.grade' | translate }}
              </button>
            }
            @if (gradeAttempt.id) {
              <button class="btn" type="button" (click)="downloadAttemptPdf(gradeAttempt.id)" [disabled]="busy">
                {{ 'examinations.quizzesPage.downloadAttempt' | translate }}
              </button>
            }
          </div>
        </div>
      </div>
    }

    @if (confirmOpen) {
      <div class="modal-backdrop" (click)="closeConfirm()">
        <div class="modal modal-sm" (click)="$event.stopPropagation()">
          <h2>{{ 'common.confirm' | translate }}</h2>
          <p class="confirm-text">{{ confirmKey | translate }}</p>
          <div class="form-actions">
            <button class="btn" type="button" (click)="closeConfirm()">{{ 'common.cancel' | translate }}</button>
            <button class="btn btn-danger" type="button" (click)="runConfirm()" [disabled]="saving">
              {{ 'common.confirm' | translate }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; gap: 12px; flex-wrap: wrap; }
    h2 { font-size: 1.2rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); margin: 0; }
    .banner { padding: 10px 14px; border-radius: 8px; margin-bottom: 12px; }
    .banner.success { background: #ecfdf5; border: 1px solid #a7f3d0; color: #047857; }
    .banner.error { background: #fef2f2; border: 1px solid #fecaca; color: #b91c1c; }
    .subtabs { display: flex; gap: 4px; margin-bottom: 14px; flex-wrap: wrap; }
    .subtab {
      padding: 8px 12px; border: 1px solid var(--color-border); background: var(--color-surface);
      border-radius: 8px; font: inherit; cursor: pointer; color: var(--color-muted); font-weight: 600;
    }
    .subtab.active { color: var(--color-primary); border-color: var(--color-primary); background: var(--color-primary-soft); }
    .filters { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; align-items: center; }
    .filters select, .filters .btn { padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; background: var(--color-surface); color: var(--color-text); min-width: 140px; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { text-align: left; padding: 10px; border-bottom: 1px solid var(--color-border); font-size: .9rem; }
    th { color: var(--color-muted); font-weight: 600; font-size: .75rem; text-transform: uppercase; background: var(--color-bg); }
    .strong { font-weight: 600; }
    .center { text-align: center; color: var(--color-muted); padding: 28px; }
    .actions-cell { display: flex; gap: 6px; flex-wrap: wrap; }
    .badge { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: .7rem; font-weight: 600; background: #e2e8f0; color: #475569; }
    .badge-success { background: #d1fae5; color: #047857; }
    .badge-danger { background: #fee2e2; color: #b91c1c; }
    .badge-warning { background: #fef3c7; color: #b45309; }
    .meta { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
    .meta .chip { padding: 6px 12px; border-radius: 20px; background: var(--color-surface); border: 1px solid var(--color-border); font-size: .82rem; }
    .cell { width: 88px; padding: 6px 8px; border: 1px solid var(--color-border); border-radius: 6px; font: inherit; }
    .cell.remarks { width: 180px; }
    .modal-backdrop {
      position: fixed; inset: 0; z-index: 100;
      background: rgba(15, 23, 42, .5);
      display: flex; align-items: flex-start; justify-content: center;
      padding: 40px 16px; overflow-y: auto;
    }
    .modal { padding: 24px; width: 760px; max-width: 100%; background: var(--color-surface); border-radius: 12px; }
    .modal-lg { width: 860px; }
    .modal-sm { width: 420px; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field-span { grid-column: 1 / -1; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    input, select, textarea { padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; }
    .chip-list { display: flex; flex-direction: column; gap: 8px; max-height: 220px; overflow-y: auto; }
    .chip { display: flex; align-items: flex-start; gap: 8px; padding: 8px 10px; border-radius: 8px; }
    .form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; align-items: center; flex-wrap: wrap; }
    .form-actions.inline { justify-content: flex-start; }
    .form-error { color: #b91c1c; font-size: .85rem; }
    .confirm-text { margin: 0 0 8px; color: var(--color-muted); line-height: 1.45; }
    .btn-danger { background: #b91c1c; border-color: #b91c1c; color: #fff; }
    .checks { justify-content: center; }
    .check { display: flex; align-items: center; gap: 8px; font-size: .9rem; }
    .option-row { display: flex; gap: 8px; margin-bottom: 8px; align-items: center; }
    .option-row .key { width: 80px; }
    .take-q { margin: 14px 0; padding: 12px; border: 1px solid var(--color-border); border-radius: 8px; }
    .timer { font-weight: 700; color: var(--color-primary); margin: 8px 0 16px; }
    @media (max-width: 640px) { .form-grid { grid-template-columns: 1fr; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuizzesComponent implements OnInit, OnDestroy {
  options: QuizOptions | null = null;
  quizzes: QuizRow[] = [];
  quizChoices: QuizRow[] = [];
  questions: QuizQuestion[] = [];
  results: ResultRow[] = [];
  offlineGrid: OfflineGrid | null = null;
  take: QuizTake | null = null;
  gradeAttempt: QuizAttempt | null = null;
  responses: Record<string, string> = {};
  timeLeft = '';
  pageTab: PageTab = 'quizzes';
  yearId = '';
  classId = '';
  sectionId = '';
  subjectId = '';
  statusFilter = '';
  bankClassId = '';
  bankSubjectId = '';
  bankType = '';
  quizQuery = '';
  bankQuery = '';
  quizPageIndex = 0;
  quizPageSize = 25;
  quizTotalElements = 0;
  quizTotalPages = 0;
  quizSortKey = 'createdAt';
  quizSortDirection: SortDirection = 'desc';
  bankPageIndex = 0;
  bankPageSize = 25;
  bankTotalElements = 0;
  bankTotalPages = 0;
  bankSortKey = 'createdAt';
  bankSortDirection: SortDirection = 'desc';
  private quizSearchTimer: ReturnType<typeof setTimeout> | null = null;
  private bankSearchTimer: ReturnType<typeof setTimeout> | null = null;
  selectedQuizId = '';
  loading = false;
  saving = false;
  busy = false;
  pageError = '';
  successMessage = '';
  formError = '';
  showQuizModal = false;
  showQuestionModal = false;
  editingQuizId: string | null = null;
  editingQuestionId: string | null = null;
  selectedQuestionIds = new Set<string>();
  questionOptions: QuestionOption[] = [];
  confirmOpen = false;
  confirmKey = '';
  private confirmAction: (() => void) | null = null;
  private timer: ReturnType<typeof setInterval> | null = null;

  readonly quizForm = new FormGroup({
    academicYearId: new FormControl('', Validators.required),
    classId: new FormControl('', Validators.required),
    sectionId: new FormControl(''),
    subjectId: new FormControl('', Validators.required),
    title: new FormControl('', Validators.required),
    instructions: new FormControl(''),
    durationMinutes: new FormControl(20, Validators.required),
    maxAttempts: new FormControl(1),
    startAt: new FormControl(''),
    endAt: new FormControl(''),
    passMarks: new FormControl<number | null>(null),
    shuffleQuestions: new FormControl(false),
    showResults: new FormControl(true),
  });

  readonly questionForm = new FormGroup({
    subjectId: new FormControl(''),
    classId: new FormControl(''),
    questionType: new FormControl('MCQ', Validators.required),
    prompt: new FormControl('', Validators.required),
    correctAnswer: new FormControl(''),
    marks: new FormControl(1, Validators.required),
    explanation: new FormControl(''),
  });

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef, private auth: AuthService) {}

  get canManage(): boolean {
    return this.auth.hasPermission('QUIZ_MANAGE');
  }

  get isStudent(): boolean {
    return this.auth.hasRole('STUDENT');
  }

  get filterSections(): SectionOption[] {
    return this.options?.classes.find((klass) => klass.id === this.classId)?.sections ?? [];
  }

  get filterSubjects(): SubjectOption[] {
    const klass = this.options?.classes.find((item) => item.id === this.classId);
    if (klass) {
      return klass.subjects;
    }
    const seen = new Set<string>();
    const subjects: SubjectOption[] = [];
    for (const item of this.options?.classes ?? []) {
      for (const subject of item.subjects) {
        if (!seen.has(subject.id)) {
          seen.add(subject.id);
          subjects.push(subject);
        }
      }
    }
    return subjects;
  }

  get bankSubjects(): SubjectOption[] {
    const klass = this.options?.classes.find((item) => item.id === this.bankClassId);
    if (klass) {
      return klass.subjects;
    }
    return this.filterSubjects;
  }

  get quizFormSections(): SectionOption[] {
    return this.options?.classes.find((klass) => klass.id === this.quizForm.value.classId)?.sections ?? [];
  }

  get quizFormSubjects(): SubjectOption[] {
    return this.options?.classes.find((klass) => klass.id === this.quizForm.value.classId)?.subjects ?? [];
  }

  get questionSubjects(): SubjectOption[] {
    const klass = this.options?.classes.find((item) => item.id === this.questionForm.value.classId);
    if (klass) {
      return klass.subjects;
    }
    return this.filterSubjects;
  }

  get resultStats(): { count: number; average: number; highest: number; lowest: number; passed: number; failed: number } | null {
    const scored = this.results.filter((row) => row.source !== 'NONE' && row.score != null);
    if (!scored.length) {
      return null;
    }
    const scores = scored.map((row) => Number(row.score));
    const sum = scores.reduce((acc, value) => acc + value, 0);
    return {
      count: scored.length,
      average: sum / scored.length,
      highest: Math.max(...scores),
      lowest: Math.min(...scores),
      passed: scored.filter((row) => row.passed).length,
      failed: scored.filter((row) => !row.passed).length,
    };
  }

  ngOnInit(): void {
    this.loadOptions();
  }

  ngOnDestroy(): void {
    this.clearTimer();
  }

  setTab(tab: PageTab): void {
    this.pageTab = tab;
    this.successMessage = '';
    this.pageError = '';
    if (tab === 'quizzes') {
      this.reloadQuizzes();
    } else if (tab === 'bank') {
      this.reloadQuestions();
    } else if (tab === 'offline') {
      this.reloadQuizChoices();
      if (this.selectedQuizId) {
        this.reloadOffline();
      }
    } else {
      this.reloadQuizChoices();
      if (this.selectedQuizId) {
        this.reloadResults();
      }
    }
    this.cdr.markForCheck();
  }

  onYearChange(event: Event): void {
    this.yearId = (event.target as HTMLSelectElement).value;
    this.quizPageIndex = 0;
    this.reloadQuizzes();
  }

  onClassFilter(event: Event): void {
    this.classId = (event.target as HTMLSelectElement).value;
    this.sectionId = '';
    this.quizPageIndex = 0;
    this.reloadQuizzes();
  }

  onSectionFilter(event: Event): void {
    this.sectionId = (event.target as HTMLSelectElement).value;
    this.quizPageIndex = 0;
    this.reloadQuizzes();
  }

  onSubjectFilter(event: Event): void {
    this.subjectId = (event.target as HTMLSelectElement).value;
    this.quizPageIndex = 0;
    this.reloadQuizzes();
  }

  onStatusFilter(event: Event): void {
    this.statusFilter = (event.target as HTMLSelectElement).value;
    this.quizPageIndex = 0;
    this.reloadQuizzes();
  }

  onQuizSearch(value: string): void {
    this.quizQuery = value;
    if (this.quizSearchTimer) {
      clearTimeout(this.quizSearchTimer);
    }
    this.quizSearchTimer = setTimeout(() => {
      this.quizPageIndex = 0;
      this.reloadQuizzes();
    }, 300);
  }

  onBankSearch(value: string): void {
    this.bankQuery = value;
    if (this.bankSearchTimer) {
      clearTimeout(this.bankSearchTimer);
    }
    this.bankSearchTimer = setTimeout(() => {
      this.bankPageIndex = 0;
      this.reloadQuestions();
    }, 300);
  }

  sortQuizzesBy(key: string): void {
    const next = toggleSort(this.quizSortKey, key, this.quizSortDirection);
    this.quizSortKey = next.key;
    this.quizSortDirection = next.direction;
    this.quizPageIndex = 0;
    this.reloadQuizzes();
  }

  sortQuestionsBy(key: string): void {
    const next = toggleSort(this.bankSortKey, key, this.bankSortDirection);
    this.bankSortKey = next.key;
    this.bankSortDirection = next.direction;
    this.bankPageIndex = 0;
    this.reloadQuestions();
  }

  quizIndicator(key: string): string {
    return sortIndicator(this.quizSortKey, key, this.quizSortDirection);
  }

  bankIndicator(key: string): string {
    return sortIndicator(this.bankSortKey, key, this.bankSortDirection);
  }

  onQuizPage(page: number): void {
    this.quizPageIndex = page;
    this.reloadQuizzes();
  }

  onQuizPageSize(size: number): void {
    this.quizPageSize = size;
    this.quizPageIndex = 0;
    this.reloadQuizzes();
  }

  onBankPage(page: number): void {
    this.bankPageIndex = page;
    this.reloadQuestions();
  }

  onBankPageSize(size: number): void {
    this.bankPageSize = size;
    this.bankPageIndex = 0;
    this.reloadQuestions();
  }

  onBankClass(event: Event): void {
    this.bankClassId = (event.target as HTMLSelectElement).value;
    this.bankPageIndex = 0;
    this.reloadQuestions();
  }

  onBankSubject(event: Event): void {
    this.bankSubjectId = (event.target as HTMLSelectElement).value;
    this.bankPageIndex = 0;
    this.reloadQuestions();
  }

  onBankType(event: Event): void {
    this.bankType = (event.target as HTMLSelectElement).value;
    this.bankPageIndex = 0;
    this.reloadQuestions();
  }

  onSelectedQuiz(event: Event): void {
    this.selectedQuizId = (event.target as HTMLSelectElement).value;
    if (this.pageTab === 'offline') {
      this.reloadOffline();
    } else {
      this.reloadResults();
    }
  }

  selectQuiz(quiz: QuizRow, tab: PageTab): void {
    this.selectedQuizId = quiz.id;
    this.setTab(tab);
  }

  canTake(quiz: QuizRow): boolean {
    return this.isStudent && quiz.status === 'PUBLISHED';
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      DRAFT: 'examinations.quizzesPage.draft',
      PUBLISHED: 'examinations.quizzesPage.published',
      CLOSED: 'examinations.quizzesPage.closed',
      ARCHIVED: 'examinations.quizzesPage.archived',
    };
    return map[status] || status;
  }

  statusClass(status: string): string {
    if (status === 'PUBLISHED') {
      return 'badge-success';
    }
    if (status === 'DRAFT') {
      return 'badge-warning';
    }
    if (status === 'CLOSED') {
      return 'badge-danger';
    }
    return '';
  }

  typeLabel(type: string): string {
    const map: Record<string, string> = {
      MCQ: 'examinations.quizzesPage.mcq',
      TRUE_FALSE: 'examinations.quizzesPage.trueFalse',
      SHORT_ANSWER: 'examinations.quizzesPage.shortAnswer',
    };
    return map[type] || type;
  }

  sourceLabel(source: string): string {
    const map: Record<string, string> = {
      ONLINE: 'examinations.quizzesPage.online',
      OFFLINE: 'examinations.quizzesPage.offlineSource',
      NONE: 'examinations.quizzesPage.none',
    };
    return map[source] || source;
  }

  formatNum(value: number | null | undefined): string {
    return value == null ? '—' : Number(value).toFixed(2).replace(/\.00$/, '');
  }

  openQuizModal(quiz?: QuizRow): void {
    this.editingQuizId = quiz?.id ?? null;
    this.formError = '';
    this.selectedQuestionIds = new Set((quiz?.items ?? []).map((item) => item.questionId));
    this.quizForm.reset({
      academicYearId: quiz?.academicYearId || this.yearId,
      classId: quiz?.classId || this.classId,
      sectionId: quiz?.sectionId || '',
      subjectId: quiz?.subjectId || '',
      title: quiz?.title || '',
      instructions: quiz?.instructions || '',
      durationMinutes: quiz?.durationMinutes ?? 20,
      maxAttempts: quiz?.maxAttempts ?? 1,
      startAt: this.toLocal(quiz?.startAt),
      endAt: this.toLocal(quiz?.endAt),
      passMarks: quiz?.passMarks ?? null,
      shuffleQuestions: quiz?.shuffleQuestions ?? false,
      showResults: quiz?.showResults ?? true,
    });
    if (!this.questions.length) {
      this.reloadQuestions();
    }
    if (quiz && !quiz.items) {
      this.http.get<ApiResponse<QuizRow>>(`/api/quizzes/${quiz.id}`).subscribe({
        next: (res) => {
          this.selectedQuestionIds = new Set((res.data.items ?? []).map((item) => item.questionId));
          this.cdr.markForCheck();
        },
        error: (err) => this.failForm(err),
      });
    }
    this.showQuizModal = true;
    this.cdr.markForCheck();
  }

  closeQuizModal(): void {
    this.showQuizModal = false;
    this.formError = '';
    this.cdr.markForCheck();
  }

  onQuizClassChange(): void {
    this.quizForm.patchValue({ sectionId: '', subjectId: '' });
  }

  toggleQuestion(id: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    if (checked) {
      this.selectedQuestionIds.add(id);
    } else {
      this.selectedQuestionIds.delete(id);
    }
    this.selectedQuestionIds = new Set(this.selectedQuestionIds);
    this.cdr.markForCheck();
  }

  saveQuiz(): void {
    if (this.quizForm.invalid || this.saving) {
      return;
    }
    this.saving = true;
    this.formError = '';
    const value = this.quizForm.value;
    const body = {
      academicYearId: value.academicYearId,
      classId: value.classId,
      sectionId: value.sectionId || null,
      subjectId: value.subjectId,
      title: value.title,
      instructions: value.instructions || null,
      durationMinutes: Number(value.durationMinutes),
      maxAttempts: Number(value.maxAttempts) || 1,
      startAt: value.startAt ? new Date(value.startAt).toISOString() : null,
      endAt: value.endAt ? new Date(value.endAt).toISOString() : null,
      passMarks: value.passMarks == null || value.passMarks === ('' as unknown) ? null : Number(value.passMarks),
      shuffleQuestions: !!value.shuffleQuestions,
      showResults: value.showResults !== false,
      items: [...this.selectedQuestionIds].map((questionId, index) => ({ questionId, sortOrder: index + 1 })),
    };
    const req$ = this.editingQuizId
      ? this.http.put<ApiResponse<QuizRow>>(`/api/quizzes/${this.editingQuizId}`, body)
      : this.http.post<ApiResponse<QuizRow>>('/api/quizzes', body);
    req$.subscribe({
      next: () => {
        this.saving = false;
        this.showQuizModal = false;
        this.successMessage = this.editingQuizId
          ? 'examinations.quizzesPage.updated'
          : 'examinations.quizzesPage.created';
        this.reloadQuizzes();
      },
      error: (err) => this.failForm(err),
    });
  }

  askPublish(quiz: QuizRow): void {
    this.openConfirm('examinations.quizzesPage.publishConfirm', () => this.setQuizStatus(quiz, 'PUBLISHED'));
  }

  setQuizStatus(quiz: QuizRow, status: string): void {
    this.saving = true;
    this.http.put<ApiResponse<QuizRow>>(`/api/quizzes/${quiz.id}/status`, { status }).subscribe({
      next: () => {
        this.saving = false;
        this.confirmOpen = false;
        this.successMessage = status === 'PUBLISHED'
          ? 'examinations.quizzesPage.publishedSuccess'
          : 'examinations.quizzesPage.closedSuccess';
        this.reloadQuizzes();
      },
      error: (err) => this.failPage(err),
    });
  }

  askDeleteQuiz(quiz: QuizRow): void {
    this.openConfirm('examinations.quizzesPage.deleteQuizConfirm', () => {
      this.saving = true;
      this.http.delete<ApiResponse<void>>(`/api/quizzes/${quiz.id}`).subscribe({
        next: () => {
          this.saving = false;
          this.confirmOpen = false;
          this.successMessage = 'examinations.quizzesPage.deleted';
          this.reloadQuizzes();
        },
        error: (err) => this.failPage(err),
      });
    });
  }

  openQuestionModal(question?: QuizQuestion): void {
    this.editingQuestionId = question?.id ?? null;
    this.formError = '';
    this.questionForm.reset({
      subjectId: question?.subjectId || this.bankSubjectId,
      classId: question?.classId || this.bankClassId,
      questionType: question?.questionType || 'MCQ',
      prompt: question?.prompt || '',
      correctAnswer: question?.correctAnswer || '',
      marks: question?.marks ?? 1,
      explanation: question?.explanation || '',
    });
    this.questionOptions = question?.options?.length
      ? question.options.map((option) => ({ ...option }))
      : this.defaultOptions(question?.questionType || 'MCQ');
    this.showQuestionModal = true;
    this.cdr.markForCheck();
  }

  closeQuestionModal(): void {
    this.showQuestionModal = false;
    this.formError = '';
    this.cdr.markForCheck();
  }

  onQuestionTypeChange(): void {
    const type = this.questionForm.value.questionType || 'MCQ';
    this.questionOptions = this.defaultOptions(type);
    this.questionForm.patchValue({ correctAnswer: type === 'TRUE_FALSE' ? 'TRUE' : '' });
  }

  addOption(): void {
    const next = String.fromCharCode(65 + this.questionOptions.length);
    this.questionOptions = [...this.questionOptions, { key: next, text: '' }];
    this.cdr.markForCheck();
  }

  onOptionKey(index: number, event: Event): void {
    this.questionOptions[index] = { ...this.questionOptions[index], key: (event.target as HTMLInputElement).value };
  }

  onOptionText(index: number, event: Event): void {
    this.questionOptions[index] = { ...this.questionOptions[index], text: (event.target as HTMLInputElement).value };
  }

  setCorrectAnswer(key: string): void {
    this.questionForm.patchValue({ correctAnswer: key });
    this.cdr.markForCheck();
  }

  saveQuestion(): void {
    if (this.questionForm.invalid || this.saving) {
      return;
    }
    this.saving = true;
    this.formError = '';
    const value = this.questionForm.value;
    const type = value.questionType || 'MCQ';
    const body = {
      subjectId: value.subjectId || null,
      classId: value.classId || null,
      questionType: type,
      prompt: value.prompt,
      options: type === 'SHORT_ANSWER' ? null : this.questionOptions,
      correctAnswer: value.correctAnswer || null,
      marks: Number(value.marks),
      explanation: value.explanation || null,
    };
    const req$ = this.editingQuestionId
      ? this.http.put<ApiResponse<QuizQuestion>>(`/api/quizzes/questions/${this.editingQuestionId}`, body)
      : this.http.post<ApiResponse<QuizQuestion>>('/api/quizzes/questions', body);
    req$.subscribe({
      next: () => {
        this.saving = false;
        this.showQuestionModal = false;
        this.successMessage = this.editingQuestionId
          ? 'examinations.quizzesPage.questionUpdated'
          : 'examinations.quizzesPage.questionCreated';
        this.reloadQuestions();
      },
      error: (err) => this.failForm(err),
    });
  }

  askDeleteQuestion(question: QuizQuestion): void {
    this.openConfirm('examinations.quizzesPage.deleteQuestionConfirm', () => {
      this.saving = true;
      this.http.delete<ApiResponse<void>>(`/api/quizzes/questions/${question.id}`).subscribe({
        next: () => {
          this.saving = false;
          this.confirmOpen = false;
          this.successMessage = 'examinations.quizzesPage.questionDeleted';
          this.reloadQuestions();
        },
        error: (err) => this.failPage(err),
      });
    });
  }

  startQuiz(quiz: QuizRow): void {
    this.saving = true;
    this.pageError = '';
    this.http.post<ApiResponse<QuizTake>>(`/api/quizzes/${quiz.id}/attempts`, {}).subscribe({
      next: (res) => {
        this.saving = false;
        this.take = res.data;
        this.responses = {};
        for (const question of res.data.questions ?? []) {
          this.responses[question.questionId] = question.response || '';
        }
        this.startTimer();
        this.cdr.markForCheck();
      },
      error: (err) => this.failPage(err),
    });
  }

  onResponse(questionId: string, event: Event): void {
    this.setResponse(questionId, (event.target as HTMLTextAreaElement).value);
  }

  setResponse(questionId: string, value: string): void {
    this.responses[questionId] = value;
    this.cdr.markForCheck();
  }

  saveProgress(): void {
    if (!this.take) {
      return;
    }
    this.saving = true;
    this.http.put<ApiResponse<QuizAttempt>>(`/api/quizzes/attempts/${this.take.attemptId}`, this.answerBody())
      .subscribe({
        next: () => {
          this.saving = false;
          this.formError = '';
          this.cdr.markForCheck();
        },
        error: (err) => this.failForm(err),
      });
  }

  submitQuiz(): void {
    if (!this.take) {
      return;
    }
    this.saving = true;
    this.http.post<ApiResponse<QuizAttempt>>(`/api/quizzes/attempts/${this.take.attemptId}/submit`, this.answerBody())
      .subscribe({
        next: (res) => {
          this.saving = false;
          this.take = null;
          this.clearTimer();
          this.successMessage = 'examinations.quizzesPage.submitted';
          this.gradeAttempt = res.data;
          this.cdr.markForCheck();
        },
        error: (err) => this.failForm(err),
      });
  }

  openAttempt(attemptId: string): void {
    this.http.get<ApiResponse<QuizAttempt>>(`/api/quizzes/attempts/${attemptId}`).subscribe({
      next: (res) => {
        this.gradeAttempt = res.data;
        this.cdr.markForCheck();
      },
      error: (err) => this.failPage(err),
    });
  }

  closeGrade(): void {
    this.gradeAttempt = null;
    this.formError = '';
    this.cdr.markForCheck();
  }

  onAwarded(answer: QuizAnswer, event: Event): void {
    const raw = (event.target as HTMLInputElement).value;
    answer.awardedMarks = raw === '' ? null : Number(raw);
  }

  onComment(answer: QuizAnswer, event: Event): void {
    answer.graderComment = (event.target as HTMLInputElement).value;
  }

  saveGrade(): void {
    if (!this.gradeAttempt) {
      return;
    }
    this.saving = true;
    const answers = (this.gradeAttempt.answers ?? [])
      .filter((answer) => answer.questionType === 'SHORT_ANSWER')
      .map((answer) => ({
        questionId: answer.questionId,
        awardedMarks: answer.awardedMarks,
        graderComment: answer.graderComment || null,
      }));
    this.http.post<ApiResponse<QuizAttempt>>(`/api/quizzes/attempts/${this.gradeAttempt.id}/grade`, { answers })
      .subscribe({
        next: (res) => {
          this.saving = false;
          this.gradeAttempt = res.data;
          this.successMessage = 'examinations.quizzesPage.graded';
          this.reloadResults();
        },
        error: (err) => this.failForm(err),
      });
  }

  onOfflineMarks(row: OfflineRow, event: Event): void {
    const raw = (event.target as HTMLInputElement).value;
    row.marks = raw === '' ? null : Number(raw);
  }

  onOfflineRemarks(row: OfflineRow, event: Event): void {
    row.remarks = (event.target as HTMLInputElement).value;
  }

  saveOffline(): void {
    if (!this.offlineGrid) {
      return;
    }
    this.busy = true;
    const body = {
      rows: this.offlineGrid.rows.map((row) => ({
        studentId: row.studentId,
        marks: row.marks,
        remarks: row.remarks || null,
      })),
    };
    this.http.post<ApiResponse<OfflineGrid>>(`/api/quizzes/${this.offlineGrid.quizId}/offline`, body).subscribe({
      next: (res) => {
        this.busy = false;
        this.offlineGrid = res.data;
        this.successMessage = 'examinations.quizzesPage.offlineSaved';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.busy = false;
        this.failPage(err);
      },
    });
  }

  downloadResultsPdf(): void {
    if (!this.selectedQuizId) {
      return;
    }
    this.downloadBlob(`/api/quizzes/${this.selectedQuizId}/results/pdf`, 'quiz-results.pdf');
  }

  downloadAttemptPdf(attemptId: string): void {
    this.downloadBlob(`/api/quizzes/attempts/${attemptId}/pdf`, 'quiz-result.pdf');
  }

  openConfirm(key: string, action: () => void): void {
    this.confirmKey = key;
    this.confirmAction = action;
    this.confirmOpen = true;
    this.cdr.markForCheck();
  }

  closeConfirm(): void {
    this.confirmOpen = false;
    this.confirmAction = null;
    this.cdr.markForCheck();
  }

  runConfirm(): void {
    this.confirmAction?.();
  }

  private loadOptions(): void {
    this.http.get<ApiResponse<QuizOptions>>('/api/quizzes/options').subscribe({
      next: (res) => {
        this.options = res.data;
        this.yearId = res.data.academicYears.find((year) => year.current)?.id ?? res.data.academicYears[0]?.id ?? '';
        this.reloadQuizzes();
        if (this.canManage) {
          this.reloadQuestions();
        }
      },
      error: (err) => this.failPage(err),
    });
  }

  private quizFilterParams(): HttpParams {
    let params = new HttpParams();
    if (this.yearId) {
      params = params.set('academicYearId', this.yearId);
    }
    if (this.classId) {
      params = params.set('classId', this.classId);
    }
    if (this.sectionId) {
      params = params.set('sectionId', this.sectionId);
    }
    if (this.subjectId) {
      params = params.set('subjectId', this.subjectId);
    }
    if (this.statusFilter) {
      params = params.set('status', this.statusFilter);
    }
    const childId = this.auth.activeChild?.studentId;
    if (this.auth.hasRole('PARENT') && childId) {
      params = params.set('studentId', childId);
    }
    return params;
  }

  private reloadQuizzes(): void {
    this.loading = true;
    this.pageError = '';
    let params = this.quizFilterParams()
      .set('page', String(this.quizPageIndex))
      .set('size', String(this.quizPageSize))
      .set('sort', this.quizSortKey)
      .set('direction', this.quizSortDirection);
    if (this.quizQuery.trim()) {
      params = params.set('query', this.quizQuery.trim());
    }
    this.http.get<ApiResponse<PagedResponse<QuizRow>>>('/api/quizzes', { params }).subscribe({
      next: (res) => {
        this.quizzes = res.data?.content ?? [];
        this.quizTotalElements = res.data?.totalElements ?? 0;
        this.quizTotalPages = res.data?.totalPages ?? 0;
        this.loading = false;
        if (!this.selectedQuizId && this.quizzes[0]) {
          this.selectedQuizId = this.quizzes[0].id;
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loading = false;
        this.failPage(err);
      },
    });
  }

  private reloadQuizChoices(): void {
    this.http.get<ApiResponse<QuizRow[]>>('/api/quizzes', { params: this.quizFilterParams() }).subscribe({
      next: (res) => {
        this.quizChoices = res.data ?? [];
        if (!this.selectedQuizId && this.quizChoices[0]) {
          this.selectedQuizId = this.quizChoices[0].id;
        }
        this.cdr.markForCheck();
      },
      error: (err) => this.failPage(err),
    });
  }

  private reloadQuestions(): void {
    this.loading = true;
    let params = new HttpParams()
      .set('page', String(this.bankPageIndex))
      .set('size', String(this.bankPageSize))
      .set('sort', this.bankSortKey)
      .set('direction', this.bankSortDirection);
    if (this.bankClassId) {
      params = params.set('classId', this.bankClassId);
    }
    if (this.bankSubjectId) {
      params = params.set('subjectId', this.bankSubjectId);
    }
    if (this.bankType) {
      params = params.set('questionType', this.bankType);
    }
    if (this.bankQuery.trim()) {
      params = params.set('query', this.bankQuery.trim());
    }
    this.http.get<ApiResponse<PagedResponse<QuizQuestion>>>('/api/quizzes/questions', { params }).subscribe({
      next: (res) => {
        this.questions = res.data?.content ?? [];
        this.bankTotalElements = res.data?.totalElements ?? 0;
        this.bankTotalPages = res.data?.totalPages ?? 0;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loading = false;
        this.failPage(err);
      },
    });
  }

  private reloadOffline(): void {
    if (!this.selectedQuizId) {
      this.offlineGrid = null;
      this.cdr.markForCheck();
      return;
    }
    this.loading = true;
    this.http.get<ApiResponse<OfflineGrid>>(`/api/quizzes/${this.selectedQuizId}/offline`).subscribe({
      next: (res) => {
        this.offlineGrid = res.data;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loading = false;
        this.failPage(err);
      },
    });
  }

  private reloadResults(): void {
    if (!this.selectedQuizId) {
      this.results = [];
      this.cdr.markForCheck();
      return;
    }
    this.loading = true;
    this.http.get<ApiResponse<ResultRow[]>>(`/api/quizzes/${this.selectedQuizId}/results`).subscribe({
      next: (res) => {
        this.results = res.data ?? [];
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loading = false;
        this.failPage(err);
      },
    });
  }

  private answerBody() {
    return {
      answers: Object.entries(this.responses).map(([questionId, response]) => ({ questionId, response })),
    };
  }

  private defaultOptions(type: string): QuestionOption[] {
    if (type === 'TRUE_FALSE') {
      return [
        { key: 'TRUE', text: 'True' },
        { key: 'FALSE', text: 'False' },
      ];
    }
    return [
      { key: 'A', text: '' },
      { key: 'B', text: '' },
      { key: 'C', text: '' },
      { key: 'D', text: '' },
    ];
  }

  private toLocal(value: string | null | undefined): string {
    if (!value) {
      return '';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '';
    }
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  private startTimer(): void {
    this.clearTimer();
    this.tick();
    this.timer = setInterval(() => this.tick(), 1000);
  }

  private tick(): void {
    if (!this.take) {
      return;
    }
    const remaining = new Date(this.take.expiresAt).getTime() - Date.now();
    if (remaining <= 0) {
      this.timeLeft = '0:00';
      this.clearTimer();
      this.submitQuiz();
      return;
    }
    const total = Math.floor(remaining / 1000);
    const minutes = Math.floor(total / 60);
    const seconds = total % 60;
    this.timeLeft = `${minutes}:${String(seconds).padStart(2, '0')}`;
    this.cdr.markForCheck();
  }

  private clearTimer(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  private downloadBlob(url: string, filename: string): void {
    this.busy = true;
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const objectUrl = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = objectUrl;
        link.download = filename;
        link.click();
        URL.revokeObjectURL(objectUrl);
        this.busy = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.busy = false;
        this.failPage(err);
      },
    });
  }

  private failForm(err: HttpErrorResponse): void {
    this.saving = false;
    const apiError = err.error as ApiError | undefined;
    this.formError = apiError?.message || 'Could not save. Please try again.';
    this.cdr.markForCheck();
  }

  private failPage(err: HttpErrorResponse): void {
    this.saving = false;
    this.busy = false;
    const apiError = err.error as ApiError | undefined;
    this.pageError = apiError?.message || 'Could not complete the request. Please try again.';
    this.cdr.markForCheck();
  }
}
