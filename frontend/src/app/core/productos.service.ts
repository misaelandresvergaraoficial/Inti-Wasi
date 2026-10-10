import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { toApiError } from './api-error';
import { ApiRequestError } from './models';
import {
  ProductoRequest,
  ProductoResponse,
} from './producto-models';
import { CategoriaResponse } from './categoria-models';
import { ProveedorResponse } from './proveedor-models';

@Injectable({ providedIn: 'root' })
export class ProductosService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly base = '/api/productos';

  list(includeInactive = false): Promise<ProductoResponse[]> {
    return this.request(
      this.http.get<ProductoResponse[]>(includeInactive ? `${this.base}/todos` : this.base),
    );
  }

  get(id: number): Promise<ProductoResponse> {
    return this.request(this.http.get<ProductoResponse>(`${this.base}/${id}`));
  }

  getLowStock(): Promise<ProductoResponse[]> {
    return this.request(this.http.get<ProductoResponse[]>(`${this.base}/stock-bajo`));
  }

  save(data: ProductoRequest, id?: number): Promise<ProductoResponse> {
    return id
      ? this.request(this.http.put<ProductoResponse>(`${this.base}/${id}`, data))
      : this.request(this.http.post<ProductoResponse>(this.base, data));
  }

  delete(id: number): Promise<void> {
    return this.request(this.http.delete<void>(`${this.base}/${id}`));
  }

  async activate(id: number): Promise<void> {
    await this.request(this.http.put<ProductoResponse>(`${this.base}/${id}/estado`, { estado: 1 }));
  }

  getCategorias(): Promise<CategoriaResponse[]> {
    return this.request(this.http.get<CategoriaResponse[]>('/api/categorias'));
  }

  getProveedores(): Promise<ProveedorResponse[]> {
    return this.request(this.http.get<ProveedorResponse[]>('/api/proveedores'));
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
