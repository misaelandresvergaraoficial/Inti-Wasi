import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { toApiError } from './api-error';
import { ApiRequestError } from './models';
import { CategoriaRequest, CategoriaResponse } from './categoria-models';

@Injectable({ providedIn: 'root' })
export class CategoriasService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly base = '/api/categorias';

  list(includeInactive = false): Promise<CategoriaResponse[]> {
    return this.request(
      this.http.get<CategoriaResponse[]>(includeInactive ? `${this.base}/todos` : this.base),
    );
  }

  get(id: number): Promise<CategoriaResponse> {
    return this.request(this.http.get<CategoriaResponse>(`${this.base}/${id}`));
  }

  save(data: CategoriaRequest, id?: number): Promise<CategoriaResponse> {
    return id
      ? this.request(this.http.put<CategoriaResponse>(`${this.base}/${id}`, data))
      : this.request(this.http.post<CategoriaResponse>(this.base, data));
  }

  delete(id: number): Promise<void> {
    return this.request(this.http.delete<void>(`${this.base}/${id}`));
  }

  async activate(id: number): Promise<void> {
    await this.request(
      this.http.put<CategoriaResponse>(`${this.base}/${id}/estado`, { estado: 1 }),
    );
  }

  private async request<T>(operation: Observable<T>): Promise<T> {
    try {
      return await firstValueFrom(operation);
    } catch (error) {
      const mapped = toApiError(error);
      if (mapped.status === 401) {
        this.auth.logout(true);
        throw new ApiRequestError('Sesión expirada. Por favor, ingresa de nuevo.', 401);
      }
      throw mapped;
    }
  }
}
