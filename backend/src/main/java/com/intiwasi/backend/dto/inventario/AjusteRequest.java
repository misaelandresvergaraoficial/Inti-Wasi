package com.intiwasi.backend.dto.inventario;

import com.intiwasi.backend.entity.enums.TipoAjuste;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AjusteRequest {
    @NotNull(message = "El tipo de ajuste es obligatorio")
    private TipoAjuste tipoAjuste;

    @NotBlank(message = "El motivo del ajuste es obligatorio")
    @Size(max = 255, message = "El motivo no debe exceder 255 caracteres")
    private String motivo;

    @Size(max = 255, message = "Las observaciones no deben exceder 255 caracteres")
    private String observaciones;

    @Valid
    @NotEmpty(message = "El ajuste debe contener al menos un movimiento")
    private List<MovimientoRequest> movimientos;
}
