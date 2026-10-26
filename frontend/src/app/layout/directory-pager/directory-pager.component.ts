import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

export const PAGE_SIZE_OPTIONS = [10, 25, 50, 100];
export type SortDirection = 'asc' | 'desc';

export function sortIndicator(activeKey: string, key: string, direction: SortDirection): string {
  if (activeKey !== key) {
    return '';
  }
  return direction === 'asc' ? '▲' : '▼';
}

export function toggleSort(
  activeKey: string,
  key: string,
  direction: SortDirection,
): { key: string; direction: SortDirection } {
  if (activeKey === key) {
    return { key, direction: direction === 'asc' ? 'desc' : 'asc' };
  }
  return { key, direction: 'asc' };
}

@Component({
  selector: 'app-directory-pager',
  imports: [TranslateModule],
  template: `
    <div class="pagination">
      <span class="muted">{{ 'common.pageInfo' | translate:{ from: pageFrom, to: pageTo, total: totalElements } }}</span>
      <span class="spacer"></span>
      <label class="page-size">
        {{ 'common.rowsPerPage' | translate }}
        <select [value]="pageSize" (change)="onPageSize(+$any($event.target).value)">
          @for (option of pageSizeOptions; track option) {
            <option [value]="option">{{ option }}</option>
          }
        </select>
      </label>
      <button class="btn btn-sm" type="button" (click)="onPage(pageIndex - 1)" [disabled]="pageIndex === 0">‹</button>
      <span class="muted">{{ pageIndex + 1 }} / {{ totalPages || 1 }}</span>
      <button class="btn btn-sm" type="button" (click)="onPage(pageIndex + 1)" [disabled]="pageIndex + 1 >= totalPages">›</button>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DirectoryPagerComponent {
  @Input() pageIndex = 0;
  @Input() pageSize = 25;
  @Input() totalElements = 0;
  @Input() totalPages = 0;
  @Output() pageChange = new EventEmitter<number>();
  @Output() pageSizeChange = new EventEmitter<number>();
  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;

  get pageFrom(): number {
    return this.totalElements === 0 ? 0 : this.pageIndex * this.pageSize + 1;
  }

  get pageTo(): number {
    return Math.min((this.pageIndex + 1) * this.pageSize, this.totalElements);
  }

  onPage(page: number): void {
    if (page < 0 || (this.totalPages > 0 && page >= this.totalPages)) {
      return;
    }
    this.pageChange.emit(page);
  }

  onPageSize(size: number): void {
    this.pageSizeChange.emit(size);
  }
}
