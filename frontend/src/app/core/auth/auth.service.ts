import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, firstValueFrom, of, tap } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import {
  AuthResponse,
  AuthUser,
  LinkedChild,
  LoginRequest,
  RefreshRequest,
  SecurityQuestion,
} from './auth.model';
import { ApiResponse } from '../models/api.model';

const STORAGE_KEY = 'schoolms.auth';
const CHILD_KEY = 'schoolms.activeChild';

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
  private readonly childSubject = new BehaviorSubject<LinkedChild | null>(this.readChild());
  readonly activeChild$ = this.childSubject.asObservable();

  constructor(private http: HttpClient) {
    const session = this.readSession();
    if (session) {
      this.userSubject.next(session.user);
    }
  }

  hasRole(role: string): boolean {
    return this.userSubject.value?.roles?.includes(role) ?? false;
  }

  hasAnyRole(roles: string[]): boolean {
    return roles.some((role) => this.hasRole(role));
  }

  hydrateUser(): Promise<void> {
    if (!this.isAuthenticated()) {
      return Promise.resolve();
    }
    void firstValueFrom(
      this.http.get<ApiResponse<AuthUser>>('/api/auth/me').pipe(
        tap((res) => this.updateUser(res.data)),
        map(() => undefined),
        catchError(() => of(undefined))
      )
    );
    return Promise.resolve();
  }

  login(username: string, password: string): Observable<AuthResponse> {
    const body: LoginRequest = { username, password };
    return this.http.post<ApiResponse<AuthResponse>>('/api/auth/login', body).pipe(
      map((res) => {
        this.setSession(res.data);
        return res.data;
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

  needsFirstLogin(): boolean {
    return this.currentUser?.mustChangePassword === true;
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

  listQuestions(): Observable<SecurityQuestion[]> {
    return this.http
      .get<ApiResponse<SecurityQuestion[]>>('/api/auth/questions')
      .pipe(map((res) => res.data ?? []));
  }

  completeFirstLogin(payload: {
    newPassword: string;
    confirmPassword: string;
    answers: { questionId: string; answer: string }[];
  }): Observable<void> {
    return this.http.post<ApiResponse<void>>('/api/auth/first-login', payload).pipe(map(() => undefined));
  }

  changePassword(payload: {
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;
  }): Observable<void> {
    return this.http.post<ApiResponse<void>>('/api/auth/change-password', payload).pipe(map(() => undefined));
  }

  startForgot(username: string): Observable<{ challengeId: string; questions: SecurityQuestion[] }> {
    return this.http
      .post<ApiResponse<{ challengeId: string; questions: SecurityQuestion[] }>>('/api/auth/forgot', { username })
      .pipe(map((res) => res.data));
  }

  verifyForgot(challengeId: string, answers: { questionId: string; answer: string }[]): Observable<string> {
    return this.http
      .post<ApiResponse<{ resetToken: string }>>('/api/auth/forgot/verify', { challengeId, answers })
      .pipe(map((res) => res.data.resetToken));
  }

  completeForgot(resetToken: string, newPassword: string, confirmPassword: string): Observable<void> {
    return this.http
      .post<ApiResponse<void>>('/api/auth/forgot/complete', { resetToken, newPassword, confirmPassword })
      .pipe(map(() => undefined));
  }

  listChildren(): Observable<LinkedChild[]> {
    return this.http.get<ApiResponse<LinkedChild[]>>('/api/auth/children').pipe(map((res) => res.data ?? []));
  }

  setActiveChild(child: LinkedChild | null): void {
    if (child) {
      localStorage.setItem(CHILD_KEY, JSON.stringify(child));
    } else {
      localStorage.removeItem(CHILD_KEY);
    }
    this.childSubject.next(child);
  }

  get activeChild(): LinkedChild | null {
    return this.childSubject.value;
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

  private updateUser(user: AuthUser): void {
    const session = this.readSession();
    if (!session || !user) {
      return;
    }
    session.user = user;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    this.userSubject.next(user);
  }

  private clearSession(): void {
    localStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem(CHILD_KEY);
    this.userSubject.next(null);
    this.childSubject.next(null);
  }

  private storedRefreshToken(): string | null {
    return this.readSession()?.refreshToken ?? null;
  }

  private readSession(): StoredSession | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return null;
      }
      const session = JSON.parse(raw) as StoredSession;
      if (!this.isUsableSession(session)) {
        localStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return session;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }

  private readChild(): LinkedChild | null {
    try {
      const raw = localStorage.getItem(CHILD_KEY);
      return raw ? (JSON.parse(raw) as LinkedChild) : null;
    } catch {
      return null;
    }
  }

  private isUsableSession(session: StoredSession | null): boolean {
    if (!session?.accessToken || !session.user) {
      return false;
    }
    if (session.expiresAt <= Date.now()) {
      return false;
    }
    if (session.accessToken === 'demo-access-token') {
      return false;
    }
    return session.accessToken.split('.').length === 3;
  }
}
