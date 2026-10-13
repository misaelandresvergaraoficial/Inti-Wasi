import { HttpClient } from '@angular/common/http';
import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthResponse, Sesion, UsuarioResponse } from './models';
import { NoticeService } from './notice.service';
import { toApiError } from './api-error';

const STORAGE_KEY = 'intiwasi.session';
const ACTIVITY_KEY = 'intiwasi.lastActivity';
const IDLE_MS = 15 * 60 * 1000;
const CONFIRM_MS = 30 * 1000;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly notice = inject(NoticeService);
  readonly session = signal<Sesion | null>(this.restore());
  readonly isAdmin = computed(() => this.session()?.rol === 'Administrador');
  readonly idleSecondsRemaining = signal<number | null>(null);

  landingRoute(): string {
    return this.isAdmin() ? '/dashboard' : '/ordenes-compra';
  }
  private verified = false;
  private checking?: Promise<boolean>;
  private timer?: ReturnType<typeof setTimeout>;

  constructor() {
    if (this.session() && !sessionStorage.getItem(ACTIVITY_KEY)) this.recordActivity();
    this.scheduleExpiration();
    this.checkInactivity();
    const idleTimer = setInterval(() => this.checkInactivity(), 1000);
    const activity = () => {
      this.checkInactivity();
      if (this.session() && this.idleSecondsRemaining() === null
          && Date.now() - this.lastActivity() >= 1000) this.recordActivity();
    };
    const resume = () => this.checkInactivity();
    const events = ['pointerdown', 'pointermove', 'keydown', 'wheel', 'touchstart'];
    for (const event of events) document.addEventListener(event, activity, { passive: true, capture: true });
    document.addEventListener('visibilitychange', resume);
    window.addEventListener('pageshow', resume);
    window.addEventListener('focus', resume);
    inject(DestroyRef).onDestroy(() => {
      clearTimeout(this.timer);
      clearInterval(idleTimer);
      for (const event of events) document.removeEventListener(event, activity, true);
      document.removeEventListener('visibilitychange', resume);
      window.removeEventListener('pageshow', resume);
      window.removeEventListener('focus', resume);
    });
  }

  async login(email: string, password: string): Promise<void> {
    try {
      const response = await firstValueFrom(
        this.http.post<AuthResponse>('/api/auth/login', { correo: email, contrasena: password }),
      );
      const expiresAt = tokenExpiration(response.token);
      const user = await firstValueFrom(
        this.http.get<UsuarioResponse>('/api/auth/me', {
          headers: { Authorization: `Bearer ${response.token}` },
        }),
      );
      this.setUser(user, response.token, expiresAt);
      this.verified = true;
    } catch (error) {
      throw toApiError(error);
    }
  }

  async ensure(): Promise<boolean> {
    if (!this.valid()) return false;
    if (this.verified) return true;
    if (!this.checking) this.checking = this.checkCurrent();
    try {
      return await this.checking;
    } finally {
      this.checking = undefined;
    }
  }

  valid(): boolean {
    this.checkInactivity();
    const current = this.session();
    if (!current) return false;
    if (current.expiresAt <= Date.now()) {
      this.clear();
      return false;
    }
    return true;
  }

  token(): string | null {
    return this.valid() ? this.session()!.token : null;
  }

  refreshUser(user: UsuarioResponse): void {
    const current = this.session();
    if (current?.idUsuario === user.idUsuario) this.setUser(user, current.token, current.expiresAt);
  }

  logout(expired = false, accountChanged = false, inactive = false): void {
    this.clear();
    void this.router.navigate(['/login'], {
      queryParams: inactive ? { sesion: 'inactiva' } : expired
        ? { sesion: 'caducada' }
        : accountChanged
          ? { cuenta: 'actualizada' }
          : {},
    });
  }

  private async checkCurrent(): Promise<boolean> {
    try {
      const current = this.session()!;
      const user = await firstValueFrom(this.http.get<UsuarioResponse>('/api/auth/me'));
      if (this.session()?.token !== current.token || !this.valid()) return false;
      this.setUser(user, current.token, current.expiresAt);
      this.verified = true;
      return true;
    } catch {
      this.clear();
      return false;
    }
  }

  private setUser(user: UsuarioResponse, token: string, expiresAt: number): void {
    if (this.session()?.token !== token) this.recordActivity();
    const current: Sesion = {
      token,
      idUsuario: user.idUsuario,
      correo: user.correo,
      nombre: user.nomUsuario,
      rol: user.rol,
      expiresAt,
    };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(current));
    this.session.set(current);
    this.scheduleExpiration();
  }

  private clear(): void {
    clearTimeout(this.timer);
    this.notice.clear();
    sessionStorage.removeItem(STORAGE_KEY);
    sessionStorage.removeItem(ACTIVITY_KEY);
    this.idleSecondsRemaining.set(null);
    this.session.set(null);
    this.verified = false;
  }

  private restore(): Sesion | null {
    try {
      const current = JSON.parse(sessionStorage.getItem(STORAGE_KEY) ?? 'null') as Sesion | null;
      if (
        !current ||
        !current.token ||
        !Number.isFinite(current.expiresAt) ||
        current.expiresAt <= Date.now() ||
        !Number.isInteger(current.idUsuario) ||
        !['Administrador', 'Operador de Almacén'].includes(current.rol)
      ) {
        sessionStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return current;
    } catch {
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }

  private scheduleExpiration(): void {
    clearTimeout(this.timer);
    const current = this.session();
    if (current) {
      this.timer = setTimeout(() => this.logout(true), Math.max(0, current.expiresAt - Date.now()));
    }
  }

  continueSession(): void {
    this.checkInactivity();
    if (!this.valid()) return;
    this.recordActivity();
    this.idleSecondsRemaining.set(null);
  }

  private lastActivity(): number {
    const value = Number(sessionStorage.getItem(ACTIVITY_KEY));
    return Number.isFinite(value) && value > 0 ? value : 0;
  }

  private recordActivity(): void {
    sessionStorage.setItem(ACTIVITY_KEY, String(Date.now()));
  }

  private checkInactivity(): void {
    const current = this.session();
    if (!current) return;
    if (current.expiresAt <= Date.now()) {
      this.logout(true);
      return;
    }
    const remaining = this.lastActivity() + IDLE_MS + CONFIRM_MS - Date.now();
    if (remaining <= 0) {
      this.logout(false, false, true);
    } else {
      this.idleSecondsRemaining.set(remaining <= CONFIRM_MS ? Math.ceil(remaining / 1000) : null);
    }
  }
}

function tokenExpiration(token: string): number {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    const expiresAt = Number(payload.exp) * 1000;
    if (Number.isFinite(expiresAt) && expiresAt > Date.now()) return expiresAt;
  } catch {
    /* El token malformado se rechaza abajo. */
  }
  throw new Error('La sesión recibida no es válida. Inténtalo de nuevo.');
}
