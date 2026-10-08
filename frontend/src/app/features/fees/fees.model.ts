export type FeeFrequency = 'ONE_TIME' | 'MONTHLY' | 'QUARTERLY' | 'HALF_YEARLY' | 'ANNUAL';
export type DiscountType = 'FIXED' | 'PERCENT';
export type LevyScope = 'GLOBAL' | 'CLASS' | 'SECTION' | 'STUDENT';

export const FEE_FREQUENCIES: FeeFrequency[] = ['ONE_TIME', 'MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'ANNUAL'];
export const LEVY_SCOPES: LevyScope[] = ['GLOBAL', 'CLASS', 'SECTION', 'STUDENT'];
export const PAYMENT_METHODS = ['CASH', 'CARD', 'UPI', 'BANK_TRANSFER'];

export interface AcademicYearOption {
  id: string;
  name: string;
  current?: boolean;
}

export interface ClassOption {
  id: string;
  name: string;
}

export interface SectionOption {
  id: string;
  name: string;
  classId: string;
}

export interface StudentOption {
  id: string;
  admissionNo: string;
  displayName: string;
}

export interface FeeHead {
  id: string;
  name: string;
  code: string;
  description: string | null;
  optional: boolean;
  refundable: boolean;
  frequency: FeeFrequency;
  status: string;
}

export interface FeeHeadPayload {
  name: string;
  code: string;
  description: string | null;
  optional: boolean;
  refundable: boolean;
  frequency: FeeFrequency;
  status: string;
}

export interface FeeStructureRow {
  id: string;
  classId: string;
  className: string;
  academicYearId: string;
  categoryId: string;
  categoryName: string;
  amount: number;
  frequency: FeeFrequency;
  dueDay: number | null;
  dueDate: string | null;
  applicableFrom: string | null;
  applicableTo: string | null;
}

export interface FeeStructurePayload {
  classId: string | null;
  applyToAllClasses: boolean;
  academicYearId: string;
  categoryId: string;
  amount: number;
  frequency: FeeFrequency;
  dueDay: number | null;
  dueDate: string | null;
}

export interface CloneResult {
  cloned: number;
  skipped: number;
}

export interface SiblingDiscountRule {
  id: string;
  academicYearId: string;
  academicYearName: string | null;
  siblingOrder: number;
  discountType: DiscountType;
  discountValue: number;
  feeCategoryId: string;
  feeCategoryName: string | null;
  status: string;
}

export interface SiblingDiscountPayload {
  academicYearId: string;
  siblingOrder: number;
  discountType: DiscountType;
  discountValue: number;
  feeCategoryId: string;
  status: string;
}

export interface AdhocLevy {
  id: string;
  academicYearId: string;
  academicYearName: string | null;
  feeCategoryId: string;
  feeCategoryName: string | null;
  amount: number;
  dueDate: string;
  scope: LevyScope;
  classId: string | null;
  className: string | null;
  sectionId: string | null;
  sectionName: string | null;
  studentId: string | null;
  studentName: string | null;
  remarks: string | null;
  assignedCount: number;
}

export interface AdhocLevyPayload {
  academicYearId: string;
  feeCategoryId: string;
  amount: number;
  dueDate: string;
  scope: LevyScope;
  classId: string | null;
  sectionId: string | null;
  studentId: string | null;
  remarks: string | null;
}

export interface FeeStructureAudit {
  id: string;
  action: string;
  className: string | null;
  categoryName: string | null;
  academicYearName: string | null;
  previousAmount: number | null;
  newAmount: number | null;
  previousFrequency: string | null;
  newFrequency: string | null;
  changedByName: string | null;
  changedAt: string;
}

export interface FeeAssignment {
  id: string;
  studentId: string;
  studentName: string;
  admissionNo: string;
  feeStructureName: string;
  amount: number;
  frequency: string;
  discountAmount: number;
  discountType: string | null;
  status: string;
  installments: Installment[];
}

export interface Installment {
  id: string;
  studentFeeAssignmentId: string;
  dueDate: string;
  amountDue: number;
  amountPaid: number;
  status: string;
  balance: number;
}

export interface FeePayment {
  id: string;
  receiptNo: string;
  studentName: string;
  amountPaid: number;
  paidAt: string;
  paymentMethod: string;
  referenceNo: string;
  remarks: string;
}
