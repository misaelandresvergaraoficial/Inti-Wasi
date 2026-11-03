import { Pagina } from './pagination';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { toApiError } from './api-error';
import { OrdenRequest, OrdenResponse, ProductoOpcion, ProveedorOpcion } from './order-models';

@Injectable({ providedIn: 'root' })
export class OrdersService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/ordenes-compra';

  list(
    filters: {
      admisibles?: boolean;
      page?: number;
      size?: number;
      q?: string;
      estado?: string;
      desde?: string;
      hasta?: string;
      sort?: string;
    } = {},
  ): Promise<Pagina<OrdenResponse>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== undefined && value !== '') params = params.set(key, String(value));
    }
    return this.request(this.http.get<Pagina<OrdenResponse>>(this.base, { params }));
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
      throw toApiError(error);
    }
  }
}
