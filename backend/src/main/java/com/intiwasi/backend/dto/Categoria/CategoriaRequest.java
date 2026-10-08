package com.intiwasi.backend.dto.Categoria;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoriaRequest {

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 100, message = "El nombre de la categoría no debe exceder los 100 caracteres")
    private String nomCategoria;

    @NotNull (message = "El estado de la categoría es obligatorio")
    @Min(value = 0, message = "El estado de la categoría debe ser 0 (Inactivo) o 1 (Activo)")
    @Max(value = 1, message = "El estado de la categoría debe ser 0 (Inactivo) o 1 (Activo)")
    private Integer estado;
}
