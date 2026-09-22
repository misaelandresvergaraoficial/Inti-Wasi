package com.intiwasi.backend.dto.OrdenCompra;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraResponse {
    private Integer idOrden;
    private Integer idProveedor;
    private String nomProveedor;
    private Integer idUsuario;
    private String nomUsuario;
    private LocalDate fechaEmision;
    private LocalDate fechaEstimadaEntrega;
    private String estado;
    private List<DetalleOrdenCompraResponse> detalles;
    private BigDecimal totalOrden;
}
