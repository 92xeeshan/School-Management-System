import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class PermissionGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot): boolean | UrlTree {
    const required = (route.data['permissions'] as string[] | undefined) ?? [];
    if (required.length === 0 || this.auth.hasAnyPermission(required)) {
      return true;
    }
    return this.router.createUrlTree(['/dashboard']);
  }
}
