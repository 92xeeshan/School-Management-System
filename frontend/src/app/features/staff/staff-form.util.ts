import { FormControl, FormGroup, Validators } from '@angular/forms';
import { StaffMember, StaffMemberPayload } from './staff.model';

/**
 * Shared reactive form definition for teaching and non-teaching staff so the
 * two tabs stay in sync.
 */
export function createStaffForm(): FormGroup {
  return new FormGroup({
    employeeNo: new FormControl('', Validators.required),
    firstName: new FormControl('', Validators.required),
    lastName: new FormControl(''),
    email: new FormControl(''),
    phone: new FormControl(''),
    gender: new FormControl(''),
    dateOfBirth: new FormControl(''),
    designation: new FormControl(''),
    department: new FormControl(''),
    qualification: new FormControl(''),
    employmentType: new FormControl(''),
    joinDate: new FormControl(''),
    address: new FormControl(''),
  });
}

export function staffFormValue(staff: StaffMember): Record<string, string> {
  return {
    employeeNo: staff.employeeNo ?? '',
    firstName: staff.firstName ?? '',
    lastName: staff.lastName ?? '',
    email: staff.email ?? '',
    phone: staff.phone ?? '',
    gender: staff.gender ?? '',
    dateOfBirth: staff.dateOfBirth ?? '',
    designation: staff.designation ?? '',
    department: staff.department ?? '',
    qualification: staff.qualification ?? '',
    employmentType: staff.employmentType ?? '',
    joinDate: staff.joinDate ?? '',
    address: staff.address ?? '',
  };
}

export function staffFormToPayload(form: FormGroup): StaffMemberPayload {
  const value = form.getRawValue() as Record<string, string>;
  return {
    employeeNo: (value['employeeNo'] ?? '').trim(),
    firstName: (value['firstName'] ?? '').trim(),
    lastName: blankToNull(value['lastName']),
    email: blankToNull(value['email']),
    phone: blankToNull(value['phone']),
    gender: blankToNull(value['gender']),
    dateOfBirth: blankToNull(value['dateOfBirth']),
    designation: blankToNull(value['designation']),
    department: blankToNull(value['department']),
    qualification: blankToNull(value['qualification']),
    employmentType: (blankToNull(value['employmentType']) as StaffMemberPayload['employmentType']),
    joinDate: blankToNull(value['joinDate']),
    address: blankToNull(value['address']),
    userId: null,
  };
}

function blankToNull(value: string | null | undefined): string | null {
  const trimmed = (value ?? '').trim();
  return trimmed.length === 0 ? null : trimmed;
}
