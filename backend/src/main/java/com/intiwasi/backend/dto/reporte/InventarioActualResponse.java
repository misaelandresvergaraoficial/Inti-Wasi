package com.intiwasi.backend.dto.reporte;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InventarioActualResponse {
    private Integer idProducto;
    private String sku;
    private String nomProducto;
    private String categoria;
    private String proveedor;
    private BigDecimal precio;
    private Integer stockActual;
    private Integer stockMinimo;
}
