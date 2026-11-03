import { HttpClient } from '@angular/common/http';
import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthResponse, Sesion, UsuarioResponse } from './models';
import { NoticeService } from './notice.service';
import { toApiError } from './api-error';

const STORAGE_KEY = 'intiwasi.session';
const ACTIVITY_KEY = 'intiwasi.lastActivity';
const CHANGE_KEY = 'intiwasi.authChange';
const IDLE_MS = 15 * 60 * 1000;
const CONFIRM_MS = 30 * 1000;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly notice = inject(NoticeService);
  readonly session = signal<Sesion | null>(null);
  readonly isAdmin = computed(() => this.session()?.rol === 'Administrador');
  readonly idleSecondsRemaining = signal<number | null>(null);
  private verified = false;
  private checking?: Promise<boolean>;
  private closing?: Promise<void>;
  private generation = 0;
  private sessionRevision: string | null = null;
  private timer?: ReturnType<typeof setTimeout>;

  constructor() {
    sessionStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem(STORAGE_KEY);
    sessionStorage.removeItem(ACTIVITY_KEY);
    const idleTimer = setInterval(() => this.checkInactivity(), 1000);
    const activity = () => {
      this.checkInactivity();
      if (this.session() && this.idleSecondsRemaining() === null && Date.now() - this.lastActivity() >= 1000)
        this.recordActivity(true);
    };
    const resume = () => {
      this.checkInactivity();
      if (this.session()) void this.revalidate();
    };
    const storage = (event: StorageEvent) => {
      if (event.key === CHANGE_KEY) void this.revalidate();
      if (event.key === ACTIVITY_KEY) this.checkInactivity(false);
    };
    const events = ['pointerdown', 'pointermove', 'keydown', 'wheel', 'touchstart'];
    for (const event of events) document.addEventListener(event, activity, { passive: true, capture: true });
    document.addEventListener('visibilitychange', resume);
    window.addEventListener('pageshow', resume);
    window.addEventListener('focus', resume);
    window.addEventListener('storage', storage);
    inject(DestroyRef).onDestroy(() => {
      clearTimeout(this.timer);
      clearInterval(idleTimer);
      for (const event of events) document.removeEventListener(event, activity, true);
      document.removeEventListener('visibilitychange', resume);
      window.removeEventListener('pageshow', resume);
      window.removeEventListener('focus', resume);
      window.removeEventListener('storage', storage);
    });
  }

  landingRoute(): string {
    return this.isAdmin() ? '/dashboard' : '/ordenes-compra';
  }

  async login(email: string, password: string): Promise<void> {
    try {
      if (this.closing) await this.closing;
      await this.withAuthLock(async () => {
        await firstValueFrom(this.http.get('/api/auth/csrf'));
        const response = await firstValueFrom(
          this.http.post<AuthResponse>('/api/auth/login', { correo: email, contrasena: password }),
        );
        const revision = this.broadcast();
        await firstValueFrom(this.http.get('/api/auth/csrf'));
        this.generation++;
        this.setUser(response.usuario, response.expiresAt, true);
        this.verified = true;
        this.sessionRevision = revision;
      });
    } catch (error) {
      throw toApiError(error);
    }
  }

  async ensure(): Promise<boolean> {
    if (this.closing) await this.closing;
    if (this.verified && this.session() && this.valid()) return true;
    if (!this.checking) {
      const check = this.checkCurrent();
      this.checking = check;
      void check.finally(() => { if (this.checking === check) this.checking = undefined; });
    }
    return this.checking;
  }

  valid(): boolean {
    this.checkInactivity();
    return !!this.session();
  }

  refreshUser(user: UsuarioResponse): void {
    const current = this.session();
    if (current?.idUsuario === user.idUsuario) this.setUser(user, current.expiresAt);
  }

  logout(expired = false, accountChanged = false, inactive = false): void {
    const hadSession = !!this.session();
    const revision = this.sessionRevision;
    this.clear();
    if (hadSession) {
      const closing = this.withAuthLock(async () => {
        if (localStorage.getItem(CHANGE_KEY) !== revision) return;
        await firstValueFrom(this.http.get('/api/auth/csrf'));
        await firstValueFrom(this.http.post<void>('/api/auth/logout', null, { timeout: 5000 }));
        this.broadcast();
      }).catch((error) => {
        if (error.status !== 401) this.notice.showNext(
          'Se cerró esta pestaña, pero el servidor no pudo confirmar el cierre. La sesión conserva sus límites de vencimiento.',
        );
      });
      this.closing = closing;
      void closing.finally(() => { if (this.closing === closing) this.closing = undefined; });
    }
    void this.router.navigate(['/login'], {
      queryParams: inactive ? { sesion: 'inactiva' } : expired ? { sesion: 'caducada' } : accountChanged ? { cuenta: 'actualizada' } : {},
    });
  }

  async revalidate(expected = this.session()): Promise<void> {
    if (expected !== this.session()) return;
    this.verified = false;
    const valid = await this.ensure();
    if (!valid && expected && !this.session()) {
      void this.router.navigate(['/login'], { queryParams: { sesion: 'caducada' } });
    }
  }

  private async checkCurrent(): Promise<boolean> {
    const generation = this.generation;
    const revision = localStorage.getItem(CHANGE_KEY);
    try {
      const response = await firstValueFrom(this.http.get<AuthResponse>('/api/auth/me'));
      if (generation !== this.generation || revision !== localStorage.getItem(CHANGE_KEY)) return false;
      this.sessionRevision = revision;
      this.setUser(response.usuario, response.expiresAt, !this.lastActivity());
      this.verified = true;
      return true;
    } catch {
      if (generation === this.generation) this.clear();
      return false;
    }
  }

  private setUser(user: UsuarioResponse, expiresAt: number, newSession = false): void {
    if (!user || !Number.isFinite(expiresAt) || expiresAt <= Date.now() ||
        !Number.isInteger(user.idUsuario) || !['Administrador', 'Operador de Almacén'].includes(user.rol))
      throw new Error('La sesión recibida no es válida. Inténtalo de nuevo.');
    if (newSession) this.recordActivity();
    this.session.set({ idUsuario: user.idUsuario, correo: user.correo, nombre: user.nomUsuario, rol: user.rol, expiresAt });
    this.scheduleExpiration();
    this.checkInactivity(false);
  }

  private clear(): void {
    this.generation++;
    clearTimeout(this.timer);
    this.notice.clear();
    sessionStorage.removeItem(STORAGE_KEY);
    this.idleSecondsRemaining.set(null);
    this.session.set(null);
    this.sessionRevision = null;
    this.verified = false;
  }

  private broadcast(): string {
    const revision = `${Date.now()}-${Math.random()}`;
    localStorage.setItem(CHANGE_KEY, revision);
    return revision;
  }

  private withAuthLock<T>(operation: () => Promise<T>): Promise<T> {
    if (!navigator.locks) return operation();
    return navigator.locks.request('intiwasi.auth', operation);
  }

  private scheduleExpiration(): void {
    clearTimeout(this.timer);
    const current = this.session();
    if (current) this.timer = setTimeout(() => this.logout(true), Math.max(0, current.expiresAt - Date.now()));
  }

  continueSession(): void {
    this.checkInactivity();
    if (!this.valid()) return;
    this.recordActivity(true);
    this.idleSecondsRemaining.set(null);
  }

  private lastActivity(): number {
    const value = Number(localStorage.getItem(ACTIVITY_KEY));
    return Number.isFinite(value) && value > 0 ? value : 0;
  }

  private recordActivity(notifyServer = false): void {
    localStorage.setItem(ACTIVITY_KEY, String(Date.now()));
    const current = this.session();
    if (notifyServer && current) {
      this.http.post<void>('/api/auth/actividad', null, { timeout: 5000 }).subscribe({
        error: (error) => { if (error.status === 401) void this.revalidate(current); },
      });
    }
  }

  private checkInactivity(allowLogout = true): void {
    const current = this.session();
    if (!current) return;
    if (current.expiresAt <= Date.now()) {
      if (allowLogout) this.logout(true);
      return;
    }
    const remaining = this.lastActivity() + IDLE_MS + CONFIRM_MS - Date.now();
    if (remaining <= 0) { if (allowLogout) this.logout(false, false, true); }
    else this.idleSecondsRemaining.set(remaining <= CONFIRM_MS ? Math.ceil(remaining / 1000) : null);
  }
}
