import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api/') || request.url.startsWith('/api/auth/')) return next(request);
  const auth = inject(AuthService);
  const current = auth.session();
  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && current) void auth.revalidate(current);
      return throwError(() => error);
    }),
  );
};
