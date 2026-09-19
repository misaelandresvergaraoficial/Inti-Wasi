package com.intiwasi.backend.dto.inventario;

import com.intiwasi.backend.entity.enums.MotivoSalida;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SalidaRequest {
    @NotNull(message = "El motivo de salida es obligatorio")
    private MotivoSalida motivo;

    @Size(max = 255, message = "Las observaciones no deben exceder 255 caracteres")
    private String observaciones;

    @Valid
    @NotEmpty(message = "La salida debe contener al menos un movimiento")
    private List<MovimientoRequest> movimientos;
}
