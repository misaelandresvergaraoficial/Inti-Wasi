export type TipoAjuste = 'Incremento' | 'Decremento';

export interface MovimientoAjuste {
  idMovimiento: number;
  idProducto: number;
  sku: string;
  nomProducto: string;
  cantidad: number;
  stockActual: number;
}

export interface AjusteResponse {
  idAjuste: number;
  idDocumento: number;
  estadoDocumento: number;
  idDocumentoOrigen: number | null;
  tipoAjuste: TipoAjuste;
  motivo: string;
  observaciones: string | null;
  fechaEmision: string;
  idUsuario: number;
  usuarioResponsable: string;
  movimientos: MovimientoAjuste[];
}

export interface AjusteRequest {
  tipoAjuste: TipoAjuste;
  motivo: string;
  observaciones: string | null;
  motivoCorreccion?: string;
  movimientos: { idProducto: number; cantidad: number }[];
}

export function estadoDocumentoAjuste(estado: number): string {
  return estado === 1 ? 'Vigente' : estado === 2 ? 'Rectificado' : 'Anulado';
}
