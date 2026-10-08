import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { toApiError } from './api-error';
import { ApiRequestError } from './models';
import { ProveedorRequest, ProveedorResponse } from './proveedor-models';

@Injectable({ providedIn: 'root' })
export class ProveedoresService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly base = '/api/proveedores';

  list(includeInactive = false): Promise<ProveedorResponse[]> {
    return this.request(
      this.http.get<ProveedorResponse[]>(includeInactive ? `${this.base}/todos` : this.base),
    );
  }

  get(id: number): Promise<ProveedorResponse> {
    return this.request(this.http.get<ProveedorResponse>(`${this.base}/${id}`));
  }

  save(data: ProveedorRequest, id?: number): Promise<ProveedorResponse> {
    return id
      ? this.request(this.http.put<ProveedorResponse>(`${this.base}/${id}`, data))
      : this.request(this.http.post<ProveedorResponse>(this.base, data));
  }

  deactivate(id: number): Promise<void> {
    return this.request(this.http.delete<void>(`${this.base}/${id}`));
  }

  async activate(id: number): Promise<void> {
    await this.request(
      this.http.put<ProveedorResponse>(`${this.base}/${id}/estado`, { estado: 1 }),
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
