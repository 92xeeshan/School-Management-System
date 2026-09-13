import { Injectable, signal } from '@angular/core';

export type ThemeMode = 'legacy' | 'modern';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly STORAGE_KEY = 'schoolms.theme';
  private currentThemeSignal = signal<ThemeMode>(this.loadTheme());

  readonly currentTheme = this.currentThemeSignal.asReadonly();

  constructor() {
    this.applyTheme(this.currentThemeSignal());
  }

  toggleTheme(): void {
    const nextTheme = this.currentThemeSignal() === 'legacy' ? 'modern' : 'legacy';
    this.currentThemeSignal.set(nextTheme);
    this.saveTheme(nextTheme);
    this.applyTheme(nextTheme);
  }

  private loadTheme(): ThemeMode {
    const saved = localStorage.getItem(this.STORAGE_KEY) as ThemeMode;
    return saved === 'modern' ? 'modern' : 'legacy';
  }

  private saveTheme(theme: ThemeMode): void {
    localStorage.setItem(this.STORAGE_KEY, theme);
  }

  private applyTheme(theme: ThemeMode): void {
    if (theme === 'modern') {
      document.body.classList.add('theme-modern');
    } else {
      document.body.classList.remove('theme-modern');
    }
  }
}
