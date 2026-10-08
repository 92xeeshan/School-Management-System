import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';
import { FEE_FREQUENCIES, FeeFrequency, FeeHead } from './fees.model';
import { FeesService } from './fees.service';

@Component({
  selector: 'app-fee-heads',
  imports: [
    TranslateModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    EmptyStateComponent,
  ],
  template: `
    <div class="stack">
      @if (canManage) {
        <div class="toolbar">
          <button mat-flat-button color="primary" type="button" (click)="toggleForm()">
            {{ editing ? ('common.edit' | translate) : ('fees.addHead' | translate) }}
          </button>
        </div>
      }
      @if (showForm) {
        <form class="card form" [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.headName' | translate }}</mat-label>
            <input matInput formControlName="name" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.code' | translate }}</mat-label>
            <input matInput formControlName="code" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'fees.frequency' | translate }}</mat-label>
            <mat-select formControlName="frequency">
              @for (freq of frequencies; track freq) {
                <mat-option [value]="freq">{{ 'fees.freq.' + freq | translate }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline" class="wide">
            <mat-label>{{ 'fees.description' | translate }}</mat-label>
            <input matInput formControlName="description" />
          </mat-form-field>
          <mat-checkbox formControlName="optional">{{ 'fees.optional' | translate }}</mat-checkbox>
          <mat-checkbox formControlName="refundable">{{ 'fees.refundable' | translate }}</mat-checkbox>
          <div class="actions">
            <button mat-button type="button" (click)="cancel()">{{ 'common.cancel' | translate }}</button>
            <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || saving">
              {{ 'common.save' | translate }}
            </button>
          </div>
        </form>
      }
      @if (heads.length === 0) {
        <app-empty-state [title]="'common.noData' | translate" [hint]="'common.emptyHint' | translate" />
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'fees.headName' | translate }}</th>
                <th>{{ 'fees.code' | translate }}</th>
                <th>{{ 'fees.frequency' | translate }}</th>
                <th>{{ 'fees.type' | translate }}</th>
                <th>{{ 'common.status' | translate }}</th>
                @if (canManage) { <th></th> }
              </tr>
            </thead>
            <tbody>
              @for (row of heads; track row.id) {
                <tr>
                  <td>{{ row.name }}</td>
                  <td>{{ row.code }}</td>
                  <td>{{ 'fees.freq.' + row.frequency | translate }}</td>
                  <td>{{ row.optional ? ('fees.optional' | translate) : ('fees.mandatory' | translate) }}</td>
                  <td><span class="badge" [class.badge-success]="row.status === 'ACTIVE'" [class.badge-muted]="row.status !== 'ACTIVE'">{{ row.status }}</span></td>
                  @if (canManage) {
                    <td class="actions-cell">
                      <button mat-button type="button" (click)="edit(row)">{{ 'common.edit' | translate }}</button>
                      @if (row.status === 'ACTIVE') {
                        <button mat-button type="button" (click)="deactivate(row)">{{ 'fees.deactivate' | translate }}</button>
                      }
                    </td>
                  }
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  styles: `
    .stack { display: flex; flex-direction: column; gap: 16px; }
    .toolbar { display: flex; justify-content: flex-end; }
    .card { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 12px; padding: 16px; }
    .form { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; align-items: center; }
    .wide { grid-column: 1 / -1; }
    .actions { grid-column: 1 / -1; display: flex; justify-content: flex-end; gap: 8px; }
    .table-wrap { overflow: auto; border: 1px solid var(--color-border); border-radius: 12px; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--color-border); }
    th { font-size: .8rem; color: var(--color-muted); }
    .actions-cell { white-space: nowrap; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeeHeadsComponent implements OnInit {
  heads: FeeHead[] = [];
  showForm = false;
  saving = false;
  editing: FeeHead | null = null;
  frequencies = FEE_FREQUENCIES;
  form;

  constructor(
    private fees: FeesService,
    private auth: AuthService,
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.nonNullable.group({
      name: ['', Validators.required],
      code: ['', Validators.required],
      description: [''],
      optional: [false],
      refundable: [false],
      frequency: ['MONTHLY' as FeeFrequency, Validators.required],
    });
  }

  get canManage(): boolean {
    return this.auth.hasPermission('FEE_STRUCTURE_MANAGE');
  }

  ngOnInit(): void {
    this.refresh();
  }

  toggleForm(): void {
    this.showForm = !this.showForm;
    if (!this.showForm) {
      this.cancel();
    }
  }

  edit(row: FeeHead): void {
    this.editing = row;
    this.showForm = true;
    this.form.patchValue({
      name: row.name,
      code: row.code,
      description: row.description ?? '',
      optional: row.optional,
      refundable: row.refundable,
      frequency: row.frequency,
    });
  }

  cancel(): void {
    this.showForm = false;
    this.editing = null;
    this.form.reset({ name: '', code: '', description: '', optional: false, refundable: false, frequency: 'MONTHLY' });
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.saving = true;
    const value = this.form.getRawValue();
    const payload = {
      name: value.name,
      code: value.code,
      description: value.description || null,
      optional: value.optional,
      refundable: value.refundable,
      frequency: value.frequency,
      status: this.editing?.status ?? 'ACTIVE',
    };
    const req = this.editing
      ? this.fees.updateHead(this.editing.id, payload)
      : this.fees.createHead(payload);
    req.subscribe({
      next: () => {
        this.saving = false;
        this.cancel();
        this.refresh();
      },
      error: () => {
        this.saving = false;
        this.cdr.markForCheck();
      },
    });
  }

  deactivate(row: FeeHead): void {
    this.fees.deactivateHead(row.id).subscribe(() => this.refresh());
  }

  private refresh(): void {
    this.fees.listHeads().subscribe((rows) => {
      this.heads = rows;
      this.cdr.markForCheck();
    });
  }
}
