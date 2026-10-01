package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.OrdenCompra.DetalleOrdenCompraRequest;
import com.intiwasi.backend.dto.OrdenCompra.OrdenCompraRequest;
import com.intiwasi.backend.entity.OrdenCompra;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.entity.Proveedor;
import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.repository.EntradaRepository;
import com.intiwasi.backend.repository.OrdenCompraRepository;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.ProveedorRepository;
import com.intiwasi.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenCompraServiceTest {
    @Mock OrdenCompraRepository ordenes;
    @Mock ProveedorRepository proveedores;
    @Mock ProductoRepository productos;
    @Mock UsuarioRepository usuarios;
    @Mock EntradaRepository entradas;
    @InjectMocks OrdenCompraService service;

    private Proveedor proveedor;
    private Producto producto;
    private OrdenCompraRequest request;

    @BeforeEach
    void setUp() {
        proveedor = new Proveedor();
        proveedor.setIdProveedor(1);
        proveedor.setEstado((byte) 1);
        producto = new Producto();
        producto.setIdProducto(12);
        producto.setSku("CPU-12");
        producto.setEstado((byte) 1);
        producto.setProveedor(proveedor);
        producto.setPrecio(new BigDecimal("680.00"));
        var detalle = new DetalleOrdenCompraRequest();
        detalle.setIdProducto(12);
        detalle.setCantidad(2);
        detalle.setPrecioUnitario(new BigDecimal("620.00"));
        request = new OrdenCompraRequest();
        request.setIdProveedor(1);
        request.setDetalles(List.of(detalle));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creaOrdenConProductoDelProveedorYConservaPrecioPactado() {
        var usuario = new Usuario();
        usuario.setIdUsuario(3);
        usuario.setNomUsuario("Administrador");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@intiwasi.test", null, List.of())
        );
        when(proveedores.findById(1)).thenReturn(Optional.of(proveedor));
        when(usuarios.findByCorreoAndEstado("admin@intiwasi.test", 1)).thenReturn(Optional.of(usuario));
        when(productos.findById(12)).thenReturn(Optional.of(producto));
        when(ordenes.save(any(OrdenCompra.class))).thenAnswer(call -> call.getArgument(0));

        var creada = service.crear(request);

        assertEquals(new BigDecimal("620.00"), creada.getDetalles().getFirst().getPrecioUnitario());
        assertEquals(new BigDecimal("680.00"), producto.getPrecio());
    }

    @Test
    void noCreaOrdenConProductoDeOtroProveedor() {
        var otro = new Proveedor();
        otro.setIdProveedor(2);
        producto.setProveedor(otro);
        var usuario = new Usuario();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@intiwasi.test", null, List.of())
        );
        when(proveedores.findById(1)).thenReturn(Optional.of(proveedor));
        when(usuarios.findByCorreoAndEstado("admin@intiwasi.test", 1)).thenReturn(Optional.of(usuario));
        when(productos.findById(12)).thenReturn(Optional.of(producto));

        assertThrows(ReglaNegocioException.class, () -> service.crear(request));

        verify(ordenes, never()).save(any(OrdenCompra.class));
    }

    @Test
    void noEditaOrdenConProductoDeOtroProveedor() {
        var otro = new Proveedor();
        otro.setIdProveedor(2);
        producto.setProveedor(otro);
        prepararEdicion();

        assertThrows(ReglaNegocioException.class, () -> service.actualizar(4, request));

        verify(ordenes, never()).save(any(OrdenCompra.class));
    }

    @Test
    void noEditaOrdenConProductoSinProveedor() {
        producto.setProveedor(null);
        prepararEdicion();

        assertThrows(ReglaNegocioException.class, () -> service.actualizar(4, request));

        verify(ordenes, never()).save(any(OrdenCompra.class));
    }

    @Test
    void noEditaOrdenConProductoInactivo() {
        producto.setEstado((byte) 0);
        prepararEdicion();

        assertThrows(ReglaNegocioException.class, () -> service.actualizar(4, request));

        verify(ordenes, never()).save(any(OrdenCompra.class));
    }

    private void prepararEdicion() {
        var orden = new OrdenCompra();
        orden.setIdOrden(4);
        orden.setEstado("Pendiente");
        orden.setFechaEmision(LocalDate.now());
        orden.setProveedor(proveedor);
        when(ordenes.findByIdConDetallesForUpdate(4)).thenReturn(Optional.of(orden));
        when(proveedores.findById(1)).thenReturn(Optional.of(proveedor));
        when(productos.findById(12)).thenReturn(Optional.of(producto));
    }
}
