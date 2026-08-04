import { Routes } from '@angular/router';
import { AuthGuard } from './core/auth/auth.guard';
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
      },
      {
        path: 'students',
        loadComponent: () =>
          import('./features/students/students.component').then((m) => m.StudentsComponent),
      },
      {
        path: 'academics',
        loadComponent: () =>
          import('./features/academics/academics.component').then((m) => m.AcademicsComponent),
      },
      {
        path: 'attendance',
        loadComponent: () =>
          import('./features/attendance/attendance.component').then((m) => m.AttendanceComponent),
      },
      {
        path: 'fees',
        loadComponent: () =>
          import('./features/fees/fees.component').then((m) => m.FeesComponent),
      },
      {
        path: 'notices',
        loadComponent: () =>
          import('./features/notices/notices.component').then((m) => m.NoticesComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
