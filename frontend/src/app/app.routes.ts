import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { AuthGuard } from './core/auth/auth.guard';
import { PermissionGuard } from './core/auth/permission.guard';
import { AuthService } from './core/auth/auth.service';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
        canActivate: [PermissionGuard],
        data: { permissions: ['DASHBOARD_VIEW'] },
      },
      {
        path: 'students',
        loadComponent: () =>
          import('./features/students/students.component').then((m) => m.StudentsComponent),
        canActivate: [PermissionGuard],
        data: { permissions: ['STUDENT_READ'] },
      },
      {
        path: 'students/:id',
        loadComponent: () =>
          import('./features/students/student-profile.component').then((m) => m.StudentProfileComponent),
        canActivate: [PermissionGuard],
        data: { permissions: ['STUDENT_READ'] },
      },
      {
        path: 'academics',
        loadComponent: () =>
          import('./features/academics/academics-shell.component').then((m) => m.AcademicsShellComponent),
        canActivate: [PermissionGuard],
        data: { permissions: ['CLASS_READ'] },
        children: [
          { path: '', redirectTo: 'classes', pathMatch: 'full' },
          {
            path: 'classes',
            loadComponent: () =>
              import('./features/academics/academics.component').then((m) => m.AcademicsComponent),
          },
          {
            path: 'subjects',
            loadComponent: () =>
              import('./features/academics/subjects.component').then((m) => m.AcademicsSubjectsComponent),
            canActivate: [PermissionGuard],
            data: { permissions: ['SUBJECT_READ'] },
          },
          {
            path: 'timetable',
            loadComponent: () =>
              import('./features/academics/timetable.component').then((m) => m.AcademicsTimetableComponent),
            canActivate: [PermissionGuard],
            data: { permissions: ['TIMETABLE_READ', 'CLASS_READ'] },
          },
          {
            path: 'syllabus',
            loadComponent: () =>
              import('./features/academics/syllabus.component').then((m) => m.AcademicsSyllabusComponent),
          },
          {
            path: 'examinations',
            loadComponent: () =>
              import('./features/academics/examinations.component').then((m) => m.AcademicsExaminationsComponent),
            canActivate: [PermissionGuard],
            data: { permissions: ['EXAM_READ', 'CLASS_READ'] },
          },
          {
            path: 'terms',
            loadComponent: () =>
              import('./features/academics/academic-terms.component').then((m) => m.AcademicsTermsComponent),
          },
        ],
      },
      {
        path: 'examinations',
        loadComponent: () =>
          import('./features/examinations/examinations-shell.component').then((m) => m.ExaminationsShellComponent),
        canActivate: [PermissionGuard],
        data: { permissions: ['EXAM_READ', 'EXAM_MANAGE'] },
        children: [
          {
            path: '',
            pathMatch: 'full',
            redirectTo: () => inject(AuthService).hasPermission('EXAM_MANAGE') ? 'schedules' : 'marks',
          },
          {
            path: 'schedules',
            loadComponent: () =>
              import('./features/examinations/exam-schedules.component').then((m) => m.ExamSchedulesComponent),
            canActivate: [PermissionGuard],
            data: { permissions: ['EXAM_MANAGE'] },
          },
          {
            path: 'marks',
            loadComponent: () =>
              import('./features/examinations/marks-entry.component').then((m) => m.MarksEntryComponent),
            canActivate: [PermissionGuard],
            data: { permissions: ['EXAM_READ', 'EXAM_MANAGE'] },
          },
          {
            path: 'report-cards',
            loadComponent: () =>
              import('./features/examinations/report-cards.component').then((m) => m.ReportCardsComponent),
            canActivate: [PermissionGuard],
            data: { permissions: ['EXAM_MANAGE'] },
          },
          {
            path: 'analytics',
            loadComponent: () =>
              import('./features/examinations/exam-analytics.component').then((m) => m.ExamAnalyticsComponent),
            canActivate: [PermissionGuard],
            data: { permissions: ['EXAM_MANAGE'] },
          },
          {
            path: 'reevaluation',
            loadComponent: () =>
              import('./features/examinations/reevaluation.component').then((m) => m.ReevaluationComponent),
            canActivate: [PermissionGuard],
            data: { permissions: ['EXAM_MANAGE'] },
          },
        ],
      },
      {
        path: 'attendance',
        loadComponent: () =>
          import('./features/attendance/attendance.component').then((m) => m.AttendanceComponent),
        canActivate: [PermissionGuard],
        data: { permissions: ['ATTENDANCE_READ', 'ATTENDANCE_MARK'] },
      },
      {
        path: 'fees',
        loadComponent: () =>
          import('./features/fees/fees.component').then((m) => m.FeesComponent),
        canActivate: [PermissionGuard],
        data: { permissions: ['FEE_READ', 'FEE_RECEIPT_VIEW'] },
      },
      {
        path: 'notices',
        loadComponent: () =>
          import('./features/notices/notices.component').then((m) => m.NoticesComponent),
        canActivate: [PermissionGuard],
        data: { permissions: ['NOTICE_READ'] },
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
