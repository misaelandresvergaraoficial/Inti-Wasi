package com.intiwasi.backend.dto.Proveedor;

import lombok.Data;

@Data
public class ProveedorResponse {

    private Integer idProveedor;
    private String nomProveedor;
    private String ruc;
    private String prefijoGuia;
    private boolean prefijoGuiaBloqueado;
    private String contacto;
    private String telefono;
    private String direccion;
    private Byte estado;
}