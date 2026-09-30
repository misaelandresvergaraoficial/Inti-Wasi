package com.intiwasi.backend.service;

import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
import com.intiwasi.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {
    @Mock UsuarioRepository repository;
    @Mock PasswordEncoder encoder;
    @InjectMocks UsuarioService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setIdUsuario(6);
        usuario.setNomUsuario("Andrés Vega");
        usuario.setCorreo("andres@intiwasi.test");
        usuario.setRol("Operador de Almacén");
        usuario.setEstado(1);
    }

    @Test
    void listarTodosIncluyeInactivos() {
        Usuario inactivo = new Usuario();
        inactivo.setIdUsuario(7);
        inactivo.setNomUsuario("Elena Vargas");
        inactivo.setEstado(0);
        when(repository.findAllByOrderByIdUsuarioAsc()).thenReturn(List.of(usuario, inactivo));

        var resultado = service.listarTodos();

        assertEquals(2, resultado.size());
        assertEquals(0, resultado.get(1).getEstado());
    }

    @Test
    void deleteDesactivaSinBorrarElRegistro() {
        when(repository.findById(6)).thenReturn(Optional.of(usuario));
        when(repository.save(usuario)).thenReturn(usuario);

        service.desactivar(6);

        assertEquals(0, usuario.getEstado());
        verify(repository).save(usuario);
        verify(repository, never()).delete(any(Usuario.class));
    }

    @Test
    void putDeEstadoReactivaLaMismaCuenta() {
        usuario.setEstado(0);
        when(repository.findById(6)).thenReturn(Optional.of(usuario));
        when(repository.save(usuario)).thenReturn(usuario);

        var resultado = service.cambiarEstado(6, 1);

        assertEquals(6, resultado.getIdUsuario());
        assertEquals(1, resultado.getEstado());
    }

    @Test
    void noReactivaUnUsuarioInexistente() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarEstado(99, 1));
    }

    @Test
    void obtenerActualSoloAceptaCuentaActiva() {
        when(repository.findByCorreoAndEstado("andres@intiwasi.test", 1)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.obtenerActual("andres@intiwasi.test"));
    }
}
