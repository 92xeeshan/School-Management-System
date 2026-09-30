export type CertificateType = 'TC' | 'BONAFIDE';
export type CertificateStatus = 'DRAFT' | 'APPROVED' | 'ISSUED';

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

export interface GenerateCertificateRequest {
  studentId: string;
  templateType: CertificateType;
  reason?: string | null;
  conductRemarks?: string | null;
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
