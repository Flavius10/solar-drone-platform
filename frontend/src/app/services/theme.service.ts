import { Injectable } from '@angular/core';

export type AppTheme = 'light' | 'dark';

const STORAGE_KEY = 'axela-theme';


@Injectable({ providedIn: 'root' })
export class ThemeService {
  private currentTheme: AppTheme;

  constructor() {
    const stored = localStorage.getItem(STORAGE_KEY);
    this.currentTheme = (stored === 'dark' || stored === 'light') ? stored : 'light';
    this.apply();
  }

  get theme(): AppTheme {
    return this.currentTheme;
  }

  setTheme(theme: AppTheme): void {
    this.currentTheme = theme;
    localStorage.setItem(STORAGE_KEY, theme);
    this.apply();
  }

  toggle(): void {
    this.setTheme(this.currentTheme === 'dark' ? 'light' : 'dark');
  }

  private apply(): void {
    const html = document.documentElement;
    if (this.currentTheme === 'dark') {
      html.setAttribute('cds-theme', 'dark');
      html.classList.add('app-dark');
    } else {
      html.removeAttribute('cds-theme');
      html.classList.remove('app-dark');
    }
  }
}
