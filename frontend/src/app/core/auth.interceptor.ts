import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api/') || request.url === '/api/auth/login') {
    return next(request);
  }

  const auth = inject(AuthService);
  const token = request.headers.has('Authorization') ? null : auth.token();
  const secured = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;
  return next(secured).pipe(
    catchError((error: HttpErrorResponse) => {
      if (
        error.status === 401 &&
        !request.url.startsWith('/api/auth/') &&
        auth.session() &&
        secured.headers.get('Authorization') === `Bearer ${auth.session()!.token}`
      ) {
        auth.logout(true);
      }
      return throwError(() => error);
    }),
  );
};
