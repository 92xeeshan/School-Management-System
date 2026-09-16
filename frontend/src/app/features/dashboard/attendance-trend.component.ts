import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { AttendanceTrendPoint } from './dashboard.model';

@Component({
  selector: 'app-attendance-trend',
  imports: [TranslateModule],
  template: `
    @if (points.length === 0) {
      <p class="empty">{{ 'common.noData' | translate }}</p>
    } @else {
      <div class="chart">
        @for (point of points; track point.date) {
          <div class="col"
               (mouseenter)="hovered = point"
               (mouseleave)="hovered = null"
               (focusin)="hovered = point"
               (focusout)="hovered = null"
               tabindex="0"
               role="group"
               [attr.aria-label]="point.date + ': ' + point.percentage + '%'">
            <div class="bar-wrap">
              <div class="bar"
                   [class.good]="point.percentage >= 90"
                   [class.ok]="point.percentage >= 75 && point.percentage < 90"
                   [class.low]="point.percentage < 75"
                   [style.height.%]="point.percentage"></div>
            </div>
            <span class="value">{{ point.percentage }}%</span>
            <span class="x-label">{{ shortDay(point.date) }}</span>
          </div>
        }
      </div>

      <div class="details">
        @if (hovered) {
          <div class="detail-card">
            <span class="detail-date">{{ hovered.date }}</span>
            <span class="chip present">{{ 'dashboard.attendanceTrend.present' | translate }}: {{ hovered.present }}</span>
            <span class="chip absent">{{ 'dashboard.attendanceTrend.absent' | translate }}: {{ hovered.absent }}</span>
            @if (hovered.late > 0) {
              <span class="chip late">{{ 'dashboard.attendanceTrend.late' | translate }}: {{ hovered.late }}</span>
            }
            @if (hovered.leave > 0) {
              <span class="chip leave">{{ 'dashboard.attendanceTrend.leave' | translate }}: {{ hovered.leave }}</span>
            }
            <span class="chip total">{{ 'dashboard.attendanceTrend.total' | translate }}: {{ hovered.total }}</span>
          </div>
        } @else {
          <span class="hint">{{ 'dashboard.attendanceTrend.hint' | translate }}</span>
        }
      </div>

      <div class="legend">
        <span><i class="swatch good"></i>{{ 'dashboard.attendanceTrend.good' | translate }}</span>
        <span><i class="swatch ok"></i>{{ 'dashboard.attendanceTrend.watch' | translate }}</span>
        <span><i class="swatch low"></i>{{ 'dashboard.attendanceTrend.low' | translate }}</span>
      </div>
    }
  `,
  styles: `
    :host { display: block; }
    .empty { color: var(--color-muted); padding: 24px 0; text-align: center; }
    .chart {
      display: flex;
      align-items: flex-end;
      gap: 10px;
      height: 190px;
      padding: 8px 4px 0;
    }
    .col {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      height: 100%;
      cursor: pointer;
      outline: none;
    }
    .bar-wrap {
      width: 100%;
      max-width: 46px;
      flex: 1;
      display: flex;
      align-items: flex-end;
      background: var(--color-primary-soft);
      border-radius: 8px 8px 4px 4px;
      overflow: hidden;
    }
    .bar {
      width: 100%;
      min-height: 3px;
      border-radius: 8px 8px 4px 4px;
      transition: height .35s ease, filter .15s ease;
    }
    .col:hover .bar, .col:focus .bar { filter: brightness(1.08); }
    .bar.good { background: linear-gradient(180deg, #34d399, #16a34a); }
    .bar.ok { background: linear-gradient(180deg, #fbbf24, #d97706); }
    .bar.low { background: linear-gradient(180deg, #f87171, #dc2626); }
    .value { font-size: .72rem; font-weight: 600; color: var(--color-text); }
    .x-label { font-size: .72rem; color: var(--color-muted); text-transform: uppercase; }
    .details { min-height: 34px; margin-top: 10px; }
    .detail-card { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; }
    .detail-date { font-weight: 600; margin-right: 4px; }
    .chip {
      font-size: .72rem;
      padding: 2px 8px;
      border-radius: 20px;
      background: var(--color-bg);
      color: var(--color-muted);
    }
    .chip.present { background: #dcfce7; color: #15803d; }
    .chip.absent { background: #fee2e2; color: #b91c1c; }
    .chip.late { background: #fef3c7; color: #b45309; }
    .hint { color: var(--color-muted); font-size: .82rem; }
    .legend { display: flex; gap: 14px; margin-top: 8px; font-size: .75rem; color: var(--color-muted); }
    .legend span { display: inline-flex; align-items: center; gap: 6px; }
    .swatch { width: 10px; height: 10px; border-radius: 3px; display: inline-block; }
    .swatch.good { background: #16a34a; }
    .swatch.ok { background: #d97706; }
    .swatch.low { background: #dc2626; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AttendanceTrendComponent {
  @Input() points: AttendanceTrendPoint[] = [];
  hovered: AttendanceTrendPoint | null = null;

  shortDay(date: string): string {
    const parsed = new Date(date + 'T00:00:00');
    return parsed.toLocaleDateString(undefined, { weekday: 'short' });
  }
}
