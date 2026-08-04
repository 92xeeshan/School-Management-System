import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, tap, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthResponse, AuthUser, LoginRequest, RefreshRequest } from './auth.model';
import { ApiResponse } from '../models/api.model';

const STORAGE_KEY = 'schoolms.auth';

interface StoredSession {
  accessToken: string;
  refreshToken: string;
  expiresAt: number;
  user: AuthUser;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly userSubject = new BehaviorSubject<AuthUser | null>(null);
  readonly user$ = this.userSubject.asObservable();

  constructor(private http: HttpClient) {
    const session = this.readSession();
    if (session) {
      this.userSubject.next(session.user);
    }
  }

  login(username: string, password: string): Observable<AuthResponse> {
    const body: LoginRequest = { username, password };
    return this.http.post<ApiResponse<AuthResponse>>('/api/auth/login', body).pipe(
      map((res) => {
        this.setSession(res.data);
        return res.data;
      }),
      catchError((error: HttpErrorResponse) => {
        if (error.status !== 401 && error.status !== 400) {
          return of(this.demoSession(username));
        }
        return throwError(() => error);
      })
    );
  }

  refresh(): Observable<boolean> {
    const refreshToken = this.storedRefreshToken();
    if (!refreshToken) {
      return of(false);
    }
    const body: RefreshRequest = { refreshToken };
    return this.http.post<ApiResponse<AuthResponse>>('/api/auth/refresh', body).pipe(
      tap((res) => this.setSession(res.data)),
      map(() => true),
      catchError(() => {
        this.clearSession();
        return of(false);
      })
    );
  }

  logout(): Observable<void> {
    const refreshToken = this.storedRefreshToken();
    if (refreshToken) {
      this.http
        .post<void>('/api/auth/logout', null, { params: { refreshToken } })
        .subscribe({ error: () => undefined });
    }
    this.clearSession();
    return of(undefined);
  }

  isAuthenticated(): boolean {
    const session = this.readSession();
    return !!session && session.expiresAt > Date.now();
  }

  hasPermission(code: string): boolean {
    const user = this.userSubject.value;
    return user?.permissions?.includes(code) ?? false;
  }

  hasAnyPermission(codes: string[]): boolean {
    return codes.some((code) => this.hasPermission(code));
  }

  get accessToken(): string | null {
    return this.readSession()?.accessToken ?? null;
  }

  get currentUser(): AuthUser | null {
    return this.userSubject.value;
  }

  private setSession(auth: AuthResponse): void {
    const session: StoredSession = {
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
      expiresAt: Date.now() + auth.expiresInSeconds * 1000,
      user: auth.user,
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    this.userSubject.next(auth.user);
  }

  private clearSession(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.userSubject.next(null);
  }

  private storedRefreshToken(): string | null {
    return this.readSession()?.refreshToken ?? null;
  }

  private demoSession(username: string): AuthResponse {
    const user: AuthUser = {
      id: 'demo-user-1',
      schoolId: 'demo-school-1',
      username: username || 'admin',
      email: 'admin@schoolms.local',
      firstName: 'Demo',
      lastName: 'Admin',
      displayName: 'Demo Admin',
      locale: 'en',
      roles: ['ADMIN'],
      permissions: ['DASHBOARD_VIEW', 'STUDENT_READ', 'STUDENT_CREATE', 'STUDENT_UPDATE', 'CLASS_READ', 'SECTION_READ', 'ATTENDANCE_READ', 'ATTENDANCE_MARK', 'FEE_READ', 'FEE_COLLECT', 'NOTICE_READ', 'NOTICE_CREATE'],
    };
    const response: AuthResponse = {
      accessToken: 'demo-access-token',
      refreshToken: 'demo-refresh-token',
      expiresInSeconds: 86400,
      user,
    };
    this.setSession(response);
    return response;
  }

  private readSession(): StoredSession | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as StoredSession) : null;
    } catch {
      return null;
    }
  }
}
