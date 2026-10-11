import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class FirstLoginGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(): boolean | UrlTree {
    if (!this.auth.isAuthenticated()) {
      return this.router.createUrlTree(['/login']);
    }
    if (!this.auth.needsFirstLogin()) {
      return this.router.createUrlTree(['/dashboard']);
    }
    return true;
  }
}
