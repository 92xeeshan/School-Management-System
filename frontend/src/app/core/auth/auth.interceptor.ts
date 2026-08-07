import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, filter, Observable, of, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private refreshing = false;

  constructor(private auth: AuthService, private router: Router) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (req.url.startsWith('/api/auth/')) {
      return next.handle(req);
    }

    let request = req;
    const token = this.auth.accessToken;
    if (token) {
      request = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 && !this.refreshing) {
          return this.handleUnauthorized(request, next);
        }
        return throwError(() => error);
      })
    );
  }

  private handleUnauthorized(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    this.refreshing = true;
    return this.auth.refresh().pipe(
      switchMap((refreshed) => {
        this.refreshing = false;
        if (refreshed) {
          const token = this.auth.accessToken ?? '';
          return next.handle(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
        }
        this.router.navigate(['/login']);
        return throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' }));
      }),
      catchError((error) => {
        this.refreshing = false;
        this.router.navigate(['/login']);
        return throwError(() => error);
      })
    );
  }
}
