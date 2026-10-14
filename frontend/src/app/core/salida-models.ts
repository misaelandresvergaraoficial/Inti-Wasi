export type MotivoSalida = 'Despacho/Venta' | 'Merma' | 'Otro';

export interface MovimientoSalida {
  idMovimiento: number;
  idProducto: number;
  sku: string;
  nomProducto: string;
  cantidad: number;
  stockActual: number;
}

export interface SalidaResponse {
  idSalida: number;
  idDocumento: number;
  estadoDocumento: number;
  idDocumentoOrigen: number | null;
  motivo: MotivoSalida;
  observaciones: string | null;
  fechaEmision: string;
  idUsuario: number;
  usuarioResponsable: string;
  movimientos: MovimientoSalida[];
}

export interface SalidaRequest {
  motivo: MotivoSalida;
  observaciones: string | null;
  motivoCorreccion?: string;
  movimientos: { idProducto: number; cantidad: number }[];
}

export function estadoDocumentoSalida(estado: number): string {
  return estado === 1 ? 'Vigente' : estado === 2 ? 'Rectificado' : 'Anulado';
}
