package com.intiwasi.backend.dto.inventario;

import com.intiwasi.backend.entity.enums.TipoAjuste;
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
public class AjusteResponse {
    private Integer idAjuste;
    private Integer idDocumento;
    private TipoAjuste tipoAjuste;
    private String motivo;
    private String observaciones;
    private LocalDateTime fechaEmision;
    private Integer idUsuario;
    private String usuarioResponsable;
    private List<MovimientoResponse> movimientos;
}
