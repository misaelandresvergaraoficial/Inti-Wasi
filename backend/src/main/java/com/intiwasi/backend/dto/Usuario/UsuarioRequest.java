package com.intiwasi.backend.dto.Usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import com.intiwasi.backend.dto.Comun.Patrones;

@Data
public class UsuarioRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(
            regexp = Patrones.NOMBRE_PERSONAL,
            message = "El nombre admite letras, tildes, espacios, apóstrofos y guiones; sin números ni emojis"
    )
    private String nomUsuario;

    public void setNomUsuario(String nomUsuario) {
        this.nomUsuario = Patrones.normalizarNombre(nomUsuario);
    }

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
