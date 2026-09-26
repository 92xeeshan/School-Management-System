import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';

/**
 * Consistent "no records found" empty state used inside cards, tables,
 * and pages across feature modules. Actions (e.g. a "Refresh" or
 * "Browse students" button) are passed in via content projection so
 * each usage stays contextual to what's empty.
 *
 * Usage:
 * <app-empty-state icon="receipt_long" titleKey="fees.noInstallments" hintKey="fees.noInstallmentsHint">
 *   <button mat-stroked-button (click)="refresh()">
 *     <mat-icon>refresh</mat-icon>{{ 'common.refresh' | translate }}
 *   </button>
 * </app-empty-state>
 */
@Component({
  selector: 'app-empty-state',
  imports: [MatIconModule, TranslateModule],
  template: `
    <mat-icon class="empty-state__icon">{{ icon }}</mat-icon>
    <p class="empty-state__title">{{ titleKey | translate }}</p>
    @if (hintKey) {
      <p class="empty-state__hint">{{ hintKey | translate }}</p>
    }
    <div class="empty-state__actions">
      <ng-content />
    </div>
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      padding: 40px 24px;
      color: var(--color-muted);
    }
    .empty-state__icon {
      font-size: 40px;
      width: 40px;
      height: 40px;
      opacity: 0.5;
      margin-bottom: 12px;
    }
    .empty-state__title {
      margin: 0;
      font-weight: 600;
      font-size: 0.95rem;
      color: var(--color-text);
    }
    .empty-state__hint {
      margin: 4px 0 0;
      font-size: 0.85rem;
      max-width: 320px;
    }
    .empty-state__actions {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
      justify-content: center;
      margin-top: 16px;
    }
    .empty-state__actions:empty {
      display: none;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyStateComponent {
  @Input({ required: true }) icon!: string;
  @Input({ required: true }) titleKey!: string;
  @Input() hintKey?: string;
}
