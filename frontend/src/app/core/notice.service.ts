import { Injectable, signal } from '@angular/core';
@Injectable({ providedIn: 'root' })
export class NoticeService {
  readonly message = signal('');
  private keepNextNavigation = false;
  show(message: string): void {
    this.message.set(message);
    this.keepNextNavigation = false;
  }
  showNext(message: string): void {
    this.message.set(message);
    this.keepNextNavigation = true;
  }
  onNavigation(): void {
    if (this.keepNextNavigation) {
      this.keepNextNavigation = false;
    } else {
      this.clear();
    }
  }
  clear(): void {
    this.message.set('');
    this.keepNextNavigation = false;
  }
}
