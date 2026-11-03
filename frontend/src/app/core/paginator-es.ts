import { MatPaginatorIntl } from '@angular/material/paginator';

export function paginadorEspanol(): MatPaginatorIntl {
  const labels = new MatPaginatorIntl();
  labels.itemsPerPageLabel = 'Registros por página';
  labels.firstPageLabel = 'Primera página';
  labels.previousPageLabel = 'Página anterior';
  labels.nextPageLabel = 'Página siguiente';
  labels.lastPageLabel = 'Última página';
  labels.getRangeLabel = (page, size, total) => {
    if (!total || !size) return `0 de ${total}`;
    return `${page * size + 1}–${Math.min((page + 1) * size, total)} de ${total}`;
  };
  return labels;
}
