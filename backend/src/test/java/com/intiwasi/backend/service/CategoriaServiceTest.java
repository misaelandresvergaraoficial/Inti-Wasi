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
        when(categorias.findByIdForUpdate(3)).thenReturn(Optional.of(categoria));
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

    @Test
    void altaForzadaInactivaSeGuardaActiva() {
        var request = new com.intiwasi.backend.dto.Categoria.CategoriaRequest();
        request.setNomCategoria("Memorias");
        request.setEstado(0);
        when(categorias.save(org.mockito.ArgumentMatchers.any())).thenAnswer(call -> call.getArgument(0));
        assertEquals((byte) 1, service.registrar(request).getEstado());
        org.mockito.Mockito.verify(categorias).save(org.mockito.ArgumentMatchers.argThat(c -> c.getEstado() == 1));
    }

    @Test
    void proveedorInactivoImpideTodaReactivacionAntesDeMutarCategoriaOProductos() {
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(3);
        categoria.setEstado((byte) 0);
        var proveedor = new com.intiwasi.backend.entity.Proveedor();
        proveedor.setIdProveedor(8);
        proveedor.setNomProveedor("Proveedor repetido");
        proveedor.setRuc("20123456789");
        proveedor.setEstado((byte) 0);
        Producto bloqueado = new Producto();
        bloqueado.setEstado((byte) 0);
        bloqueado.setDesactivadoPorCategoria((byte) 1);
        bloqueado.setProveedor(proveedor);
        bloqueado.setStockActual(17);
        Producto libre = new Producto();
        libre.setEstado((byte) 0);
        libre.setDesactivadoPorCategoria((byte) 1);
        when(categorias.findByIdForUpdate(3)).thenReturn(Optional.of(categoria));
        when(productos.findByCategoria_IdCategoriaAndDesactivadoPorCategoria(3, (byte) 1))
                .thenReturn(List.of(libre, bloqueado));

        var error = org.junit.jupiter.api.Assertions.assertThrows(
                com.intiwasi.backend.exception.ReglaNegocioException.class, () -> service.cambiarEstado(3, 1));

        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("20123456789"));
        assertEquals((byte) 0, categoria.getEstado());
        assertEquals((byte) 0, libre.getEstado());
        assertEquals((byte) 1, bloqueado.getDesactivadoPorCategoria());
        assertEquals(17, bloqueado.getStockActual());
        org.mockito.Mockito.verify(categorias, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(productos, org.mockito.Mockito.never()).saveAll(org.mockito.ArgumentMatchers.any());
    }
}
