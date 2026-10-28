import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';
import {
  ASSET_TYPES,
  INVENTORY_CONDITIONS,
  InventoryAssignee,
  InventoryCategory,
  InventoryCondition,
  InventoryItem,
  InventoryItemPayload,
  InventoryTransaction,
  InventoryTransactionType,
  STOCK_TYPES,
} from './inventory.model';
import { InventoryService } from './inventory.service';
import { DirectoryPagerComponent, SortDirection, sortIndicator, toggleSort } from '../../layout/directory-pager/directory-pager.component';

@Component({
  selector: 'app-inventory-items',
  imports: [ReactiveFormsModule, TranslateModule, EmptyStateComponent, DirectoryPagerComponent],
  template: `
    @if (pageError) {
      <p class="banner error">{{ pageError | translate }}</p>
    }
    @if (importResult) {
      <div class="banner" [class.success]="importResult.errors.length === 0" [class.warn]="importResult.errors.length > 0">
        <strong>{{ 'inventory.importSummary' | translate:{ succeeded: importResult.succeeded, total: importResult.total } }}</strong>
        @if (importResult.errors.length > 0) {
          <ul class="import-errors">
            @for (err of importResult.errors; track err) {
              <li>{{ err }}</li>
            }
          </ul>
        }
      </div>
    }

    <div class="card">
      <div class="card-toolbar filters">
        <input class="search" type="search" [value]="search" [placeholder]="'common.search' | translate"
               (input)="onSearch($any($event.target).value)" />
        <select [value]="categoryFilter" (change)="onCategory($any($event.target).value)">
          <option value="">{{ 'inventory.allCategories' | translate }}</option>
          @for (category of categories; track category.id) {
            <option [value]="category.id">{{ category.name }}</option>
          }
        </select>
        <select [value]="conditionFilter" (change)="onCondition($any($event.target).value)">
          <option value="">{{ 'inventory.allConditions' | translate }}</option>
          @for (condition of conditions; track condition) {
            <option [value]="condition">{{ ('inventory.condition.' + condition) | translate }}</option>
          }
        </select>
        <input class="loc" type="text" [value]="locationFilter"
               [placeholder]="'inventory.location' | translate" (change)="onLocation($any($event.target).value)" />
        <label class="toggle">
          <input type="checkbox" [checked]="lowStockOnly" (change)="onLowStock($any($event.target).checked)" />
          {{ 'inventory.lowStockOnly' | translate }}
        </label>
        <div class="spacer"></div>
        <button class="btn" type="button" (click)="exportReport()">{{ 'inventory.exportReport' | translate }}</button>
        @if (canImport) {
          <label class="btn">
            {{ 'inventory.importCsv' | translate }}
            <input type="file" accept=".csv" hidden (change)="onImport($event)" />
          </label>
        }
        @if (canManage) {
          <button class="btn btn-primary" type="button" (click)="openAdd()">{{ 'inventory.addItem' | translate }}</button>
        }
      </div>

      @if (items.length === 0) {
        <app-empty-state icon="inventory_2" [title]="'common.noData' | translate" [hint]="'common.emptyHint' | translate">
          @if (canManage) {
            <button class="btn btn-primary" type="button" (click)="openAdd()">{{ 'inventory.addItem' | translate }}</button>
          }
        </app-empty-state>
      } @else {
        <div class="data-table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th class="sortable" (click)="sortBy('name')">{{ 'inventory.name' | translate }}<span class="sort-ind">{{ indicator('name') }}</span></th>
                <th>{{ 'inventory.category' | translate }}</th>
                <th class="sortable" (click)="sortBy('sku')">{{ 'inventory.sku' | translate }}<span class="sort-ind">{{ indicator('sku') }}</span></th>
                <th class="sortable" (click)="sortBy('quantity')">{{ 'inventory.quantity' | translate }}<span class="sort-ind">{{ indicator('quantity') }}</span></th>
                <th class="sortable" (click)="sortBy('location')">{{ 'inventory.location' | translate }}<span class="sort-ind">{{ indicator('location') }}</span></th>
                <th class="sortable" (click)="sortBy('condition')">{{ 'inventory.conditionLabel' | translate }}<span class="sort-ind">{{ indicator('condition') }}</span></th>
                <th>{{ 'inventory.assignedTo' | translate }}</th>
                @if (canManage) {
                  <th>{{ 'common.actions' | translate }}</th>
                }
              </tr>
            </thead>
            <tbody>
              @for (item of items; track item.id) {
                <tr [class.low]="item.lowStock">
                  <td>
                    <div class="strong">{{ item.name }}</div>
                    @if (item.lowStock) {
                      <span class="badge badge-warning">{{ 'inventory.lowStock' | translate }}</span>
                    }
                  </td>
                  <td>{{ item.categoryName || '—' }}</td>
                  <td class="mono">{{ item.sku || '—' }}</td>
                  <td>{{ item.quantity }} {{ item.unit }}</td>
                  <td>{{ item.location || '—' }}</td>
                  <td>
                    <span class="badge"
                          [class.badge-success]="item.condition === 'NEW' || item.condition === 'GOOD'"
                          [class.badge-danger]="item.condition === 'DAMAGED' || item.condition === 'RETIRED'"
                          [class.badge-warning]="item.condition === 'UNDER_REPAIR'">
                      {{ ('inventory.condition.' + item.condition) | translate }}
                    </span>
                  </td>
                  <td>{{ item.assignedToName || item.assignedRoom || '—' }}</td>
                  @if (canManage) {
                    <td>
                      <div class="row-actions">
                        <button class="btn btn-sm" type="button" (click)="openTransaction(item)">
                          {{ 'inventory.recordMovement' | translate }}
                        </button>
                        <button class="btn btn-sm" type="button" (click)="openHistory(item)">
                          {{ 'inventory.history' | translate }}
                        </button>
                        <button class="btn btn-sm" type="button" (click)="openEdit(item)">{{ 'common.edit' | translate }}</button>
                        <button class="btn btn-sm btn-danger" type="button"
                                [disabled]="deletingId === item.id" (click)="askDelete(item)">
                          {{ deletingId === item.id ? ('common.loading' | translate) : ('common.delete' | translate) }}
                        </button>
                      </div>
                    </td>
                  }
                </tr>
              }
            </tbody>
          </table>
        </div>
        <app-directory-pager
          [pageIndex]="pageIndex" [pageSize]="pageSize"
          [totalElements]="totalElements" [totalPages]="totalPages"
          (pageChange)="onPage($event)" (pageSizeChange)="onPageSize($event)" />
      }
    </div>

    @if (showForm) {
      <div class="modal-backdrop" (click)="closeForm()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editingId ? ('inventory.editItem' | translate) : ('inventory.addItem' | translate) }}</h2>
          @if (formError) {
            <p class="banner error">{{ formError | translate }}</p>
          }
          <form [formGroup]="form" (ngSubmit)="submitForm()">
            <div class="form-grid">
              <div class="field">
                <label>{{ 'inventory.name' | translate }} *</label>
                <input type="text" formControlName="name" />
              </div>
              <div class="field">
                <label>{{ 'inventory.category' | translate }} *</label>
                <select formControlName="categoryId">
                  <option value="" disabled>{{ 'inventory.selectCategory' | translate }}</option>
                  @for (category of categories; track category.id) {
                    <option [value]="category.id">{{ category.name }}</option>
                  }
                </select>
              </div>
              <div class="field">
                <label>{{ 'inventory.sku' | translate }}</label>
                <input type="text" formControlName="sku" />
              </div>
              <div class="field">
                <label>{{ 'inventory.unit' | translate }}</label>
                <input type="text" formControlName="unit" placeholder="PCS" />
              </div>
              @if (!editingId) {
                <div class="field">
                  <label>{{ 'inventory.quantity' | translate }}</label>
                  <input type="number" min="0" formControlName="quantity" />
                </div>
              }
              <div class="field">
                <label>{{ 'inventory.reorderThreshold' | translate }}</label>
                <input type="number" min="0" formControlName="reorderThreshold" />
              </div>
              <div class="field">
                <label>{{ 'inventory.purchaseDate' | translate }}</label>
                <input type="date" formControlName="purchaseDate" />
              </div>
              <div class="field">
                <label>{{ 'inventory.purchaseCost' | translate }}</label>
                <input type="number" min="0" formControlName="purchaseCost" />
              </div>
              <div class="field">
                <label>{{ 'inventory.vendor' | translate }}</label>
                <input type="text" formControlName="vendor" />
              </div>
              <div class="field">
                <label>{{ 'inventory.location' | translate }}</label>
                <input type="text" formControlName="location" />
              </div>
              <div class="field">
                <label>{{ 'inventory.conditionLabel' | translate }}</label>
                <select formControlName="condition">
                  @for (condition of conditions; track condition) {
                    <option [value]="condition">{{ ('inventory.condition.' + condition) | translate }}</option>
                  }
                </select>
              </div>
            </div>
            <div class="form-actions">
              <button class="btn" type="button" (click)="closeForm()">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving">
                {{ saving ? ('common.loading' | translate) : ('common.save' | translate) }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    @if (transactionItem) {
      <div class="modal-backdrop" (click)="closeTransaction()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ 'inventory.recordMovement' | translate }}</h2>
          <p class="muted">{{ transactionItem.name }} — {{ transactionItem.quantity }} {{ transactionItem.unit }}</p>
          @if (txnError) {
            <p class="banner error">{{ txnError | translate }}</p>
          }
          <form [formGroup]="txnForm" (ngSubmit)="submitTransaction()">
            <div class="form-grid">
              <div class="field">
                <label>{{ 'inventory.movementType' | translate }}</label>
                <select formControlName="type">
                  @for (type of movementTypes; track type) {
                    <option [value]="type">{{ ('inventory.type.' + type) | translate }}</option>
                  }
                </select>
              </div>
              @if (isStockMovement) {
                <div class="field">
                  <label>{{ 'inventory.quantity' | translate }}</label>
                  <input type="number" min="0" formControlName="quantity" />
                </div>
              }
              @if (isCheckout) {
                <div class="field">
                  <label>{{ 'inventory.assignTo' | translate }}</label>
                  <select formControlName="assignedToUserId">
                    <option value="">{{ 'inventory.none' | translate }}</option>
                    @for (assignee of assignees; track assignee.id) {
                      <option [value]="assignee.id">{{ assignee.name }}</option>
                    }
                  </select>
                </div>
              }
              <div class="field">
                <label>{{ 'inventory.room' | translate }}</label>
                <input type="text" formControlName="room" />
              </div>
              <div class="field">
                <label>{{ 'inventory.date' | translate }}</label>
                <input type="date" formControlName="transactionDate" />
              </div>
              <div class="field wide">
                <label>{{ 'inventory.reason' | translate }}</label>
                <input type="text" formControlName="reason" />
              </div>
            </div>
            <div class="form-actions">
              <button class="btn" type="button" (click)="closeTransaction()">{{ 'common.cancel' | translate }}</button>
              <button class="btn btn-primary" type="submit" [disabled]="txnSaving">
                {{ txnSaving ? ('common.loading' | translate) : ('inventory.recordMovement' | translate) }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    @if (historyItem) {
      <div class="modal-backdrop" (click)="closeHistory()">
        <div class="modal modal-lg" (click)="$event.stopPropagation()">
          <h2>{{ 'inventory.history' | translate }} — {{ historyItem.name }}</h2>
          @if (history.length === 0) {
            <p class="muted">{{ 'common.noData' | translate }}</p>
          } @else {
            <ul class="timeline">
              @for (txn of history; track txn.id) {
                <li>
                  <span class="badge"
                        [class.badge-success]="txn.type === 'IN' || txn.type === 'CHECKIN'"
                        [class.badge-danger]="txn.type === 'OUT' || txn.type === 'CHECKOUT'">
                    {{ ('inventory.type.' + txn.type) | translate }}
                  </span>
                  <span class="strong">{{ txn.quantity }}</span>
                  <span>{{ txn.reason || '—' }}</span>
                  <span class="muted">{{ txn.assignedToName || txn.room || '' }}</span>
                  <span class="muted">{{ txn.transactionDate }}</span>
                </li>
              }
            </ul>
          }
          <div class="form-actions">
            <button class="btn" type="button" (click)="closeHistory()">{{ 'common.close' | translate }}</button>
          </div>
        </div>
      </div>
    }

    @if (pendingDelete) {
      <div class="modal-backdrop" (click)="cancelDelete()">
        <div class="modal modal-sm" (click)="$event.stopPropagation()">
          <h2>{{ 'common.delete' | translate }}</h2>
          <p>{{ 'inventory.deleteConfirm' | translate:{ name: pendingDelete.name } }}</p>
          <div class="form-actions">
            <button class="btn" type="button" [disabled]="!!deletingId" (click)="cancelDelete()">{{ 'common.cancel' | translate }}</button>
            <button class="btn btn-danger" type="button" [disabled]="!!deletingId" (click)="confirmDelete()">
              {{ deletingId ? ('common.loading' | translate) : ('common.delete' | translate) }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    .filters { align-items: center; flex-wrap: wrap; gap: 10px; }
    .filters .spacer { flex: 1; }
    .filters select, .filters input.loc {
      padding: 8px 10px; border: 1px solid var(--color-border); border-radius: 8px;
      background: var(--color-surface); color: var(--color-text); font: inherit;
    }
    .toggle { display: inline-flex; align-items: center; gap: 6px; font-size: .9rem; }
    .row-actions { display: flex; gap: 6px; flex-wrap: wrap; }
    .mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }
    tr.low { background: color-mix(in srgb, var(--color-warning) 8%, transparent); }
    .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; }
    .field { display: flex; flex-direction: column; gap: 6px; }
    .field.wide { grid-column: 1 / -1; }
    .field label { font-size: .85rem; font-weight: 500; }
    .field input, .field select {
      padding: 8px 10px; border: 1px solid var(--color-border); border-radius: 8px;
      background: var(--color-surface); color: var(--color-text); font: inherit;
    }
    .form-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
    .timeline { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
    .timeline li { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid var(--color-border); }
    .import-errors { margin: 8px 0 0; padding-left: 18px; }
    .modal-lg { width: 640px; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InventoryItemsComponent implements OnInit {
  items: InventoryItem[] = [];
  categories: InventoryCategory[] = [];
  assignees: InventoryAssignee[] = [];
  conditions = INVENTORY_CONDITIONS;
  movementTypes: InventoryTransactionType[] = [...STOCK_TYPES, ...ASSET_TYPES];

  search = '';
  categoryFilter = '';
  conditionFilter = '';
  locationFilter = '';
  lowStockOnly = false;
  pageIndex = 0;
  pageSize = 25;
  totalElements = 0;
  totalPages = 0;
  sortKey = 'name';
  sortDirection: SortDirection = 'asc';
  private searchTimer: ReturnType<typeof setTimeout> | null = null;

  showForm = false;
  editingId: string | null = null;
  saving = false;
  formError = '';

  transactionItem: InventoryItem | null = null;
  txnSaving = false;
  txnError = '';

  historyItem: InventoryItem | null = null;
  history: InventoryTransaction[] = [];

  pendingDelete: InventoryItem | null = null;
  deletingId: string | null = null;
  pageError = '';
  importResult: { total: number; succeeded: number; errors: string[] } | null = null;

  readonly form = new FormGroup({
    name: new FormControl('', Validators.required),
    categoryId: new FormControl('', Validators.required),
    sku: new FormControl(''),
    unit: new FormControl('PCS'),
    quantity: new FormControl(0),
    reorderThreshold: new FormControl(0),
    purchaseDate: new FormControl(''),
    purchaseCost: new FormControl<number | null>(null),
    vendor: new FormControl(''),
    location: new FormControl(''),
    condition: new FormControl<InventoryCondition>('GOOD', Validators.required),
  });

  readonly txnForm = new FormGroup({
    type: new FormControl<InventoryTransactionType>('OUT', Validators.required),
    quantity: new FormControl(0),
    assignedToUserId: new FormControl(''),
    room: new FormControl(''),
    transactionDate: new FormControl(this.today()),
    reason: new FormControl(''),
  });

  constructor(
    private inventory: InventoryService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadAssignees();
    this.loadItems();
  }

  get canManage(): boolean {
    return this.auth.hasPermission('INVENTORY_MANAGE');
  }

  get canImport(): boolean {
    return this.auth.hasPermission('INVENTORY_IMPORT');
  }

  get isStockMovement(): boolean {
    const type = this.txnForm.controls.type.value;
    return type === 'IN' || type === 'OUT';
  }

  get isCheckout(): boolean {
    return this.txnForm.controls.type.value === 'CHECKOUT';
  }

  onSearch(value: string): void {
    this.search = value;
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
    this.searchTimer = setTimeout(() => {
      this.pageIndex = 0;
      this.loadItems();
    }, 300);
  }

  sortBy(key: string): void {
    const next = toggleSort(this.sortKey, key, this.sortDirection);
    this.sortKey = next.key;
    this.sortDirection = next.direction;
    this.pageIndex = 0;
    this.loadItems();
  }

  indicator(key: string): string {
    return sortIndicator(this.sortKey, key, this.sortDirection);
  }

  onPage(page: number): void {
    this.pageIndex = page;
    this.loadItems();
  }

  onPageSize(size: number): void {
    this.pageSize = size;
    this.pageIndex = 0;
    this.loadItems();
  }

  onCategory(value: string): void {
    this.categoryFilter = value;
    this.pageIndex = 0;
    this.loadItems();
  }

  onCondition(value: string): void {
    this.conditionFilter = value;
    this.pageIndex = 0;
    this.loadItems();
  }

  onLocation(value: string): void {
    this.locationFilter = value;
    this.pageIndex = 0;
    this.loadItems();
  }

  onLowStock(value: boolean): void {
    this.lowStockOnly = value;
    this.pageIndex = 0;
    this.loadItems();
  }

  openAdd(): void {
    this.editingId = null;
    this.formError = '';
    this.form.reset({
      name: '',
      categoryId: this.categories[0]?.id ?? '',
      sku: '',
      unit: 'PCS',
      quantity: 0,
      reorderThreshold: 0,
      purchaseDate: '',
      purchaseCost: null,
      vendor: '',
      location: '',
      condition: 'GOOD',
    });
    this.showForm = true;
    this.cdr.markForCheck();
  }

  openEdit(item: InventoryItem): void {
    this.editingId = item.id;
    this.formError = '';
    this.form.reset({
      name: item.name,
      categoryId: item.categoryId,
      sku: item.sku ?? '',
      unit: item.unit,
      quantity: item.quantity,
      reorderThreshold: item.reorderThreshold,
      purchaseDate: item.purchaseDate ?? '',
      purchaseCost: item.purchaseCost,
      vendor: item.vendor ?? '',
      location: item.location ?? '',
      condition: item.condition,
    });
    this.showForm = true;
    this.cdr.markForCheck();
  }

  closeForm(): void {
    if (this.saving) {
      return;
    }
    this.showForm = false;
    this.cdr.markForCheck();
  }

  submitForm(): void {
    if (this.form.invalid || this.saving) {
      return;
    }
    const value = this.form.getRawValue();
    const payload: InventoryItemPayload = {
      categoryId: value.categoryId ?? '',
      name: value.name ?? '',
      sku: value.sku || null,
      unit: value.unit || 'PCS',
      quantity: value.quantity ?? 0,
      reorderThreshold: value.reorderThreshold ?? 0,
      purchaseDate: value.purchaseDate || null,
      purchaseCost: value.purchaseCost ?? null,
      vendor: value.vendor || null,
      location: value.location || null,
      condition: value.condition ?? 'GOOD',
    };
    this.saving = true;
    this.formError = '';
    this.cdr.markForCheck();
    const request = this.editingId
      ? this.inventory.updateItem(this.editingId, payload)
      : this.inventory.createItem(payload);
    request.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.loadItems();
      },
      error: (error: HttpErrorResponse) => {
        this.saving = false;
        this.formError = (error.error && error.error.code) || 'common.error';
        this.cdr.markForCheck();
      },
    });
  }

  openTransaction(item: InventoryItem): void {
    this.transactionItem = item;
    this.txnError = '';
    this.txnForm.reset({
      type: 'OUT',
      quantity: 0,
      assignedToUserId: '',
      room: item.location ?? '',
      transactionDate: this.today(),
      reason: '',
    });
    this.cdr.markForCheck();
  }

  closeTransaction(): void {
    if (this.txnSaving) {
      return;
    }
    this.transactionItem = null;
    this.cdr.markForCheck();
  }

  submitTransaction(): void {
    if (!this.transactionItem || this.txnSaving) {
      return;
    }
    const value = this.txnForm.getRawValue();
    this.txnSaving = true;
    this.txnError = '';
    this.cdr.markForCheck();
    this.inventory.recordTransaction(this.transactionItem.id, {
      type: value.type ?? 'OUT',
      quantity: value.quantity ?? 0,
      reason: value.reason || null,
      assignedToUserId: value.assignedToUserId || null,
      room: value.room || null,
      transactionDate: value.transactionDate || this.today(),
    }).subscribe({
      next: () => {
        this.txnSaving = false;
        this.transactionItem = null;
        this.loadItems();
      },
      error: (error: HttpErrorResponse) => {
        this.txnSaving = false;
        this.txnError = (error.error && error.error.code) || 'common.error';
        this.cdr.markForCheck();
      },
    });
  }

  openHistory(item: InventoryItem): void {
    this.historyItem = item;
    this.history = [];
    this.cdr.markForCheck();
    this.inventory.listTransactions(item.id).subscribe({
      next: (rows) => {
        this.history = rows;
        this.cdr.markForCheck();
      },
      error: () => {
        this.history = [];
        this.cdr.markForCheck();
      },
    });
  }

  closeHistory(): void {
    this.historyItem = null;
    this.cdr.markForCheck();
  }

  askDelete(item: InventoryItem): void {
    this.pendingDelete = item;
    this.cdr.markForCheck();
  }

  cancelDelete(): void {
    if (this.deletingId) {
      return;
    }
    this.pendingDelete = null;
    this.cdr.markForCheck();
  }

  confirmDelete(): void {
    const item = this.pendingDelete;
    if (!item) {
      return;
    }
    this.deletingId = item.id;
    this.cdr.markForCheck();
    this.inventory.deleteItem(item.id).subscribe({
      next: () => {
        this.deletingId = null;
        this.pendingDelete = null;
        this.loadItems();
      },
      error: (error: HttpErrorResponse) => {
        this.deletingId = null;
        this.pageError = (error.error && error.error.code) || 'common.error';
        this.cdr.markForCheck();
      },
    });
  }

  onImport(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.importResult = null;
    this.cdr.markForCheck();
    this.inventory.importCsv(file).subscribe({
      next: (result) => {
        this.importResult = result;
        input.value = '';
        this.loadItems();
      },
      error: (error: HttpErrorResponse) => {
        this.pageError = (error.error && error.error.code) || 'common.error';
        input.value = '';
        this.cdr.markForCheck();
      },
    });
  }

  exportReport(): void {
    this.inventory.exportReport({
      category: this.categoryFilter || undefined,
      location: this.locationFilter || undefined,
      condition: this.conditionFilter || undefined,
      lowStock: this.lowStockOnly || undefined,
    }).subscribe({
      next: (blob) => this.saveBlob(blob, 'inventory-report.pdf'),
      error: () => {
        this.pageError = 'common.error';
        this.cdr.markForCheck();
      },
    });
  }

  private loadItems(): void {
    this.inventory.listItems({
      category: this.categoryFilter || undefined,
      location: this.locationFilter || undefined,
      condition: this.conditionFilter || undefined,
      lowStock: this.lowStockOnly || undefined,
      query: this.search.trim() || undefined,
      page: this.pageIndex,
      size: this.pageSize,
      sort: this.sortKey,
      direction: this.sortDirection,
    }).subscribe({
      next: (page) => {
        this.items = page?.content ?? [];
        this.totalElements = page?.totalElements ?? 0;
        this.totalPages = page?.totalPages ?? 0;
        this.pageError = '';
        this.cdr.markForCheck();
      },
      error: () => {
        this.pageError = 'common.error';
        this.cdr.markForCheck();
      },
    });
  }

  private loadCategories(): void {
    this.inventory.listCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  private loadAssignees(): void {
    this.inventory.listAssignees().subscribe({
      next: (assignees) => {
        this.assignees = assignees;
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  private saveBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
