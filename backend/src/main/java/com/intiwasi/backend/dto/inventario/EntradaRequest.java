package com.intiwasi.backend.dto.inventario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class EntradaRequest {
    @NotNull(message = "La orden de compra es obligatoria")
    private Integer idOrden;

    @NotBlank(message = "El número de guía de remisión es obligatorio")
    @Size(max = 50, message = "El número de guía de remisión no debe exceder 50 caracteres")
    private String numeroGuiaRemision;

    @Size(max = 255, message = "El motivo de corrección no debe exceder 255 caracteres")
    private String motivoCorreccion;

    @Size(max = 255, message = "Las observaciones no deben exceder 255 caracteres")
    private String observaciones;

    @Valid
    @NotEmpty(message = "La entrada debe contener al menos un movimiento")
    private List<MovimientoRequest> movimientos;
}
