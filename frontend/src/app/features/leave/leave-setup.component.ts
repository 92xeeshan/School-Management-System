import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import {
  LeavePolicy,
  LeaveStaffCategory,
  LeaveType,
  LeaveYearMode,
  LeaveYearSetting,
  StaffLeaveOption,
  StaffType,
} from './leave.model';
import { LeaveService } from './leave.service';

@Component({
  selector: 'app-leave-setup',
  imports: [TranslateModule, ReactiveFormsModule, MatButtonModule],
  template: `
    <div class="stack">
      @if (error) {
        <p class="error">{{ error }}</p>
      }
      @if (message) {
        <p class="banner success">{{ message }}</p>
      }

      <section class="card">
        <h2>{{ 'leave.year' | translate }}</h2>
        @if (year) {
          <p class="muted">{{ year.yearStart }} — {{ year.yearEnd }}</p>
        }
        <div class="row">
          <select [value]="year?.mode" (change)="saveYear($any($event.target).value)">
            <option value="ACADEMIC">{{ 'leave.yearMode.ACADEMIC' | translate }}</option>
            <option value="CALENDAR">{{ 'leave.yearMode.CALENDAR' | translate }}</option>
          </select>
        </div>
        <p class="muted">{{ 'leave.holidayHint' | translate }}</p>
      </section>

      <section class="card">
        <div class="toolbar">
          <h2>{{ 'leave.types' | translate }}</h2>
          <button mat-flat-button color="primary" type="button" (click)="showTypeForm = !showTypeForm">
            {{ 'leave.addType' | translate }}
          </button>
        </div>
        @if (showTypeForm) {
          <form class="form" [formGroup]="typeForm" (ngSubmit)="saveType()">
            <input formControlName="code" [placeholder]="'leave.code' | translate" />
            <input formControlName="name" [placeholder]="'leave.name' | translate" />
            <input type="number" formControlName="yearlyQuota" [placeholder]="'leave.quota' | translate" />
            <label class="check"><input type="checkbox" formControlName="paid" /> {{ 'leave.paid' | translate }}</label>
            <label class="check"><input type="checkbox" formControlName="reasonMandatory" /> {{ 'leave.reasonMandatory' | translate }}</label>
            <button mat-flat-button color="primary" type="submit" [disabled]="typeForm.invalid">{{ 'common.save' | translate }}</button>
          </form>
        }
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'leave.code' | translate }}</th>
                <th>{{ 'leave.name' | translate }}</th>
                <th>{{ 'leave.quota' | translate }}</th>
                <th>{{ 'leave.paid' | translate }}</th>
                <th>{{ 'common.status' | translate }}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (type of types; track type.id) {
                <tr>
                  <td>{{ type.code }}</td>
                  <td>{{ type.name }}</td>
                  <td>{{ type.yearlyQuota }}</td>
                  <td>{{ type.paid ? ('common.yes' | translate) : ('common.no' | translate) }}</td>
                  <td>{{ type.active ? ('leave.active' | translate) : ('leave.inactive' | translate) }}</td>
                  <td>
                    @if (type.active) {
                      <button mat-button type="button" (click)="deactivate(type)">{{ 'leave.deactivate' | translate }}</button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>

      <section class="card">
        <h2>{{ 'leave.policies' | translate }}</h2>
        <form class="form" [formGroup]="policyForm" (ngSubmit)="savePolicy()">
          <input formControlName="name" [placeholder]="'leave.policyName' | translate" />
          <select formControlName="staffCategory">
            <option value="TEACHING">{{ 'leave.category.TEACHING' | translate }}</option>
            <option value="NON_TEACHING">{{ 'leave.category.NON_TEACHING' | translate }}</option>
            <option value="ALL">{{ 'leave.category.ALL' | translate }}</option>
          </select>
          <button mat-flat-button color="primary" type="submit" [disabled]="policyForm.invalid">{{ 'common.save' | translate }}</button>
        </form>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'leave.name' | translate }}</th>
                <th>{{ 'leave.staffCategory' | translate }}</th>
                <th>{{ 'leave.types' | translate }}</th>
              </tr>
            </thead>
            <tbody>
              @for (policy of policies; track policy.id) {
                <tr>
                  <td>{{ policy.name }}</td>
                  <td>{{ ('leave.category.' + policy.staffCategory) | translate }}</td>
                  <td>{{ itemSummary(policy) }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>

      <section class="card">
        <h2>{{ 'leave.assign' | translate }}</h2>
        <form class="form" [formGroup]="assignForm" (ngSubmit)="assign()">
          <select formControlName="policyId">
            @for (policy of policies; track policy.id) {
              <option [value]="policy.id">{{ policy.name }}</option>
            }
          </select>
          <select formControlName="staffType">
            <option value="TEACHING">{{ 'leave.category.TEACHING' | translate }}</option>
            <option value="NON_TEACHING">{{ 'leave.category.NON_TEACHING' | translate }}</option>
          </select>
          <select formControlName="staffId">
            @for (person of staffForAssign; track person.staffId) {
              <option [value]="person.staffId">{{ person.staffName }} ({{ person.employeeNo }})</option>
            }
          </select>
          <button mat-flat-button color="primary" type="submit" [disabled]="assignForm.invalid">{{ 'leave.assign' | translate }}</button>
        </form>
      </section>
    </div>
  `,
  styles: `
    .stack { display: flex; flex-direction: column; gap: 20px; }
    .card { padding: 16px; border: 1px solid var(--color-border); border-radius: 12px; display: grid; gap: 12px; }
    .form, .row, .toolbar { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; }
    input, select { padding: 8px 10px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; }
    .table-wrap { overflow: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--color-border); }
    .check { display: flex; gap: 6px; align-items: center; font-size: .86rem; }
    .error { color: #b91c1c; }
    .banner.success { color: #166534; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeaveSetupComponent implements OnInit {
  types: LeaveType[] = [];
  policies: LeavePolicy[] = [];
  staff: StaffLeaveOption[] = [];
  year: LeaveYearSetting | null = null;
  showTypeForm = false;
  error = '';
  message = '';
  typeForm;
  policyForm;
  assignForm;

  constructor(private leave: LeaveService, private fb: FormBuilder, private cdr: ChangeDetectorRef) {
    this.typeForm = this.fb.nonNullable.group({
      code: ['', Validators.required],
      name: ['', Validators.required],
      yearlyQuota: [0, Validators.required],
      paid: [true],
      reasonMandatory: [true],
    });
    this.policyForm = this.fb.nonNullable.group({
      name: ['', Validators.required],
      staffCategory: this.fb.nonNullable.control<LeaveStaffCategory>('TEACHING'),
    });
    this.assignForm = this.fb.nonNullable.group({
      policyId: ['', Validators.required],
      staffType: this.fb.nonNullable.control<StaffType>('TEACHING'),
      staffId: ['', Validators.required],
    });
  }

  get staffForAssign(): StaffLeaveOption[] {
    const type = this.assignForm.value.staffType;
    return this.staff.filter((row) => row.staffType === type);
  }

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.leave.listTypes().subscribe((rows) => {
      this.types = rows;
      this.cdr.markForCheck();
    });
    this.leave.listPolicies().subscribe((rows) => {
      this.policies = rows;
      if (!this.assignForm.value.policyId && rows.length) {
        this.assignForm.patchValue({ policyId: rows[0].id });
      }
      this.cdr.markForCheck();
    });
    this.leave.listStaff().subscribe((rows) => {
      this.staff = rows;
      if (!this.assignForm.value.staffId && rows.length) {
        this.assignForm.patchValue({ staffId: rows[0].staffId, staffType: rows[0].staffType });
      }
      this.cdr.markForCheck();
    });
    this.leave.yearSetting().subscribe((year) => {
      this.year = year;
      this.cdr.markForCheck();
    });
  }

  saveType(): void {
    if (this.typeForm.invalid) {
      return;
    }
    this.leave.createType(this.typeForm.getRawValue()).subscribe({
      next: () => {
        this.message = 'Leave type saved.';
        this.typeForm.reset({ paid: true, reasonMandatory: true, yearlyQuota: 0, code: '', name: '' });
        this.reload();
      },
      error: (err) => this.fail(err),
    });
  }

  deactivate(type: LeaveType): void {
    this.leave.deactivateType(type.id).subscribe({
      next: () => this.reload(),
      error: (err) => this.fail(err),
    });
  }

  savePolicy(): void {
    if (this.policyForm.invalid) {
      return;
    }
    const items = this.types.filter((type) => type.active).map((type) => ({
      leaveTypeId: type.id,
      yearlyQuota: type.yearlyQuota,
    }));
    this.leave.savePolicy({ ...this.policyForm.getRawValue(), items, active: true }).subscribe({
      next: () => {
        this.message = 'Leave policy saved.';
        this.reload();
      },
      error: (err) => this.fail(err),
    });
  }

  assign(): void {
    if (this.assignForm.invalid) {
      return;
    }
    const value = this.assignForm.getRawValue();
    this.leave.assignPolicy(value.policyId, value.staffType, [value.staffId]).subscribe({
      next: () => {
        this.message = 'Leave policy assigned.';
        this.reload();
      },
      error: (err) => this.fail(err),
    });
  }

  saveYear(mode: LeaveYearMode): void {
    this.leave.updateYear(mode).subscribe({
      next: (year) => {
        this.year = year;
        this.message = 'Leave year saved.';
        this.cdr.markForCheck();
      },
      error: (err) => this.fail(err),
    });
  }

  itemSummary(policy: LeavePolicy): string {
    return policy.items.map((item) => item.leaveTypeCode).join(', ');
  }

  private fail(err: { error?: { message?: string } }): void {
    this.error = err?.error?.message || 'Could not save leave setup.';
    this.cdr.markForCheck();
  }
}
