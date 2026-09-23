export interface GenderBreakdown {
  boys: number;
  girls: number;
  other: number;
  total: number;
  boysPercent: number;
  girlsPercent: number;
  otherPercent: number;
}

export interface AttendanceTrendPoint {
  date: string;
  present: number;
  absent: number;
  late: number;
  leave: number;
  total: number;
  percentage: number;
}

export interface StarStudent {
  studentId: string;
  name: string;
  admissionNo: string;
  className: string | null;
  sectionName: string | null;
  achievement: string;
  category: string;
  badge: string;
  points: number;
  awardedDate: string;
}

export interface AttendanceSummary {
  studentId: string;
  studentName: string;
  present: number;
  absent: number;
  late: number;
  leave: number;
  total: number;
  percentage: number;
}

export interface MySection {
  id: string;
  name: string | null;
  className: string | null;
  studentCount: number;
  averagePercentage: number | null;
}

export interface MyChild {
  id: string;
  name: string;
  admissionNo: string;
  className: string | null;
  sectionName: string | null;
}

export interface DashboardSummary {
  totalStudents: number;
  totalTeachers: number;
  totalStaff: number;
  totalClasses: number;
  totalSections: number;
  totalAwards: number;
  presentToday: number;
  absentToday: number;
  publishedNotices: number;
  unreadNotices: number;
  feesCollected: number;
  feesOverdue: number;
  gender: GenderBreakdown;
  attendanceTrend: AttendanceTrendPoint[];
  starStudents: StarStudent[];
  mySections: MySection[];
  myChildren: MyChild[];
  myAttendance: AttendanceSummary | null;
  feeBalance: number | null;
  pendingGrading: number;
}

export type EventType =
  | 'HOLIDAY'
  | 'EXAM'
  | 'MEETING'
  | 'EVENT'
  | 'ACTIVITY'
  | 'OTHER'
  | 'PTM'
  | 'SPORTS'
  | 'NOTICE';

export interface SchoolEvent {
  id: string;
  title: string;
  description: string | null;
  eventType: EventType;
  startDate: string;
  endDate: string | null;
  allDay: boolean;
  startTime: string | null;
  endTime: string | null;
  location: string | null;
  visibilityScope: string;
  audienceRole?: string | null;
  classId: string | null;
  className: string | null;
  sectionId: string | null;
  sectionName: string | null;
  updatedAt?: string | null;
}

export interface DashboardNotice {
  id: string;
  title: string;
  body: string;
  priority: string;
  publishAt: string | null;
}

export interface SchoolEventPayload {
  title: string;
  description: string | null;
  eventType: EventType;
  startDate: string;
  endDate: string | null;
  allDay: boolean;
  startTime: string | null;
  endTime: string | null;
  location: string | null;
  visibilityScope: string;
}

export type WidgetId =
  | 'gender'
  | 'attendanceTrend'
  | 'starStudents'
  | 'roleInsights'
  | 'calendar'
  | 'agenda'
  | 'upcoming'
  | 'notices';
