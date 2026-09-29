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
        path: 'acceso-denegado',
        title: 'Acceso restringido · Inti Wasi',
        loadComponent: () => import('./features/auth/access-denied').then((m) => m.AccessDenied),
      },
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      { path: '**', redirectTo: 'inicio' },
    ],
  },
];
