package com.intiwasi.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {

    private Integer idUsuario;
    private String nomUsuario;
    private String correo;
    private String rol;
    private String telefono;
    private Integer estado;
    private LocalDateTime fechaRegistro;
}