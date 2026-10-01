export type EstadoOrden = 'Pendiente' | 'Parcial' | 'Recibida' | 'Cancelada';

export interface DetalleOrdenResponse {
  idDetalle: number;
  idProducto: number;
  sku: string;
  nomProducto: string;
  cantidad: number;
  cantidadRecibida: number;
  cantidadPorRecibir: number;
  precioUnitario: number;
  subtotal: number;
}

export interface OrdenResponse {
  idOrden: number;
  idProveedor: number;
  nomProveedor: string;
  idUsuario: number;
  nomUsuario: string;
  fechaEmision: string;
  fechaEstimadaEntrega: string | null;
  estado: EstadoOrden;
  detalles: DetalleOrdenResponse[];
  totalOrden: number;
}

export interface OrdenRequest {
  idProveedor: number;
  fechaEstimadaEntrega: string | null;
  detalles: { idProducto: number; cantidad: number; precioUnitario: number }[];
}

export interface ProveedorOpcion {
  idProveedor: number;
  nomProveedor: string;
  estado: number;
}

export interface ProductoOpcion {
  idProducto: number;
  sku: string;
  nomProducto: string;
  idProveedor: number | null;
  precio: number;
  estado: number;
}

export function puedeEditarOrden(order: OrdenResponse): boolean {
  // El servidor comprueba además si hubo entradas posteriormente anuladas.
  return (
    order.estado === 'Pendiente' && order.detalles.every((item) => item.cantidadRecibida === 0)
  );
}

export function puedeCancelarOrden(order: OrdenResponse): boolean {
  return order.estado === 'Pendiente' || order.estado === 'Parcial';
}

export function fechaVisible(value: string | null): string {
  return value
    ? new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium' }).format(
        new Date(`${value}T12:00:00`),
      )
    : 'Sin fecha estimada';
}

export function importeVisible(value: number): string {
  return new Intl.NumberFormat('es-PE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}
