package com.intiwasi.backend.dto.inventario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoResponse {
    private Integer idMovimiento;
    private Integer idProducto;
    private String sku;
    private String nomProducto;
    private Integer cantidad;
    private Integer stockActual;
}
