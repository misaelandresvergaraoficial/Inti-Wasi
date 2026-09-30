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
    return this.request(this.http.get<UsuarioResponse[]>(`${this.base}/todos`));
  }

  get(id: number): Promise<UsuarioResponse> {
    return this.request(this.http.get<UsuarioResponse>(`${this.base}/${id}`));
  }

  save(request: UsuarioRequest, id?: number): Promise<UsuarioResponse> {
    return id
      ? this.request(this.http.put<UsuarioResponse>(`${this.base}/${id}`, request))
      : this.request(this.http.post<UsuarioResponse>(this.base, request));
  }

  deactivate(id: number): Promise<void> {
    return this.request(this.http.delete<void>(`${this.base}/${id}`));
  }

  async activate(id: number): Promise<void> {
    await this.request(this.http.put<UsuarioResponse>(`${this.base}/${id}/estado`, { estado: 1 }));
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
