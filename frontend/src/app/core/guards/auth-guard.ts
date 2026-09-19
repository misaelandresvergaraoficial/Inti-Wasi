import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Si hay un token en el localStorage, lo dejamos pasar
  if (authService.getToken()) {
    return true;
  }

  // Si no está logueado, lo pateamos a la pantalla de login
  router.navigate(['/login']);
  return false;
};