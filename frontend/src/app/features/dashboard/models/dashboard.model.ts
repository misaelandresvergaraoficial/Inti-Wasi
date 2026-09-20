export interface ProductoPorReponer {
  idProducto: number;
  nomCategoria: string;
  nomProducto: string;
  nomProveedor: string | null;
  sku: string;
  stockActual: number;
  stockMinimo: number;
  unidadesPorReponer: number;
}

export interface DashboardResumen {
  entradasDelDia: number;
  productosConStockBajo: number;
  salidasDelDia: number;
  totalProductosActivos: number;
  productosPorReponer: ProductoPorReponer[];
}