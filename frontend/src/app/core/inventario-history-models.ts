export interface FiltrosHistorialInventario {
  q?: string;
  estado?: string;
  desde?: string;
  hasta?: string;
  tipo?: string;
  sort?: 'asc' | 'desc';
}
