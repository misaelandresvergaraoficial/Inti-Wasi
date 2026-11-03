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
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
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
    void permitePrecioCeroSinCambiarPrecioDelCatalogo() {
        var usuario = new Usuario(); usuario.setIdUsuario(3);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin@intiwasi.test", null, List.of()));
        request.getDetalles().getFirst().setPrecioUnitario(BigDecimal.ZERO);
        when(proveedores.findById(1)).thenReturn(Optional.of(proveedor));
        when(usuarios.findByCorreoAndEstado("admin@intiwasi.test", 1)).thenReturn(Optional.of(usuario));
        when(productos.findById(12)).thenReturn(Optional.of(producto));
        when(ordenes.save(any(OrdenCompra.class))).thenAnswer(call -> call.getArgument(0));
        assertEquals(BigDecimal.ZERO, service.crear(request).getTotalOrden());
        assertEquals(new BigDecimal("680.00"), producto.getPrecio());
    }

    @Test
    void historialUsaLimiteServidorYOrdenEstable() {
        when(ordenes.findAll(org.mockito.ArgumentMatchers.<Specification<OrdenCompra>>any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());
        service.consultar(3, 200, "proveedor", "Pendiente", null, null, "asc", false);
        verify(ordenes).findAll(org.mockito.ArgumentMatchers.<Specification<OrdenCompra>>any(), org.mockito.ArgumentMatchers.argThat((org.springframework.data.domain.Pageable page) ->
                page.getOffset() == 600 && page.getPageSize() == 200
                        && page.getSort().getOrderFor("fechaEmision").isAscending()
                        && page.getSort().getOrderFor("idOrden").isAscending()));
        verify(ordenes, never()).findAllConDetalles();
    }

    @Test
    void historialRechazaPaginaFueraDeContratoAntesDeConsultar() {
        assertThrows(IllegalArgumentException.class, () -> service.consultar(0, 201, null, null, null, null, "desc", false));
        assertThrows(IllegalArgumentException.class, () -> service.consultar(-1, 20, null, null, null, null, "desc", false));
        assertThrows(IllegalArgumentException.class, () -> service.consultar(0, 20, null, null, java.time.LocalDate.of(2026, 11, 2), java.time.LocalDate.of(2026, 11, 1), "desc", false));
        org.mockito.Mockito.verifyNoInteractions(ordenes);
    }

    @Test
    void ordenPendienteConEntradaAnuladaSigueSinSerEditable() {
        var orden = new OrdenCompra();
        orden.setIdOrden(7); orden.setProveedor(proveedor); orden.setUsuario(new Usuario()); orden.setEstado("Pendiente");
        when(ordenes.findByIdConDetalles(7)).thenReturn(Optional.of(orden));
        when(entradas.existsByOrdenCompra_IdOrden(7)).thenReturn(true);
        org.junit.jupiter.api.Assertions.assertFalse(service.obtenerPorId(7).isEditable());
        when(ordenes.findByIdConDetallesForUpdate(7)).thenReturn(Optional.of(orden));
        assertThrows(ReglaNegocioException.class, () -> service.actualizar(7, request));
        verify(ordenes, never()).save(any());
    }

    @Test
    void cienUnaLineasSeRechazanAntesDePersistir() {
        request.setDetalles(java.util.Collections.nCopies(101, request.getDetalles().getFirst()));
        assertThrows(ReglaNegocioException.class, () -> service.crear(request));
        org.mockito.Mockito.verifyNoInteractions(proveedores, productos, ordenes);
    }
}
