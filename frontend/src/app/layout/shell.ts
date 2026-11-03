import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { A11yModule } from '@angular/cdk/a11y';
import { filter } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../core/auth.service';
import { ThemeService } from '../core/theme.service';
import { Icon } from '../shared/icon';
import { NoticeService } from '../core/notice.service';

@Component({
  selector: 'iw-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, A11yModule, Icon],
  templateUrl: './shell.html',
})
export class Shell {
  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);
  private readonly router = inject(Router);
  private readonly notice = inject(NoticeService);
  private readonly content = viewChild<ElementRef<HTMLElement>>('content');
  private readonly sidebar = viewChild<ElementRef<HTMLElement>>('sidebar');
  private readonly menuButton = viewChild('menuButton', { read: ElementRef });
  readonly compact = signal(false);
  readonly menuOpen = signal(false);
  readonly pageTitle = signal('');
  readonly navigation = [
    {
      label: 'Operaciones',
      items: [
        { path: '/ordenes-compra', label: 'Órdenes de compra', icon: 'orders', admin: false },
        { path: '/entradas', label: 'Entradas', icon: 'entrada', admin: false },
        { path: '/salidas', label: 'Salidas', icon: 'salida', admin: false },
        { path: '/ajustes', label: 'Ajustes', icon: 'adjust', admin: true },
      ],
    },
    {
      label: 'Catálogos',
      items: [
        { path: '/productos', label: 'Productos', icon: 'box', admin: false },
        { path: '/proveedores', label: 'Proveedores', icon: 'truck', admin: false },
        { path: '/categorias', label: 'Categorías', icon: 'appstore-o', admin: true },
      ],
    },
    {
      label: 'Consultas',
      items: [
        { path: '/documentos', label: 'Documentos', icon: 'document', admin: false },
        { path: '/movimientos', label: 'Movimientos', icon: 'movement', admin: false },
        { path: '/reportes', label: 'Reportes', icon: 'chart', admin: true },
      ],
    },
  ];

  constructor() {
    this.updateTitle();
    inject(BreakpointObserver)
      .observe('(max-width: 699px)')
      .pipe(takeUntilDestroyed())
      .subscribe((state) => {
        const wasCompact = this.compact();
        const wasOpen = this.menuOpen();
        this.compact.set(state.matches);
        if (!state.matches) {
          this.menuOpen.set(false);
          if (
            wasCompact &&
            (wasOpen ||
              document.activeElement === document.body ||
              document.activeElement?.matches('.menu-close, .menu-toggle'))
          ) {
            requestAnimationFrame(() => {
              const sidebar = this.sidebar()?.nativeElement;
              const link =
                sidebar?.querySelector<HTMLElement>('a[aria-current="page"]') ??
                sidebar?.querySelector<HTMLElement>('a');
              link?.focus();
            });
          }
        }
      });
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        this.notice.onNavigation();
        this.menuOpen.set(false);
        this.updateTitle();
        requestAnimationFrame(() => {
          const heading = this.content()?.nativeElement.querySelector<HTMLElement>('h1');
          if (heading) heading.tabIndex = -1;
          (heading ?? this.content()?.nativeElement)?.focus();
        });
      });
  }

  skipToContent(event: Event): void {
    event.preventDefault();
    this.content()?.nativeElement.focus();
  }

  openMenu(): void {
    this.menuOpen.set(true);
    requestAnimationFrame(() => {
      if (this.compact() && this.menuOpen())
        this.sidebar()?.nativeElement.querySelector<HTMLElement>('.menu-close')?.focus();
    });
  }

  closeMenu(): void {
    if (!this.menuOpen()) return;
    this.menuOpen.set(false);
    this.menuButton()?.nativeElement.focus();
  }

  initials(): string {
    return (
      this.auth
        .session()
        ?.nombre.split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((word) => word[0])
        .join('') ?? ''
    );
  }

  private updateTitle(): void {
    let route = this.router.routerState.snapshot.root;
    while (route.firstChild) route = route.firstChild;
    this.pageTitle.set(route.title?.split(' · ')[0] ?? 'Almacén');
  }
}
