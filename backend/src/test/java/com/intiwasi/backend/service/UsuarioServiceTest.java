package com.intiwasi.backend.service;

import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.dto.UsuarioRequest;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.exception.ConflictoException;
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
        when(repository.bloquearUsuarios()).thenReturn(List.of(administrador, usuario));
        when(repository.save(usuario)).thenReturn(usuario);

        service.desactivar(6);

        assertEquals(0, usuario.getEstado());
        verify(repository).save(usuario);
        verify(repository, never()).delete(any(Usuario.class));
    }

    @Test
    void putDeEstadoReactivaLaMismaCuenta() {
        usuario.setEstado(0);
        when(repository.bloquearUsuarios()).thenReturn(List.of(administrador, usuario));
        when(repository.save(usuario)).thenReturn(usuario);

        var resultado = service.cambiarEstado(6, 1);

        assertEquals(6, resultado.getIdUsuario());
        assertEquals(1, resultado.getEstado());
    }

    @Test
    void noReactivaUnUsuarioInexistente() {
        when(repository.bloquearUsuarios()).thenReturn(List.of(administrador, usuario));
        assertThrows(RecursoNoEncontradoException.class, () -> service.cambiarEstado(99, 1));
    }

    @Test
    void obtenerActualSoloAceptaCuentaActiva() {
        when(repository.findByCorreoAndEstado("andres@intiwasi.test", 1)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.obtenerActual("andres@intiwasi.test"));
    }

    @Test
    void bloqueaDesactivacionPorAmbasRutasYCambioDeRolDelUltimoAdministrador() {
        when(repository.bloquearUsuarios()).thenReturn(List.of(administrador, usuario));
        assertThrows(ReglaNegocioException.class, () -> service.desactivar(1));
        assertThrows(ReglaNegocioException.class, () -> service.cambiarEstado(1, 0));
        assertThrows(ReglaNegocioException.class, () -> service.actualizar(1, solicitud()));
        assertEquals(1, administrador.getEstado());
        assertEquals("Administrador", administrador.getRol());
        verify(repository, never()).save(any());
    }

    @Test
    void unAdministradorInactivoNoPermiteDesactivarAlUltimoActivo() {
        usuario.setRol("Administrador");
        usuario.setEstado(0);
        when(repository.bloquearUsuarios()).thenReturn(List.of(administrador, usuario));
        assertThrows(ReglaNegocioException.class, () -> service.desactivar(1));
    }

    @Test
    void permiteDesactivarSiQuedaOtroAdministradorActivo() {
        usuario.setRol("Administrador");
        when(repository.bloquearUsuarios()).thenReturn(List.of(administrador, usuario));
        when(repository.save(administrador)).thenReturn(administrador);
        service.desactivar(1);
        assertEquals(0, administrador.getEstado());
        assertEquals(1, usuario.getEstado());
    }

    @Test
    void rechazaDominioAjenoYCorreoIncompatibleConRol() {
        var request = solicitud();
        request.setCorreo("operador.nombre@otrocorreo.pe");
        assertThrows(IllegalArgumentException.class, () -> service.registrar(request));
        request.setCorreo("admin.nombre@intiwasi.pe");
        assertThrows(IllegalArgumentException.class, () -> service.actualizar(6, request));
        request.setCorreo("operador.@intiwasi.pe");
        assertThrows(IllegalArgumentException.class, () -> service.registrar(request));
        for (String correo : List.of("operador.carlos.mendoza@intiwasi.pe",
                "operador.carlos1@intiwasi.pe", "operador.carlos_m@intiwasi.pe")) {
            request.setCorreo(correo);
            assertThrows(IllegalArgumentException.class, () -> service.registrar(request));
        }
        verify(repository, never()).save(any());
    }

    @Test
    void aceptaInicialDelApellidoEnElNombreDelCorreo() {
        var request = solicitud();
        request.setCorreo("operador.carlosm@intiwasi.pe");
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        assertEquals(request.getCorreo(), service.registrar(request).getCorreo());
    }

    @Test
    void rechazaTelefonoDuplicadoDespuesDeQuitarEspaciosExternos() {
        var request = solicitud();
        request.setTelefono(" 987654321 ");
        when(repository.existsByTelefono("987654321")).thenReturn(true);
        assertThrows(ConflictoException.class, () -> service.registrar(request));
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaTelefonosConLetrasSignosOCero() {
        var request = solicitud();
        for (String telefono : List.of("123a", "-123", "+123", "000", "12 34")) {
            request.setTelefono(telefono);
            assertThrows(IllegalArgumentException.class, () -> service.registrar(request));
        }
        verify(repository, never()).save(any());
    }

    @Test
    void conservaTelefonoPropioYContrasenaAlEditar() {
        usuario.setCorreo("operador.nombre@intiwasi.pe");
        usuario.setTelefono("987654321");
        usuario.setContrasena("hash-anterior");
        var request = solicitud();
        request.setContrasena(null);
        request.setTelefono("987654321");
        when(repository.bloquearUsuarios()).thenReturn(List.of(administrador, usuario));
        when(repository.save(usuario)).thenReturn(usuario);
        service.actualizar(6, request);
        assertEquals("hash-anterior", usuario.getContrasena());
        assertEquals("987654321", usuario.getTelefono());
        verify(repository, never()).existsByTelefono(any());
    }

    @Test
    void guardaTelefonoVacioComoNull() {
        var request = solicitud();
        request.setTelefono("   ");
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        assertNull(service.registrar(request).getTelefono());
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
