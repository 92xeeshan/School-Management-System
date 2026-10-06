import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { ExpenseChart } from './payroll.model';

const COLORS = ['#4f46e5', '#0ea5e9', '#16a34a', '#f59e0b', '#ef4444', '#8b5cf6', '#14b8a6'];

@Component({
  selector: 'app-expense-chart',
  imports: [TranslateModule],
  template: `
    @if (!chart || chart.yearTotal === 0) {
      <p class="empty">{{ 'common.noData' | translate }}</p>
    } @else {
      <div class="charts">
        <div class="card">
          <h3>{{ 'payroll.monthlySpend' | translate }}</h3>
          <div class="bars">
            @for (month of chart.months; track month.month) {
              <div class="col" [attr.aria-label]="month.month + ': ' + month.total">
                <div class="bar-wrap">
                  <div class="bar" [style.height.%]="barHeight(month.total)"></div>
                </div>
                <span class="x">{{ month.month }}</span>
              </div>
            }
          </div>
        </div>
        <div class="card">
          <h3>{{ 'payroll.byCategory' | translate }}</h3>
          <ul class="legend">
            @for (category of chart.categories; track category.categoryId; let i = $index) {
              <li>
                <span class="dot" [style.background]="color(i)"></span>
                <span class="name">{{ category.name }}</span>
                <span class="value">{{ formatMoney(category.total) }}</span>
              </li>
            }
          </ul>
        </div>
      </div>
    }
  `,
  styles: `
    .empty { color: var(--color-muted); text-align: center; padding: 16px 0; }
    .charts { display: grid; grid-template-columns: 2fr 1fr; gap: 16px; }
    .card { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 12px; padding: 16px; }
    h3 { margin: 0 0 12px; font-size: 1rem; }
    .bars { display: flex; align-items: flex-end; gap: 8px; height: 180px; }
    .col { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; gap: 4px; }
    .bar-wrap { flex: 1; width: 100%; display: flex; align-items: flex-end; }
    .bar { width: 100%; background: var(--color-primary); border-radius: 6px 6px 0 0; min-height: 2px; }
    .x { font-size: .7rem; color: var(--color-muted); }
    .legend { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
    .legend li { display: flex; align-items: center; gap: 8px; }
    .dot { width: 10px; height: 10px; border-radius: 50%; }
    .name { flex: 1; }
    .value { font-variant-numeric: tabular-nums; font-weight: 600; }
    @media (max-width: 768px) { .charts { grid-template-columns: 1fr; } }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExpenseChartComponent {
  @Input() chart: ExpenseChart | null = null;

  barHeight(total: number): number {
    const max = this.chart?.months.reduce((acc, month) => Math.max(acc, month.total), 0) ?? 0;
    if (max <= 0) {
      return 0;
    }
    return Math.max(4, (total / max) * 100);
  }

  color(index: number): string {
    return COLORS[index % COLORS.length];
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(value);
  }
}
