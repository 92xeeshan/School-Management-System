import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, HostListener, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { NotificationService } from '../../core/notifications/notification.service';
import { AppNotification } from '../../core/notifications/notification.model';

@Component({
  selector: 'app-notification-bell',
  imports: [TranslateModule],
  template: `
    <div class="bell-wrap">
      <button type="button" class="bell-btn" (click)="toggle($event)" [attr.aria-label]="'notifications.open' | translate">
        <svg class="bell-icon" viewBox="0 0 24 24" aria-hidden="true">
          <path fill="currentColor"
            d="M12 22a2.5 2.5 0 0 0 2.45-2h-4.9A2.5 2.5 0 0 0 12 22Zm8-6V11a8 8 0 1 0-16 0v5l-2 2v1h20v-1l-2-2Z"/>
        </svg>
        @if (unreadCount > 0) {
          <span class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        }
      </button>
      @if (open) {
        <div class="panel" role="dialog" [attr.aria-label]="'notifications.title' | translate">
          <div class="panel-head">
            <strong>{{ 'notifications.title' | translate }}</strong>
            @if (unreadCount > 0) {
              <button type="button" class="link-btn" (click)="markAll($event)">{{ 'notifications.markAllRead' | translate }}</button>
            }
          </div>
          @if (loading) {
            <div class="empty">{{ 'common.loading' | translate }}</div>
          } @else if (items.length === 0) {
            <div class="empty">{{ 'notifications.empty' | translate }}</div>
          } @else {
            <ul class="list">
              @for (item of items; track item.id) {
                <li [class.unread]="!item.read" (click)="openItem(item, $event)">
                  <span class="cat">{{ iconFor(item.category) }}</span>
                  <span class="body">
                    <span class="title">{{ item.title }}</span>
                    <span class="preview">{{ item.message }}</span>
                    <span class="meta">{{ relativeTime(item.createdAt) }}</span>
                  </span>
                  @if (!item.read) {
                    <span class="dot" [attr.aria-label]="'notifications.unread' | translate"></span>
                  }
                </li>
              }
            </ul>
          }
        </div>
      }
    </div>
  `,
  styles: `
    :host { position: relative; display: inline-flex; }
    .bell-wrap { position: relative; }
    .bell-btn {
      position: relative;
      width: 36px; height: 36px;
      border: 1px solid var(--color-border);
      border-radius: 50%;
      background: #fff;
      color: #334155;
      display: inline-flex; align-items: center; justify-content: center;
      cursor: pointer;
    }
    .bell-btn:hover { background: #f8fafc; }
    .bell-icon { width: 18px; height: 18px; }
    .badge {
      position: absolute; top: -4px; right: -4px;
      min-width: 18px; height: 18px; padding: 0 5px;
      border-radius: 999px;
      background: #dc2626; color: #fff;
      font-size: 11px; font-weight: 700; line-height: 18px;
    }
    .panel {
      position: absolute; right: 0; top: calc(100% + 8px);
      width: 360px; max-width: 92vw;
      background: #fff;
      border: 1px solid var(--color-border);
      border-radius: 12px;
      box-shadow: 0 12px 32px rgba(15, 23, 42, .12);
      z-index: 40;
      overflow: hidden;
    }
    .panel-head {
      display: flex; align-items: center; justify-content: space-between;
      padding: 12px 14px;
      border-bottom: 1px solid var(--color-border);
    }
    .link-btn {
      border: 0; background: none; color: var(--color-primary);
      cursor: pointer; font: inherit; font-size: .85rem;
    }
    .empty { padding: 28px 16px; text-align: center; color: var(--color-muted); }
    .list { list-style: none; margin: 0; padding: 0; max-height: 420px; overflow: auto; }
    .list li {
      display: grid; grid-template-columns: 28px 1fr 10px; gap: 10px;
      padding: 12px 14px;
      border-bottom: 1px solid var(--color-border);
      cursor: pointer;
    }
    .list li:hover { background: #f8fafc; }
    .list li.unread { background: #eff6ff; }
    .cat { font-size: 1.05rem; line-height: 1.4; }
    .body { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
    .title { font-weight: 600; font-size: .92rem; }
    .preview {
      color: var(--color-muted);
      font-size: .82rem;
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .meta { color: var(--color-muted); font-size: .75rem; }
    .dot {
      width: 8px; height: 8px; margin-top: 6px; border-radius: 50%;
      background: var(--color-primary); justify-self: end;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationBellComponent implements OnInit {
  open = false;
  loading = false;
  items: AppNotification[] = [];
  unreadCount = 0;

  constructor(
    private notifications: NotificationService,
    private router: Router,
    private host: ElementRef<HTMLElement>,
    private translate: TranslateService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.notifications.unreadCount$.subscribe((count) => {
      this.unreadCount = count;
      this.cdr.markForCheck();
    });
    this.notifications.loadUnreadCount().subscribe();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.open) {
      return;
    }
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.open = false;
      this.cdr.markForCheck();
    }
  }

  toggle(event: Event): void {
    event.stopPropagation();
    this.open = !this.open;
    if (this.open) {
      this.loadItems();
    }
    this.cdr.markForCheck();
  }

  markAll(event: Event): void {
    event.stopPropagation();
    this.notifications.markAllRead().subscribe(() => {
      this.items = this.items.map((item) => ({ ...item, read: true }));
      this.cdr.markForCheck();
    });
  }

  openItem(item: AppNotification, event: Event): void {
    event.stopPropagation();
    const go = () => {
      this.open = false;
      this.cdr.markForCheck();
      if (item.actionUrl) {
        void this.router.navigateByUrl(item.actionUrl);
      }
    };
    if (item.read) {
      go();
      return;
    }
    this.notifications.markRead(item.id).subscribe({
      next: () => {
        item.read = true;
        go();
      },
      error: () => go(),
    });
  }

  iconFor(category: string): string {
    switch (category) {
      case 'EXAM':
        return '\u{1F4CB}';
      case 'ATTENDANCE':
        return '\u2705';
      case 'FEE':
        return '\u{1F4B3}';
      case 'ASSIGNMENT':
        return '\u{1F4C4}';
      default:
        return '\u{1F514}';
    }
  }

  relativeTime(value: string): string {
    const created = new Date(value).getTime();
    if (Number.isNaN(created)) {
      return '';
    }
    const minutes = Math.max(0, Math.floor((Date.now() - created) / 60000));
    if (minutes < 1) {
      return this.translate.instant('notifications.justNow');
    }
    if (minutes < 60) {
      return this.translate.instant('notifications.minutesAgo', { count: minutes });
    }
    const hours = Math.floor(minutes / 60);
    if (hours < 24) {
      return this.translate.instant('notifications.hoursAgo', { count: hours });
    }
    return this.translate.instant('notifications.daysAgo', { count: Math.floor(hours / 24) });
  }

  private loadItems(): void {
    this.loading = true;
    this.notifications.list(0, 20).subscribe({
      next: (page) => {
        this.items = page?.content ?? [];
        this.loading = false;
        const unread = this.items.filter((item) => !item.read).length;
        if (unread > this.unreadCount) {
          this.notifications.setUnreadCount(unread);
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }
}
