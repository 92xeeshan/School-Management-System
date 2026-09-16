import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { SchoolEvent } from './dashboard.model';

interface CalendarDay {
  iso: string;
  day: number;
  inMonth: boolean;
  isToday: boolean;
  isSelected: boolean;
  events: SchoolEvent[];
}

@Component({
  selector: 'app-mini-calendar',
  imports: [TranslateModule],
  template: `
    <div class="cal-header">
      <button type="button" class="nav-btn" (click)="shiftMonth(-1)"
              [attr.aria-label]="'dashboard.calendar.prevMonth' | translate">&#8249;</button>
      <div class="cal-title">
        <span class="month">{{ monthLabel }}</span>
        <button type="button" class="today-btn" (click)="selectToday()">
          {{ 'dashboard.calendar.today' | translate }}
        </button>
      </div>
      <button type="button" class="nav-btn" (click)="shiftMonth(1)"
              [attr.aria-label]="'dashboard.calendar.nextMonth' | translate">&#8250;</button>
    </div>

    <div class="day-nav">
      <button type="button" class="btn btn-ghost" (click)="shiftDay(-1)">
        &#8249; {{ 'dashboard.calendar.prevDay' | translate }}
      </button>
      <span class="selected">{{ selectedDate }}</span>
      <button type="button" class="btn btn-ghost" (click)="shiftDay(1)">
        {{ 'dashboard.calendar.nextDay' | translate }} &#8250;
      </button>
    </div>

    <div class="weekdays">
      @for (label of weekdayLabels; track label) {
        <span>{{ label }}</span>
      }
    </div>

    <div class="grid" role="grid">
      @for (day of days; track day.iso) {
        <button type="button"
                class="day"
                role="gridcell"
                [class.out]="!day.inMonth"
                [class.today]="day.isToday && !day.isSelected"
                [class.selected]="day.isSelected"
                (click)="select(day.iso)">
          <span class="num">{{ day.day }}</span>
          <span class="dots">
            @for (event of day.events.slice(0, 3); track event.id) {
              <i class="dot" [class]="'type-' + event.eventType.toLowerCase()"></i>
            }
          </span>
        </button>
      }
    </div>
  `,
  styles: `
    :host { display: block; }
    .cal-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
    .cal-title { display: flex; align-items: center; gap: 10px; }
    .month { font-weight: 600; }
    .nav-btn {
      width: 30px; height: 30px;
      border-radius: 8px;
      border: 1px solid var(--color-border);
      background: var(--color-surface);
      color: var(--color-text);
      font-size: 1.1rem;
      cursor: pointer;
      line-height: 1;
    }
    .nav-btn:hover { border-color: var(--color-primary); color: var(--color-primary); }
    .today-btn {
      border: 1px solid var(--color-border);
      background: transparent;
      color: var(--color-primary);
      border-radius: 20px;
      padding: 3px 10px;
      font-size: .75rem;
      font-weight: 600;
      cursor: pointer;
    }
    .day-nav {
      display: flex; align-items: center; justify-content: space-between;
      margin-bottom: 12px;
      font-size: .8rem;
    }
    .day-nav .btn { padding: 4px 8px; font-size: .78rem; }
    .selected { font-weight: 600; color: var(--color-muted); }
    .weekdays {
      display: grid;
      grid-template-columns: repeat(7, 1fr);
      text-align: center;
      font-size: .68rem;
      text-transform: uppercase;
      letter-spacing: .04em;
      color: var(--color-muted);
      margin-bottom: 6px;
    }
    .grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; }
    .day {
      position: relative;
      aspect-ratio: 1;
      border: 1px solid transparent;
      background: transparent;
      border-radius: 8px;
      cursor: pointer;
      color: var(--color-text);
      font: inherit;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 2px;
      padding: 2px;
    }
    .day:hover { background: var(--color-primary-soft); }
    .day.out { color: var(--color-muted); opacity: .45; }
    .day.today { border-color: var(--color-primary); color: var(--color-primary); font-weight: 700; }
    .day.selected { background: var(--color-primary); color: #fff; font-weight: 700; }
    .num { font-size: .82rem; line-height: 1; }
    .dots { display: inline-flex; gap: 2px; height: 5px; }
    .dot { width: 4px; height: 4px; border-radius: 50%; display: inline-block; background: var(--color-primary); }
    .dot.type-holiday { background: #ef4444; }
    .dot.type-exam { background: #f59e0b; }
    .dot.type-meeting { background: #3b82f6; }
    .dot.type-activity { background: #10b981; }
    .dot.type-event { background: #8b5cf6; }
    .day.selected .dot { background: #fff; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MiniCalendarComponent {
  readonly weekdayLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  days: CalendarDay[] = [];
  monthLabel = '';

  private events: SchoolEvent[] = [];
  private viewMonth = new Date();
  private _selectedDate = this.todayIso();

  @Output() selectedDateChange = new EventEmitter<string>();

  @Input()
  set selectedDate(value: string) {
    if (value && value !== this._selectedDate) {
      this._selectedDate = value;
      this.viewMonth = this.parse(value);
      this.rebuild();
    }
  }

  get selectedDate(): string {
    return this._selectedDate;
  }

  @Input()
  set eventsInput(value: SchoolEvent[]) {
    this.events = value ?? [];
    this.rebuild();
  }

  shiftMonth(delta: number): void {
    this.viewMonth = new Date(this.viewMonth.getFullYear(), this.viewMonth.getMonth() + delta, 1);
    this.rebuild();
  }

  shiftDay(delta: number): void {
    const next = this.parse(this._selectedDate);
    next.setDate(next.getDate() + delta);
    this.select(this.toIso(next));
  }

  selectToday(): void {
    this.select(this.todayIso());
  }

  select(iso: string): void {
    this._selectedDate = iso;
    this.viewMonth = this.parse(iso);
    this.rebuild();
    this.selectedDateChange.emit(iso);
  }

  private rebuild(): void {
    const year = this.viewMonth.getFullYear();
    const month = this.viewMonth.getMonth();
    this.monthLabel = this.viewMonth.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });

    const first = new Date(year, month, 1);
    const offset = (first.getDay() + 6) % 7;
    const start = new Date(year, month, 1 - offset);
    const today = this.todayIso();
    const byDate = this.eventsByDate();
    const days: CalendarDay[] = [];

    for (let i = 0; i < 42; i++) {
      const date = new Date(start.getFullYear(), start.getMonth(), start.getDate() + i);
      const iso = this.toIso(date);
      days.push({
        iso,
        day: date.getDate(),
        inMonth: date.getMonth() === month,
        isToday: iso === today,
        isSelected: iso === this._selectedDate,
        events: byDate.get(iso) ?? [],
      });
    }
    this.days = days;
  }

  private eventsByDate(): Map<string, SchoolEvent[]> {
    const map = new Map<string, SchoolEvent[]>();
    for (const event of this.events) {
      const start = this.parse(event.startDate);
      const end = this.parse(event.endDate ?? event.startDate);
      const cursor = new Date(start.getFullYear(), start.getMonth(), start.getDate());
      while (cursor <= end) {
        const iso = this.toIso(cursor);
        const list = map.get(iso) ?? [];
        list.push(event);
        map.set(iso, list);
        cursor.setDate(cursor.getDate() + 1);
      }
    }
    return map;
  }

  private todayIso(): string {
    return this.toIso(new Date());
  }

  private parse(iso: string): Date {
    const [year, month, day] = iso.split('-').map(Number);
    return new Date(year, (month ?? 1) - 1, day ?? 1);
  }

  private toIso(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }
}
