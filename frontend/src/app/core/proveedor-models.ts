export interface ProveedorResponse {
  idProveedor: number;
  nomProveedor: string;
  ruc: string;
  contacto: string | null;
  telefono: string;
  direccion: string | null;
  estado: number;
}

export interface ProveedorRequest {
  nomProveedor: string;
  ruc: string;
  contacto: string | null;
  telefono: string;
  direccion: string | null;
}
