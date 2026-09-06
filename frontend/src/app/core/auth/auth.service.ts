import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, tap } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
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

  hasRole(role: string): boolean {
    return this.userSubject.value?.roles?.includes(role) ?? false;
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
