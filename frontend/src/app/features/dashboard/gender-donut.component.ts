import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { GenderBreakdown } from './dashboard.model';

interface Segment {
  key: string;
  labelKey: string;
  count: number;
  percent: number;
  color: string;
  dash: number;
  offset: number;
}

@Component({
  selector: 'app-gender-donut',
  imports: [TranslateModule],
  template: `
    @if (!breakdown || breakdown.total === 0) {
      <p class="empty">{{ 'common.noData' | translate }}</p>
    } @else {
      <div class="donut-wrap">
        <svg class="donut" viewBox="0 0 120 120" role="img"
             [attr.aria-label]="'dashboard.gender.title' | translate">
          <circle class="track" cx="60" cy="60" r="46" />
          @for (segment of segments; track segment.key) {
            <circle class="segment"
                    cx="60" cy="60" r="46"
                    [attr.stroke]="segment.color"
                    [attr.stroke-dasharray]="segment.dash + ' ' + (circumference - segment.dash)"
                    [attr.stroke-dashoffset]="-segment.offset"
                    [attr.data-percent]="segment.percent" />
          }
          <text class="center-value" x="60" y="56" text-anchor="middle">{{ breakdown.total }}</text>
          <text class="center-label" x="60" y="72" text-anchor="middle">
            {{ 'dashboard.gender.total' | translate }}
          </text>
        </svg>
        <ul class="legend">
          @for (segment of segments; track segment.key) {
            <li>
              <span class="dot" [style.background]="segment.color"></span>
              <div class="legend-body">
                <span class="legend-label">{{ segment.labelKey | translate }}</span>
                <span class="legend-value">{{ segment.count }} · {{ segment.percent }}%</span>
              </div>
            </li>
          }
        </ul>
      </div>
    }
  `,
  styles: `
    :host { display: block; }
    .empty { color: var(--color-muted); padding: 24px 0; text-align: center; }
    .donut-wrap { display: flex; align-items: center; gap: 20px; flex-wrap: wrap; }
    .donut { width: 150px; height: 150px; flex-shrink: 0; }
    .track {
      fill: none;
      stroke: var(--color-border);
      stroke-width: 14;
    }
    .segment {
      fill: none;
      stroke-width: 14;
      stroke-linecap: butt;
      transform: rotate(-90deg);
      transform-origin: 60px 60px;
      transition: opacity .15s ease;
    }
    .segment:hover { opacity: .8; }
    .center-value { font-size: 22px; font-weight: 700; fill: var(--color-text); }
    .center-label { font-size: 9px; fill: var(--color-muted); text-transform: uppercase; letter-spacing: .06em; }
    .legend { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
    .legend li { display: flex; align-items: center; gap: 10px; }
    .dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
    .legend-body { display: flex; flex-direction: column; }
    .legend-label { font-size: .85rem; color: var(--color-muted); }
    .legend-value { font-weight: 600; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenderDonutComponent {
  readonly circumference = 2 * Math.PI * 46;

  @Input()
  set breakdown(value: GenderBreakdown | null) {
    this._breakdown = value;
    this.segments = this.buildSegments(value);
  }

  get breakdown(): GenderBreakdown | null {
    return this._breakdown;
  }

  private _breakdown: GenderBreakdown | null = null;
  segments: Segment[] = [];

  private buildSegments(value: GenderBreakdown | null): Segment[] {
    if (!value || value.total === 0) {
      return [];
    }
    const raw: Array<{ key: string; labelKey: string; count: number; percent: number; color: string }> = [
      { key: 'boys', labelKey: 'dashboard.gender.boys', count: value.boys, percent: value.boysPercent, color: '#3b82f6' },
      { key: 'girls', labelKey: 'dashboard.gender.girls', count: value.girls, percent: value.girlsPercent, color: '#ec4899' },
      { key: 'other', labelKey: 'dashboard.gender.other', count: value.other, percent: value.otherPercent, color: '#94a3b8' },
    ];
    let offset = 0;
    return raw
      .filter((item) => item.count > 0)
      .map((item) => {
        const dash = (item.percent / 100) * this.circumference;
        const segment: Segment = { ...item, dash, offset };
        offset += dash;
        return segment;
      });
  }
}
