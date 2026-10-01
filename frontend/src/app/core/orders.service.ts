import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { toApiError } from './api-error';
import { ApiRequestError } from './models';
import { OrdenRequest, OrdenResponse, ProductoOpcion, ProveedorOpcion } from './order-models';

@Injectable({ providedIn: 'root' })
export class OrdersService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly base = '/api/ordenes-compra';

  list(): Promise<OrdenResponse[]> {
    return this.request(this.http.get<OrdenResponse[]>(this.base));
  }

  get(id: number): Promise<OrdenResponse> {
    return this.request(this.http.get<OrdenResponse>(`${this.base}/${id}`));
  }

  save(data: OrdenRequest, id?: number): Promise<OrdenResponse> {
    return id
      ? this.request(this.http.put<OrdenResponse>(`${this.base}/${id}`, data))
      : this.request(this.http.post<OrdenResponse>(this.base, data));
  }

  cancel(id: number): Promise<void> {
    return this.request(this.http.delete<void>(`${this.base}/${id}`));
  }

  suppliers(): Promise<ProveedorOpcion[]> {
    return this.request(this.http.get<ProveedorOpcion[]>('/api/proveedores'));
  }

  products(): Promise<ProductoOpcion[]> {
    return this.request(this.http.get<ProductoOpcion[]>('/api/productos'));
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
