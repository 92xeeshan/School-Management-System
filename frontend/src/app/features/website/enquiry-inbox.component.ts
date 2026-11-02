import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { PageHeaderComponent } from '../../layout/page-header/page-header.component';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';
import { Enquiry } from '../public/public.model';
import { PublicService } from '../public/public.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-enquiry-inbox',
  imports: [TranslateModule, MatButtonModule, PageHeaderComponent, EmptyStateComponent],
  template: `
    <div class="page">
      <app-page-header [title]="'enquiry.title' | translate" [subtitle]="'enquiry.subtitle' | translate" />
      @if (error) {
        <p class="error">{{ error | translate }}</p>
      }
      <div class="toolbar">
        <select [value]="statusFilter" (change)="onStatus($any($event.target).value)">
          <option value="NEW">{{ 'enquiry.status.NEW' | translate }}</option>
          <option value="CONTACTED">{{ 'enquiry.status.CONTACTED' | translate }}</option>
          <option value="CLOSED">{{ 'enquiry.status.CLOSED' | translate }}</option>
          <option value="">{{ 'enquiry.allStatuses' | translate }}</option>
        </select>
      </div>
      @if (rows.length === 0) {
        <app-empty-state [title]="'enquiry.empty' | translate" />
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>{{ 'enquiry.parentName' | translate }}</th>
                <th>{{ 'enquiry.phone' | translate }}</th>
                <th>{{ 'enquiry.email' | translate }}</th>
                <th>{{ 'enquiry.class' | translate }}</th>
                <th>{{ 'enquiry.message' | translate }}</th>
                <th>{{ 'common.status' | translate }}</th>
                <th>{{ 'enquiry.received' | translate }}</th>
                <th>{{ 'common.actions' | translate }}</th>
              </tr>
            </thead>
            <tbody>
              @for (row of rows; track row.id) {
                <tr>
                  <td>{{ row.parentName }}</td>
                  <td>{{ row.phone }}</td>
                  <td>{{ row.email || '—' }}</td>
                  <td>{{ row.className || '—' }}</td>
                  <td>{{ row.message || '—' }}</td>
                  <td>{{ ('enquiry.status.' + row.status) | translate }}</td>
                  <td>{{ receivedOn(row) }}</td>
                  <td>
                    @if (canManage && row.status === 'NEW') {
                      <button mat-flat-button color="primary" type="button" (click)="update(row, 'CONTACTED')">
                        {{ 'enquiry.markContacted' | translate }}
                      </button>
                    }
                    @if (canManage && row.status !== 'CLOSED') {
                      <button mat-button type="button" (click)="update(row, 'CLOSED')">
                        {{ 'enquiry.close' | translate }}
                      </button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  styles: `
    .toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
    select { padding: 8px 10px; border: 1px solid var(--color-border); border-radius: 8px; font: inherit; }
    .table-wrap { overflow: auto; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--color-border); vertical-align: top; }
    .error { color: #b91c1c; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EnquiryInboxComponent implements OnInit {
  rows: Enquiry[] = [];
  statusFilter = 'NEW';
  error = '';
  canManage = false;

  constructor(
    private publicApi: PublicService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.canManage = this.auth.hasPermission('ENQUIRY_MANAGE');
    this.load();
  }

  receivedOn(row: Enquiry): string {
    return row.createdAt ? row.createdAt.slice(0, 10) : '';
  }

  onStatus(status: string): void {
    this.statusFilter = status;
    this.load();
  }

  update(row: Enquiry, status: Enquiry['status']): void {
    this.publicApi.updateEnquiry(row.id, { status }).subscribe({
      next: () => this.load(),
      error: () => {
        this.error = 'enquiry.updateFailed';
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.error = '';
    this.publicApi.listEnquiries({ status: this.statusFilter, page: 0, size: 50 }).subscribe({
      next: (page) => {
        this.rows = page.content ?? [];
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'enquiry.loadFailed';
        this.cdr.markForCheck();
      },
    });
  }
}
