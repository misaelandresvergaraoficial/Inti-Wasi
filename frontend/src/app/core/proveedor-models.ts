export interface ProveedorResponse {
  idProveedor: number;
  nomProveedor: string;
  ruc: string;
  prefijoGuia: string | null;
  contacto: string | null;
  telefono: string;
  direccion: string | null;
  estado: number;
  prefijoGuiaBloqueado: boolean;
}

export interface ProveedorRequest {
  nomProveedor: string;
  ruc: string;
  prefijoGuia: string;
  contacto: string | null;
  telefono: string;
  direccion: string | null;
}