import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login';
import { LayoutComponent } from './core/components/layout/layout';
import { ResumenComponent } from './features/dashboard/resumen/resumen';
import { authGuard } from './core/guards/auth-guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: ResumenComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'login' }
];