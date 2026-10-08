export type Rol = 'Administrador' | 'Operador de Almacén';
export interface UsuarioResponse {
  idUsuario: number;
  nomUsuario: string;
  correo: string;
  rol: Rol;
  telefono: string | null;
  estado: number;
  fechaRegistro: string;
}
export interface UsuarioRequest {
  nomUsuario: string;
  correo: string;
  contrasena?: string;
  rol: Rol;
  telefono: string | null;
}
export interface AuthResponse {
  token: string;
  correo: string;
  rol: `ROLE_${Rol}`;
}
export interface Sesion {
  token: string;
  idUsuario: number;
  correo: string;
  nombre: string;
  rol: Rol;
  expiresAt: number;
}
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  code?: 'ACCOUNT_INACTIVE' | 'INVALID_CREDENTIALS';
}
export class ApiRequestError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly field?: string,
  ) {
    super(message);
  }
}
