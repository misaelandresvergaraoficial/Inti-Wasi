export interface ProductoResponse {
  idProducto: number;
  sku: string;
  nomProducto: string;
  idCategoria: number;
  nomCategoria: string;
  idProveedor?: number;
  nomProveedor?: string;
  precio: number;
  stockMinimo: number;
  stockActual: number;
  estado: number;
}

export interface ProductoRequest {
  sku: string;
  nomProducto: string;
  idCategoria: number;
  idProveedor?: number | null;
  precio: number;
  stockMinimo: number;
}

