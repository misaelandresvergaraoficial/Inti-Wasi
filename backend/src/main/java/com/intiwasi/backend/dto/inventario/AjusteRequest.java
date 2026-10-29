package com.intiwasi.backend.dto.inventario;

import com.intiwasi.backend.entity.enums.TipoAjuste;
import com.intiwasi.backend.dto.Comun.Patrones;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class AjusteRequest {
    @NotNull(message = "El tipo de ajuste es obligatorio")
    private TipoAjuste tipoAjuste;

    @NotBlank(message = "El motivo del ajuste es obligatorio")
    @Size(max = 255, message = "El motivo no debe exceder 255 caracteres")
    @Pattern(
            regexp = Patrones.SIN_EMOJIS,
            message = "El motivo no debe contener emojis"
    )
    private String motivo;

    @Size(max = 255, message = "Las observaciones no deben exceder 255 caracteres")
    @Pattern(
            regexp = Patrones.SIN_EMOJIS,
            message = "Las observaciones no deben contener emojis"
    )
    private String observaciones;

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones == null || observaciones.isBlank() ? null : observaciones;
    }

    @Size(max = 255, message = "El motivo de corrección no debe exceder 255 caracteres")
    @Pattern(
            regexp = Patrones.SIN_EMOJIS,
            message = "El motivo no debe contener emojis"
    )
    private String motivoCorreccion;

    public void setMotivoCorreccion(String motivoCorreccion) {
        this.motivoCorreccion = motivoCorreccion == null || motivoCorreccion.isBlank() ? null : motivoCorreccion;
    }

    @Valid
    @NotEmpty(message = "El ajuste debe contener al menos un movimiento")
    private List<MovimientoRequest> movimientos;
}
