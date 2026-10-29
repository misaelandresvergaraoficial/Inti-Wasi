package com.intiwasi.backend.dto.Proveedor;

import com.intiwasi.backend.dto.Comun.Patrones;
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
    @Pattern(
            regexp = Patrones.RAZON_SOCIAL,
            message = "El nombre admite letras, números, espacios y . , & - ( ) /; sin emojis"
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
    @Pattern(
            regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,100}$",
            message = "El contacto solo admite letras y espacios; sin números, signos ni emojis"
    )
    private String contacto;

    public void setContacto(String contacto) {
        this.contacto = contacto == null || contacto.isBlank() ? null : contacto;
    }

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(
            max = 9,
            message = "El teléfono debe tener exactamente 9 dígitos"
    )
    @Pattern(
            regexp = "^(9\\d{8}|0[1-9]\\d{7})$",
            message = "El teléfono debe tener 9 dígitos: celular (9xxxxxxxx) o fijo nacional (01xxxxxxx)"
    )
    private String telefono;

    @Size(
            max = 150,
            message = "La dirección no debe exceder los 150 caracteres"
    )
    private String direccion;
}