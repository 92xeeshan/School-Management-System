import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

export interface PasswordResetPayload {
  temporaryPassword?: string;
}

@Component({
  selector: 'app-password-reset-dialog',
  imports: [TranslateModule, ReactiveFormsModule],
  template: `
    @if (open) {
      <div class="modal-backdrop" (click)="cancel()">
        <div class="modal" role="dialog" aria-modal="true" [attr.aria-label]="'users.resetPassword' | translate" (click)="$event.stopPropagation()">
          <h2>{{ 'users.resetPassword' | translate }}</h2>
          @if (targetName) {
            <p class="hint">{{ targetName }}</p>
          }
          <form [formGroup]="form" (ngSubmit)="submit()">
            <fieldset>
              <label class="choice">
                <input type="radio" formControlName="mode" value="auto" />
                {{ 'users.resetAuto' | translate }}
              </label>
              <label class="choice">
                <input type="radio" formControlName="mode" value="custom" />
                {{ 'users.resetCustom' | translate }}
              </label>
            </fieldset>
            @if (form.value.mode === 'custom') {
              <div class="field">
                <label>{{ 'credentials.temporaryPassword' | translate }}</label>
                <input type="text" formControlName="temporaryPassword" autocomplete="off" />
                <p class="hint">{{ 'auth.firstLogin.hint' | translate }}</p>
              </div>
            }
            @if (error) {
              <p class="error">{{ error | translate }}</p>
            }
            <div class="form-actions">
              <button class="btn" type="button" [disabled]="saving" (click)="cancel()">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="saving || form.invalid">
                {{ saving ? ('common.loading' | translate) : ('users.resetPassword' | translate) }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: `
    .modal-backdrop { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 50; }
    .modal { width: 420px; max-width: 94vw; background: var(--color-surface); border-radius: 12px; padding: 24px; }
    h2 { margin: 0 0 8px; font-size: 1.15rem; }
    .hint { color: var(--color-muted); font-size: .85rem; margin: 0 0 12px; }
    fieldset { border: 0; padding: 0; margin: 0 0 12px; display: flex; flex-direction: column; gap: 8px; }
    .choice { display: flex; align-items: center; gap: 8px; font-size: .9rem; }
    .field { margin-bottom: 12px; }
    label { display: block; margin-bottom: 6px; font-size: .85rem; color: var(--color-muted); }
    input[type='text'] { width: 100%; padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; background: var(--color-surface); color: var(--color-text); }
    .error { color: #b91c1c; font-size: .85rem; }
    .form-actions { display: flex; justify-content: flex-end; gap: 10px; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PasswordResetDialogComponent implements OnChanges {
  @Input() open = false;
  @Input() targetName = '';
  @Input() saving = false;
  @Input() error = '';
  @Output() cancelled = new EventEmitter<void>();
  @Output() confirmed = new EventEmitter<PasswordResetPayload>();

  readonly form = new FormGroup({
    mode: new FormControl<'auto' | 'custom'>('auto', { nonNullable: true }),
    temporaryPassword: new FormControl(''),
  });

  constructor(private cdr: ChangeDetectorRef) {
    this.form.controls.mode.valueChanges.subscribe((mode) => {
      const control = this.form.controls.temporaryPassword;
      if (mode === 'custom') {
        control.setValidators([Validators.required, Validators.minLength(8), Validators.maxLength(72)]);
      } else {
        control.clearValidators();
        control.setValue('');
      }
      control.updateValueAndValidity();
      this.cdr.markForCheck();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open']?.currentValue) {
      this.form.reset({ mode: 'auto', temporaryPassword: '' });
      this.form.controls.temporaryPassword.clearValidators();
      this.form.controls.temporaryPassword.updateValueAndValidity();
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const mode = this.form.controls.mode.value;
    const custom = this.form.controls.temporaryPassword.value?.trim() ?? '';
    if (mode === 'custom') {
      this.confirmed.emit({ temporaryPassword: custom });
      return;
    }
    this.confirmed.emit({});
  }

  cancel(): void {
    if (this.saving) {
      return;
    }
    this.form.reset({ mode: 'auto', temporaryPassword: '' });
    this.cancelled.emit();
  }
}
