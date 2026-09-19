export interface AuthRequest {
  correo: string;
  contrasena: string;
}

export interface AuthResponse {
  token: string;
  correo: string;
  rol: string;
}