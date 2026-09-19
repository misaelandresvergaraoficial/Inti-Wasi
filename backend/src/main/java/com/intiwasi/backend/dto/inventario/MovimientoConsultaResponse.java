package com.intiwasi.backend.dto.inventario;

import com.intiwasi.backend.entity.enums.TipoDocumento;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MovimientoConsultaResponse {
    private Integer idMovimiento;
    private Integer idDocumento;
    private TipoDocumento tipoDocumento;
    private LocalDateTime fechaEmision;
    private Integer idUsuario;
    private String usuarioResponsable;
    private Integer idProducto;
    private String sku;
    private String nomProducto;
    private String nomCategoria;
    private Integer cantidad;
    private Integer cantidadConSigno;
    private String motivo;
    private Integer idOrden;
    private String documentoRef;
}
