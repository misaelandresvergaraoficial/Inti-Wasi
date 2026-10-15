package com.intiwasi.backend.service;

import com.intiwasi.backend.entity.Categoria;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.repository.CategoriaRepository;
import com.intiwasi.backend.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {
    @Mock CategoriaRepository categorias;
    @Mock ProductoRepository productos;
    @InjectMocks CategoriaService service;

    @Test
    void reactivaSoloLosProductosDesactivadosPorLaCategoria() {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(3);
        categoria.setNomCategoria("Memorias");
        categoria.setEstado((byte) 1);
        Producto afectado = new Producto();
        afectado.setEstado((byte) 1);
        afectado.setDesactivadoPorCategoria((byte) 0);
        Producto inactivoAnterior = new Producto();
        inactivoAnterior.setEstado((byte) 0);
        inactivoAnterior.setDesactivadoPorCategoria((byte) 0);
        when(categorias.findById(3)).thenReturn(Optional.of(categoria));
        when(categorias.save(categoria)).thenReturn(categoria);
        when(productos.findByCategoria_IdCategoriaAndEstado(3, (byte) 1))
                .thenReturn(List.of(afectado));
        when(productos.findByCategoria_IdCategoriaAndDesactivadoPorCategoria(3, (byte) 1))
                .thenReturn(List.of(afectado));

        service.cambiarEstado(3, 0);
        assertEquals((byte) 0, afectado.getEstado());
        assertEquals((byte) 1, afectado.getDesactivadoPorCategoria());
        service.cambiarEstado(3, 1);
        assertEquals((byte) 1, afectado.getEstado());
        assertEquals((byte) 0, afectado.getDesactivadoPorCategoria());
        assertEquals((byte) 0, inactivoAnterior.getEstado());
    }
}
