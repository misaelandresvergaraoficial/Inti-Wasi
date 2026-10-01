import { computed, Injectable, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'intiwasi.theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<Theme>(document.documentElement.dataset['theme'] === 'dark' ? 'dark' : 'light');
  readonly isDark = computed(() => this.theme() === 'dark');

  toggle(): void {
    const next: Theme = this.theme() === 'dark' ? 'light' : 'dark';
    this.theme.set(next);
    document.documentElement.dataset['theme'] = next;
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', next === 'dark' ? '#102126' : '#175b65');
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // Theme switching still works when browser storage is unavailable.
    }
  }
}
