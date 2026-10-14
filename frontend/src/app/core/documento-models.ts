export type TipoDocumento = 'Entrada' | 'Salida' | 'Ajuste' | 'Correccion';

export interface DocumentoResponse {
  idDocumento: number;
  tipoDocumento: TipoDocumento;
  fechaEmision: string;
  estado: number;
  idDocumentoOrigen: number | null;
  motivoCorreccion: string | null;
  idUsuario: number;
  usuarioResponsable: string;
}

export function nombreEstadoDocumento(estado: number): string {
  return estado === 1 ? 'Vigente' : estado === 2 ? 'Rectificado' : 'Anulado';
}
