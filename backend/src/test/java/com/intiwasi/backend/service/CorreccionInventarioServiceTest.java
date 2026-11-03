package com.intiwasi.backend.service;

import com.intiwasi.backend.entity.*;
import com.intiwasi.backend.entity.enums.*;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class CorreccionInventarioServiceTest {
    @Mock DocumentoRepository documentos;
    @Mock MovimientoInventarioRepository movimientos;
    @Mock ProductoRepository productos;
    @Mock AjusteRepository ajustes;
    @Mock EntityManager entityManager;
    @InjectMocks CorreccionInventarioService service;

    @Test
    void rechazaReversionDeEntradaSinStockAntesDeEscribir() {
        Documento original = original(TipoDocumento.ENTRADA);
        Producto producto = producto(2);
        MovimientoInventario linea = linea(producto, 5);
        when(documentos.findByIdForUpdate(9)).thenReturn(Optional.of(original));
        when(movimientos.findByDocumento_IdDocumentoOrderByIdMovimientoAsc(9)).thenReturn(List.of(linea));
        when(productos.findAllByIdForUpdate(List.of(7))).thenReturn(List.of(producto));
        var error = assertThrows(ReglaNegocioException.class,
                () -> service.revertir(9, TipoDocumento.ENTRADA, new Usuario(), "Cantidad equivocada", (byte) 2));
        assertTrue(error.getMessage().contains("producto: 7"));
        assertTrue(error.getMessage().contains("Stock disponible: 2"));
        assertTrue(error.getMessage().contains("cantidad necesaria: 5"));
        assertEquals((byte) 1, original.getEstado());
        verify(documentos, never()).saveAndFlush(any());
        verify(movimientos, never()).save(any());
    }

    @Test
    void anularSalidaCopiaTodasLasLineasYConservaIdentidadOriginal() {
        Documento original = original(TipoDocumento.SALIDA);
        Usuario responsable = new Usuario(); original.setUsuario(responsable);
        Usuario actor = new Usuario();
        Producto producto = producto(2);
        when(documentos.findByIdForUpdate(9)).thenReturn(Optional.of(original));
        when(documentos.findById(9)).thenReturn(Optional.of(original));
        when(movimientos.findByDocumento_IdDocumentoOrderByIdMovimientoAsc(9)).thenReturn(List.of(linea(producto, 5)));
        when(productos.findAllByIdForUpdate(List.of(7))).thenReturn(List.of(producto));
        when(documentos.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        service.revertir(9, TipoDocumento.SALIDA, actor, "Salida duplicada", (byte) 0);
        assertEquals((byte) 0, original.getEstado());
        assertSame(responsable, original.getUsuario());
        assertEquals(TipoDocumento.SALIDA, original.getTipoDocumento());
        verify(documentos).saveAndFlush(argThat(d -> d.getTipoDocumento() == TipoDocumento.CORRECCION
                && d.getDocumentoOrigen() == original && d.getUsuario() == actor
                && "Salida duplicada".equals(d.getMotivoCorreccion())));
        verify(movimientos).save(argThat(m -> m.getProducto() == producto && m.getCantidad() == 5
                && m.getDocumento().getTipoDocumento() == TipoDocumento.CORRECCION));
    }

    @Test
    void rechazaCorregirDeNuevoUnOriginalNoVigente() {
        Documento original = original(TipoDocumento.SALIDA); original.setEstado((byte) 2);
        when(documentos.findByIdForUpdate(9)).thenReturn(Optional.of(original));
        assertThrows(ReglaNegocioException.class,
                () -> service.revertir(9, TipoDocumento.SALIDA, new Usuario(), "Repetida", (byte) 0));
        verifyNoInteractions(movimientos, productos);
    }

    private Documento original(TipoDocumento tipo) {
        Documento d = new Documento(); d.setIdDocumento(9); d.setTipoDocumento(tipo); d.setEstado((byte) 1); return d;
    }
    private Producto producto(int stock) {
        Producto p = new Producto(); p.setIdProducto(7); p.setStockActual(stock); return p;
    }
    private MovimientoInventario linea(Producto p, int cantidad) {
        MovimientoInventario m = new MovimientoInventario(); m.setProducto(p); m.setCantidad(cantidad); return m;
    }
}
