import { TipoDocumento } from './documento-models';

export interface MovimientoConsultaResponse {
  idMovimiento: number;
  idDocumento: number;
  idDocumentoOrigen: number | null;
  estadoDocumento: number;
  tipoDocumento: TipoDocumento;
  fechaEmision: string;
  idUsuario: number;
  usuarioResponsable: string;
  idProducto: number;
  sku: string;
  nomProducto: string;
  nomCategoria: string;
  cantidad: number;
  cantidadConSigno: number;
  motivo: string | null;
  idOrden: number | null;
  numeroGuiaRemision: string | null;
}

export interface FiltrosMovimiento {
  fechaInicial?: string;
  fechaFinal?: string;
  idProducto?: number;
  tipoDocumento?: TipoDocumento;
  idUsuario?: number;
}

export interface PaginaMovimientos {
  content: MovimientoConsultaResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
}
