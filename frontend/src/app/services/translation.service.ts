import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export type AppLanguage = 'en' | 'ro';

const STORAGE_KEY = 'axela-lang';


@Injectable({ providedIn: 'root' })
export class TranslationService {
  private translations: Record<string, string> = {};
  private currentLang: AppLanguage;
  private listeners = new Set<() => void>();

  constructor(private http: HttpClient) {
    const stored = localStorage.getItem(STORAGE_KEY);
    this.currentLang = (stored === 'en' || stored === 'ro') ? stored : 'ro';
    this.load(this.currentLang);
  }

  get lang(): AppLanguage {
    return this.currentLang;
  }

  setLanguage(lang: AppLanguage): void {
    if (lang === this.currentLang && Object.keys(this.translations).length > 0) return;
    this.currentLang = lang;
    localStorage.setItem(STORAGE_KEY, lang);
    this.load(lang);
  }

  
  
  onChange(callback: () => void): () => void {
    this.listeners.add(callback);
    return () => this.listeners.delete(callback);
  }

  t(key: string, fallback?: string): string {
    return this.translations[key] ?? fallback ?? key;
  }

  private load(lang: AppLanguage): void {
    this.http.get(`assets/i18n/${lang}.properties`, { responseType: 'text' }).subscribe({
      next: (text) => {
        this.translations = this.parseProperties(text);
        this.listeners.forEach(cb => cb());
      },
      error: () => {
        
      }
    });
  }

  private parseProperties(text: string): Record<string, string> {
    const map: Record<string, string> = {};
    for (const rawLine of text.split('\n')) {
      const line = rawLine.trim();
      if (!line || line.startsWith('#') || line.startsWith('!')) continue;
      const idx = line.indexOf('=');
      if (idx === -1) continue;
      map[line.substring(0, idx).trim()] = line.substring(idx + 1).trim();
    }
    return map;
  }
}
