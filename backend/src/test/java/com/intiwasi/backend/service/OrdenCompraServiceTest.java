package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.OrdenCompra.DetalleOrdenCompraRequest;
import com.intiwasi.backend.dto.OrdenCompra.OrdenCompraRequest;
import com.intiwasi.backend.dto.OrdenCompra.OrdenCompraResponse;
import com.intiwasi.backend.entity.DetalleOrdenCompra;
import com.intiwasi.backend.entity.OrdenCompra;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.entity.Proveedor;
import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
import com.intiwasi.backend.exception.ReglaNegocioException;
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
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenCompraServiceTest {

    @Mock
    private OrdenCompraRepository ordenCompraRepository;

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private OrdenCompraService ordenCompraService;

    private Proveedor proveedor;
    private Usuario usuario;
    private Producto producto;

    @BeforeEach
    void setUp() {
        proveedor = proveedor(1, "Proveedor Andino", (byte) 1);
        usuario = usuario(7, "Administrador", "admin@intiwasi.pe");
        producto = producto(10, "SKU-10", "Quinua", (byte) 1, 25);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario.getCorreo(), null, List.of())
        );
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void crearAsignaDatosDelBackendCalculaTotalesYNoModificaStock() {
        OrdenCompraRequest request = requestValido();
        request.getDetalles().add(detalleRequest(11, 3, "4.00"));
        Producto segundoProducto = producto(11, "SKU-11", "Kiwicha", (byte) 1, 8);

        prepararCreacionValida();
        when(productoRepository.findById(11)).thenReturn(Optional.of(segundoProducto));
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(invocation -> {
            OrdenCompra orden = invocation.getArgument(0);
            orden.setIdOrden(100);
            int idDetalle = 1;
            for (DetalleOrdenCompra detalle : orden.getDetalles()) {
                detalle.setIdDetalle(idDetalle++);
            }
            return orden;
        });

        OrdenCompraResponse response = ordenCompraService.crear(request);

        assertEquals(100, response.getIdOrden());
        assertEquals("Pendiente", response.getEstado());
        assertEquals(LocalDate.now(), response.getFechaEmision());
        assertEquals(usuario.getIdUsuario(), response.getIdUsuario());
        assertEquals(new BigDecimal("19.00"), response.getTotalOrden());
        assertEquals(new BigDecimal("7.00"), response.getDetalles().get(0).getSubtotal());
        assertEquals(new BigDecimal("12.00"), response.getDetalles().get(1).getSubtotal());
        assertEquals(25, producto.getStockActual());
        assertEquals(8, segundoProducto.getStockActual());
    }

    @Test
    void crearRechazaOrdenSinDetalles() {
        OrdenCompraRequest request = requestValido();
        request.setDetalles(List.of());

        ReglaNegocioException error = assertThrows(
                ReglaNegocioException.class,
                () -> ordenCompraService.crear(request)
        );

        assertEquals("La orden debe contener al menos un producto", error.getMessage());
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    void crearRechazaCantidadCeroONegativa() {
        for (int cantidad : List.of(0, -1)) {
            OrdenCompraRequest request = requestValido();
            request.getDetalles().get(0).setCantidad(cantidad);

            IllegalArgumentException error = assertThrows(
                    IllegalArgumentException.class,
                    () -> ordenCompraService.crear(request)
            );

            assertEquals("La cantidad debe ser mayor que cero", error.getMessage());
        }
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    void crearRechazaPrecioNegativo() {
        OrdenCompraRequest request = requestValido();
        request.getDetalles().get(0).setPrecioUnitario(new BigDecimal("-0.01"));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> ordenCompraService.crear(request)
        );

        assertEquals("El precio unitario no puede ser negativo", error.getMessage());
    }

    @Test
    void crearRechazaProductoRepetido() {
        OrdenCompraRequest request = requestValido();
        request.getDetalles().add(detalleRequest(10, 1, "3.00"));

        ReglaNegocioException error = assertThrows(
                ReglaNegocioException.class,
                () -> ordenCompraService.crear(request)
        );

        assertEquals("El producto no puede repetirse dentro de la orden", error.getMessage());
    }

    @Test
    void crearRechazaProveedorInexistenteOInactivo() {
        OrdenCompraRequest request = requestValido();
        when(proveedorRepository.findById(1)).thenReturn(Optional.empty());

        RecursoNoEncontradoException inexistente = assertThrows(
                RecursoNoEncontradoException.class,
                () -> ordenCompraService.crear(request)
        );
        assertEquals("El proveedor no existe o está inactivo", inexistente.getMessage());

        proveedor.setEstado((byte) 0);
        when(proveedorRepository.findById(1)).thenReturn(Optional.of(proveedor));

        ReglaNegocioException inactivo = assertThrows(
                ReglaNegocioException.class,
                () -> ordenCompraService.crear(request)
        );
        assertEquals("El proveedor no existe o está inactivo", inactivo.getMessage());
    }

    @Test
    void crearRechazaProductoInexistenteOInactivo() {
        OrdenCompraRequest request = requestValido();
        when(proveedorRepository.findById(1)).thenReturn(Optional.of(proveedor));
        when(usuarioRepository.findByCorreoAndEstado(usuario.getCorreo(), 1)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(10)).thenReturn(Optional.empty());

        RecursoNoEncontradoException inexistente = assertThrows(
                RecursoNoEncontradoException.class,
                () -> ordenCompraService.crear(request)
        );
        assertEquals("El producto no existe o está inactivo", inexistente.getMessage());

        producto.setEstado((byte) 0);
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));

        ReglaNegocioException inactivo = assertThrows(
                ReglaNegocioException.class,
                () -> ordenCompraService.crear(request)
        );
        assertEquals("El producto no existe o está inactivo", inactivo.getMessage());
    }

    @Test
    void crearRechazaFechaEstimadaAnteriorALaEmision() {
        OrdenCompraRequest request = requestValido();
        request.setFechaEstimadaEntrega(LocalDate.now().minusDays(1));

        ReglaNegocioException error = assertThrows(
                ReglaNegocioException.class,
                () -> ordenCompraService.crear(request)
        );

        assertEquals("La fecha estimada de entrega no puede ser menor que la fecha de emisión", error.getMessage());
    }

    @Test
    void crearRechazaUsuarioAutenticadoNoRegistrado() {
        OrdenCompraRequest request = requestValido();
        when(proveedorRepository.findById(1)).thenReturn(Optional.of(proveedor));
        when(usuarioRepository.findByCorreoAndEstado(usuario.getCorreo(), 1)).thenReturn(Optional.empty());

        AuthenticationCredentialsNotFoundException error = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> ordenCompraService.crear(request)
        );

        assertEquals("Usuario autenticado no encontrado", error.getMessage());
    }

    @Test
    void listarYObtenerDevuelvenDtosConTotalCalculado() {
        OrdenCompra orden = ordenPersistida("Pendiente");
        when(ordenCompraRepository.findAllConDetalles()).thenReturn(List.of(orden));
        when(ordenCompraRepository.findByIdConDetalles(50)).thenReturn(Optional.of(orden));

        List<OrdenCompraResponse> lista = ordenCompraService.listarTodas();
        OrdenCompraResponse detalle = ordenCompraService.obtenerPorId(50);

        assertEquals(1, lista.size());
        assertEquals(new BigDecimal("7.00"), lista.get(0).getTotalOrden());
        assertEquals(50, detalle.getIdOrden());
        assertEquals("Quinua", detalle.getDetalles().get(0).getNomProducto());
    }

    @Test
    void actualizarOrdenPendienteReemplazaDetallesYConservaDatosInmutables() {
        OrdenCompra orden = ordenPersistida("Pendiente");
        Usuario usuarioOriginal = orden.getUsuario();
        LocalDate fechaOriginal = orden.getFechaEmision();
        Proveedor proveedorNuevo = proveedor(2, "Proveedor Nuevo", (byte) 1);
        Producto productoNuevo = producto(12, "SKU-12", "Cañihua", (byte) 1, 14);
        OrdenCompraRequest request = requestValido();
        request.setIdProveedor(2);
        request.setDetalles(List.of(detalleRequest(12, 4, "2.50")));

        when(ordenCompraRepository.findByIdConDetalles(50)).thenReturn(Optional.of(orden));
        when(proveedorRepository.findById(2)).thenReturn(Optional.of(proveedorNuevo));
        when(productoRepository.findById(12)).thenReturn(Optional.of(productoNuevo));
        when(ordenCompraRepository.save(orden)).thenReturn(orden);

        OrdenCompraResponse response = ordenCompraService.actualizar(50, request);

        assertSame(usuarioOriginal, orden.getUsuario());
        assertEquals(fechaOriginal, orden.getFechaEmision());
        assertEquals("Pendiente", orden.getEstado());
        assertEquals(2, response.getIdProveedor());
        assertEquals(1, response.getDetalles().size());
        assertEquals(new BigDecimal("10.00"), response.getTotalOrden());
        assertEquals(14, productoNuevo.getStockActual());
    }

    @Test
    void actualizarConservaElDetalleExistenteCuandoElProductoContinuaEnLaOrden() {
        OrdenCompra orden = ordenPersistida("Pendiente");
        OrdenCompraRequest request = requestValido();
        request.setDetalles(List.of(detalleRequest(10, 5, "4.00")));

        when(ordenCompraRepository.findByIdConDetalles(50)).thenReturn(Optional.of(orden));
        when(proveedorRepository.findById(1)).thenReturn(Optional.of(proveedor));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(ordenCompraRepository.save(orden)).thenReturn(orden);

        OrdenCompraResponse response = ordenCompraService.actualizar(50, request);

        assertEquals(70, response.getDetalles().get(0).getIdDetalle());
        assertEquals(5, response.getDetalles().get(0).getCantidad());
        assertEquals(new BigDecimal("20.00"), response.getTotalOrden());
    }

    @Test
    void actualizarRechazaOrdenRecibida() {
        OrdenCompra orden = ordenPersistida("Recibida");
        when(ordenCompraRepository.findByIdConDetalles(50)).thenReturn(Optional.of(orden));

        ReglaNegocioException error = assertThrows(
                ReglaNegocioException.class,
                () -> ordenCompraService.actualizar(50, requestValido())
        );

        assertEquals("No es posible editar una orden que ya ha sido recibida", error.getMessage());
        verify(ordenCompraRepository, never()).save(any());
    }

    @Test
    void obtenerRechazaOrdenInexistente() {
        when(ordenCompraRepository.findByIdConDetalles(404)).thenReturn(Optional.empty());

        RecursoNoEncontradoException error = assertThrows(
                RecursoNoEncontradoException.class,
                () -> ordenCompraService.obtenerPorId(404)
        );

        assertEquals("Orden de compra no encontrada con ID: 404", error.getMessage());
    }

    private void prepararCreacionValida() {
        when(proveedorRepository.findById(1)).thenReturn(Optional.of(proveedor));
        when(usuarioRepository.findByCorreoAndEstado(usuario.getCorreo(), 1)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
    }

    private OrdenCompraRequest requestValido() {
        OrdenCompraRequest request = new OrdenCompraRequest();
        request.setIdProveedor(1);
        request.setFechaEstimadaEntrega(LocalDate.now().plusDays(5));
        request.setDetalles(new java.util.ArrayList<>(List.of(detalleRequest(10, 2, "3.50"))));
        return request;
    }

    private DetalleOrdenCompraRequest detalleRequest(Integer idProducto, Integer cantidad, String precio) {
        DetalleOrdenCompraRequest detalle = new DetalleOrdenCompraRequest();
        detalle.setIdProducto(idProducto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(new BigDecimal(precio));
        return detalle;
    }

    private OrdenCompra ordenPersistida(String estado) {
        OrdenCompra orden = new OrdenCompra();
        orden.setIdOrden(50);
        orden.setProveedor(proveedor);
        orden.setUsuario(usuario);
        orden.setFechaEmision(LocalDate.now().minusDays(2));
        orden.setFechaEstimadaEntrega(LocalDate.now().plusDays(5));
        orden.setEstado(estado);

        DetalleOrdenCompra detalle = new DetalleOrdenCompra();
        detalle.setIdDetalle(70);
        detalle.setProducto(producto);
        detalle.setCantidad(2);
        detalle.setPrecioUnitario(new BigDecimal("3.50"));
        orden.agregarDetalle(detalle);
        return orden;
    }

    private Proveedor proveedor(Integer id, String nombre, byte estado) {
        Proveedor item = new Proveedor();
        item.setIdProveedor(id);
        item.setNomProveedor(nombre);
        item.setEstado(estado);
        return item;
    }

    private Usuario usuario(Integer id, String nombre, String correo) {
        Usuario item = new Usuario();
        item.setIdUsuario(id);
        item.setNomUsuario(nombre);
        item.setCorreo(correo);
        item.setEstado(1);
        return item;
    }

    private Producto producto(Integer id, String sku, String nombre, byte estado, int stock) {
        return Producto.builder()
                .idProducto(id)
                .sku(sku)
                .nomProducto(nombre)
                .precio(BigDecimal.ONE)
                .stockMinimo(1)
                .stockActual(stock)
                .estado(estado)
                .build();
    }
}
