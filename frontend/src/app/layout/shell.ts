import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet, NavigationEnd } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../core/auth.service';
import { ThemeService } from '../core/theme.service';
import { Icon } from '../shared/icon';
@Component({
  selector: 'iw-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, Icon],
  template: ` <a class="skip-link" href="#contenido">Saltar al contenido</a>
    <div class="app-layout">
      <aside class="sidebar">
        <a routerLink="/inicio" class="brand" aria-label="Inti Wasi, inicio"
          ><img src="marca.svg" width="38" height="38" alt="" /><span
            >Inti Wasi<small>GESTIÓN DE ALMACÉN</small></span
          ></a
        >
        <div class="nav-section">ESPACIO DE TRABAJO</div>
        <nav aria-label="Navegación principal">
          <a
            routerLink="/inicio"
            routerLinkActive="active"
            ariaCurrentWhenActive="page"
            aria-label="Inicio"
            ><iw-icon name="home" />Inicio</a
          >
          <a
            routerLink="/ordenes-compra"
            routerLinkActive="active"
            ariaCurrentWhenActive="page"
            aria-label="Órdenes de compra"
            ><iw-icon name="orders" />Órdenes de compra</a
          >
          <a
            routerLink="/productos"
            routerLinkActive="active"
            ariaCurrentWhenActive="page"
            aria-label="Productos"
            ><iw-icon name="menu" />Productos</a
          >
          <a
            routerLink="/proveedores"
            routerLinkActive="active"
            ariaCurrentWhenActive="page"
            aria-label="Proveedores"
            ><iw-icon name="truck" />Proveedores</a
          >
          @if (auth.isAdmin()) {
            <a
              routerLink="/usuarios"
              routerLinkActive="active"
              ariaCurrentWhenActive="page"
              aria-label="Usuarios"
              ><iw-icon name="users" />Usuarios</a
            >
          }
        </nav>
        <div class="sidebar-bottom">
          <iw-icon name="shield" />
          <p>
            Acceso por roles<span>{{ auth.session()?.rol }}</span>
          </p>
        </div>
      </aside>
      <div class="workspace">
        <header class="topbar">
          <span class="company-name">Distribuidora Inti Wasi S.A.C.</span>
          <div class="account">
            <button
              mat-button
              type="button"
              class="theme-toggle"
              (click)="theme.toggle()"
              [attr.aria-pressed]="theme.isDark()"
              [attr.aria-label]="theme.isDark() ? 'Activar modo claro' : 'Activar modo oscuro'"
            >
              <iw-icon [name]="theme.isDark() ? 'sun' : 'moon'" />
              <span>{{ theme.isDark() ? 'Activar modo claro' : 'Activar modo oscuro' }}</span>
            </button>
            <span class="avatar small">{{ initials() }}</span>
            <div class="account-text">
              <strong>{{ auth.session()?.nombre }}</strong
              ><span>{{ auth.session()?.rol }}</span>
            </div>
            <span class="topbar-divider"></span
            ><button mat-button (click)="auth.logout()">
              <iw-icon name="logout" /><span>Cerrar sesión</span>
            </button>
          </div>
        </header>
        <main id="contenido" class="main-content" tabindex="-1"><router-outlet /></main>
        <footer class="app-footer">
          <span>Inti Wasi · Inventario y almacén</span><span>Acceso seguro por rol</span>
        </footer>
      </div>
    </div>`,
})
export class Shell {
  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);
  constructor() {
    inject(Router)
      .events.pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        requestAnimationFrame(() => document.querySelector<HTMLElement>('main h1')?.focus());
      });
  }
  initials(): string {
    return (
      this.auth
        .session()
        ?.nombre.split(' ')
        .slice(0, 2)
        .map((word) => word[0])
        .join('') ?? ''
    );
  }
}
