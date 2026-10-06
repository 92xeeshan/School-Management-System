export type PayrollRunStatus = 'DRAFT' | 'PROCESSED' | 'PUBLISHED' | 'PAID';
export type StaffType = 'TEACHING' | 'NON_TEACHING';
export type PaymentMethod = 'CASH' | 'CARD' | 'UPI' | 'BANK_TRANSFER';

export interface SalaryLineItem {
  name: string;
  amount: number;
}

export interface ExpenseCategory {
  id: string;
  name: string;
  code: string;
  description: string | null;
}

export interface Expense {
  id: string;
  categoryId: string;
  categoryName: string | null;
  categoryCode: string | null;
  amount: number;
  expenseDate: string;
  vendor: string | null;
  description: string | null;
  paymentMethod: PaymentMethod;
}

export interface ExpenseMonthTotal {
  month: number;
  total: number;
}

export interface ExpenseCategoryTotal {
  categoryId: string;
  code: string;
  name: string;
  total: number;
  months: ExpenseMonthTotal[];
}

export interface ExpenseChart {
  year: number;
  yearTotal: number;
  months: ExpenseMonthTotal[];
  categories: ExpenseCategoryTotal[];
}

export interface PayrollRun {
  id: string;
  year: number;
  month: number;
  status: PayrollRunStatus;
  processedAt: string | null;
  publishedAt: string | null;
  paidAt: string | null;
  notes: string | null;
  payslipCount: number;
  netTotal: number;
}

export interface Payslip {
  id: string;
  payrollRunId: string;
  year: number;
  month: number;
  runStatus: PayrollRunStatus;
  staffType: StaffType;
  staffId: string;
  userId: string | null;
  employeeNo: string;
  staffName: string;
  designation: string | null;
  department: string | null;
  basic: number;
  hra: number;
  allowances: SalaryLineItem[];
  deductions: SalaryLineItem[];
  gross: number;
  totalDeductions: number;
  net: number;
}

export interface ExpensePayload {
  categoryId: string;
  amount: number;
  expenseDate: string;
  vendor: string | null;
  description: string | null;
  paymentMethod: PaymentMethod;
}

export interface PayrollRunPayload {
  year: number;
  month: number;
  notes: string | null;
}

export const PAYMENT_METHODS: PaymentMethod[] = ['CASH', 'CARD', 'UPI', 'BANK_TRANSFER'];
export const MONTHS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];
