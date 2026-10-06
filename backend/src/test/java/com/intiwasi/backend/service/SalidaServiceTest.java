package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.inventario.MovimientoRequest;
import com.intiwasi.backend.dto.inventario.SalidaRequest;
import com.intiwasi.backend.entity.Documento;
import com.intiwasi.backend.entity.MovimientoInventario;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.entity.Salida;
import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.entity.enums.MotivoSalida;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.repository.DocumentoRepository;
import com.intiwasi.backend.repository.MovimientoInventarioRepository;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.SalidaRepository;
import com.intiwasi.backend.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalidaServiceTest {
    @Mock SalidaRepository salidas;
    @Mock DocumentoRepository documentos;
    @Mock MovimientoInventarioRepository movimientos;
    @Mock ProductoRepository productos;
    @Mock UsuarioRepository usuarios;
    @Mock CorreccionInventarioService correcciones;
    @Mock EntityManager entityManager;
    @InjectMocks SalidaService service;

    private Producto producto;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setIdProducto(12);
        producto.setSku("CPU-12");
        producto.setNomProducto("Procesador");
        producto.setEstado((byte) 1);
        producto.setStockActual(10);

        usuario = new Usuario();
        usuario.setIdUsuario(3);
        usuario.setNomUsuario("Responsable");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registraSalidaConStockDisponible() {
        prepararSalida(3);

        var respuesta = service.registrar(request(3));

        assertEquals(50, respuesta.getIdSalida());
        assertEquals(3, respuesta.getMovimientos().getFirst().getCantidad());
        comprobarGuardado(3);
    }

    @Test
    void rechazaSalidaDeProductoInactivo() {
        producto.setEstado((byte) 0);
        when(productos.findAllByIdForUpdate(List.of(12))).thenReturn(List.of(producto));

        assertThrows(ReglaNegocioException.class, () -> service.registrar(request(1)));

        verifyNoInteractions(documentos, salidas, movimientos);
    }

    @Test
    void aceptaSalidaExactamenteIgualAlStockDisponible() {
        prepararSalida(10);

        var respuesta = service.registrar(request(10));

        assertEquals(10, respuesta.getMovimientos().getFirst().getCantidad());
        comprobarGuardado(10);
    }

    @Test
    void rechazaSalidaQueSuperaElStockDisponible() {
        when(productos.findAllByIdForUpdate(List.of(12))).thenReturn(List.of(producto));

        assertThrows(ReglaNegocioException.class, () -> service.registrar(request(11)));

        verifyNoInteractions(documentos, salidas, movimientos);
    }

    private SalidaRequest request(int cantidad) {
        MovimientoRequest movimiento = new MovimientoRequest();
        movimiento.setIdProducto(12);
        movimiento.setCantidad(cantidad);
        SalidaRequest request = new SalidaRequest();
        request.setMotivo(MotivoSalida.DESPACHO_VENTA);
        request.setMovimientos(List.of(movimiento));
        return request;
    }

    private void prepararSalida(int cantidad) {
        Documento documentoGuardado = new Documento();
        documentoGuardado.setIdDocumento(30);
        documentoGuardado.setUsuario(usuario);
        Salida salidaGuardada = new Salida();
        salidaGuardada.setIdSalida(50);
        salidaGuardada.setDocumento(documentoGuardado);
        salidaGuardada.setMotivo(MotivoSalida.DESPACHO_VENTA);
        MovimientoInventario movimientoGuardado = new MovimientoInventario();
        movimientoGuardado.setProducto(producto);
        movimientoGuardado.setCantidad(cantidad);

        when(productos.findAllByIdForUpdate(List.of(12))).thenReturn(List.of(producto));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("responsable@intiwasi.test", null, List.of()));
        when(usuarios.findByCorreoAndEstado("responsable@intiwasi.test", 1)).thenReturn(Optional.of(usuario));
        when(documentos.saveAndFlush(any(Documento.class))).thenReturn(documentoGuardado);
        when(salidas.saveAndFlush(any(Salida.class))).thenReturn(salidaGuardada);
        when(salidas.findByIdConRelaciones(50)).thenReturn(Optional.of(salidaGuardada));
        when(movimientos.findByDocumento_IdDocumentoOrderByIdMovimientoAsc(30))
                .thenReturn(List.of(movimientoGuardado));
    }

    private void comprobarGuardado(int cantidad) {
        verify(documentos).saveAndFlush(argThat(d -> d.getTipoDocumento() == TipoDocumento.SALIDA
                && d.getUsuario() == usuario));
        verify(salidas).saveAndFlush(argThat(s -> s.getMotivo() == MotivoSalida.DESPACHO_VENTA));
        verify(movimientos).save(argThat(m -> m.getProducto() == producto
                && m.getCantidad() == cantidad));
    }
}
