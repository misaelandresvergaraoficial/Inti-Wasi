export interface Producto {
  idProducto: number;
  sku: string;
  nomProducto: string;
  nomCategoria: string;
  nomProveedor: string | null;
  precio: number;
  stockActual: number;
  stockMinimo: number;
  estado: number;
  idCategoria: number;
  idProveedor: number | null;
}