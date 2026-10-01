package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.AuthRequest;
import com.intiwasi.backend.dto.ErrorResponse;
import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.repository.UsuarioRepository;
import com.intiwasi.backend.security.JwtUtil;
import com.intiwasi.backend.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtUtil jwtUtil;
    @Mock UsuarioService usuarioService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AuthController controller;

    @Test
    void credencialesValidasDeCuentaInactivaRecibenMensajeEspecifico() {
        AuthRequest request = new AuthRequest();
        request.setCorreo("inactivo@intiwasi.test");
        request.setContrasena("correcta");
        Usuario usuario = new Usuario();
        usuario.setEstado(0);
        usuario.setContrasena("hash");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid"));
        when(usuarioRepository.findByCorreo(request.getCorreo())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("correcta", "hash")).thenReturn(true);

        var response = controller.login(request);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Su cuenta no está disponible. Contacte al Administrador.",
                ((ErrorResponse) response.getBody()).getMessage());
    }

    @Test
    void contrasenaIncorrectaNoRevelaSiLaCuentaEstaInactiva() {
        AuthRequest request = new AuthRequest();
        request.setCorreo("inactivo@intiwasi.test");
        request.setContrasena("incorrecta");
        Usuario usuario = new Usuario();
        usuario.setEstado(0);
        usuario.setContrasena("hash");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Invalid"));
        when(usuarioRepository.findByCorreo(request.getCorreo())).thenReturn(Optional.of(usuario));

        var response = controller.login(request);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("El correo o la contraseña son inválidos",
                ((ErrorResponse) response.getBody()).getMessage());
    }
}
