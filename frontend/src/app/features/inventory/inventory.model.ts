export type InventoryCondition = 'NEW' | 'GOOD' | 'DAMAGED' | 'UNDER_REPAIR' | 'RETIRED';
export type InventoryTransactionType = 'IN' | 'OUT' | 'CHECKOUT' | 'CHECKIN';

export const INVENTORY_CONDITIONS: InventoryCondition[] = [
  'NEW',
  'GOOD',
  'DAMAGED',
  'UNDER_REPAIR',
  'RETIRED',
];

export const STOCK_TYPES: InventoryTransactionType[] = ['IN', 'OUT'];
export const ASSET_TYPES: InventoryTransactionType[] = ['CHECKOUT', 'CHECKIN'];

export interface InventoryCategory {
  id: string;
  name: string;
  description: string | null;
}

export interface InventoryAssignee {
  id: string;
  name: string;
  username: string;
}

export interface InventoryItem {
  id: string;
  categoryId: string;
  categoryName: string | null;
  name: string;
  sku: string | null;
  unit: string;
  quantity: number;
  reorderThreshold: number;
  lowStock: boolean;
  purchaseDate: string | null;
  purchaseCost: number | null;
  vendor: string | null;
  location: string | null;
  condition: InventoryCondition;
  assignedToUserId: string | null;
  assignedToName: string | null;
  assignedRoom: string | null;
}

export interface InventoryItemPayload {
  categoryId: string;
  name: string;
  sku: string | null;
  unit: string;
  quantity: number | null;
  reorderThreshold: number;
  purchaseDate: string | null;
  purchaseCost: number | null;
  vendor: string | null;
  location: string | null;
  condition: InventoryCondition;
}

export interface InventoryTransaction {
  id: string;
  itemId: string;
  type: InventoryTransactionType;
  quantity: number;
  reason: string | null;
  assignedToUserId: string | null;
  assignedToName: string | null;
  room: string | null;
  transactionDate: string;
  createdAt: string;
}

export interface InventoryTransactionPayload {
  type: InventoryTransactionType;
  quantity: number;
  reason: string | null;
  assignedToUserId: string | null;
  room: string | null;
  transactionDate: string;
}

export interface InventoryImportResult {
  total: number;
  succeeded: number;
  errors: string[];
}

export interface InventoryFilters {
  category: string;
  location: string;
  condition: string;
  lowStock: boolean;
  search: string;
}
