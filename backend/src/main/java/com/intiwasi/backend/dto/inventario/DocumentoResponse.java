package com.intiwasi.backend.dto.inventario;

import com.intiwasi.backend.entity.enums.TipoDocumento;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentoResponse {
    private Integer idDocumento;
    private TipoDocumento tipoDocumento;
    private LocalDateTime fechaEmision;
    private Byte estado;
    private Integer idDocumentoOrigen;
    private String motivoCorreccion;
    private Integer idUsuario;
    private String usuarioResponsable;
}
