import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { toApiError } from './api-error';
import { ApiRequestError, UsuarioRequest, UsuarioResponse } from './models';

@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly base = '/api/usuarios';

  list(): Promise<UsuarioResponse[]> {
    return this.request(
      this.http.get<UsuarioResponse[]>(`${this.base}/todos`, {
        headers: this.auth.authorization(),
      }),
    );
  }

  get(id: number): Promise<UsuarioResponse> {
    return this.request(
      this.http.get<UsuarioResponse>(`${this.base}/${id}`, {
        headers: this.auth.authorization(),
      }),
    );
  }

  save(request: UsuarioRequest, id?: number): Promise<UsuarioResponse> {
    const options = { headers: this.auth.authorization() };
    return id
      ? this.request(this.http.put<UsuarioResponse>(`${this.base}/${id}`, request, options))
      : this.request(this.http.post<UsuarioResponse>(this.base, request, options));
  }

  deactivate(id: number): Promise<void> {
    return this.request(
      this.http.delete<void>(`${this.base}/${id}`, {
        headers: this.auth.authorization(),
      }),
    );
  }

  async activate(id: number): Promise<void> {
    await this.request(
      this.http.put<UsuarioResponse>(
        `${this.base}/${id}/estado`,
        { estado: 1 },
        {
          headers: this.auth.authorization(),
        },
      ),
    );
  }

  private async request<T>(operation: Observable<T>): Promise<T> {
    try {
      return await firstValueFrom(operation);
    } catch (error) {
      const mapped = toApiError(error);
      if (mapped.status === 401) {
        this.auth.logout(true);
        throw new ApiRequestError('Tu sesión ha caducado. Inicia sesión nuevamente.', 401);
      }
      throw mapped;
    }
  }
}
