package com.intiwasi.backend.service;

import com.intiwasi.backend.entity.Categoria;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
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

}
