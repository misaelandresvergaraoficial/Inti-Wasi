package com.intiwasi.backend.dto.Usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UsuarioRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 50, message = "El nombre no debe superar los 50 caracteres")
    @Pattern(
            regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{2,50}$",
            message = "El nombre solo admite letras y espacios; sin números, signos ni emojis"
    )
    private String nomUsuario;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Debe ingresar un formato de correo válido")
    @Size(max = 100, message = "El correo no debe superar los 100 caracteres")
    private String correo;

    @Size(min = 6, max = 20, message = "La contraseña debe tener entre 6 y 20 caracteres")
    private String contrasena;

    @NotBlank(message = "El rol es obligatorio")
    @Pattern(
            regexp = "^(Administrador|Operador de Almacén)$",
            message = "El rol debe ser Administrador u Operador de Almacén"
    )
    private String rol;

    @Size(max = 9, message = "El teléfono debe tener exactamente 9 dígitos")
    @Pattern(
            regexp = "^9\\d{8}$",
            message = "El teléfono debe tener 9 dígitos y comenzar con 9 (ej. 979348552)"
    )
    private String telefono;

    public void setTelefono(String telefono) {
        this.telefono = telefono == null || telefono.isBlank() ? null : telefono;
    }
}
