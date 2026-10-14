export interface MovimientoEntrada {
  idMovimiento: number;
  idProducto: number;
  sku: string;
  nomProducto: string;
  cantidad: number;
  stockActual: number;
}

export interface EntradaResponse {
  idEntrada: number;
  idDocumento: number;
  idOrden: number;
  numeroGuiaRemision: string;
  estadoDocumento: number;
  idDocumentoOrigen: number | null;
  observaciones: string | null;
  fechaEmision: string;
  idUsuario: number;
  usuarioResponsable: string;
  estadoOrden: string;
  movimientos: MovimientoEntrada[];
}

export interface EntradaRequest {
  idOrden: number;
  numeroGuiaRemision: string;
  observaciones: string | null;
  motivoCorreccion?: string;
  movimientos: { idProducto: number; cantidad: number }[];
}

export function estadoDocumento(estado: number): string {
  return estado === 1 ? 'Vigente' : estado === 2 ? 'Rectificado' : 'Anulado';
}
