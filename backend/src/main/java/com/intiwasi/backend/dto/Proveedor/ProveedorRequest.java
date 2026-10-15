package com.intiwasi.backend.dto.Proveedor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProveedorRequest {

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Size(
            max = 100,
            message = "El nombre del proveedor no debe exceder los 100 caracteres"
    )
    private String nomProveedor;

    @NotBlank(message = "El prefijo de guía es obligatorio")
    @Size(
            max = 30,
            message = "El prefijo de guía no debe exceder 30 caracteres"
    )
    @Pattern(
            regexp = "^GUIA-[A-Z][A-Z0-9]{0,23}-$",
            message = "Usa GUIA-CODIGO-, en mayúsculas; el código empieza "
                    + "con una letra y admite letras y números"
    )
    private String prefijoGuia;

    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(
            regexp = "^[0-9]{11}$",
            message = "El RUC debe tener 11 dígitos numéricos exactos"
    )
    private String ruc;

    @Size(
            max = 100,
            message = "El contacto no debe exceder los 100 caracteres"
    )
    private String contacto;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(
            max = 20,
            message = "El teléfono no debe exceder los 20 dígitos"
    )
    @Pattern(
            regexp = "^(?=.*[1-9])[0-9]{1,20}$",
            message = "El teléfono debe contener solo dígitos, sin signos, "
                    + "espacios ni decimales y no puede ser solo ceros"
    )
    private String telefono;

    @Size(
            max = 150,
            message = "La dirección no debe exceder los 150 caracteres"
    )
    private String direccion;
}