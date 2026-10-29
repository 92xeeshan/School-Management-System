export type StaffType = 'TEACHING' | 'NON_TEACHING';
export type LeaveStaffCategory = 'TEACHING' | 'NON_TEACHING' | 'ALL';
export type LeaveYearMode = 'ACADEMIC' | 'CALENDAR';
export type LeaveRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface LeaveType {
  id: string;
  code: string;
  name: string;
  yearlyQuota: number;
  paid: boolean;
  reasonMandatory: boolean;
  active: boolean;
}

export interface LeaveTypePayload {
  code: string;
  name: string;
  yearlyQuota: number;
  paid: boolean;
  reasonMandatory: boolean;
  active?: boolean;
}

export interface LeavePolicyItem {
  leaveTypeId: string;
  leaveTypeCode: string;
  leaveTypeName: string;
  yearlyQuota: number;
}

export interface LeavePolicy {
  id: string;
  name: string;
  staffCategory: LeaveStaffCategory;
  active: boolean;
  items: LeavePolicyItem[];
}

export interface LeavePolicyPayload {
  name: string;
  staffCategory: LeaveStaffCategory;
  active?: boolean;
  items: { leaveTypeId: string; yearlyQuota: number }[];
}

export interface LeaveYearSetting {
  id: string | null;
  mode: LeaveYearMode;
  yearStart: string;
  yearEnd: string;
}

export interface LeaveAssignment {
  id: string;
  staffType: StaffType;
  staffId: string;
  staffName: string;
  employeeNo: string;
  policyId: string;
  policyName: string;
  yearStart: string;
}

export interface StaffLeaveOption {
  staffType: StaffType;
  staffId: string;
  staffName: string;
  employeeNo: string;
  policyId: string | null;
  policyName: string | null;
}

export interface LeaveBalance {
  leaveTypeId: string;
  leaveTypeCode: string;
  leaveTypeName: string;
  paid: boolean;
  staffType: StaffType;
  staffId: string;
  staffName: string;
  yearStart: string;
  entitled: number;
  used: number;
  pending: number;
  available: number;
}

export interface LeaveRequest {
  id: string;
  staffType: StaffType;
  staffId: string;
  staffName: string;
  employeeNo: string;
  leaveTypeId: string;
  leaveTypeCode: string;
  leaveTypeName: string;
  fromDate: string;
  toDate: string;
  days: number;
  reason: string;
  status: LeaveRequestStatus;
  supportingDocName: string | null;
  rejectionReason: string | null;
  cancelReason: string | null;
  createdAt: string;
  decidedAt: string | null;
  canCancel: boolean;
  canApprove: boolean;
  balance: LeaveBalance | null;
}

export interface LeaveDayCount {
  fromDate: string;
  toDate: string;
  days: number;
}
