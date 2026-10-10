import { inject } from '@angular/core';
import { CanActivateFn, CanDeactivateFn, RedirectFunction, Router } from '@angular/router';
import { AuthService } from './auth.service';
export const authGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return (await auth.ensure()) || router.createUrlTree(['/login']);
};
export const adminGuard: CanActivateFn = () =>
  inject(AuthService).isAdmin() || inject(Router).createUrlTree(['/acceso-denegado']);
export const guestGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return !(await auth.ensure()) || router.createUrlTree([auth.landingRoute()]);
};
export const landingRedirect: RedirectFunction = () => inject(AuthService).landingRoute();
export interface PendingChanges {
  canLeave(): boolean | Promise<boolean>;
}
export const changesGuard: CanDeactivateFn<PendingChanges> = (component) => component.canLeave();
