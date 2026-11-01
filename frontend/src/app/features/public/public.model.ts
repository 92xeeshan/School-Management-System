export interface PublicCopy {
  locale: string;
  tagline: string | null;
  about: string | null;
  principalMessage: string | null;
  facilities: string | null;
}

export interface PublicContact {
  address: string | null;
  phone: string | null;
  email: string | null;
}

export interface PublicNotice {
  id: string;
  title: string;
  body: string;
  priority: string | null;
  publishAt: string | null;
}

export interface PublicEvent {
  id: string;
  title: string;
  description: string | null;
  eventType: string | null;
  startDate: string;
  endDate: string | null;
  allDay: boolean;
  startTime: string | null;
  endTime: string | null;
  location: string | null;
}

export interface PublicClass {
  id: string;
  name: string;
}

export interface PublicSite {
  schoolId: string;
  code: string;
  name: string;
  logoUrl: string | null;
  published: boolean;
  heroImageUrl: string | null;
  principalName: string | null;
  copy: PublicCopy;
  contact: PublicContact;
  notices: PublicNotice[];
  events: PublicEvent[];
  classes: PublicClass[];
}

export interface PublicSiteAdmin {
  id: string;
  schoolId: string;
  published: boolean;
  heroImageUrl: string | null;
  principalName: string | null;
  copies: PublicCopy[];
}

export interface Enquiry {
  id: string;
  parentName: string;
  phone: string;
  email: string | null;
  classId: string | null;
  className: string | null;
  message: string | null;
  status: 'NEW' | 'CONTACTED' | 'CLOSED';
  notes: string | null;
  createdAt: string;
  reviewedAt: string | null;
}

export interface EnquiryPayload {
  parentName: string;
  phone: string;
  email?: string | null;
  classId?: string | null;
  className?: string | null;
  message?: string | null;
}
