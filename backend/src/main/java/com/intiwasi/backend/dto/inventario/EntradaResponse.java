package com.intiwasi.backend.dto.inventario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntradaResponse {
    private Integer idEntrada;
    private Integer idDocumento;
    private Integer idOrden;
    private String documentoRef;
    private String observaciones;
    private LocalDateTime fechaEmision;
    private Integer idUsuario;
    private String usuarioResponsable;
    private String estadoOrden;
    private List<MovimientoResponse> movimientos;
}
