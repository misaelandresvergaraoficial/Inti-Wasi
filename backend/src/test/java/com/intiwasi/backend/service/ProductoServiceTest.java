package com.intiwasi.backend.service;

import com.intiwasi.backend.entity.Categoria;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.repository.CategoriaRepository;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.ProveedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {
    @Mock ProductoRepository productos;
    @Mock CategoriaRepository categorias;
    @Mock ProveedorRepository proveedores;
    @InjectMocks ProductoService service;

    private Producto producto;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        categoria = new Categoria();
        categoria.setIdCategoria(2);
        categoria.setEstado((byte) 1);
        producto = new Producto();
        producto.setIdProducto(12);
        producto.setCategoria(categoria);
        producto.setEstado((byte) 0);
    }

    @Test
    void operadorSoloPuedeConsultarProductoActivoPorId() {
        when(productos.findById(12)).thenReturn(Optional.of(producto));

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(12, false));

        producto.setEstado((byte) 1);
        assertEquals(Integer.valueOf(12), service.obtenerPorId(12, false).getIdProducto());
    }

    @Test
    void administradorPuedeConsultarProductoInactivoPorId() {
        when(productos.findById(12)).thenReturn(Optional.of(producto));

        assertEquals(0, service.obtenerPorId(12, true).getEstado().intValue());
    }

    @Test
    void rechazaEstadosFueraDeCeroYUno() {
        assertThrows(IllegalArgumentException.class, () -> service.cambiarEstado(12, null));
        assertThrows(IllegalArgumentException.class, () -> service.cambiarEstado(12, 2));
        verifyNoInteractions(productos);
    }

    @Test
    void reactivaProductoConCategoriaActiva() {
        when(productos.findById(12)).thenReturn(Optional.of(producto));
        when(productos.save(producto)).thenReturn(producto);

        var resultado = service.cambiarEstado(12, 1);

        assertEquals(Byte.valueOf((byte) 1), resultado.getEstado());
        verify(productos).save(producto);
    }

    @Test
    void noReactivaProductoConCategoriaInactiva() {
        categoria.setEstado((byte) 0);
        when(productos.findById(12)).thenReturn(Optional.of(producto));

        assertThrows(ReglaNegocioException.class, () -> service.cambiarEstado(12, 1));

        verify(productos, never()).save(producto);
    }

    @Test
    void deleteSoloDesactivaElProducto() {
        producto.setEstado((byte) 1);
        when(productos.findById(12)).thenReturn(Optional.of(producto));

        service.desactivarProducto(12);

        assertEquals(Byte.valueOf((byte) 0), producto.getEstado());
        verify(productos).save(producto);
        verify(productos, never()).delete(producto);
    }
}
