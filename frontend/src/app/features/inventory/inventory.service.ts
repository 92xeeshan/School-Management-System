import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../../core/models/api.model';
import {
  InventoryAssignee,
  InventoryCategory,
  InventoryImportResult,
  InventoryItem,
  InventoryItemPayload,
  InventoryTransaction,
  InventoryTransactionPayload,
} from './inventory.model';

@Injectable({ providedIn: 'root' })
export class InventoryService {
  constructor(private http: HttpClient) {}

  listCategories(): Observable<InventoryCategory[]> {
    return this.http
      .get<ApiResponse<InventoryCategory[]>>('/api/inventory/categories')
      .pipe(map((res) => res.data ?? []));
  }

  createCategory(payload: { name: string; description: string | null }): Observable<InventoryCategory> {
    return this.http
      .post<ApiResponse<InventoryCategory>>('/api/inventory/categories', payload)
      .pipe(map((res) => res.data));
  }

  deleteCategory(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`/api/inventory/categories/${id}`)
      .pipe(map(() => undefined));
  }

  listAssignees(): Observable<InventoryAssignee[]> {
    return this.http
      .get<ApiResponse<InventoryAssignee[]>>('/api/inventory/assignees')
      .pipe(map((res) => res.data ?? []));
  }

  listItems(filters?: {
    category?: string;
    location?: string;
    condition?: string;
    lowStock?: boolean;
  }): Observable<InventoryItem[]> {
    let params = new HttpParams();
    if (filters?.category) {
      params = params.set('category', filters.category);
    }
    if (filters?.location) {
      params = params.set('location', filters.location);
    }
    if (filters?.condition) {
      params = params.set('condition', filters.condition);
    }
    if (filters?.lowStock) {
      params = params.set('lowStock', 'true');
    }
    return this.http
      .get<ApiResponse<InventoryItem[]>>('/api/inventory/items', { params })
      .pipe(map((res) => res.data ?? []));
  }

  createItem(payload: InventoryItemPayload): Observable<InventoryItem> {
    return this.http
      .post<ApiResponse<InventoryItem>>('/api/inventory/items', payload)
      .pipe(map((res) => res.data));
  }

  updateItem(id: string, payload: InventoryItemPayload): Observable<InventoryItem> {
    return this.http
      .put<ApiResponse<InventoryItem>>(`/api/inventory/items/${id}`, payload)
      .pipe(map((res) => res.data));
  }

  deleteItem(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`/api/inventory/items/${id}`)
      .pipe(map(() => undefined));
  }

  listTransactions(itemId: string): Observable<InventoryTransaction[]> {
    return this.http
      .get<ApiResponse<InventoryTransaction[]>>(`/api/inventory/items/${itemId}/transactions`)
      .pipe(map((res) => res.data ?? []));
  }

  recordTransaction(itemId: string, payload: InventoryTransactionPayload): Observable<InventoryItem> {
    return this.http
      .post<ApiResponse<InventoryItem>>(`/api/inventory/items/${itemId}/transactions`, payload)
      .pipe(map((res) => res.data));
  }

  importCsv(file: File): Observable<InventoryImportResult> {
    const body = new FormData();
    body.append('file', file);
    return this.http
      .post<ApiResponse<InventoryImportResult>>('/api/inventory/import', body)
      .pipe(map((res) => res.data));
  }

  exportReport(filters?: {
    category?: string;
    location?: string;
    condition?: string;
    lowStock?: boolean;
  }): Observable<Blob> {
    let params = new HttpParams();
    if (filters?.category) {
      params = params.set('category', filters.category);
    }
    if (filters?.location) {
      params = params.set('location', filters.location);
    }
    if (filters?.condition) {
      params = params.set('condition', filters.condition);
    }
    if (filters?.lowStock) {
      params = params.set('lowStock', 'true');
    }
    return this.http.get('/api/inventory/report', { params, responseType: 'blob' });
  }
}
