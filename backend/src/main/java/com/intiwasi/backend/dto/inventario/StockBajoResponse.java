package com.intiwasi.backend.dto.inventario;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockBajoResponse {
    private Integer idProducto;
    private String sku;
    private String nomProducto;
    private String nomCategoria;
    private String nomProveedor;
    private Integer stockActual;
    private Integer stockMinimo;
    private Integer unidadesPorReponer;
}
