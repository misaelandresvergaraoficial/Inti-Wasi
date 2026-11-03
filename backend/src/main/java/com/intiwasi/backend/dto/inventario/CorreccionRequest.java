package com.intiwasi.backend.dto.inventario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import jakarta.validation.constraints.Pattern;
import com.intiwasi.backend.dto.Comun.Patrones;

@Data
public class CorreccionRequest {
    @NotBlank(message = "El motivo de corrección es obligatorio")
    @Size(max = 255, message = "El motivo de corrección no debe exceder 255 caracteres")
    @Pattern(regexp = Patrones.SIN_EMOJIS, message = "El motivo no debe contener emojis")
    private String motivo;
}
