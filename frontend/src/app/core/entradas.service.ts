import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { toApiError } from './api-error';
import { ApiRequestError } from './models';
import { EntradaRequest, EntradaResponse } from './entrada-models';

@Injectable({ providedIn: 'root' })
export class EntradasService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly base = '/api/entradas';

  list(): Promise<EntradaResponse[]> {
    return this.request(this.http.get<EntradaResponse[]>(this.base));
  }

  get(id: number): Promise<EntradaResponse> {
    return this.request(this.http.get<EntradaResponse>(`${this.base}/${id}`));
  }

  save(data: EntradaRequest, id?: number): Promise<EntradaResponse> {
    return this.request(
      id === undefined
        ? this.http.post<EntradaResponse>(this.base, data)
        : this.http.put<EntradaResponse>(`${this.base}/${id}`, data),
    );
  }

  annul(id: number, motivo: string): Promise<void> {
    return this.request(this.http.delete<void>(`${this.base}/${id}`, { body: { motivo } }));
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
