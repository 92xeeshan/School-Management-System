import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Injector, inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

let refreshing = false;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.includes('/api/') || req.url.includes('/api/auth/login') || req.url.includes('/api/auth/refresh')) {
    return next(req);
  }

  const auth = inject(AuthService);
  const injector = inject(Injector);

  let request = req;
  const token = auth.accessToken;
  if (token) {
    request = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !refreshing) {
        refreshing = true;
        return auth.refresh().pipe(
          switchMap((refreshed) => {
            refreshing = false;
            if (refreshed) {
              const nextToken = auth.accessToken ?? '';
              return next(request.clone({ setHeaders: { Authorization: `Bearer ${nextToken}` } }));
            }
            injector.get(Router).navigate(['/login']);
            return throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' }));
          }),
          catchError((refreshError) => {
            refreshing = false;
            injector.get(Router).navigate(['/login']);
            return throwError(() => refreshError);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
