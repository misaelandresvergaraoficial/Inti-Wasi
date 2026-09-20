import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors, withFetch } from '@angular/common/http';
import { routes } from './app.routes';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection(), // <-- Quitamos la coalescencia de eventos
    provideRouter(routes),
    provideHttpClient(
      withFetch(), // <-- FUNDAMENTAL: Fuerza el uso del motor de red moderno
      withInterceptors([jwtInterceptor])
    ) 
  ]
};