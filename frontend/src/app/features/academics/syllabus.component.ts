import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { ApiError, ApiResponse } from '../../core/models/api.model';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';

interface AcademicYear {
  id: string;
  name: string;
  current: boolean;
}

interface SchoolSection {
  id: string;
  name: string;
}

interface SchoolClass {
  id: string;
  name: string;
  code: string;
  sections: SchoolSection[];
}

interface SubjectOption {
  id: string;
  name: string;
  code: string;
}

interface TeacherOption {
  id: string;
  firstName: string;
  lastName: string;
  displayName: string;
  status: string;
}

interface SyllabusChapter {
  id: string;
  unitId: string;
  title: string;
  description: string | null;
  sortOrder: number;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
  targetCompletionDate: string | null;
  completedAt: string | null;
  overdue: boolean;
}

interface SyllabusUnit {
  id: string;
  title: string;
  description: string | null;
  sortOrder: number;
  targetCompletionDate: string | null;
  totalChapters: number;
  completedChapters: number;
  completionPercent: number;
  overdue: boolean;
  chapters: SyllabusChapter[];
}

interface SyllabusMaterial {
  id: string;
  unitId: string | null;
  kind: 'OUTLINE' | 'DOCUMENT' | 'LINK';
  title: string;
  fileName: string | null;
  contentType: string | null;
  fileSize: number | null;
  externalUrl: string | null;
  downloadable: boolean;
  createdAt: string;
}

interface SyllabusCourse {
  id: string;
  academicYearId: string;
  academicYearName: string | null;
  classId: string;
  className: string | null;
  sectionId: string | null;
  sectionName: string | null;
  subjectId: string;
  subjectName: string | null;
  teacherId: string | null;
  teacherName: string | null;
  title: string;
  description: string | null;
  targetCompletionDate: string | null;
  status: string;
  progressStatus: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
  totalUnits: number;
  completedUnits: number;
  totalChapters: number;
  completedChapters: number;
  inProgressChapters: number;
  remainingChapters: number;
  completionPercent: number;
  overdue: boolean;
}

interface SyllabusDetail {
  course: SyllabusCourse;
  units: SyllabusUnit[];
  materials: SyllabusMaterial[];
}

@Component({
  selector: 'app-academics-syllabus',
  imports: [TranslateModule, ReactiveFormsModule, EmptyStateComponent, DatePipe],
  template: `
    <div class="tab-page">
      <div class="page-header">
        <div>
          <h2>{{ 'academics.syllabusPage.title' | translate }}</h2>
          <p class="muted">{{ 'academics.syllabusPage.subtitle' | translate }}</p>
        </div>
        @if (canManage) {
          <div class="header-actions">
            <button class="btn btn-primary" type="button" (click)="openCourseModal()">
              {{ 'academics.syllabus.addCourse' | translate }}
            </button>
          </div>
        }
      </div>

      @if (successMessage) {
        <p class="banner success">{{ successMessage | translate }}</p>
      }
      @if (pageError) {
        <p class="banner error">{{ pageError }}</p>
      }

      <div class="filters card">
        <select [value]="yearId" (change)="onYearChange($event)" [attr.aria-label]="'academics.syllabus.academicYear' | translate">
          <option value="">{{ 'academics.syllabus.allYears' | translate }}</option>
          @for (year of years; track year.id) {
            <option [value]="year.id">{{ year.name }}</option>
          }
        </select>
        <select [value]="classId" (change)="onClassChange($event)" [attr.aria-label]="'academics.syllabus.classLabel' | translate">
          <option value="">{{ 'academics.syllabus.allClasses' | translate }}</option>
          @for (klass of classes; track klass.id) {
            <option [value]="klass.id">{{ klass.name }}</option>
          }
        </select>
        <select [value]="sectionId" (change)="onSectionChange($event)" [attr.aria-label]="'academics.syllabus.sectionLabel' | translate">
          <option value="">{{ 'academics.syllabus.allSections' | translate }}</option>
          @for (section of availableSections; track section.id) {
            <option [value]="section.id">{{ section.name }}</option>
          }
        </select>
        <select [value]="subjectId" (change)="onSubjectChange($event)" [attr.aria-label]="'academics.syllabus.subjectLabel' | translate">
          <option value="">{{ 'academics.syllabus.allSubjects' | translate }}</option>
          @for (subject of subjects; track subject.id) {
            <option [value]="subject.id">{{ subject.name }}</option>
          }
        </select>
        <select [value]="teacherId" (change)="onTeacherChange($event)" [attr.aria-label]="'academics.syllabus.teacherLabel' | translate">
          <option value="">{{ 'academics.syllabus.allTeachers' | translate }}</option>
          @for (teacher of teachers; track teacher.id) {
            <option [value]="teacher.id">{{ teacher.displayName }}</option>
          }
        </select>
        <select [value]="progressStatus" (change)="onStatusChange($event)" [attr.aria-label]="'academics.syllabus.progress' | translate">
          <option value="">{{ 'academics.syllabus.allStatuses' | translate }}</option>
          <option value="NOT_STARTED">{{ 'academics.syllabus.statusNotStarted' | translate }}</option>
          <option value="IN_PROGRESS">{{ 'academics.syllabus.statusInProgress' | translate }}</option>
          <option value="COMPLETED">{{ 'academics.syllabus.statusCompleted' | translate }}</option>
        </select>
        <input type="search" [value]="search" (input)="onSearch($event)"
               [placeholder]="'academics.syllabus.searchPlaceholder' | translate" />
        <button class="btn btn-ghost" type="button" (click)="clearFilters()">
          {{ 'academics.syllabus.clearFilters' | translate }}
        </button>
      </div>

      <div class="layout">
        <section class="course-list">
          @if (loading) {
            <div class="card loading-block">{{ 'common.loading' | translate }}</div>
          } @else if (courses.length === 0) {
            <div class="card">
              <app-empty-state icon="menu_book"
                [title]="'academics.syllabus.noCourses' | translate"
                [hint]="'academics.syllabus.noCoursesHint' | translate">
                @if (canManage) {
                  <button class="btn btn-primary" type="button" (click)="openCourseModal()">
                    {{ 'academics.syllabus.addCourse' | translate }}
                  </button>
                }
              </app-empty-state>
            </div>
          } @else {
            @for (course of courses; track course.id) {
              <button class="card course-card" type="button"
                      [class.selected]="selectedId === course.id"
                      (click)="selectCourse(course.id)">
                <div class="course-head">
                  <div class="course-title">
                    <span class="strong">{{ course.title }}</span>
                    <span class="muted small">{{ scopeLabel(course) }}</span>
                  </div>
                  <span class="badge" [class.badge-success]="course.progressStatus === 'COMPLETED'"
                        [class.badge-warn]="course.progressStatus === 'IN_PROGRESS'"
                        [class.badge-muted]="course.progressStatus === 'NOT_STARTED'">
                    {{ progressLabel(course.progressStatus) | translate }}
                  </span>
                </div>
                <div class="progress" [attr.aria-label]="'academics.syllabus.progress' | translate">
                  <span class="progress-bar" [class.complete]="course.progressStatus === 'COMPLETED'"
                        [class.danger]="course.overdue"
                        [style.width.%]="course.completionPercent"></span>
                </div>
                <div class="course-foot">
                  <span class="muted small">
                    {{ course.completedChapters }}/{{ course.totalChapters }}
                    {{ 'academics.syllabus.chapters' | translate }} -
                    {{ course.completionPercent }}%
                  </span>
                  @if (course.targetCompletionDate) {
                    <span class="small" [class.overdue]="course.overdue">
                      {{ 'academics.syllabus.targetShort' | translate }}
                      {{ course.targetCompletionDate | date: 'mediumDate' }}
                    </span>
                  }
                </div>
              </button>
            }
          }
        </section>

        <section class="detail card">
          @if (detailLoading) {
            <p class="muted detail-placeholder">{{ 'common.loading' | translate }}</p>
          } @else if (!detail) {
            <div class="detail-placeholder">
              <app-empty-state icon="fact_check"
                [title]="'academics.syllabus.selectCourse' | translate"
                [hint]="'academics.syllabus.selectCourseHint' | translate" />
            </div>
          } @else {
            <header class="detail-head">
              <div>
                <h3>{{ detail.course.title }}</h3>
                <p class="muted small">{{ scopeLabel(detail.course) }}</p>
                @if (detail.course.description) {
                  <p class="detail-desc">{{ detail.course.description }}</p>
                }
                <div class="detail-meta">
                  @if (detail.course.teacherName) {
                    <span class="chip">{{ detail.course.teacherName }}</span>
                  }
                  @if (detail.course.targetCompletionDate) {
                    <span class="chip" [class.overdue]="detail.course.overdue">
                      {{ 'academics.syllabus.targetCompletion' | translate }}:
                      {{ detail.course.targetCompletionDate | date: 'mediumDate' }}
                    </span>
                  }
                  <span class="chip">{{ detail.course.completedChapters }}/{{ detail.course.totalChapters }} {{ 'academics.syllabus.chapters' | translate }}</span>
                  <span class="chip">{{ detail.course.completionPercent }}%</span>
                </div>
              </div>
              @if (canManage) {
                <div class="row-actions">
                  <button class="btn btn-ghost btn-sm" type="button" (click)="openCourseModal(detail.course)">
                    {{ 'common.edit' | translate }}
                  </button>
                  <button class="btn btn-danger btn-sm" type="button" (click)="askDeleteCourse()">
                    {{ 'common.delete' | translate }}
                  </button>
                </div>
              }
            </header>

            <div class="progress large">
              <span class="progress-bar" [class.complete]="detail.course.progressStatus === 'COMPLETED'"
                    [class.danger]="detail.course.overdue"
                    [style.width.%]="detail.course.completionPercent"></span>
            </div>

            <div class="section-head">
              <h4>{{ 'academics.syllabus.units' | translate }}</h4>
              @if (canManage) {
                <button class="btn btn-ghost btn-sm" type="button" (click)="openUnitModal()">
                  {{ 'academics.syllabus.addUnit' | translate }}
                </button>
              }
            </div>

            @if (detail.units.length === 0) {
              <p class="muted empty-line">{{ 'academics.syllabus.noUnits' | translate }}</p>
            } @else {
              <div class="unit-list">
                @for (unit of detail.units; track unit.id) {
                  <article class="unit">
                    <header class="unit-head">
                      <div class="unit-title">
                        <strong>{{ unit.sortOrder }}. {{ unit.title }}</strong>
                        <span class="muted small">
                          {{ unit.completedChapters }}/{{ unit.totalChapters }}
                          {{ 'academics.syllabus.chapters' | translate }} - {{ unit.completionPercent }}%
                        </span>
                      </div>
                      <div class="row-actions">
                        @if (canManage) {
                          <button class="btn btn-ghost btn-sm" type="button" (click)="openChapterModal(unit)">
                            {{ 'academics.syllabus.addChapter' | translate }}
                          </button>
                          <button class="btn btn-ghost btn-sm" type="button" (click)="openUnitModal(unit)">
                            {{ 'common.edit' | translate }}
                          </button>
                          <button class="btn btn-danger btn-sm" type="button" (click)="askDeleteUnit(unit)">
                            {{ 'common.delete' | translate }}
                          </button>
                        }
                      </div>
                    </header>
                    @if (unit.description) {
                      <p class="muted small unit-desc">{{ unit.description }}</p>
                    }
                    <div class="progress">
                      <span class="progress-bar" [class.complete]="unit.completionPercent === 100"
                            [class.danger]="unit.overdue"
                            [style.width.%]="unit.completionPercent"></span>
                    </div>
                    @if (unit.chapters.length === 0) {
                      <p class="muted small empty-line">{{ 'academics.syllabus.noChapters' | translate }}</p>
                    } @else {
                      <ul class="chapter-list">
                        @for (chapter of unit.chapters; track chapter.id) {
                          <li class="chapter" [class.done]="chapter.status === 'COMPLETED'">
                            <div class="chapter-main">
                              <span class="chapter-dot" [attr.data-status]="chapter.status"></span>
                              <div>
                                <span class="chapter-title">{{ chapter.title }}</span>
                                @if (chapter.targetCompletionDate) {
                                  <span class="muted small">
                                    - {{ chapter.targetCompletionDate | date: 'mediumDate' }}
                                  </span>
                                }
                                @if (chapter.description) {
                                  <p class="muted small chapter-desc">{{ chapter.description }}</p>
                                }
                              </div>
                            </div>
                            <div class="chapter-actions">
                              @if (canManage) {
                                <select class="status-select" [value]="chapter.status"
                                        (change)="setChapterStatus(chapter, $event)">
                                  <option value="PENDING">{{ 'academics.syllabus.statusPending' | translate }}</option>
                                  <option value="IN_PROGRESS">{{ 'academics.syllabus.statusInProgress' | translate }}</option>
                                  <option value="COMPLETED">{{ 'academics.syllabus.statusCompleted' | translate }}</option>
                                </select>
                                <button class="btn btn-ghost btn-sm" type="button" (click)="openChapterModal(unit, chapter)">
                                  {{ 'common.edit' | translate }}
                                </button>
                                <button class="btn btn-danger btn-sm" type="button" (click)="askDeleteChapter(chapter)">
                                  {{ 'common.delete' | translate }}
                                </button>
                              } @else {
                                <span class="badge" [class.badge-success]="chapter.status === 'COMPLETED'"
                                      [class.badge-warn]="chapter.status === 'IN_PROGRESS'"
                                      [class.badge-muted]="chapter.status === 'PENDING'">
                                  {{ chapterStatusLabel(chapter.status) | translate }}
                                </span>
                              }
                            </div>
                          </li>
                        }
                      </ul>
                    }
                  </article>
                }
              </div>
            }

            <div class="section-head">
              <h4>{{ 'academics.syllabus.materials' | translate }}</h4>
              @if (canManage) {
                <button class="btn btn-ghost btn-sm" type="button" (click)="openMaterialModal()">
                  {{ 'academics.syllabus.addMaterial' | translate }}
                </button>
              }
            </div>

            @if (detail.materials.length === 0) {
              <p class="muted empty-line">{{ 'academics.syllabus.noMaterials' | translate }}</p>
            } @else {
              <ul class="material-list">
                @for (material of detail.materials; track material.id) {
                  <li class="material">
                    <div>
                      <span class="strong">{{ material.title }}</span>
                      <span class="badge badge-muted kind">{{ kindLabel(material.kind) | translate }}</span>
                      @if (material.fileName) {
                        <span class="muted small"> - {{ material.fileName }} ({{ formatSize(material.fileSize) }})</span>
                      }
                    </div>
                    <div class="row-actions">
                      @if (material.downloadable) {
                        <button class="btn btn-ghost btn-sm" type="button" (click)="downloadMaterial(material)">
                          {{ 'academics.syllabus.download' | translate }}
                        </button>
                      }
                      @if (material.externalUrl) {
                        <a class="btn btn-ghost btn-sm" [href]="material.externalUrl" target="_blank" rel="noopener noreferrer">
                          {{ 'academics.syllabus.openLink' | translate }}
                        </a>
                      }
                      @if (canManage) {
                        <button class="btn btn-danger btn-sm" type="button" (click)="askDeleteMaterial(material)">
                          {{ 'common.delete' | translate }}
                        </button>
                      }
                    </div>
                  </li>
                }
              </ul>
            }
          }
        </section>
      </div>

      @if (courseModalOpen) {
        <div class="modal-backdrop" (click)="closeModals()">
          <div class="modal" (click)="$event.stopPropagation()">
            <h2>{{ (editingCourseId ? 'academics.syllabus.editCourse' : 'academics.syllabus.addCourse') | translate }}</h2>
            @if (formError) {
              <p class="form-error">{{ formError }}</p>
            }
            <form [formGroup]="courseForm" (ngSubmit)="saveCourse()">
              <div class="form-grid">
                <div class="field">
                  <label for="syllabus-year">{{ 'academics.syllabus.academicYear' | translate }}</label>
                  <select id="syllabus-year" formControlName="academicYearId">
                    @for (year of years; track year.id) {
                      <option [value]="year.id">{{ year.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label for="syllabus-class">{{ 'academics.syllabus.classLabel' | translate }}</label>
                  <select id="syllabus-class" formControlName="classId">
                    @for (klass of classes; track klass.id) {
                      <option [value]="klass.id">{{ klass.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label for="syllabus-section">{{ 'academics.syllabus.sectionLabel' | translate }}</label>
                  <select id="syllabus-section" formControlName="sectionId">
                    <option value="">{{ 'academics.syllabus.allSections' | translate }}</option>
                    @for (section of formSections; track section.id) {
                      <option [value]="section.id">{{ section.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label for="syllabus-subject">{{ 'academics.syllabus.subjectLabel' | translate }}</label>
                  <select id="syllabus-subject" formControlName="subjectId">
                    @for (subject of subjects; track subject.id) {
                      <option [value]="subject.id">{{ subject.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label for="syllabus-teacher">{{ 'academics.syllabus.teacherLabel' | translate }}</label>
                  <select id="syllabus-teacher" formControlName="teacherId">
                    <option value="">{{ 'academics.syllabus.noTeacher' | translate }}</option>
                    @for (teacher of teachers; track teacher.id) {
                      <option [value]="teacher.id">{{ teacher.displayName }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label for="syllabus-target">{{ 'academics.syllabus.targetCompletion' | translate }}</label>
                  <input id="syllabus-target" type="date" formControlName="targetCompletionDate" />
                </div>
                <div class="field span-2">
                  <label for="syllabus-title">{{ 'academics.syllabus.courseName' | translate }}</label>
                  <input id="syllabus-title" type="text" formControlName="title" />
                </div>
                <div class="field span-2">
                  <label for="syllabus-description">{{ 'academics.syllabus.description' | translate }}</label>
                  <textarea id="syllabus-description" rows="3" formControlName="description"></textarea>
                </div>
                <div class="field">
                  <label for="syllabus-status">{{ 'common.status' | translate }}</label>
                  <select id="syllabus-status" formControlName="status">
                    <option value="ACTIVE">{{ 'academics.syllabus.courseActive' | translate }}</option>
                    <option value="ARCHIVED">{{ 'academics.syllabus.courseArchived' | translate }}</option>
                  </select>
                </div>
              </div>
              <div class="form-actions">
                <button class="btn btn-ghost" type="button" (click)="closeModals()">{{ 'common.cancel' | translate }}</button>
                <button class="btn btn-primary" type="submit" [disabled]="courseForm.invalid || saving">
                  {{ 'common.save' | translate }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      @if (unitModalOpen) {
        <div class="modal-backdrop" (click)="closeModals()">
          <div class="modal" (click)="$event.stopPropagation()">
            <h2>{{ (editingUnitId ? 'academics.syllabus.editUnit' : 'academics.syllabus.addUnit') | translate }}</h2>
            @if (formError) {
              <p class="form-error">{{ formError }}</p>
            }
            <form [formGroup]="unitForm" (ngSubmit)="saveUnit()">
              <div class="form-grid">
                <div class="field span-2">
                  <label for="unit-title">{{ 'academics.syllabus.unitTitle' | translate }}</label>
                  <input id="unit-title" type="text" formControlName="title" />
                </div>
                <div class="field">
                  <label for="unit-order">{{ 'academics.syllabus.sortOrder' | translate }}</label>
                  <input id="unit-order" type="number" min="0" formControlName="sortOrder" />
                </div>
                <div class="field">
                  <label for="unit-target">{{ 'academics.syllabus.targetCompletion' | translate }}</label>
                  <input id="unit-target" type="date" formControlName="targetCompletionDate" />
                </div>
                <div class="field span-2">
                  <label for="unit-description">{{ 'academics.syllabus.description' | translate }}</label>
                  <textarea id="unit-description" rows="3" formControlName="description"></textarea>
                </div>
              </div>
              <div class="form-actions">
                <button class="btn btn-ghost" type="button" (click)="closeModals()">{{ 'common.cancel' | translate }}</button>
                <button class="btn btn-primary" type="submit" [disabled]="unitForm.invalid || saving">
                  {{ 'common.save' | translate }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      @if (chapterModalOpen) {
        <div class="modal-backdrop" (click)="closeModals()">
          <div class="modal" (click)="$event.stopPropagation()">
            <h2>{{ (editingChapterId ? 'academics.syllabus.editChapter' : 'academics.syllabus.addChapter') | translate }}</h2>
            @if (formError) {
              <p class="form-error">{{ formError }}</p>
            }
            <form [formGroup]="chapterForm" (ngSubmit)="saveChapter()">
              <div class="form-grid">
                <div class="field span-2">
                  <label for="chapter-title">{{ 'academics.syllabus.chapterTitle' | translate }}</label>
                  <input id="chapter-title" type="text" formControlName="title" />
                </div>
                <div class="field">
                  <label for="chapter-order">{{ 'academics.syllabus.sortOrder' | translate }}</label>
                  <input id="chapter-order" type="number" min="0" formControlName="sortOrder" />
                </div>
                <div class="field">
                  <label for="chapter-status">{{ 'common.status' | translate }}</label>
                  <select id="chapter-status" formControlName="status">
                    <option value="PENDING">{{ 'academics.syllabus.statusPending' | translate }}</option>
                    <option value="IN_PROGRESS">{{ 'academics.syllabus.statusInProgress' | translate }}</option>
                    <option value="COMPLETED">{{ 'academics.syllabus.statusCompleted' | translate }}</option>
                  </select>
                </div>
                <div class="field">
                  <label for="chapter-target">{{ 'academics.syllabus.targetCompletion' | translate }}</label>
                  <input id="chapter-target" type="date" formControlName="targetCompletionDate" />
                </div>
                <div class="field span-2">
                  <label for="chapter-description">{{ 'academics.syllabus.description' | translate }}</label>
                  <textarea id="chapter-description" rows="3" formControlName="description"></textarea>
                </div>
              </div>
              <div class="form-actions">
                <button class="btn btn-ghost" type="button" (click)="closeModals()">{{ 'common.cancel' | translate }}</button>
                <button class="btn btn-primary" type="submit" [disabled]="chapterForm.invalid || saving">
                  {{ 'common.save' | translate }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      @if (materialModalOpen) {
        <div class="modal-backdrop" (click)="closeModals()">
          <div class="modal" (click)="$event.stopPropagation()">
            <h2>{{ 'academics.syllabus.addMaterial' | translate }}</h2>
            @if (formError) {
              <p class="form-error">{{ formError }}</p>
            }
            <form [formGroup]="materialForm" (ngSubmit)="saveMaterial()">
              <div class="form-grid">
                <div class="field span-2">
                  <label for="material-title">{{ 'academics.syllabus.materialTitle' | translate }}</label>
                  <input id="material-title" type="text" formControlName="title" />
                </div>
                <div class="field">
                  <label for="material-kind">{{ 'academics.syllabus.materialKind' | translate }}</label>
                  <select id="material-kind" formControlName="kind">
                    <option value="OUTLINE">{{ 'academics.syllabus.kindOutline' | translate }}</option>
                    <option value="DOCUMENT">{{ 'academics.syllabus.kindDocument' | translate }}</option>
                    <option value="LINK">{{ 'academics.syllabus.kindLink' | translate }}</option>
                  </select>
                </div>
                <div class="field">
                  <label for="material-unit">{{ 'academics.syllabus.unitLabel' | translate }}</label>
                  <select id="material-unit" formControlName="unitId">
                    <option value="">{{ 'academics.syllabus.allUnits' | translate }}</option>
                    @for (unit of detail?.units ?? []; track unit.id) {
                      <option [value]="unit.id">{{ unit.title }}</option>
                    }
                  </select>
                </div>
                <div class="field span-2">
                  <label for="material-url">{{ 'academics.syllabus.externalUrl' | translate }}</label>
                  <input id="material-url" type="url" formControlName="externalUrl" placeholder="https://" />
                </div>
                <div class="field span-2">
                  <label for="material-file">{{ 'academics.syllabus.uploadFile' | translate }}</label>
                  <input id="material-file" type="file" (change)="onMaterialFile($event)" />
                  <span class="muted small">{{ 'academics.syllabus.fileHint' | translate }}</span>
                </div>
              </div>
              <div class="form-actions">
                <button class="btn btn-ghost" type="button" (click)="closeModals()">{{ 'common.cancel' | translate }}</button>
                <button class="btn btn-primary" type="submit" [disabled]="materialForm.invalid || saving">
                  {{ 'common.save' | translate }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      @if (confirmOpen) {
        <div class="modal-backdrop" (click)="closeConfirm()">
          <div class="modal small" (click)="$event.stopPropagation()">
            <h2>{{ confirmTitleKey | translate }}</h2>
            <p class="confirm-text">{{ confirmBodyKey | translate }}</p>
            <div class="form-actions">
              <button class="btn btn-ghost" type="button" (click)="closeConfirm()">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-danger" type="button" [disabled]="saving" (click)="runConfirm()">
                {{ 'common.delete' | translate }}
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: `
    .page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 18px; flex-wrap: wrap; }
    h2 { font-size: 1.2rem; margin: 0 0 4px; }
    .muted { color: var(--color-muted); margin: 0; }
    .small { font-size: .82rem; }
    .strong { font-weight: 600; }
    .filters {
      display: flex; flex-wrap: wrap; gap: 10px; align-items: center;
      padding: 14px; margin-bottom: 18px;
    }
    .filters select, .filters input {
      padding: 8px 10px; border: 1px solid var(--color-border);
      border-radius: 8px; font: inherit; background: var(--color-surface); color: var(--color-text);
    }
    .filters input { min-width: 200px; flex: 1; }
    .layout { display: grid; grid-template-columns: minmax(0, 360px) minmax(0, 1fr); gap: 18px; align-items: start; }
    .course-list { display: flex; flex-direction: column; gap: 12px; }
    .loading-block { padding: 24px; text-align: center; color: var(--color-muted); }
    .course-card {
      display: block; width: 100%; text-align: left; cursor: pointer;
      padding: 14px; font: inherit; color: inherit;
      border: 1px solid var(--color-border); transition: border-color .12s ease, box-shadow .12s ease;
    }
    .course-card:hover { border-color: var(--color-primary); }
    .course-card.selected { border-color: var(--color-primary); box-shadow: 0 0 0 2px var(--color-primary-soft); }
    .course-head { display: flex; justify-content: space-between; gap: 10px; align-items: flex-start; }
    .course-title { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
    .course-foot { display: flex; justify-content: space-between; gap: 10px; margin-top: 8px; flex-wrap: wrap; }
    .progress { height: 6px; border-radius: 999px; background: var(--color-bg); overflow: hidden; margin: 10px 0 0; }
    .progress.large { height: 9px; margin: 4px 0 18px; }
    .progress-bar { display: block; height: 100%; border-radius: 999px; background: var(--color-primary); transition: width .3s ease; }
    .progress-bar.complete { background: #059669; }
    .progress-bar.danger { background: #dc2626; }
    .detail { padding: 20px; min-height: 260px; }
    .detail-placeholder { padding: 24px 0; }
    .detail-head { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; flex-wrap: wrap; }
    .detail-head h3 { margin: 0 0 4px; font-size: 1.1rem; }
    .detail-desc { margin: 8px 0 0; font-size: .9rem; }
    .detail-meta { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
    .chip { padding: 3px 10px; border-radius: 999px; background: var(--color-bg); border: 1px solid var(--color-border); font-size: .8rem; }
    .chip.overdue, .overdue { color: #b91c1c; }
    .section-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin: 22px 0 10px; }
    .section-head h4 { margin: 0; font-size: .98rem; }
    .unit-list { display: flex; flex-direction: column; gap: 14px; }
    .unit { border: 1px solid var(--color-border); border-radius: 10px; padding: 14px; }
    .unit-head { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; flex-wrap: wrap; }
    .unit-title { display: flex; flex-direction: column; gap: 2px; }
    .unit-desc { margin: 6px 0 0; }
    .chapter-list { list-style: none; margin: 12px 0 0; padding: 0; display: flex; flex-direction: column; gap: 6px; }
    .chapter {
      display: flex; justify-content: space-between; gap: 12px; align-items: center;
      padding: 8px 10px; border-radius: 8px; background: var(--color-bg);
    }
    .chapter.done .chapter-title { text-decoration: line-through; color: var(--color-muted); }
    .chapter-main { display: flex; gap: 10px; align-items: flex-start; min-width: 0; }
    .chapter-dot { width: 9px; height: 9px; border-radius: 50%; margin-top: 5px; flex: 0 0 auto; background: #cbd5e1; }
    .chapter-dot[data-status='IN_PROGRESS'] { background: #f59e0b; }
    .chapter-dot[data-status='COMPLETED'] { background: #059669; }
    .chapter-title { font-weight: 500; }
    .chapter-desc { margin: 2px 0 0; }
    .chapter-actions { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; justify-content: flex-end; }
    .status-select { padding: 5px 8px; border: 1px solid var(--color-border); border-radius: 7px; font: inherit; font-size: .8rem; background: var(--color-surface); color: var(--color-text); }
    .material-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
    .material { display: flex; justify-content: space-between; gap: 12px; align-items: center; padding: 10px 12px; border: 1px solid var(--color-border); border-radius: 8px; flex-wrap: wrap; }
    .kind { margin-left: 6px; }
    .empty-line { padding: 10px 0; }
    .banner { background: #eff6ff; border: 1px solid #bfdbfe; color: #1e40af; padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
    .banner.success { background: #ecfdf5; border-color: #a7f3d0; color: #047857; }
    .banner.error { background: #fef2f2; border-color: #fecaca; color: #b91c1c; }
    .row-actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .btn-sm { padding: 5px 10px; font-size: .82rem; }
    .modal-backdrop {
      position: fixed; inset: 0; z-index: 100; background: rgba(15, 23, 42, .5);
      display: flex; align-items: flex-start; justify-content: center;
      padding: 40px 16px; overflow-y: auto;
    }
    .modal {
      background: var(--color-surface); border-radius: var(--radius);
      padding: 24px; width: 600px; max-width: 100%;
      box-shadow: 0 20px 50px rgba(0,0,0,.25);
    }
    .modal.small { width: 420px; }
    .modal h2 { margin: 0 0 18px; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    .field input, .field select, .field textarea {
      padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit;
      background: var(--color-surface); color: var(--color-text);
    }
    .form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
    .form-error { margin: 0 0 14px; color: #b91c1c; font-size: .9rem; }
    .confirm-text { margin: 0; color: var(--color-muted); }
    .span-2 { grid-column: span 2; }
    @media (max-width: 900px) {
      .layout { grid-template-columns: 1fr; }
    }
    @media (max-width: 640px) {
      .form-grid { grid-template-columns: 1fr; }
      .span-2 { grid-column: span 1; }
      .chapter { align-items: flex-start; flex-direction: column; }
      .chapter-actions { justify-content: flex-start; }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcademicsSyllabusComponent implements OnInit {
  years: AcademicYear[] = [];
  classes: SchoolClass[] = [];
  subjects: SubjectOption[] = [];
  teachers: TeacherOption[] = [];

  courses: SyllabusCourse[] = [];
  detail: SyllabusDetail | null = null;
  selectedId: string | null = null;

  loading = false;
  detailLoading = false;
  saving = false;
  pageError = '';
  successMessage = '';
  formError = '';

  yearId = '';
  classId = '';
  sectionId = '';
  subjectId = '';
  teacherId = '';
  progressStatus = '';
  search = '';

  courseModalOpen = false;
  unitModalOpen = false;
  chapterModalOpen = false;
  materialModalOpen = false;

  editingCourseId: string | null = null;
  editingUnitId: string | null = null;
  editingChapterId: string | null = null;
  chapterUnitId: string | null = null;

  materialFile: File | null = null;

  confirmOpen = false;
  confirmTitleKey = '';
  confirmBodyKey = '';
  private confirmAction: (() => void) | null = null;

  readonly courseForm = new FormGroup({
    academicYearId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    classId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    sectionId: new FormControl('', { nonNullable: true }),
    subjectId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    teacherId: new FormControl('', { nonNullable: true }),
    title: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true }),
    targetCompletionDate: new FormControl('', { nonNullable: true }),
    status: new FormControl('ACTIVE', { nonNullable: true }),
  });

  readonly unitForm = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true }),
    sortOrder: new FormControl<number | null>(null),
    targetCompletionDate: new FormControl('', { nonNullable: true }),
  });

  readonly chapterForm = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true }),
    sortOrder: new FormControl<number | null>(null),
    status: new FormControl('PENDING', { nonNullable: true }),
    targetCompletionDate: new FormControl('', { nonNullable: true }),
  });

  readonly materialForm = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    kind: new FormControl('DOCUMENT', { nonNullable: true }),
    unitId: new FormControl('', { nonNullable: true }),
    externalUrl: new FormControl('', { nonNullable: true }),
  });

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef, private auth: AuthService) {}

  get canManage(): boolean {
    return this.auth.hasPermission('SYLLABUS_MANAGE');
  }

  get availableSections(): SchoolSection[] {
    const selected = this.classes.find((klass) => klass.id === this.classId);
    return selected?.sections ?? [];
  }

  get formSections(): SchoolSection[] {
    const selected = this.classes.find((klass) => klass.id === this.courseForm.controls.classId.value);
    return selected?.sections ?? [];
  }

  ngOnInit(): void {
    this.loadLookups();
    this.loadCourses();
  }

  scopeLabel(course: SyllabusCourse): string {
    return [course.className, course.sectionName, course.subjectName, course.teacherName]
      .filter((value) => !!value)
      .join(' - ');
  }

  progressLabel(status: string): string {
    if (status === 'COMPLETED') {
      return 'academics.syllabus.statusCompleted';
    }
    if (status === 'IN_PROGRESS') {
      return 'academics.syllabus.statusInProgress';
    }
    return 'academics.syllabus.statusNotStarted';
  }

  chapterStatusLabel(status: string): string {
    if (status === 'COMPLETED') {
      return 'academics.syllabus.statusCompleted';
    }
    if (status === 'IN_PROGRESS') {
      return 'academics.syllabus.statusInProgress';
    }
    return 'academics.syllabus.statusPending';
  }

  kindLabel(kind: string): string {
    if (kind === 'OUTLINE') {
      return 'academics.syllabus.kindOutline';
    }
    if (kind === 'LINK') {
      return 'academics.syllabus.kindLink';
    }
    return 'academics.syllabus.kindDocument';
  }

  formatSize(size: number | null): string {
    if (!size) {
      return '0 KB';
    }
    if (size < 1024) {
      return `${size} B`;
    }
    if (size < 1024 * 1024) {
      return `${Math.round(size / 1024)} KB`;
    }
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }

  onYearChange(event: Event): void {
    this.yearId = (event.target as HTMLSelectElement).value;
    this.loadCourses();
  }

  onClassChange(event: Event): void {
    this.classId = (event.target as HTMLSelectElement).value;
    if (this.sectionId && !this.availableSections.some((section) => section.id === this.sectionId)) {
      this.sectionId = '';
    }
    this.loadCourses();
  }

  onSectionChange(event: Event): void {
    this.sectionId = (event.target as HTMLSelectElement).value;
    this.loadCourses();
  }

  onSubjectChange(event: Event): void {
    this.subjectId = (event.target as HTMLSelectElement).value;
    this.loadCourses();
  }

  onTeacherChange(event: Event): void {
    this.teacherId = (event.target as HTMLSelectElement).value;
    this.loadCourses();
  }

  onStatusChange(event: Event): void {
    this.progressStatus = (event.target as HTMLSelectElement).value;
    this.loadCourses();
  }

  onSearch(event: Event): void {
    this.search = (event.target as HTMLInputElement).value;
    this.loadCourses();
  }

  clearFilters(): void {
    this.yearId = '';
    this.classId = '';
    this.sectionId = '';
    this.subjectId = '';
    this.teacherId = '';
    this.progressStatus = '';
    this.search = '';
    this.loadCourses();
  }

  selectCourse(id: string): void {
    this.selectedId = id;
    this.loadDetail(id);
  }

  // ---- Course modal ---------------------------------------------------

  openCourseModal(course?: SyllabusCourse): void {
    this.formError = '';
    this.courseModalOpen = true;
    this.editingCourseId = course?.id ?? null;
    const defaultYear = this.years.find((year) => year.current)?.id ?? this.years[0]?.id ?? '';
    this.courseForm.reset({
      academicYearId: course?.academicYearId ?? defaultYear,
      classId: course?.classId ?? this.classes[0]?.id ?? '',
      sectionId: course?.sectionId ?? '',
      subjectId: course?.subjectId ?? this.subjects[0]?.id ?? '',
      teacherId: course?.teacherId ?? '',
      title: course?.title ?? '',
      description: course?.description ?? '',
      targetCompletionDate: course?.targetCompletionDate ?? '',
      status: course?.status ?? 'ACTIVE',
    });
    this.cdr.markForCheck();
  }

  saveCourse(): void {
    if (this.courseForm.invalid || this.saving) {
      return;
    }
    const value = this.courseForm.getRawValue();
    const payload = {
      academicYearId: value.academicYearId,
      classId: value.classId,
      sectionId: value.sectionId || null,
      subjectId: value.subjectId,
      teacherId: value.teacherId || null,
      title: value.title.trim(),
      description: value.description.trim() || null,
      targetCompletionDate: value.targetCompletionDate || null,
      status: value.status,
    };
    const wasEdit = !!this.editingCourseId;
    this.saving = true;
    this.formError = '';
    const request$ = wasEdit
      ? this.http.put<ApiResponse<SyllabusCourse>>(`/api/syllabus/courses/${this.editingCourseId}`, payload)
      : this.http.post<ApiResponse<SyllabusCourse>>('/api/syllabus/courses', payload);
    request$.subscribe({
      next: (res) => {
        this.saving = false;
        this.closeModals();
        this.successMessage = wasEdit ? 'academics.syllabus.courseUpdated' : 'academics.syllabus.courseCreated';
        this.loadCourses(res.data?.id);
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => this.fail(err),
    });
  }

  askDeleteCourse(): void {
    if (!this.detail) {
      return;
    }
    const id = this.detail.course.id;
    this.askConfirm('academics.syllabus.deleteCourse', 'academics.syllabus.deleteCourseConfirm', () => {
      this.http.delete<ApiResponse<void>>(`/api/syllabus/courses/${id}`).subscribe({
        next: () => {
          this.successMessage = 'academics.syllabus.courseDeleted';
          this.detail = null;
          this.selectedId = null;
          this.loadCourses();
          this.cdr.markForCheck();
        },
        error: (err: HttpErrorResponse) => this.fail(err),
      });
    });
  }

  // ---- Unit modal -----------------------------------------------------

  openUnitModal(unit?: SyllabusUnit): void {
    this.formError = '';
    this.unitModalOpen = true;
    this.editingUnitId = unit?.id ?? null;
    this.unitForm.reset({
      title: unit?.title ?? '',
      description: unit?.description ?? '',
      sortOrder: unit?.sortOrder ?? (this.detail?.units.length ?? 0) + 1,
      targetCompletionDate: unit?.targetCompletionDate ?? '',
    });
    this.cdr.markForCheck();
  }

  saveUnit(): void {
    if (this.unitForm.invalid || this.saving || !this.detail) {
      return;
    }
    const value = this.unitForm.getRawValue();
    const payload = {
      title: value.title.trim(),
      description: value.description.trim() || null,
      sortOrder: value.sortOrder === null ? null : Number(value.sortOrder),
      targetCompletionDate: value.targetCompletionDate || null,
    };
    const courseId = this.detail.course.id;
    const wasEdit = !!this.editingUnitId;
    this.saving = true;
    this.formError = '';
    const request$ = wasEdit
      ? this.http.put<ApiResponse<SyllabusUnit>>(`/api/syllabus/units/${this.editingUnitId}`, payload)
      : this.http.post<ApiResponse<SyllabusUnit>>(`/api/syllabus/courses/${courseId}/units`, payload);
    request$.subscribe({
      next: () => {
        this.saving = false;
        this.closeModals();
        this.successMessage = wasEdit ? 'academics.syllabus.unitUpdated' : 'academics.syllabus.unitCreated';
        this.reloadDetail();
      },
      error: (err: HttpErrorResponse) => this.fail(err),
    });
  }

  askDeleteUnit(unit: SyllabusUnit): void {
    this.askConfirm('academics.syllabus.deleteUnit', 'academics.syllabus.deleteUnitConfirm', () => {
      this.http.delete<ApiResponse<void>>(`/api/syllabus/units/${unit.id}`).subscribe({
        next: () => {
          this.successMessage = 'academics.syllabus.unitDeleted';
          this.reloadDetail();
        },
        error: (err: HttpErrorResponse) => this.fail(err),
      });
    });
  }

  // ---- Chapter modal --------------------------------------------------

  openChapterModal(unit: SyllabusUnit, chapter?: SyllabusChapter): void {
    this.formError = '';
    this.chapterModalOpen = true;
    this.chapterUnitId = unit.id;
    this.editingChapterId = chapter?.id ?? null;
    this.chapterForm.reset({
      title: chapter?.title ?? '',
      description: chapter?.description ?? '',
      sortOrder: chapter?.sortOrder ?? unit.chapters.length + 1,
      status: chapter?.status ?? 'PENDING',
      targetCompletionDate: chapter?.targetCompletionDate ?? '',
    });
    this.cdr.markForCheck();
  }

  saveChapter(): void {
    if (this.chapterForm.invalid || this.saving || !this.chapterUnitId) {
      return;
    }
    const value = this.chapterForm.getRawValue();
    const payload = {
      title: value.title.trim(),
      description: value.description.trim() || null,
      sortOrder: value.sortOrder === null ? null : Number(value.sortOrder),
      status: value.status,
      targetCompletionDate: value.targetCompletionDate || null,
    };
    const wasEdit = !!this.editingChapterId;
    this.saving = true;
    this.formError = '';
    const request$ = wasEdit
      ? this.http.put<ApiResponse<SyllabusChapter>>(`/api/syllabus/chapters/${this.editingChapterId}`, payload)
      : this.http.post<ApiResponse<SyllabusChapter>>(`/api/syllabus/units/${this.chapterUnitId}/chapters`, payload);
    request$.subscribe({
      next: () => {
        this.saving = false;
        this.closeModals();
        this.successMessage = wasEdit ? 'academics.syllabus.chapterUpdated' : 'academics.syllabus.chapterCreated';
        this.reloadDetail();
      },
      error: (err: HttpErrorResponse) => this.fail(err),
    });
  }

  setChapterStatus(chapter: SyllabusChapter, event: Event): void {
    const status = (event.target as HTMLSelectElement).value;
    if (status === chapter.status) {
      return;
    }
    this.http.patch<ApiResponse<SyllabusChapter>>(`/api/syllabus/chapters/${chapter.id}/status`, { status })
      .subscribe({
        next: () => {
          this.successMessage = 'academics.syllabus.chapterUpdated';
          this.reloadDetail();
        },
        error: (err: HttpErrorResponse) => this.fail(err),
      });
  }

  askDeleteChapter(chapter: SyllabusChapter): void {
    this.askConfirm('academics.syllabus.deleteChapter', 'academics.syllabus.deleteChapterConfirm', () => {
      this.http.delete<ApiResponse<void>>(`/api/syllabus/chapters/${chapter.id}`).subscribe({
        next: () => {
          this.successMessage = 'academics.syllabus.chapterDeleted';
          this.reloadDetail();
        },
        error: (err: HttpErrorResponse) => this.fail(err),
      });
    });
  }

  // ---- Materials ------------------------------------------------------

  openMaterialModal(): void {
    this.formError = '';
    this.materialModalOpen = true;
    this.materialFile = null;
    this.materialForm.reset({ title: '', kind: 'DOCUMENT', unitId: '', externalUrl: '' });
    this.cdr.markForCheck();
  }

  onMaterialFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.materialFile = input.files && input.files.length > 0 ? input.files[0] : null;
  }

  saveMaterial(): void {
    if (this.materialForm.invalid || this.saving || !this.detail) {
      return;
    }
    const value = this.materialForm.getRawValue();
    if (!this.materialFile && !value.externalUrl.trim()) {
      this.formError = 'Attach a file or provide a link.';
      this.cdr.markForCheck();
      return;
    }
    const form = new FormData();
    form.append('title', value.title.trim());
    form.append('kind', value.kind);
    if (value.unitId) {
      form.append('unitId', value.unitId);
    }
    if (value.externalUrl.trim()) {
      form.append('externalUrl', value.externalUrl.trim());
    }
    if (this.materialFile) {
      form.append('file', this.materialFile);
    }
    this.saving = true;
    this.formError = '';
    this.http.post<ApiResponse<SyllabusMaterial>>(
      `/api/syllabus/courses/${this.detail.course.id}/materials`, form).subscribe({
      next: () => {
        this.saving = false;
        this.closeModals();
        this.successMessage = 'academics.syllabus.materialAdded';
        this.reloadDetail();
      },
      error: (err: HttpErrorResponse) => this.fail(err),
    });
  }

  downloadMaterial(material: SyllabusMaterial): void {
    this.http.get(`/api/syllabus/materials/${material.id}/download`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = material.fileName || 'material';
        link.click();
        URL.revokeObjectURL(url);
      },
      error: (err: HttpErrorResponse) => this.fail(err),
    });
  }

  askDeleteMaterial(material: SyllabusMaterial): void {
    this.askConfirm('academics.syllabus.deleteMaterial', 'academics.syllabus.deleteMaterialConfirm', () => {
      this.http.delete<ApiResponse<void>>(`/api/syllabus/materials/${material.id}`).subscribe({
        next: () => {
          this.successMessage = 'academics.syllabus.materialDeleted';
          this.reloadDetail();
        },
        error: (err: HttpErrorResponse) => this.fail(err),
      });
    });
  }

  // ---- Confirm --------------------------------------------------------

  askConfirm(titleKey: string, bodyKey: string, action: () => void): void {
    this.confirmTitleKey = titleKey;
    this.confirmBodyKey = bodyKey;
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

  closeModals(): void {
    if (this.saving) {
      return;
    }
    this.courseModalOpen = false;
    this.unitModalOpen = false;
    this.chapterModalOpen = false;
    this.materialModalOpen = false;
    this.editingCourseId = null;
    this.editingUnitId = null;
    this.editingChapterId = null;
    this.chapterUnitId = null;
    this.materialFile = null;
    this.formError = '';
    this.cdr.markForCheck();
  }

  // ---- Data -----------------------------------------------------------

  private loadCourses(selectId?: string): void {
    this.loading = true;
    this.pageError = '';
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
    if (this.teacherId) {
      params = params.set('teacherId', this.teacherId);
    }
    if (this.progressStatus) {
      params = params.set('progressStatus', this.progressStatus);
    }
    if (this.search.trim()) {
      params = params.set('query', this.search.trim());
    }
    this.http.get<ApiResponse<SyllabusCourse[]>>('/api/syllabus/courses', { params }).subscribe({
      next: (res) => {
        this.courses = res.data ?? [];
        this.loading = false;
        const target = selectId
          ?? (this.selectedId && this.courses.some((course) => course.id === this.selectedId) ? this.selectedId : null)
          ?? this.courses[0]?.id
          ?? null;
        if (target) {
          this.selectedId = target;
          this.loadDetail(target);
        } else {
          this.selectedId = null;
          this.detail = null;
        }
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.courses = [];
        this.detail = null;
        this.selectedId = null;
        this.fail(err, 'academics.syllabus.loadFailed');
      },
    });
  }

  private loadDetail(id: string): void {
    this.detailLoading = true;
    this.http.get<ApiResponse<SyllabusDetail>>(`/api/syllabus/courses/${id}`).subscribe({
      next: (res) => {
        this.detail = res.data ?? null;
        this.detailLoading = false;
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorResponse) => {
        this.detailLoading = false;
        this.detail = null;
        this.fail(err);
      },
    });
  }

  private reloadDetail(): void {
    if (this.selectedId) {
      this.loadCourses(this.selectedId);
    }
  }

  private loadLookups(): void {
    this.http.get<ApiResponse<AcademicYear[]>>('/api/academic-years').subscribe({
      next: (res) => {
        this.years = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
    this.http.get<ApiResponse<SchoolClass[]>>('/api/classes').subscribe({
      next: (res) => {
        this.classes = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
    this.http.get<ApiResponse<SubjectOption[]>>('/api/subjects').subscribe({
      next: (res) => {
        this.subjects = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
    this.http.get<ApiResponse<TeacherOption[]>>('/api/teachers').subscribe({
      next: (res) => {
        this.teachers = (res.data ?? []).filter((teacher) => teacher.status !== 'INACTIVE');
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  private fail(err: HttpErrorResponse, fallback = 'Could not complete the request. Please try again.'): void {
    const apiError = err.error as ApiError | undefined;
    const message = apiError?.message || fallback;
    this.saving = false;
    const modalOpen = this.courseModalOpen || this.unitModalOpen
      || this.chapterModalOpen || this.materialModalOpen;
    if (modalOpen) {
      this.formError = message;
    } else {
      this.pageError = message;
    }
    this.cdr.markForCheck();
  }
}
