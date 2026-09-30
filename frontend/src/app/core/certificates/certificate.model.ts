export type CertificateType = 'TC' | 'BONAFIDE' | 'CHARACTER' | 'COURSE_COMPLETION';
export type CertificateStatus = 'DRAFT' | 'APPROVED' | 'ISSUED';
export type CertificateRequestStatus =
  | 'SUBMITTED'
  | 'TEACHER_REVIEWED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'ISSUED';

export interface CertificateTemplate {
  id: string;
  type: CertificateType;
  headerHtml: string | null;
  footerHtml: string | null;
  signatureImageUrl: string | null;
  sealImageUrl: string | null;
  active: boolean;
  requiresApproval: boolean;
}

export interface CertificateIssued {
  id: string;
  studentId: string;
  studentName: string;
  admissionNo: string;
  className: string;
  sectionName: string;
  certificateType: CertificateType;
  certificateNo: string | null;
  issuedDate: string | null;
  issuedByUserId: string;
  issuedByName: string;
  approvedByUserId: string | null;
  approvedByName: string | null;
  approvedAt: string | null;
  status: CertificateStatus;
  reason: string | null;
  conductRemarks: string | null;
  guardianName: string;
  academicYearName: string;
  dateOfBirth: string;
  duplicate: boolean;
  canApprove: boolean;
  canDownload: boolean;
}

export interface CertificateRequest {
  id: string;
  studentId: string;
  studentName: string;
  admissionNo: string;
  rollNo: string;
  className: string;
  sectionName: string;
  dateOfBirth: string;
  guardianName: string;
  certificateType: CertificateType;
  status: CertificateRequestStatus;
  reason: string | null;
  conductRemarks: string | null;
  academicProgress: string | null;
  lastExamAttended: string | null;
  duesLibrary: boolean | null;
  duesAccounts: boolean | null;
  duesSports: boolean | null;
  teacherNotes: string | null;
  rejectionReason: string | null;
  supportingDocName: string | null;
  issuedId: string | null;
  certificateNo: string | null;
  createdAt: string;
  reviewedAt: string | null;
  approvedAt: string | null;
  canReview: boolean;
  canCancel: boolean;
  canApprove: boolean;
  canReject: boolean;
  canDownload: boolean;
}

export interface GenerateCertificateRequest {
  studentId: string;
  templateType: CertificateType;
  reason?: string | null;
  conductRemarks?: string | null;
  academicProgress?: string | null;
  lastExamAttended?: string | null;
  duesLibrary?: boolean | null;
  duesAccounts?: boolean | null;
  duesSports?: boolean | null;
  duplicate?: boolean;
}

export interface ReviewCertificateRequest {
  conductRemarks?: string | null;
  academicProgress?: string | null;
  lastExamAttended?: string | null;
  reason?: string | null;
  teacherNotes?: string | null;
  duesLibrary?: boolean | null;
  duesAccounts?: boolean | null;
  duesSports?: boolean | null;
}

export interface CertificateTemplateRequest {
  type: CertificateType;
  headerHtml?: string | null;
  footerHtml?: string | null;
  signatureImageUrl?: string | null;
  sealImageUrl?: string | null;
  active?: boolean;
  requiresApproval?: boolean;
}
