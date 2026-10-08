import { Routes } from '@angular/router';
import { adminGuard, authGuard, changesGuard, guestGuard } from './core/guards';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Iniciar sesión · Inti Wasi',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login').then((m) => m.Login),
  },
  {
    path: '',
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children: [
      {
        path: 'inicio',
        title: 'Mi espacio · Inti Wasi',
        loadComponent: () => import('./features/home/home').then((m) => m.Home),
      },
      {
        path: 'usuarios',
        title: 'Usuarios · Inti Wasi',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/users/users-list').then((m) => m.UsersList),
      },
      {
        path: 'usuarios/nuevo',
        title: 'Crear usuario · Inti Wasi',
        canActivate: [adminGuard],
        canDeactivate: [changesGuard],
        loadComponent: () => import('./features/users/user-form').then((m) => m.UserForm),
      },
      {
        path: 'usuarios/:id/editar',
        title: 'Editar usuario · Inti Wasi',
        canActivate: [adminGuard],
        canDeactivate: [changesGuard],
        loadComponent: () => import('./features/users/user-form').then((m) => m.UserForm),
      },
      {
        path: 'ordenes-compra',
        title: 'Órdenes de compra · Inti Wasi',
        loadComponent: () => import('./features/orders/orders-list').then((m) => m.OrdersList),
      },
      {
        path: 'ordenes-compra/nueva',
        title: 'Crear orden de compra · Inti Wasi',
        canActivate: [adminGuard],
        canDeactivate: [changesGuard],
        loadComponent: () => import('./features/orders/order-form').then((m) => m.OrderForm),
      },
      {
        path: 'ordenes-compra/:id/editar',
        title: 'Editar orden de compra · Inti Wasi',
        canActivate: [adminGuard],
        canDeactivate: [changesGuard],
        loadComponent: () => import('./features/orders/order-form').then((m) => m.OrderForm),
      },
      {
        path: 'ordenes-compra/:id',
        title: 'Detalle de orden de compra · Inti Wasi',
        loadComponent: () => import('./features/orders/order-detail').then((m) => m.OrderDetail),
      },
      {
        path: 'acceso-denegado',
        title: 'Acceso restringido · Inti Wasi',
        loadComponent: () => import('./features/auth/access-denied').then((m) => m.AccessDenied),
      },
      {
        path: 'categorias',
        title: 'Categorías · Inti Wasi',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/categorias/categorias').then((m) => m.CategoriasList),
      },
      {
        path: 'categorias/nuevo',
        title: 'Crear categoría · Inti Wasi',
        canActivate: [adminGuard],
        canDeactivate: [changesGuard],
        loadComponent: () =>
          import('./features/categorias/categoria-form').then((m) => m.CategoriaForm),
      },
      {
        path: 'categorias/:id/editar',
        title: 'Editar categoría · Inti Wasi',
        canActivate: [adminGuard],
        canDeactivate: [changesGuard],
        loadComponent: () =>
          import('./features/categorias/categoria-form').then((m) => m.CategoriaForm),
      },
      {
        path: 'productos',
        title: 'Productos · Inti Wasi',
        loadComponent: () => import('./features/productos/productos-list').then((m) => m.ProductosList),
      },
      {
        path: 'productos/nuevo',
        title: 'Crear producto · Inti Wasi',
        canActivate: [adminGuard],
        canDeactivate: [changesGuard],
        loadComponent: () => import('./features/productos/producto-form').then((m) => m.ProductoForm),
      },
      {
        path: 'productos/stock-bajo',
        title: 'Stock Bajo · Inti Wasi',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/productos/stock-bajo-list').then((m) => m.StockBajoList),
      },
      {
        path: 'productos/:id/editar',
        title: 'Editar producto · Inti Wasi',
        canActivate: [adminGuard],
        canDeactivate: [changesGuard],
        loadComponent: () => import('./features/productos/producto-form').then((m) => m.ProductoForm),
      },
      
      // Las rutas de redirección deben ir obligatoriamente al final
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      { path: '**', redirectTo: 'inicio' },
    ],
  },
];