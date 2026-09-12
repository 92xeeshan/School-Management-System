import { ApplicationConfig, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideTranslateService } from '@ngx-translate/core';

import { routes } from './app.routes';
import { translateConfig } from './i18n/i18n.config';
import { authInterceptor } from './core/auth/auth.interceptor';
import { LocaleService } from './i18n/locale.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideTranslateService(translateConfig),
    provideAppInitializer(() => inject(LocaleService).init()),
  ],
};
