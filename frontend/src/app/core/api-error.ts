import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorResponse, ApiRequestError } from './models';

export function toApiError(error: unknown): ApiRequestError {
  if (!(error instanceof HttpErrorResponse)) {
    return new ApiRequestError(
      error instanceof Error
        ? error.message
        : 'No se pudo completar la operación. Inténtalo de nuevo.',
      0,
    );
  }
  if (error.status === 0) {
    return new ApiRequestError('No se pudo conectar. Revisa tu conexión e inténtalo de nuevo.', 0);
  }
  const body = error.error as Partial<ApiErrorResponse> | null;
  const message = typeof body?.message === 'string' ? body.message : '';
  if (error.status === 401) {
    return new ApiRequestError(
      message.includes('cuenta no está disponible')
        ? 'Su cuenta no está disponible. Contacte al Administrador.'
        : 'Correo o contraseña incorrectos. Revisa tus datos e inténtalo de nuevo.',
      401,
    );
  }
  if (error.status === 403) {
    return new ApiRequestError('No tienes permiso para realizar esta acción.', 403);
  }
  if (error.status >= 500) {
    return new ApiRequestError(
      'El servidor no pudo completar la operación. Inténtalo más tarde.',
      error.status,
    );
  }
  return new ApiRequestError(
    message || 'No se pudo completar la operación.',
    error.status,
    fieldFromMessage(message),
  );
}

function fieldFromMessage(message: string): string | undefined {
  const lower = message.toLocaleLowerCase();
  if (lower.includes('nomproveedor') || lower.includes('nombre del proveedor')) {
    return 'nomProveedor';
  }
  if (lower.includes('ruc')) return 'ruc';
  if (lower.includes('contacto')) return 'contacto';
  if (lower.includes('dirección') || lower.includes('direccion')) return 'direccion';
  if (lower.includes('correo')) return 'correo';
  if (lower.includes('nombre')) return 'nomUsuario';
  if (lower.includes('teléfono') || lower.includes('telefono')) return 'telefono';
  if (lower.includes('contraseña') || lower.includes('contrasena')) return 'contrasena';
  if (lower.includes('rol')) return 'rol';
  return undefined;
}
