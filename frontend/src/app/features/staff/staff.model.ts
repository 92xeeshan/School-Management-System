export type StaffType = 'TEACHING' | 'NON_TEACHING';

export type EmploymentType = 'FULL_TIME' | 'PART_TIME' | 'CONTRACT' | 'VOLUNTEER';

export interface SectionRef {
  id: string;
  classId: string;
  className: string | null;
  name: string;
  label: string;
  classTeacherId: string | null;
  classTeacherName: string | null;
}

export interface StaffMember {
  id: string;
  staffType: StaffType;
  userId: string | null;
  employeeNo: string;
  firstName: string;
  lastName: string | null;
  displayName: string;
  email: string | null;
  phone: string | null;
  gender: string | null;
  dateOfBirth: string | null;
  designation: string | null;
  department: string | null;
  qualification: string | null;
  employmentType: EmploymentType | null;
  joinDate: string | null;
  address: string | null;
  status: string;
  subjectIds: string[] | null;
  classTeacherOf: SectionRef[] | null;
}

export interface StaffMemberPayload {
  employeeNo: string;
  firstName: string;
  lastName: string | null;
  email: string | null;
  phone: string | null;
  gender: string | null;
  dateOfBirth: string | null;
  designation: string | null;
  department: string | null;
  qualification: string | null;
  employmentType: EmploymentType | null;
  joinDate: string | null;
  address: string | null;
  userId: string | null;
}

export const EMPLOYMENT_TYPES: EmploymentType[] = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'VOLUNTEER'];
export const GENDERS = ['MALE', 'FEMALE', 'OTHER'];
