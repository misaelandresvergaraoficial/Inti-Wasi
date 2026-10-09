package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.inventario.EntradaRequest;
import com.intiwasi.backend.dto.inventario.MovimientoRequest;
import com.intiwasi.backend.entity.DetalleOrdenCompra;
import com.intiwasi.backend.entity.Documento;
import com.intiwasi.backend.entity.Entrada;
import com.intiwasi.backend.entity.MovimientoInventario;
import com.intiwasi.backend.entity.OrdenCompra;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.repository.DocumentoRepository;
import com.intiwasi.backend.repository.EntradaRepository;
import com.intiwasi.backend.repository.MovimientoInventarioRepository;
import com.intiwasi.backend.repository.OrdenCompraRepository;
import com.intiwasi.backend.repository.ProductoRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntradaServiceTest {
    @Mock EntradaRepository entradas;
    @Mock DocumentoRepository documentos;
    @Mock MovimientoInventarioRepository movimientos;
    @Mock OrdenCompraRepository ordenes;
    @Mock ProductoRepository productos;
    @Mock UsuarioRepository usuarios;
    @Mock CorreccionInventarioService correcciones;
    @Mock EntityManager entityManager;
    @InjectMocks EntradaService service;

    private Producto producto;
    private OrdenCompra orden;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        producto = new Producto();
        producto.setIdProducto(12);
        producto.setSku("CPU-12");
        producto.setNomProducto("Procesador");
        producto.setEstado((byte) 1);
        producto.setStockActual(4);

        orden = new OrdenCompra();
        orden.setIdOrden(7);
        orden.setEstado("Pendiente");
        DetalleOrdenCompra detalle = new DetalleOrdenCompra();
        detalle.setProducto(producto);
        detalle.setCantidad(10);
        orden.agregarDetalle(detalle);

        usuario = new Usuario();
        usuario.setIdUsuario(3);
        usuario.setNomUsuario("Responsable");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registraRecepcionParcialValida() {
        prepararRecepcion(2, 3);

        var respuesta = service.registrar(request(12, 3));

        assertEquals(40, respuesta.getIdEntrada());
        assertEquals(7, respuesta.getIdOrden());
        assertEquals(3, respuesta.getMovimientos().getFirst().getCantidad());
        comprobarGuardado(3);
    }

    @Test
    void rechazaProductoQueNoPerteneceALaOrden() {
        Producto ajeno = new Producto();
        ajeno.setIdProducto(13);
        ajeno.setEstado((byte) 1);
        when(ordenes.findByIdConDetallesForUpdate(7)).thenReturn(Optional.of(orden));
        when(productos.findAllByIdForUpdate(List.of(13))).thenReturn(List.of(ajeno));

        assertThrows(ReglaNegocioException.class, () -> service.registrar(request(13, 1)));

        verifyNoInteractions(documentos, entradas, movimientos);
    }

    @Test
    void aceptaRecepcionExactamenteIgualAlSaldoPendiente() {
        prepararRecepcion(8, 2);

        var respuesta = service.registrar(request(12, 2));

        assertEquals(2, respuesta.getMovimientos().getFirst().getCantidad());
        comprobarGuardado(2);
    }

    @Test
    void rechazaRecepcionQueSuperaElSaldoPendiente() {
        when(ordenes.findByIdConDetallesForUpdate(7)).thenReturn(Optional.of(orden));
        when(productos.findAllByIdForUpdate(List.of(12))).thenReturn(List.of(producto));
        when(entradas.cantidadRecibida(7, 12)).thenReturn(8);

        assertThrows(ReglaNegocioException.class, () -> service.registrar(request(12, 3)));

        verifyNoInteractions(documentos, movimientos);
        verify(entradas).cantidadRecibida(7, 12);
        verify(entradas, never()).saveAndFlush(any(Entrada.class));
    }

    private EntradaRequest request(int idProducto, int cantidad) {
        MovimientoRequest movimiento = new MovimientoRequest();
        movimiento.setIdProducto(idProducto);
        movimiento.setCantidad(cantidad);
        EntradaRequest request = new EntradaRequest();
        request.setIdOrden(7);
        request.setNumeroGuiaRemision(" GUIA-7 ");
        request.setMovimientos(List.of(movimiento));
        return request;
    }

    private void prepararRecepcion(int recibida, int cantidad) {
        Documento documentoGuardado = new Documento();
        documentoGuardado.setIdDocumento(30);
        documentoGuardado.setUsuario(usuario);
        Entrada entradaGuardada = new Entrada();
        entradaGuardada.setIdEntrada(40);
        entradaGuardada.setDocumento(documentoGuardado);
        entradaGuardada.setOrdenCompra(orden);
        entradaGuardada.setNumeroGuiaRemision("GUIA-7");
        MovimientoInventario movimientoGuardado = new MovimientoInventario();
        movimientoGuardado.setProducto(producto);
        movimientoGuardado.setCantidad(cantidad);

        when(ordenes.findByIdConDetallesForUpdate(7)).thenReturn(Optional.of(orden));
        when(productos.findAllByIdForUpdate(List.of(12))).thenReturn(List.of(producto));
        when(entradas.cantidadRecibida(7, 12)).thenReturn(recibida);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("responsable@intiwasi.test", null, List.of()));
        when(usuarios.findByCorreoAndEstado("responsable@intiwasi.test", 1)).thenReturn(Optional.of(usuario));
        when(documentos.saveAndFlush(any(Documento.class))).thenReturn(documentoGuardado);
        when(entradas.saveAndFlush(any(Entrada.class))).thenReturn(entradaGuardada);
        when(entradas.findByIdConRelaciones(40)).thenReturn(Optional.of(entradaGuardada));
        when(movimientos.findByDocumento_IdDocumentoOrderByIdMovimientoAsc(30))
                .thenReturn(List.of(movimientoGuardado));
    }

    private void comprobarGuardado(int cantidad) {
        verify(documentos).saveAndFlush(argThat(d -> d.getTipoDocumento() == TipoDocumento.ENTRADA
                && d.getUsuario() == usuario));
        verify(entradas).saveAndFlush(argThat(e -> e.getOrdenCompra() == orden
                && "GUIA-7".equals(e.getNumeroGuiaRemision())));
        verify(movimientos).save(argThat(m -> m.getProducto() == producto
                && m.getCantidad() == cantidad));
    }
}
