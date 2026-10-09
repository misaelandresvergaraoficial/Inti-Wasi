export interface StockBajo {
  idProducto: number;
  sku: string;
  nomProducto: string;
  nomCategoria: string;
  nomProveedor: string | null;
  stockActual: number;
  stockMinimo: number;
  unidadesPorReponer: number;
}

export interface DashboardResumen {
  totalProductosActivos: number;
  productosConStockBajo: number;
  entradasDelDia: number;
  salidasDelDia: number;
  productosPorReponer: StockBajo[];
}

export interface InventarioActual {
  idProducto: number;
  sku: string;
  nomProducto: string;
  categoria: string;
  proveedor: string | null;
  precio: number;
  stockActual: number;
  stockMinimo: number;
}

export interface MovimientoReporte {
  idMovimiento: number;
  fechaEmision: string;
  tipoDocumento: string;
  sku: string;
  nomProducto: string;
  cantidadConSigno: number;
  motivo: string | null;
  usuarioResponsable: string;
}

export type TipoReporte = 'inventario' | 'movimientos' | 'reposicion';
export type FilaReporte = InventarioActual | MovimientoReporte | StockBajo;

export interface Pagina<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export interface FiltrosReporte {
  idProducto?: number;
  fechaInicial?: string;
  fechaFinal?: string;
  tipoMovimiento?: string;
  idUsuario?: number;
}
