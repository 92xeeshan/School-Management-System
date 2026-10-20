import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { applyApiBaseUrl, normalizeBackendUrl } from './api-base-url';

export const apiBaseInterceptor: HttpInterceptorFn = (req, next) => {
  const backendUrl = normalizeBackendUrl(environment.backendUrl);
  const url = applyApiBaseUrl(req.url, backendUrl);
  return next(url === req.url ? req : req.clone({ url }));
};
