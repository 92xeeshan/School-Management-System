import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

export interface IssuedCredentials {
  username: string;
  temporaryPassword: string;
}

@Component({
  selector: 'app-credential-handover',
  imports: [TranslateModule],
  template: `
    @if (credentials) {
      <div class="modal-backdrop" (click)="dismiss()">
        <div class="modal" role="dialog" aria-modal="true" [attr.aria-label]="'credentials.issued' | translate" (click)="$event.stopPropagation()">
          <h2>{{ 'credentials.issued' | translate }}</h2>
          <p class="hint">{{ 'credentials.copyHint' | translate }}</p>
          <dl>
            <div>
              <dt>{{ 'credentials.username' | translate }}</dt>
              <dd><code>{{ credentials.username }}</code></dd>
            </div>
            <div>
              <dt>{{ 'credentials.temporaryPassword' | translate }}</dt>
              <dd><code>{{ credentials.temporaryPassword }}</code></dd>
            </div>
          </dl>
          <p class="once">{{ 'credentials.shownOnce' | translate }}</p>
          <div class="form-actions">
            <button class="btn" type="button" (click)="copy()">
              {{ copied ? ('credentials.copied' | translate) : ('credentials.copy' | translate) }}
            </button>
            <button class="btn btn-primary" type="button" (click)="dismiss()">{{ 'credentials.close' | translate }}</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    .modal-backdrop { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: center; justify-content: center; z-index: 50; }
    .modal { width: 420px; max-width: 94vw; background: var(--color-surface); border-radius: 12px; padding: 24px; }
    h2 { margin: 0 0 8px; font-size: 1.15rem; }
    .hint, .once { color: var(--color-muted); font-size: .9rem; margin: 0 0 16px; }
    .once { margin-top: 4px; }
    dl { margin: 0 0 8px; display: flex; flex-direction: column; gap: 12px; }
    dt { font-size: .8rem; color: var(--color-muted); margin-bottom: 4px; }
    dd { margin: 0; }
    code { font-size: 1rem; background: var(--color-bg, #f4f4f5); padding: 4px 8px; border-radius: 6px; }
    .form-actions { display: flex; justify-content: flex-end; gap: 10px; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CredentialHandoverComponent {
  @Input() credentials: IssuedCredentials | null = null;
  @Output() closed = new EventEmitter<void>();

  copied = false;

  constructor(private cdr: ChangeDetectorRef) {}

  copy(): void {
    if (!this.credentials) {
      return;
    }
    const text = `${this.credentials.username}\n${this.credentials.temporaryPassword}`;
    const done = () => {
      this.copied = true;
      this.cdr.markForCheck();
    };
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(text).then(done).catch(() => this.fallbackCopy(text, done));
    } else {
      this.fallbackCopy(text, done);
    }
  }

  dismiss(): void {
    this.copied = false;
    this.closed.emit();
  }

  private fallbackCopy(text: string, done: () => void): void {
    const area = document.createElement('textarea');
    area.value = text;
    area.setAttribute('readonly', '');
    area.style.position = 'fixed';
    area.style.left = '-9999px';
    document.body.appendChild(area);
    area.select();
    try {
      document.execCommand('copy');
    } catch {
      /* ignore */
    }
    document.body.removeChild(area);
    done();
  }
}
