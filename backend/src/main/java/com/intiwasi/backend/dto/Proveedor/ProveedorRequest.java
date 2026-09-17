package com.intiwasi.backend.dto.Proveedor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProveedorRequest {

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Size(max = 100, message = "El nombre del proveedor no debe exceder los 100 caracteres")
    private String nomProveedor;

    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "^[0-9]{11}$", message = "El RUC debe tener 11 dígitos numéricos exactos")
    private String ruc;

    @Size(max = 100, message = "El contacto no debe exceder los 100 caracteres")
    private String contacto;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 20, message = "El teléfono no debe exceder los 20 caracteres")
    private String telefono;

    @Size(max = 150, message = "La dirección no debe exceder los 150 caracteres")
    private String direccion;
}
