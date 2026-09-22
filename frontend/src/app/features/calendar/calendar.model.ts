export type CalendarEventType =
  | 'HOLIDAY'
  | 'EXAM'
  | 'MEETING'
  | 'EVENT'
  | 'ACTIVITY'
  | 'OTHER'
  | 'PTM'
  | 'SPORTS'
  | 'NOTICE';

export type CalendarView = 'month' | 'week' | 'agenda';

export interface CalendarEvent {
  id: string;
  title: string;
  description: string | null;
  eventType: CalendarEventType;
  startDate: string;
  endDate: string | null;
  allDay: boolean;
  startTime: string | null;
  endTime: string | null;
  location: string | null;
  visibilityScope: string;
  audienceRole: string | null;
  classId: string | null;
  className: string | null;
  sectionId: string | null;
  sectionName: string | null;
  updatedAt: string | null;
}

export interface CalendarEventPayload {
  title: string;
  description: string | null;
  eventType: CalendarEventType;
  startDate: string;
  endDate: string | null;
  allDay: boolean;
  startTime: string | null;
  endTime: string | null;
  location: string | null;
  visibilityScope: string;
  audienceRole: string | null;
  classId: string | null;
  sectionId: string | null;
}

export interface CalendarSectionOption {
  id: string;
  name: string;
}

export interface CalendarClassOption {
  id: string;
  name: string;
  sections: CalendarSectionOption[];
}

export interface CalendarOptions {
  eventTypes: string[];
  visibilityScopes: string[];
  roles: string[];
  classes: CalendarClassOption[];
}

export const EVENT_TYPES: CalendarEventType[] = [
  'HOLIDAY',
  'EXAM',
  'PTM',
  'SPORTS',
  'NOTICE',
  'MEETING',
  'EVENT',
  'ACTIVITY',
  'OTHER',
];
