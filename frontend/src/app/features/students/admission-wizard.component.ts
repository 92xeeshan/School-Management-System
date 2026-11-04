import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ApiError, ApiResponse } from '../../core/models/api.model';
import { AuthService } from '../../core/auth/auth.service';
import { PageHeaderComponent } from '../../layout/page-header/page-header.component';
import { CredentialHandoverComponent, IssuedCredentials } from '../../layout/credential-handover/credential-handover.component';

interface LookupOption {
  id: string;
  name: string;
  status?: string;
}

interface ClassOption {
  id: string;
  name: string;
  streamEnabled?: boolean;
}

interface SectionOption {
  id: string;
  classId: string;
  name: string;
}

interface AcademicYear {
  id: string;
  name: string;
  current: boolean;
}

interface ClassSubjectOption {
  subjectId: string;
  name: string;
  code: string;
  compulsory: boolean;
  streamId: string | null;
}

interface FamilyMember {
  id?: string | null;
  firstName?: string | null;
  lastName?: string | null;
  occupation?: string | null;
  phone?: string | null;
  email?: string | null;
  qualification?: string | null;
  relationship?: string | null;
  address?: string | null;
  primary?: boolean;
}

interface Address {
  addressType?: string;
  line1?: string | null;
  line2?: string | null;
  city?: string | null;
  district?: string | null;
  state?: string | null;
  pin?: string | null;
}

interface PreviousSchool {
  noPreviousSchool: boolean;
  schoolName?: string | null;
  board?: string | null;
  medium?: string | null;
  lastClass?: string | null;
  yearOfPassing?: string | null;
  percentage?: string | null;
  tcNumber?: string | null;
  tcDate?: string | null;
  reason?: string | null;
}

interface StudentDocument {
  id: string;
  docType: string;
  fileName: string;
  contentType: string;
  url: string | null;
  verified: boolean;
}

interface Sibling {
  id: string;
  admissionNo: string;
  displayName: string;
  className: string | null;
  sectionName: string | null;
}

interface StepStatus {
  stepNo: number;
  status: string;
}

interface AdmissionWizard {
  id: string;
  admissionNo: string;
  firstName: string;
  middleName: string | null;
  lastName: string | null;
  displayName: string;
  dateOfBirth: string | null;
  gender: string | null;
  bloodGroup: string | null;
  religionId: string | null;
  categoryId: string | null;
  nationality: string | null;
  motherTongueId: string | null;
  aadhaarNo: string | null;
  apaarId: string | null;
  photoUrl: string | null;
  admissionDate: string | null;
  status: string;
  classId: string | null;
  className: string | null;
  streamEnabled: boolean;
  sectionId: string | null;
  sectionName: string | null;
  rollNumber: number | null;
  academicYearId: string | null;
  academicYearName: string | null;
  houseId: string | null;
  mediumId: string | null;
  streamId: string | null;
  subjectIds: string[];
  father: FamilyMember | null;
  mother: FamilyMember | null;
  guardian: FamilyMember | null;
  guardianDifferent: boolean;
  emergencyName: string | null;
  emergencyRelation: string | null;
  emergencyPhone: string | null;
  primaryContact: string | null;
  currentAddress: Address | null;
  permanentAddress: Address | null;
  sameAsCurrent: boolean;
  phone: string | null;
  email: string | null;
  previousSchool: PreviousSchool | null;
  transportRequired: boolean;
  hostelRequired: boolean;
  medicalNotes: string | null;
  remarks: string | null;
  documents: StudentDocument[];
  siblings: Sibling[];
  steps: StepStatus[];
  profilePending: boolean;
  username?: string | null;
  temporaryPassword?: string | null;
}

const DOC_TYPES = [
  'BIRTH_CERTIFICATE',
  'TRANSFER_CERTIFICATE',
  'PREVIOUS_MARKSHEET',
  'AADHAAR',
  'CASTE_CERTIFICATE',
  'OTHER',
];

@Component({
  selector: 'app-admission-wizard',
  imports: [TranslateModule, ReactiveFormsModule, RouterLink, PageHeaderComponent, CredentialHandoverComponent],
  template: `
    <div class="page">
      <app-page-header
        [title]="wizard?.displayName || ('students.admission.title' | translate)"
        [subtitle]="'students.admission.subtitle' | translate">
        <a class="back" routerLink="/students">{{ 'common.back' | translate }}</a>
      </app-page-header>

      <app-credential-handover [credentials]="issuedCredentials" (closed)="issuedCredentials = null" />

      @if (pageError) {
        <p class="banner error">{{ pageError }}</p>
      }
      @if (successMessage) {
        <p class="banner success">{{ successMessage | translate }}</p>
      }

      <ol class="steps">
        @for (step of stepDefs; track step.no) {
          <li [class.active]="currentStep === step.no" [class.done]="isCompleted(step.no)">
            <button type="button" (click)="goToStep(step.no)" [disabled]="!canOpen(step.no)">
              <span class="num">{{ step.no }}</span>
              <span>{{ step.key | translate }}</span>
            </button>
          </li>
        }
      </ol>

      @if (loading) {
        <div class="card pad">{{ 'common.loading' | translate }}</div>
      } @else {
        <div class="card">
          @if (currentStep === 1) {
            <form [formGroup]="step1" (ngSubmit)="saveStep1()">
              <h2>{{ 'students.admission.step1' | translate }}</h2>
              <div class="form-grid">
                <div class="field">
                  <label>{{ 'students.studentId' | translate }}</label>
                  <input type="text" [value]="wizard?.admissionNo || ('students.admission.autoAdmission' | translate)" readonly />
                </div>
                <div class="field">
                  <label>{{ 'students.firstName' | translate }} *</label>
                  <input type="text" formControlName="firstName" />
                </div>
                <div class="field">
                  <label>{{ 'students.admission.middleName' | translate }}</label>
                  <input type="text" formControlName="middleName" />
                </div>
                <div class="field">
                  <label>{{ 'students.lastName' | translate }}</label>
                  <input type="text" formControlName="lastName" />
                </div>
                <div class="field">
                  <label>{{ 'students.className' | translate }} *</label>
                  <select formControlName="classId" (change)="onClassChange()">
                    <option value="">—</option>
                    @for (klass of classes; track klass.id) {
                      <option [value]="klass.id">{{ klass.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label>{{ 'students.section' | translate }} *</label>
                  <select formControlName="sectionId">
                    <option value="">—</option>
                    @for (section of sectionsForClass; track section.id) {
                      <option [value]="section.id">{{ section.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label>{{ 'students.dateOfBirth' | translate }} *</label>
                  <input type="date" formControlName="dateOfBirth" />
                </div>
                <div class="field">
                  <label>{{ 'students.gender' | translate }} *</label>
                  <select formControlName="gender">
                    <option value="MALE">{{ 'students.admission.male' | translate }}</option>
                    <option value="FEMALE">{{ 'students.admission.female' | translate }}</option>
                    <option value="OTHER">{{ 'students.admission.other' | translate }}</option>
                  </select>
                </div>
                <div class="field">
                  <label>{{ 'students.bloodGroup' | translate }}</label>
                  <select formControlName="bloodGroup">
                    <option value="">—</option>
                    @for (bg of bloodGroups; track bg) {
                      <option [value]="bg">{{ bg }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label>{{ 'students.religion' | translate }}</label>
                  <select formControlName="religionId">
                    <option value="">—</option>
                    @for (item of religions; track item.id) {
                      <option [value]="item.id">{{ item.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label>{{ 'students.admission.category' | translate }}</label>
                  <select formControlName="categoryId">
                    <option value="">—</option>
                    @for (item of categories; track item.id) {
                      <option [value]="item.id">{{ item.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label>{{ 'students.nationality' | translate }}</label>
                  <input type="text" formControlName="nationality" />
                </div>
                <div class="field">
                  <label>{{ 'students.admission.motherTongue' | translate }}</label>
                  <select formControlName="motherTongueId">
                    <option value="">—</option>
                    @for (item of motherTongues; track item.id) {
                      <option [value]="item.id">{{ item.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label>{{ 'students.admission.aadhaar' | translate }}</label>
                  <input type="text" formControlName="aadhaarNo" maxlength="12" />
                </div>
                <div class="field">
                  <label>{{ 'students.admission.apaar' | translate }}</label>
                  <input type="text" formControlName="apaarId" maxlength="12" />
                </div>
                @if (studentId) {
                  <div class="field span-2">
                    <label>{{ 'students.admission.photo' | translate }}</label>
                    <div class="photo-row">
                      @if (wizard?.photoUrl) {
                        <img class="photo" [src]="wizard!.photoUrl!" alt="" />
                      }
                      <input type="file" accept="image/jpeg,image/png" (change)="onPhoto($event)" />
                    </div>
                    <span class="hint">{{ 'students.admission.photoHint' | translate }}</span>
                  </div>
                }
              </div>
              <div class="form-actions">
                <button class="btn btn-primary" type="submit" [disabled]="step1.invalid || saving || !canWriteStep1">
                  {{ saving ? ('common.loading' | translate) : ('students.admission.saveNext' | translate) }}
                </button>
              </div>
            </form>
          }

          @if (currentStep === 2) {
            <form [formGroup]="step2" (ngSubmit)="saveStep2()">
              <h2>{{ 'students.admission.step2' | translate }}</h2>
              <h3>{{ 'students.fatherName' | translate }}</h3>
              <div class="form-grid" formGroupName="father">
                <div class="field"><label>{{ 'students.firstName' | translate }}</label><input type="text" formControlName="firstName" /></div>
                <div class="field"><label>{{ 'students.lastName' | translate }}</label><input type="text" formControlName="lastName" /></div>
                <div class="field"><label>{{ 'students.admission.occupation' | translate }}</label><input type="text" formControlName="occupation" /></div>
                <div class="field"><label>{{ 'students.phone' | translate }}</label><input type="text" formControlName="phone" maxlength="10" /></div>
                <div class="field"><label>{{ 'students.email' | translate }}</label><input type="email" formControlName="email" /></div>
                <div class="field"><label>{{ 'students.admission.qualification' | translate }}</label><input type="text" formControlName="qualification" /></div>
              </div>
              <h3>{{ 'students.motherName' | translate }}</h3>
              <div class="form-grid" formGroupName="mother">
                <div class="field"><label>{{ 'students.firstName' | translate }}</label><input type="text" formControlName="firstName" /></div>
                <div class="field"><label>{{ 'students.lastName' | translate }}</label><input type="text" formControlName="lastName" /></div>
                <div class="field"><label>{{ 'students.admission.occupation' | translate }}</label><input type="text" formControlName="occupation" /></div>
                <div class="field"><label>{{ 'students.phone' | translate }}</label><input type="text" formControlName="phone" maxlength="10" /></div>
                <div class="field"><label>{{ 'students.email' | translate }}</label><input type="email" formControlName="email" /></div>
                <div class="field"><label>{{ 'students.admission.qualification' | translate }}</label><input type="text" formControlName="qualification" /></div>
              </div>
              <label class="check">
                <input type="checkbox" formControlName="guardianDifferent" />
                {{ 'students.admission.guardianDifferent' | translate }}
              </label>
              @if (step2.value.guardianDifferent) {
                <h3>{{ 'students.guardianName' | translate }}</h3>
                <div class="form-grid" formGroupName="guardian">
                  <div class="field"><label>{{ 'students.firstName' | translate }}</label><input type="text" formControlName="firstName" /></div>
                  <div class="field"><label>{{ 'students.lastName' | translate }}</label><input type="text" formControlName="lastName" /></div>
                  <div class="field"><label>{{ 'students.admission.occupation' | translate }}</label><input type="text" formControlName="occupation" /></div>
                  <div class="field"><label>{{ 'students.phone' | translate }}</label><input type="text" formControlName="phone" maxlength="10" /></div>
                  <div class="field"><label>{{ 'students.email' | translate }}</label><input type="email" formControlName="email" /></div>
                  <div class="field"><label>{{ 'students.admission.qualification' | translate }}</label><input type="text" formControlName="qualification" /></div>
                </div>
              }
              <h3>{{ 'students.emergencyContact' | translate }}</h3>
              <div class="form-grid">
                <div class="field"><label>{{ 'students.name' | translate }}</label><input type="text" formControlName="emergencyName" /></div>
                <div class="field"><label>{{ 'students.admission.relation' | translate }}</label><input type="text" formControlName="emergencyRelation" /></div>
                <div class="field"><label>{{ 'students.phone' | translate }}</label><input type="text" formControlName="emergencyPhone" maxlength="10" /></div>
                <div class="field">
                  <label>{{ 'students.admission.primaryContact' | translate }}</label>
                  <select formControlName="primaryContact">
                    <option value="FATHER">{{ 'students.fatherName' | translate }}</option>
                    <option value="MOTHER">{{ 'students.motherName' | translate }}</option>
                    <option value="GUARDIAN">{{ 'students.guardianName' | translate }}</option>
                  </select>
                </div>
              </div>
              <div class="form-actions">
                <button class="btn" type="button" (click)="currentStep = 1">{{ 'students.admission.back' | translate }}</button>
                <button class="btn btn-primary" type="submit" [disabled]="saving || !canUpdate">
                  {{ saving ? ('common.loading' | translate) : ('students.admission.saveNext' | translate) }}
                </button>
              </div>
            </form>
          }

          @if (currentStep === 3) {
            <form [formGroup]="step3" (ngSubmit)="saveStep3()">
              <h2>{{ 'students.admission.step3' | translate }}</h2>
              <h3>{{ 'students.presentAddress' | translate }}</h3>
              <div class="form-grid" formGroupName="currentAddress">
                <div class="field span-2"><label>{{ 'students.admission.line1' | translate }}</label><input type="text" formControlName="line1" /></div>
                <div class="field span-2"><label>{{ 'students.admission.line2' | translate }}</label><input type="text" formControlName="line2" /></div>
                <div class="field"><label>{{ 'students.admission.city' | translate }}</label><input type="text" formControlName="city" /></div>
                <div class="field"><label>{{ 'students.admission.district' | translate }}</label><input type="text" formControlName="district" /></div>
                <div class="field"><label>{{ 'students.admission.state' | translate }}</label><input type="text" formControlName="state" /></div>
                <div class="field"><label>{{ 'students.admission.pin' | translate }}</label><input type="text" formControlName="pin" maxlength="6" /></div>
              </div>
              <label class="check">
                <input type="checkbox" formControlName="sameAsCurrent" />
                {{ 'students.admission.sameAsCurrent' | translate }}
              </label>
              @if (!step3.value.sameAsCurrent) {
                <h3>{{ 'students.permanentAddress' | translate }}</h3>
                <div class="form-grid" formGroupName="permanentAddress">
                  <div class="field span-2"><label>{{ 'students.admission.line1' | translate }}</label><input type="text" formControlName="line1" /></div>
                  <div class="field span-2"><label>{{ 'students.admission.line2' | translate }}</label><input type="text" formControlName="line2" /></div>
                  <div class="field"><label>{{ 'students.admission.city' | translate }}</label><input type="text" formControlName="city" /></div>
                  <div class="field"><label>{{ 'students.admission.district' | translate }}</label><input type="text" formControlName="district" /></div>
                  <div class="field"><label>{{ 'students.admission.state' | translate }}</label><input type="text" formControlName="state" /></div>
                  <div class="field"><label>{{ 'students.admission.pin' | translate }}</label><input type="text" formControlName="pin" maxlength="6" /></div>
                </div>
              }
              <div class="form-grid">
                <div class="field"><label>{{ 'students.phone' | translate }}</label><input type="text" formControlName="phone" maxlength="10" /></div>
                <div class="field"><label>{{ 'students.email' | translate }}</label><input type="email" formControlName="email" /></div>
              </div>
              <div class="form-actions">
                <button class="btn" type="button" (click)="currentStep = 2">{{ 'students.admission.back' | translate }}</button>
                <button class="btn btn-primary" type="submit" [disabled]="saving || !canUpdate">
                  {{ saving ? ('common.loading' | translate) : ('students.admission.saveNext' | translate) }}
                </button>
              </div>
            </form>
          }

          @if (currentStep === 4) {
            <form [formGroup]="step4" (ngSubmit)="saveStep4()">
              <h2>{{ 'students.admission.step4' | translate }}</h2>
              <div class="form-grid">
                <div class="field">
                  <label>{{ 'students.academicYear' | translate }} *</label>
                  <select formControlName="academicYearId">
                    @for (year of academicYears; track year.id) {
                      <option [value]="year.id">{{ year.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label>{{ 'students.admissionDate' | translate }}</label>
                  <input type="date" formControlName="admissionDate" />
                </div>
                <div class="field">
                  <label>{{ 'students.rollNumber' | translate }}</label>
                  <input type="number" formControlName="rollNumber" min="1" />
                </div>
                <div class="field">
                  <label>{{ 'students.admission.house' | translate }}</label>
                  <select formControlName="houseId">
                    <option value="">—</option>
                    @for (item of houses; track item.id) {
                      <option [value]="item.id">{{ item.name }}</option>
                    }
                  </select>
                </div>
                <div class="field">
                  <label>{{ 'students.admission.medium' | translate }}</label>
                  <select formControlName="mediumId">
                    <option value="">—</option>
                    @for (item of media; track item.id) {
                      <option [value]="item.id">{{ item.name }}</option>
                    }
                  </select>
                </div>
                @if (streamEnabled) {
                  <div class="field">
                    <label>{{ 'students.admission.stream' | translate }}</label>
                    <select formControlName="streamId" (change)="loadSubjects()">
                      <option value="">—</option>
                      @for (item of streams; track item.id) {
                        <option [value]="item.id">{{ item.name }}</option>
                      }
                    </select>
                  </div>
                }
                <div class="field span-2">
                  <label>{{ 'academics.subjects' | translate }}</label>
                  <div class="multi-select">
                    @for (subject of classSubjects; track subject.subjectId) {
                      <label class="check-item">
                        <input type="checkbox"
                               [checked]="isSubjectSelected(subject.subjectId)"
                               [disabled]="subject.compulsory"
                               (change)="toggleSubject(subject.subjectId)" />
                        <span>{{ subject.name }}{{ subject.compulsory ? ' *' : '' }}</span>
                      </label>
                    } @empty {
                      <span class="muted">{{ 'common.noData' | translate }}</span>
                    }
                  </div>
                </div>
              </div>
              <div class="form-actions">
                <button class="btn" type="button" (click)="currentStep = 3">{{ 'students.admission.back' | translate }}</button>
                <button class="btn btn-primary" type="submit" [disabled]="step4.invalid || saving || !canUpdate">
                  {{ saving ? ('common.loading' | translate) : ('students.admission.saveNext' | translate) }}
                </button>
              </div>
            </form>
          }

          @if (currentStep === 5) {
            <form [formGroup]="step5" (ngSubmit)="saveStep5()">
              <h2>{{ 'students.admission.step5' | translate }}</h2>
              <label class="check">
                <input type="checkbox" formControlName="noPreviousSchool" />
                {{ 'students.admission.noPreviousSchool' | translate }}
              </label>
              @if (!step5.value.noPreviousSchool) {
                <div class="form-grid">
                  <div class="field span-2"><label>{{ 'students.previousSchool' | translate }}</label><input type="text" formControlName="schoolName" /></div>
                  <div class="field"><label>{{ 'students.admission.board' | translate }}</label><input type="text" formControlName="board" /></div>
                  <div class="field"><label>{{ 'students.admission.medium' | translate }}</label><input type="text" formControlName="medium" /></div>
                  <div class="field"><label>{{ 'students.admission.lastClass' | translate }}</label><input type="text" formControlName="lastClass" /></div>
                  <div class="field"><label>{{ 'students.admission.yearOfPassing' | translate }}</label><input type="text" formControlName="yearOfPassing" /></div>
                  <div class="field"><label>{{ 'students.admission.percentageMarks' | translate }}</label><input type="text" formControlName="percentage" /></div>
                  <div class="field"><label>{{ 'students.admission.tcNumber' | translate }}</label><input type="text" formControlName="tcNumber" /></div>
                  <div class="field"><label>{{ 'students.admission.tcDate' | translate }}</label><input type="date" formControlName="tcDate" /></div>
                  <div class="field span-2"><label>{{ 'students.admission.reason' | translate }}</label><input type="text" formControlName="reason" /></div>
                </div>
              }
              <h3>{{ 'students.admission.documents' | translate }}</h3>
              <div class="doc-upload">
                <select [value]="docType" (change)="docType = $any($event.target).value">
                  @for (type of docTypes; track type) {
                    <option [value]="type">{{ ('students.admission.docTypes.' + type) | translate }}</option>
                  }
                </select>
                <input type="file" accept="image/jpeg,image/png,application/pdf" (change)="onDocument($event)" />
                <span class="hint">{{ 'students.admission.docHint' | translate }}</span>
              </div>
              <ul class="docs">
                @for (doc of documents; track doc.id) {
                  <li>
                    <span>{{ ('students.admission.docTypes.' + doc.docType) | translate }} — {{ doc.fileName }}</span>
                    <span class="spacer"></span>
                    @if (doc.url) {
                      <a class="btn btn-sm" [href]="doc.url" target="_blank" rel="noopener">{{ 'common.export' | translate }}</a>
                    }
                    @if (canUpdate) {
                      <button class="btn btn-sm btn-danger" type="button" (click)="deleteDocument(doc.id)">{{ 'common.delete' | translate }}</button>
                    }
                  </li>
                }
              </ul>
              <div class="form-actions">
                <button class="btn" type="button" (click)="currentStep = 4">{{ 'students.admission.back' | translate }}</button>
                <button class="btn btn-primary" type="submit" [disabled]="saving || !canUpdate">
                  {{ saving ? ('common.loading' | translate) : ('students.admission.saveNext' | translate) }}
                </button>
              </div>
            </form>
          }

          @if (currentStep === 6) {
            <form [formGroup]="step6" (ngSubmit)="saveStep6()">
              <h2>{{ 'students.admission.step6' | translate }}</h2>
              <label class="check"><input type="checkbox" formControlName="transportRequired" /> {{ 'students.admission.transport' | translate }}</label>
              <label class="check"><input type="checkbox" formControlName="hostelRequired" /> {{ 'students.admission.hostel' | translate }}</label>
              <div class="form-grid">
                <div class="field span-2">
                  <label>{{ 'students.admission.medicalNotes' | translate }}</label>
                  <textarea formControlName="medicalNotes" rows="3"></textarea>
                </div>
                <div class="field span-2">
                  <label>{{ 'students.admission.remarks' | translate }}</label>
                  <textarea formControlName="remarks" rows="3"></textarea>
                </div>
                <div class="field span-2">
                  <label>{{ 'students.admission.siblings' | translate }}</label>
                  <input type="search" [value]="siblingQuery" (input)="onSiblingSearch($any($event.target).value)"
                         [placeholder]="'students.admission.siblingSearch' | translate" />
                  @if (siblingHits.length) {
                    <ul class="hits">
                      @for (hit of siblingHits; track hit.id) {
                        <li>
                          <button type="button" (click)="addSibling(hit)">{{ hit.displayName }} ({{ hit.admissionNo }})</button>
                        </li>
                      }
                    </ul>
                  }
                  <div class="chips">
                    @for (sib of selectedSiblings; track sib.id) {
                      <span class="chip">
                        {{ sib.displayName }}
                        <button type="button" (click)="removeSibling(sib.id)">×</button>
                      </span>
                    }
                  </div>
                </div>
              </div>
              <div class="form-actions">
                <button class="btn" type="button" (click)="currentStep = 5">{{ 'students.admission.back' | translate }}</button>
                <button class="btn btn-primary" type="submit" [disabled]="saving || !canUpdate">
                  {{ saving ? ('common.loading' | translate) : ('students.admission.finish' | translate) }}
                </button>
              </div>
            </form>
          }
        </div>
      }
    </div>
  `,
  styles: `
    .back { margin-right: 8px; }
    .banner { padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; }
    .banner.error { background: #fef2f2; border: 1px solid #fecaca; color: #b91c1c; }
    .banner.success { background: #ecfdf5; border: 1px solid #a7f3d0; color: #047857; }
    .pad { padding: 24px; }
    .steps { display: flex; gap: 8px; list-style: none; padding: 0; margin: 0 0 16px; overflow-x: auto; }
    .steps li button {
      display: flex; align-items: center; gap: 8px; padding: 8px 12px; border-radius: 8px;
      border: 1px solid var(--color-border); background: var(--color-surface); color: var(--color-muted);
      cursor: pointer; white-space: nowrap; font: inherit;
    }
    .steps li.active button { border-color: var(--color-primary); color: var(--color-primary); background: var(--color-primary-soft); }
    .steps li.done button { border-color: #059669; }
    .steps .num {
      width: 22px; height: 22px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center;
      background: var(--color-bg); font-size: .78rem; font-weight: 700;
    }
    .steps button:disabled { opacity: .5; cursor: not-allowed; }
    h2 { margin: 0 0 16px; font-size: 1.1rem; }
    h3 { margin: 18px 0 10px; font-size: .95rem; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field label { font-weight: 500; font-size: .85rem; color: var(--color-muted); }
    input, select, textarea {
      padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit;
      background: var(--color-surface); color: var(--color-text);
    }
    .span-2 { grid-column: span 2; }
    .form-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
    .check { display: flex; align-items: center; gap: 8px; margin: 12px 0; }
    .hint { font-size: .78rem; color: var(--color-muted); }
    .photo-row { display: flex; align-items: center; gap: 12px; }
    .photo { width: 72px; height: 90px; object-fit: cover; border: 1px solid var(--color-border); border-radius: 6px; }
    .multi-select {
      display: flex; flex-wrap: wrap; gap: 8px; padding: 10px 12px;
      border: 1px solid var(--color-border); border-radius: 8px; max-height: 180px; overflow-y: auto;
    }
    .check-item {
      display: flex; align-items: center; gap: 6px; padding: 4px 10px; border-radius: 16px;
      background: var(--color-bg); border: 1px solid var(--color-border); font-size: .85rem;
    }
    .docs { list-style: none; padding: 0; margin: 12px 0 0; display: flex; flex-direction: column; gap: 8px; }
    .docs li { display: flex; gap: 8px; align-items: center; border: 1px solid var(--color-border); padding: 8px 10px; border-radius: 8px; }
    .spacer { flex: 1; }
    .doc-upload { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
    .btn-sm { padding: 4px 10px; font-size: .82rem; }
    .btn-danger { background: #b91c1c; border-color: #b91c1c; color: #fff; }
    .hits { list-style: none; padding: 0; margin: 8px 0; border: 1px solid var(--color-border); border-radius: 8px; }
    .hits button { width: 100%; text-align: left; background: none; border: 0; padding: 8px 10px; cursor: pointer; font: inherit; }
    .hits button:hover { background: var(--color-primary-soft); }
    .chips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
    .chip { padding: 4px 8px; border-radius: 16px; background: var(--color-bg); border: 1px solid var(--color-border); }
    .chip button { border: 0; background: none; cursor: pointer; font: inherit; }
    .muted { color: var(--color-muted); }
    @media (max-width: 720px) { .form-grid { grid-template-columns: 1fr; } .span-2 { grid-column: span 1; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdmissionWizardComponent implements OnInit {
  readonly stepDefs = [
    { no: 1, key: 'students.admission.step1' },
    { no: 2, key: 'students.admission.step2' },
    { no: 3, key: 'students.admission.step3' },
    { no: 4, key: 'students.admission.step4' },
    { no: 5, key: 'students.admission.step5' },
    { no: 6, key: 'students.admission.step6' },
  ];
  readonly bloodGroups = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
  readonly docTypes = DOC_TYPES;

  studentId: string | null = null;
  wizard: AdmissionWizard | null = null;
  currentStep = 1;
  loading = false;
  saving = false;
  pageError = '';
  successMessage = '';
  issuedCredentials: IssuedCredentials | null = null;
  classes: ClassOption[] = [];
  sections: SectionOption[] = [];
  academicYears: AcademicYear[] = [];
  religions: LookupOption[] = [];
  categories: LookupOption[] = [];
  motherTongues: LookupOption[] = [];
  houses: LookupOption[] = [];
  media: LookupOption[] = [];
  streams: LookupOption[] = [];
  classSubjects: ClassSubjectOption[] = [];
  selectedSubjectIds: string[] = [];
  documents: StudentDocument[] = [];
  docType = 'BIRTH_CERTIFICATE';
  siblingQuery = '';
  siblingHits: Sibling[] = [];
  selectedSiblings: Sibling[] = [];
  private siblingTimer: ReturnType<typeof setTimeout> | null = null;

  readonly step1 = new FormGroup({
    firstName: new FormControl('', Validators.required),
    middleName: new FormControl(''),
    lastName: new FormControl(''),
    classId: new FormControl('', Validators.required),
    sectionId: new FormControl('', Validators.required),
    dateOfBirth: new FormControl('', Validators.required),
    gender: new FormControl('MALE', Validators.required),
    bloodGroup: new FormControl(''),
    religionId: new FormControl(''),
    categoryId: new FormControl(''),
    nationality: new FormControl('IN'),
    motherTongueId: new FormControl(''),
    aadhaarNo: new FormControl('', Validators.pattern(/^$|^\d{12}$/)),
    apaarId: new FormControl('', Validators.pattern(/^$|^\d{12}$/)),
  });

  readonly step2 = new FormGroup({
    father: this.memberGroup(),
    mother: this.memberGroup(),
    guardian: this.memberGroup(),
    guardianDifferent: new FormControl(false),
    emergencyName: new FormControl(''),
    emergencyRelation: new FormControl(''),
    emergencyPhone: new FormControl('', Validators.pattern(/^$|^\d{10}$/)),
    primaryContact: new FormControl('FATHER'),
  });

  readonly step3 = new FormGroup({
    currentAddress: this.addressGroup(),
    permanentAddress: this.addressGroup(),
    sameAsCurrent: new FormControl(true),
    phone: new FormControl('', Validators.pattern(/^$|^\d{10}$/)),
    email: new FormControl('', Validators.email),
  });

  readonly step4 = new FormGroup({
    academicYearId: new FormControl('', Validators.required),
    admissionDate: new FormControl(''),
    rollNumber: new FormControl<number | null>(null),
    houseId: new FormControl(''),
    mediumId: new FormControl(''),
    streamId: new FormControl(''),
  });

  readonly step5 = new FormGroup({
    noPreviousSchool: new FormControl(true),
    schoolName: new FormControl(''),
    board: new FormControl(''),
    medium: new FormControl(''),
    lastClass: new FormControl(''),
    yearOfPassing: new FormControl(''),
    percentage: new FormControl(''),
    tcNumber: new FormControl(''),
    tcDate: new FormControl(''),
    reason: new FormControl(''),
  });

  readonly step6 = new FormGroup({
    transportRequired: new FormControl(false),
    hostelRequired: new FormControl(false),
    medicalNotes: new FormControl(''),
    remarks: new FormControl(''),
  });

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private auth: AuthService,
  ) {}

  get canCreate(): boolean {
    return this.auth.hasPermission('STUDENT_CREATE');
  }

  get canUpdate(): boolean {
    return this.auth.hasPermission('STUDENT_UPDATE');
  }

  get canWriteStep1(): boolean {
    return this.studentId ? this.canUpdate : this.canCreate;
  }

  get sectionsForClass(): SectionOption[] {
    const classId = this.step1.value.classId;
    return this.sections.filter((s) => s.classId === classId);
  }

  get streamEnabled(): boolean {
    const classId = this.step1.value.classId || this.wizard?.classId;
    return this.classes.find((c) => c.id === classId)?.streamEnabled === true || this.wizard?.streamEnabled === true;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.studentId = id && id !== 'new' ? id : null;
    this.loadMeta();
    if (this.studentId) {
      this.loadWizard(this.studentId);
    }
  }

  isCompleted(stepNo: number): boolean {
    return this.wizard?.steps?.some((s) => s.stepNo === stepNo && s.status === 'COMPLETED') ?? false;
  }

  canOpen(stepNo: number): boolean {
    if (stepNo === 1) {
      return true;
    }
    return !!this.studentId && (stepNo === this.currentStep || this.isCompleted(stepNo) || this.isCompleted(stepNo - 1));
  }

  goToStep(stepNo: number): void {
    if (!this.canOpen(stepNo) || this.saving) {
      return;
    }
    this.currentStep = stepNo;
    if (stepNo === 4) {
      this.loadSubjects();
    }
    this.cdr.markForCheck();
  }

  onClassChange(): void {
    this.step1.patchValue({ sectionId: '' });
    this.loadSubjects();
  }

  isSubjectSelected(id: string): boolean {
    return this.selectedSubjectIds.includes(id);
  }

  toggleSubject(id: string): void {
    if (this.selectedSubjectIds.includes(id)) {
      this.selectedSubjectIds = this.selectedSubjectIds.filter((item) => item !== id);
    } else {
      this.selectedSubjectIds = [...this.selectedSubjectIds, id];
    }
    this.cdr.markForCheck();
  }

  saveStep1(): void {
    if (this.step1.invalid || this.saving) {
      return;
    }
    const v = this.step1.getRawValue();
    const body = {
      firstName: v.firstName,
      middleName: v.middleName || null,
      lastName: v.lastName || null,
      classId: v.classId,
      sectionId: v.sectionId,
      academicYearId: this.academicYears.find((y) => y.current)?.id || this.academicYears[0]?.id || null,
      dateOfBirth: v.dateOfBirth,
      gender: v.gender,
      bloodGroup: v.bloodGroup || null,
      religionId: v.religionId || null,
      categoryId: v.categoryId || null,
      nationality: v.nationality || 'IN',
      motherTongueId: v.motherTongueId || null,
      aadhaarNo: v.aadhaarNo || null,
      apaarId: v.apaarId || null,
    };
    if (this.studentId) {
      this.putStep(1, body, 2);
      return;
    }
    this.saving = true;
    this.pageError = '';
    this.http.post<ApiResponse<AdmissionWizard>>('/api/students/admission', body).subscribe({
      next: (res) => {
        this.saving = false;
        this.applyWizard(res.data);
        if (res.data.username && res.data.temporaryPassword) {
          this.issuedCredentials = { username: res.data.username, temporaryPassword: res.data.temporaryPassword };
        }
        this.studentId = res.data.id;
        this.currentStep = 2;
        this.successMessage = 'students.admission.stepSaved';
        void this.router.navigate(['/students', res.data.id, 'admission'], { replaceUrl: true });
        this.cdr.markForCheck();
      },
      error: (err) => this.onError(err),
    });
  }

  saveStep2(): void {
    const v = this.step2.getRawValue();
    this.putStep(2, {
      father: this.memberPayload(v.father),
      mother: this.memberPayload(v.mother),
      guardian: v.guardianDifferent ? this.memberPayload(v.guardian) : null,
      guardianDifferent: !!v.guardianDifferent,
      emergencyName: v.emergencyName || null,
      emergencyRelation: v.emergencyRelation || null,
      emergencyPhone: v.emergencyPhone || null,
      primaryContact: v.primaryContact,
    }, 3);
  }

  saveStep3(): void {
    const v = this.step3.getRawValue();
    this.putStep(3, {
      currentAddress: { addressType: 'CURRENT', ...v.currentAddress },
      permanentAddress: { addressType: 'PERMANENT', ...v.permanentAddress },
      sameAsCurrent: !!v.sameAsCurrent,
      phone: v.phone || null,
      email: v.email || null,
    }, 4);
  }

  saveStep4(): void {
    if (this.step4.invalid) {
      return;
    }
    const v = this.step4.getRawValue();
    this.putStep(4, {
      academicYearId: v.academicYearId,
      admissionDate: v.admissionDate || null,
      rollNumber: v.rollNumber || null,
      houseId: v.houseId || null,
      mediumId: v.mediumId || null,
      streamId: v.streamId || null,
      subjectIds: this.selectedSubjectIds,
    }, 5);
  }

  saveStep5(): void {
    const v = this.step5.getRawValue();
    this.putStep(5, {
      previousSchool: {
        noPreviousSchool: !!v.noPreviousSchool,
        schoolName: v.schoolName || null,
        board: v.board || null,
        medium: v.medium || null,
        lastClass: v.lastClass || null,
        yearOfPassing: v.yearOfPassing || null,
        percentage: v.percentage || null,
        tcNumber: v.tcNumber || null,
        tcDate: v.tcDate || null,
        reason: v.reason || null,
      },
    }, 6);
  }

  saveStep6(): void {
    const v = this.step6.getRawValue();
    this.putStep(6, {
      transportRequired: !!v.transportRequired,
      hostelRequired: !!v.hostelRequired,
      medicalNotes: v.medicalNotes || null,
      remarks: v.remarks || null,
      siblingIds: this.selectedSiblings.map((s) => s.id),
    }, 0);
  }

  onPhoto(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file || !this.studentId) {
      return;
    }
    if (file.size > 2 * 1024 * 1024 || !['image/jpeg', 'image/png', 'image/jpg'].includes(file.type)) {
      this.pageError = this.translateFallback('students.admission.photoHint');
      this.cdr.markForCheck();
      return;
    }
    const data = new FormData();
    data.append('file', file);
    this.http.post<ApiResponse<AdmissionWizard>>(`/api/students/${this.studentId}/photo`, data).subscribe({
      next: (res) => {
        this.applyWizard(res.data);
        this.successMessage = 'students.admission.photoSaved';
        this.cdr.markForCheck();
      },
      error: (err) => this.onError(err),
    });
  }

  onDocument(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file || !this.studentId) {
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.pageError = this.translateFallback('students.admission.docHint');
      this.cdr.markForCheck();
      return;
    }
    const data = new FormData();
    data.append('file', file);
    this.http.post<ApiResponse<StudentDocument>>(`/api/students/${this.studentId}/documents`, data, {
      params: { docType: this.docType },
    }).subscribe({
      next: (res) => {
        this.documents = [res.data, ...this.documents];
        this.successMessage = 'students.admission.docSaved';
        this.cdr.markForCheck();
      },
      error: (err) => this.onError(err),
    });
    (event.target as HTMLInputElement).value = '';
  }

  deleteDocument(id: string): void {
    if (!this.studentId) {
      return;
    }
    this.http.delete(`/api/students/${this.studentId}/documents/${id}`).subscribe({
      next: () => {
        this.documents = this.documents.filter((d) => d.id !== id);
        this.cdr.markForCheck();
      },
      error: (err) => this.onError(err),
    });
  }

  onSiblingSearch(value: string): void {
    this.siblingQuery = value;
    if (this.siblingTimer) {
      clearTimeout(this.siblingTimer);
    }
    this.siblingTimer = setTimeout(() => {
      if (value.trim().length < 2) {
        this.siblingHits = [];
        this.cdr.markForCheck();
        return;
      }
      this.http.get<ApiResponse<Sibling[]>>('/api/students/siblings', { params: { query: value.trim() } }).subscribe({
        next: (res) => {
          const selected = new Set(this.selectedSiblings.map((s) => s.id));
          this.siblingHits = (res.data ?? []).filter((s) => s.id !== this.studentId && !selected.has(s.id));
          this.cdr.markForCheck();
        },
        error: () => undefined,
      });
    }, 250);
  }

  addSibling(hit: Sibling): void {
    this.selectedSiblings = [...this.selectedSiblings, hit];
    this.siblingHits = [];
    this.siblingQuery = '';
    this.cdr.markForCheck();
  }

  removeSibling(id: string): void {
    this.selectedSiblings = this.selectedSiblings.filter((s) => s.id !== id);
    this.cdr.markForCheck();
  }

  loadSubjects(): void {
    const classId = this.step1.value.classId || this.wizard?.classId;
    if (!classId) {
      return;
    }
    const params: Record<string, string> = {};
    const streamId = this.step4.value.streamId;
    if (streamId) {
      params['streamId'] = streamId;
    }
    this.http.get<ApiResponse<ClassSubjectOption[]>>(`/api/classes/${classId}/subjects`, { params }).subscribe({
      next: (res) => {
        this.classSubjects = res.data ?? [];
        const compulsory = this.classSubjects.filter((s) => s.compulsory).map((s) => s.subjectId);
        const allowed = new Set(this.classSubjects.map((s) => s.subjectId));
        const keep = this.selectedSubjectIds.filter((id) => allowed.has(id));
        this.selectedSubjectIds = [...new Set([...compulsory, ...keep])];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  private putStep(stepNo: number, body: unknown, next: number): void {
    if (!this.studentId) {
      return;
    }
    this.saving = true;
    this.pageError = '';
    this.http.put<ApiResponse<AdmissionWizard>>(`/api/students/${this.studentId}/steps/${stepNo}`, body).subscribe({
      next: (res) => {
        this.saving = false;
        this.applyWizard(res.data);
        this.successMessage = 'students.admission.stepSaved';
        if (next === 0) {
          void this.router.navigate(['/students', this.studentId]);
        } else {
          this.currentStep = next;
          if (next === 4) {
            this.loadSubjects();
          }
        }
        this.cdr.markForCheck();
      },
      error: (err) => this.onError(err),
    });
  }

  private loadWizard(id: string): void {
    this.loading = true;
    this.http.get<ApiResponse<AdmissionWizard>>(`/api/students/${id}/admission`).subscribe({
      next: (res) => {
        this.applyWizard(res.data);
        const pending = (res.data.steps ?? []).find((s) => s.status !== 'COMPLETED');
        this.currentStep = pending?.stepNo ?? 6;
        if (this.currentStep === 4) {
          this.loadSubjects();
        }
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loading = false;
        this.onError(err);
      },
    });
  }

  private applyWizard(data: AdmissionWizard): void {
    this.wizard = data;
    this.documents = data.documents ?? [];
    this.selectedSiblings = data.siblings ?? [];
    this.selectedSubjectIds = data.subjectIds ?? [];
    this.step1.patchValue({
      firstName: data.firstName ?? '',
      middleName: data.middleName ?? '',
      lastName: data.lastName ?? '',
      classId: data.classId ?? '',
      sectionId: data.sectionId ?? '',
      dateOfBirth: data.dateOfBirth ?? '',
      gender: data.gender ?? 'MALE',
      bloodGroup: data.bloodGroup ?? '',
      religionId: data.religionId ?? '',
      categoryId: data.categoryId ?? '',
      nationality: data.nationality ?? 'IN',
      motherTongueId: data.motherTongueId ?? '',
      aadhaarNo: data.aadhaarNo ?? '',
      apaarId: data.apaarId ?? '',
    });
    this.patchMember('father', data.father);
    this.patchMember('mother', data.mother);
    this.patchMember('guardian', data.guardian);
    this.step2.patchValue({
      guardianDifferent: data.guardianDifferent,
      emergencyName: data.emergencyName ?? '',
      emergencyRelation: data.emergencyRelation ?? '',
      emergencyPhone: data.emergencyPhone ?? '',
      primaryContact: data.primaryContact ?? 'FATHER',
    });
    this.patchAddress('currentAddress', data.currentAddress);
    this.patchAddress('permanentAddress', data.permanentAddress);
    this.step3.patchValue({
      sameAsCurrent: data.sameAsCurrent,
      phone: data.phone ?? '',
      email: data.email ?? '',
    });
    this.step4.patchValue({
      academicYearId: data.academicYearId ?? this.academicYears.find((y) => y.current)?.id ?? '',
      admissionDate: data.admissionDate ?? '',
      rollNumber: data.rollNumber,
      houseId: data.houseId ?? '',
      mediumId: data.mediumId ?? '',
      streamId: data.streamId ?? '',
    });
    const prev = data.previousSchool;
    this.step5.patchValue({
      noPreviousSchool: prev?.noPreviousSchool ?? true,
      schoolName: prev?.schoolName ?? '',
      board: prev?.board ?? '',
      medium: prev?.medium ?? '',
      lastClass: prev?.lastClass ?? '',
      yearOfPassing: prev?.yearOfPassing ?? '',
      percentage: prev?.percentage ?? '',
      tcNumber: prev?.tcNumber ?? '',
      tcDate: prev?.tcDate ?? '',
      reason: prev?.reason ?? '',
    });
    this.step6.patchValue({
      transportRequired: data.transportRequired,
      hostelRequired: data.hostelRequired,
      medicalNotes: data.medicalNotes ?? '',
      remarks: data.remarks ?? '',
    });
  }

  private loadMeta(): void {
    this.http.get<ApiResponse<ClassOption[]>>('/api/classes').subscribe({
      next: (res) => {
        this.classes = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
    this.http.get<ApiResponse<SectionOption[]>>('/api/sections').subscribe({
      next: (res) => {
        this.sections = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
    this.http.get<ApiResponse<AcademicYear[]>>('/api/academic-years').subscribe({
      next: (res) => {
        this.academicYears = res.data ?? [];
        if (!this.step4.value.academicYearId) {
          const current = this.academicYears.find((y) => y.current) ?? this.academicYears[0];
          if (current) {
            this.step4.patchValue({ academicYearId: current.id });
          }
        }
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
    this.loadLookup('RELIGION', (rows) => { this.religions = rows; });
    this.loadLookup('CATEGORY', (rows) => { this.categories = rows; });
    this.loadLookup('MOTHER_TONGUE', (rows) => { this.motherTongues = rows; });
    this.loadLookup('HOUSE', (rows) => { this.houses = rows; });
    this.loadLookup('MEDIUM', (rows) => { this.media = rows; });
    this.http.get<ApiResponse<LookupOption[]>>('/api/streams').subscribe({
      next: (res) => {
        this.streams = res.data ?? [];
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  private loadLookup(kind: string, assign: (rows: LookupOption[]) => void): void {
    this.http.get<ApiResponse<LookupOption[]>>(`/api/lookups/${kind}`).subscribe({
      next: (res) => {
        assign(res.data ?? []);
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  private memberGroup(): FormGroup {
    return new FormGroup({
      firstName: new FormControl(''),
      lastName: new FormControl(''),
      occupation: new FormControl(''),
      phone: new FormControl('', Validators.pattern(/^$|^\d{10}$/)),
      email: new FormControl('', Validators.email),
      qualification: new FormControl(''),
    });
  }

  private addressGroup(): FormGroup {
    return new FormGroup({
      line1: new FormControl(''),
      line2: new FormControl(''),
      city: new FormControl(''),
      district: new FormControl(''),
      state: new FormControl(''),
      pin: new FormControl('', Validators.pattern(/^$|^\d{6}$/)),
    });
  }

  private memberPayload(value: Record<string, string | null | undefined> | null | undefined): FamilyMember | null {
    if (!value || !value['firstName']) {
      return null;
    }
    return {
      firstName: value['firstName'],
      lastName: value['lastName'] || null,
      occupation: value['occupation'] || null,
      phone: value['phone'] || null,
      email: value['email'] || null,
      qualification: value['qualification'] || null,
    };
  }

  private patchMember(key: 'father' | 'mother' | 'guardian', member: FamilyMember | null): void {
    this.step2.get(key)?.patchValue({
      firstName: member?.firstName ?? '',
      lastName: member?.lastName ?? '',
      occupation: member?.occupation ?? '',
      phone: member?.phone ?? '',
      email: member?.email ?? '',
      qualification: member?.qualification ?? '',
    });
  }

  private patchAddress(key: 'currentAddress' | 'permanentAddress', address: Address | null): void {
    this.step3.get(key)?.patchValue({
      line1: address?.line1 ?? '',
      line2: address?.line2 ?? '',
      city: address?.city ?? '',
      district: address?.district ?? '',
      state: address?.state ?? '',
      pin: address?.pin ?? '',
    });
  }

  private onError(err: HttpErrorResponse): void {
    this.saving = false;
    const body = err.error as ApiError | undefined;
    this.pageError = body?.message || err.message || 'Error';
    this.cdr.markForCheck();
  }

  private translateFallback(key: string): string {
    return key;
  }
}
