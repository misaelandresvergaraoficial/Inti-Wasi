package com.intiwasi.backend.service;

import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.dto.Usuario.UsuarioRequest;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.intiwasi.backend.security.SessionRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {
    @Mock UsuarioRepository repository;
    @Mock PasswordEncoder encoder;
    @Mock SessionRegistry sessions;
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

    @Test
    void cambiarContrasenaRevocaLasSesionesDeLaIdentidadAnterior() {
        usuario.setCorreo("operador.nombre@intiwasi.pe");
        when(repository.bloquearUsuarios()).thenReturn(List.of(administrador, usuario));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(encoder.encode("segura123")).thenReturn("hash-de-prueba");
        service.actualizar(6, solicitud());
        verify(sessions).revokeUser("operador.nombre@intiwasi.pe");
    }

    @Test
    void desactivarRevocaSesionesPeroReactivarNoLasRecupera() {
        when(repository.bloquearUsuarios()).thenReturn(List.of(administrador, usuario));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        service.cambiarEstado(6, 0);
        verify(sessions).revokeUser(usuario.getCorreo());
        clearInvocations(sessions);
        service.cambiarEstado(6, 1);
        verifyNoInteractions(sessions);
    }


    @Test
    void permiteHomonimoConCorreoDistintoPeroConservaUnicidadCorreo() {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(encoder.encode("segura123")).thenReturn("hash-de-prueba");
        var request = solicitud();
        assertEquals("Andrés Vega", service.registrar(request).getNomUsuario());
        verify(repository, never()).existsByNomUsuario(any());
        when(repository.existsByCorreo(request.getCorreo())).thenReturn(true);
        assertThrows(com.intiwasi.backend.exception.ConflictoException.class, () -> service.registrar(request));
        verify(repository, times(1)).save(any());
    }

    @Test
    void noPermiteCambiarRolDelUltimoAdministradorNiConservaCambioParcial() {
        when(repository.bloquearUsuarios()).thenReturn(List.of(administrador, usuario));
        assertThrows(ReglaNegocioException.class, () -> service.actualizar(1, solicitud()));
        assertEquals("Administrador", administrador.getRol());
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
