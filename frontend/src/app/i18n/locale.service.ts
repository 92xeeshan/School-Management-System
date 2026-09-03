import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { LOCALE_META, SupportedLocale, SUPPORTED_LOCALES } from './i18n.config';

const STORAGE_KEY = 'schoolms.locale';

@Injectable({ providedIn: 'root' })
export class LocaleService {
  private readonly localeSubject = new BehaviorSubject<SupportedLocale>(this.storedLocale());
  readonly locale$ = this.localeSubject.asObservable();

  constructor(private translate: TranslateService) {
    this.translate.setDefaultLang('en');
  }

  init(): Promise<unknown> {
    return this.apply(this.localeSubject.value);
  }

  setLocale(locale: SupportedLocale): void {
    if (!SUPPORTED_LOCALES.includes(locale)) {
      locale = 'en';
    }
    localStorage.setItem(STORAGE_KEY, locale);
    this.localeSubject.next(locale);
    void this.apply(locale);
  }

  get current(): SupportedLocale {
    return this.localeSubject.value;
  }

  private apply(locale: SupportedLocale): Promise<unknown> {
    const meta = LOCALE_META[locale];
    document.documentElement.lang = locale;
    document.documentElement.dir = meta.dir;
    document.documentElement.classList.toggle('rtl', meta.dir === 'rtl');
    return firstValueFrom(this.translate.use(locale)).catch(() => undefined);
  }

  private storedLocale(): SupportedLocale {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored && SUPPORTED_LOCALES.includes(stored as SupportedLocale)) {
      return stored as SupportedLocale;
    }
    const nav = navigator.language?.split('-')[0]?.toLowerCase();
    return SUPPORTED_LOCALES.includes(nav as SupportedLocale) ? (nav as SupportedLocale) : 'en';
  }
}
