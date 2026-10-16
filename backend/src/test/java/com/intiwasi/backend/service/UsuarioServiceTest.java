package com.intiwasi.backend.service;

import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.dto.UsuarioRequest;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {
    @Mock UsuarioRepository repository;
    @Mock PasswordEncoder encoder;
    @InjectMocks UsuarioService service;
    private Usuario usuario;
    private Usuario administrador;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setIdUsuario(6);
        usuario.setNomUsuario("Andrés Vega");
        usuario.setCorreo("andres@intiwasi.test");
        usuario.setRol("Operador de Almacén");
        usuario.setEstado(1);
        administrador = new Usuario();
        administrador.setIdUsuario(1);
        administrador.setNomUsuario("Administrador");
        administrador.setCorreo("admin.nombre@intiwasi.pe");
        administrador.setRol("Administrador");
        administrador.setEstado(1);
    }

    @Test
    void noDesactivaAlUltimoAdministrador() {
        when(repository.bloquearUsuarios()).thenReturn(List.of(administrador, usuario));
        assertThrows(ReglaNegocioException.class, () -> service.desactivar(1));
        assertEquals(1, administrador.getEstado());
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaCorreoDeOtroDominio() {
        var request = solicitud();
        request.setCorreo("operador.nombre@otrocorreo.pe");
        assertThrows(IllegalArgumentException.class, () -> service.registrar(request));
        verify(repository, never()).save(any());
    }

    private UsuarioRequest solicitud() {
        var request = new UsuarioRequest();
        request.setNomUsuario("Andrés Vega");
        request.setCorreo("operador.nombre@intiwasi.pe");
        request.setRol("Operador de Almacén");
        request.setContrasena("segura123");
        return request;
    }
}
