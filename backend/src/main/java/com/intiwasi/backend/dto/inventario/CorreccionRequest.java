package com.intiwasi.backend.dto.inventario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CorreccionRequest {
    @NotBlank(message = "El motivo de corrección es obligatorio")
    @Size(max = 255, message = "El motivo de corrección no debe exceder 255 caracteres")
    private String motivo;
}
