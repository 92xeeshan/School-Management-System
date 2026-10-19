import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../layout/empty-state/empty-state.component';
import { InventoryCategory } from './inventory.model';
import { InventoryService } from './inventory.service';

@Component({
  selector: 'app-inventory-categories',
  imports: [ReactiveFormsModule, TranslateModule, EmptyStateComponent],
  template: `
    @if (pageError) {
      <p class="banner error">{{ pageError | translate }}</p>
    }

    <div class="card">
      <div class="card-toolbar">
        @if (canManage) {
          <button class="btn btn-primary" type="button" (click)="toggleForm()">
            {{ 'inventory.addCategory' | translate }}
          </button>
        }
      </div>

      @if (showForm && canManage) {
        <form class="inline-form" [formGroup]="form" (ngSubmit)="submit()">
          <div class="field">
            <label>{{ 'inventory.name' | translate }} *</label>
            <input type="text" formControlName="name" />
          </div>
          <div class="field grow">
            <label>{{ 'inventory.description' | translate }}</label>
            <input type="text" formControlName="description" />
          </div>
          <div class="form-actions">
            <button class="btn" type="button" (click)="toggleForm()">{{ 'common.cancel' | translate }}</button>
            <button class="btn btn-primary" type="submit" [disabled]="form.invalid || saving">
              {{ saving ? ('common.loading' | translate) : ('common.save' | translate) }}
            </button>
          </div>
        </form>
      }

      @if (categories.length === 0) {
        <app-empty-state icon="category" [title]="'common.noData' | translate" [hint]="'common.emptyHint' | translate" />
      } @else {
        <div class="data-table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>{{ 'inventory.name' | translate }}</th>
                <th>{{ 'inventory.description' | translate }}</th>
                @if (canManage) {
                  <th>{{ 'common.actions' | translate }}</th>
                }
              </tr>
            </thead>
            <tbody>
              @for (category of categories; track category.id) {
                <tr>
                  <td class="strong">{{ category.name }}</td>
                  <td>{{ category.description || '—' }}</td>
                  @if (canManage) {
                    <td>
                      <button class="btn btn-sm btn-danger" type="button"
                              [disabled]="deletingId === category.id" (click)="remove(category)">
                        {{ deletingId === category.id ? ('common.loading' | translate) : ('common.delete' | translate) }}
                      </button>
                    </td>
                  }
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  styles: `
    .inline-form { display: flex; gap: 12px; align-items: flex-end; flex-wrap: wrap; padding: 12px 0 18px;
                   border-bottom: 1px solid var(--color-border); margin-bottom: 14px; }
    .field { display: flex; flex-direction: column; gap: 6px; min-width: 180px; }
    .field.grow { flex: 1; }
    .field label { font-size: .85rem; font-weight: 500; }
    .field input { padding: 8px 10px; border: 1px solid var(--color-border); border-radius: 8px;
                   background: var(--color-surface); color: var(--color-text); font: inherit; }
    .form-actions { display: flex; gap: 8px; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InventoryCategoriesComponent implements OnInit {
  categories: InventoryCategory[] = [];
  showForm = false;
  saving = false;
  deletingId: string | null = null;
  pageError = '';

  readonly form = new FormGroup({
    name: new FormControl('', Validators.required),
    description: new FormControl(''),
  });

  constructor(
    private inventory: InventoryService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  get canManage(): boolean {
    return this.auth.hasPermission('INVENTORY_MANAGE');
  }

  toggleForm(): void {
    this.showForm = !this.showForm;
    this.form.reset({ name: '', description: '' });
    this.cdr.markForCheck();
  }

  submit(): void {
    if (this.form.invalid || this.saving) {
      return;
    }
    this.saving = true;
    const value = this.form.getRawValue();
    this.inventory.createCategory({ name: value.name ?? '', description: value.description || null }).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.form.reset({ name: '', description: '' });
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.saving = false;
        this.pageError = (error.error && error.error.code) || 'common.error';
        this.cdr.markForCheck();
      },
    });
  }

  remove(category: InventoryCategory): void {
    this.deletingId = category.id;
    this.pageError = '';
    this.cdr.markForCheck();
    this.inventory.deleteCategory(category.id).subscribe({
      next: () => {
        this.deletingId = null;
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.deletingId = null;
        this.pageError = (error.error && error.error.code) || 'common.error';
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.inventory.listCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.pageError = '';
        this.cdr.markForCheck();
      },
      error: () => {
        this.pageError = 'common.error';
        this.cdr.markForCheck();
      },
    });
  }
}
