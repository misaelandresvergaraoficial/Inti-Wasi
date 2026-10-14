import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { toApiError } from './api-error';
import { AjusteRequest, AjusteResponse } from './ajuste-models';
import { ApiRequestError } from './models';

@Injectable({ providedIn: 'root' })
export class AjustesService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly base = '/api/ajustes';

  list(): Promise<AjusteResponse[]> {
    return this.request(this.http.get<AjusteResponse[]>(this.base));
  }

  get(id: number): Promise<AjusteResponse> {
    return this.request(this.http.get<AjusteResponse>(`${this.base}/${id}`));
  }

  save(data: AjusteRequest, id?: number): Promise<AjusteResponse> {
    return this.request(
      id === undefined
        ? this.http.post<AjusteResponse>(this.base, data)
        : this.http.put<AjusteResponse>(`${this.base}/${id}`, data),
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
