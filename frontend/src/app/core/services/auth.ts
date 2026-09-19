import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthRequest, AuthResponse } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/auth';

  login(credentials: AuthRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        // Guardamos los datos de sesión
        localStorage.setItem('token', response.token);
        localStorage.setItem('correo', response.correo);
        localStorage.setItem('rol', response.rol);
      })
    );
  }

  // 👇 MÉTODOS AUXILIARES QUE FALTABAN 👇

  // Obtiene el token guardado
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  // Obtiene el rol del usuario actual
  getRol(): string | null {
    return localStorage.getItem('rol');
  }

  getCorreo(): string | null {
    return localStorage.getItem('correo');
  }

  // Cierra sesión limpiando el localStorage
  logout(): void {
    localStorage.clear();
  }

  // Verifica si hay una sesión activa
  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}