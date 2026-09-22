package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.OrdenCompra.DetalleOrdenCompraRequest;
import com.intiwasi.backend.dto.OrdenCompra.DetalleOrdenCompraResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdenCompraService {

    private static final String ESTADO_PENDIENTE = "Pendiente";
    private static final String ESTADO_RECIBIDA = "Recibida";

    private final OrdenCompraRepository ordenCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<OrdenCompraResponse> listarTodas() {
        return ordenCompraRepository.findAllConDetalles().stream()
                .map(this::convertirAResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrdenCompraResponse obtenerPorId(Integer id) {
        return convertirAResponse(buscarOrden(id));
    }

    @Transactional
    public OrdenCompraResponse crear(OrdenCompraRequest request) {
        LocalDate fechaEmision = LocalDate.now();
        validarFechaEstimada(request.getFechaEstimadaEntrega(), fechaEmision);
        validarDetalles(request.getDetalles());

        Proveedor proveedor = buscarProveedorActivo(request.getIdProveedor());
        Usuario usuario = buscarUsuarioAutenticado();
        List<DetalleOrdenCompra> detalles = construirDetalles(request.getDetalles());

        OrdenCompra orden = new OrdenCompra();
        orden.setProveedor(proveedor);
        orden.setUsuario(usuario);
        orden.setFechaEmision(fechaEmision);
        orden.setFechaEstimadaEntrega(request.getFechaEstimadaEntrega());
        orden.setEstado(ESTADO_PENDIENTE);
        detalles.forEach(orden::agregarDetalle);

        return convertirAResponse(ordenCompraRepository.save(orden));
    }

    @Transactional
    public OrdenCompraResponse actualizar(Integer id, OrdenCompraRequest request) {
        OrdenCompra orden = buscarOrden(id);
        if (ESTADO_RECIBIDA.equals(orden.getEstado())) {
            throw new ReglaNegocioException("No es posible editar una orden que ya ha sido recibida");
        }

        validarFechaEstimada(request.getFechaEstimadaEntrega(), orden.getFechaEmision());
        validarDetalles(request.getDetalles());
        Proveedor proveedor = buscarProveedorActivo(request.getIdProveedor());
        List<DetalleOrdenCompra> detallesNuevos = construirDetalles(request.getDetalles());

        orden.setProveedor(proveedor);
        orden.setFechaEstimadaEntrega(request.getFechaEstimadaEntrega());
        reemplazarDetalles(orden, detallesNuevos);

        return convertirAResponse(ordenCompraRepository.save(orden));
    }

    private OrdenCompra buscarOrden(Integer id) {
        return ordenCompraRepository.findByIdConDetalles(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Orden de compra no encontrada con ID: " + id
                ));
    }

    private Proveedor buscarProveedorActivo(Integer idProveedor) {
        if (idProveedor == null) {
            throw new IllegalArgumentException("El proveedor no existe o está inactivo");
        }

        Proveedor proveedor = proveedorRepository.findById(idProveedor)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El proveedor no existe o está inactivo"
                ));
        if (!Byte.valueOf((byte) 1).equals(proveedor.getEstado())) {
            throw new ReglaNegocioException("El proveedor no existe o está inactivo");
        }
        return proveedor;
    }

    private Usuario buscarUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new AuthenticationCredentialsNotFoundException("Usuario autenticado no encontrado");
        }

        return usuarioRepository.findByCorreoAndEstado(authentication.getName(), 1)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario autenticado no encontrado"
                ));
    }

    private List<DetalleOrdenCompra> construirDetalles(List<DetalleOrdenCompraRequest> requests) {
        List<DetalleOrdenCompra> detalles = new ArrayList<>();

        for (DetalleOrdenCompraRequest request : requests) {
            Producto producto = productoRepository.findById(request.getIdProducto())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "El producto no existe o está inactivo"
                    ));
            if (!Byte.valueOf((byte) 1).equals(producto.getEstado())) {
                throw new ReglaNegocioException("El producto no existe o está inactivo");
            }

            DetalleOrdenCompra detalle = new DetalleOrdenCompra();
            detalle.setProducto(producto);
            detalle.setCantidad(request.getCantidad());
            detalle.setPrecioUnitario(request.getPrecioUnitario());
            detalles.add(detalle);
        }

        return detalles;
    }

    private void reemplazarDetalles(OrdenCompra orden, List<DetalleOrdenCompra> detallesNuevos) {
        Map<Integer, DetalleOrdenCompra> existentesPorProducto = orden.getDetalles().stream()
                .collect(Collectors.toMap(
                        detalle -> detalle.getProducto().getIdProducto(),
                        Function.identity()
                ));

        Set<Integer> productosSolicitados = detallesNuevos.stream()
                .map(detalle -> detalle.getProducto().getIdProducto())
                .collect(Collectors.toSet());

        orden.getDetalles().removeIf(
                detalle -> !productosSolicitados.contains(detalle.getProducto().getIdProducto())
        );

        for (DetalleOrdenCompra detalleNuevo : detallesNuevos) {
            Integer idProducto = detalleNuevo.getProducto().getIdProducto();
            DetalleOrdenCompra detalleExistente = existentesPorProducto.get(idProducto);

            if (detalleExistente != null) {
                detalleExistente.setProducto(detalleNuevo.getProducto());
                detalleExistente.setCantidad(detalleNuevo.getCantidad());
                detalleExistente.setPrecioUnitario(detalleNuevo.getPrecioUnitario());
            } else {
                orden.agregarDetalle(detalleNuevo);
            }
        }
    }

    private void validarDetalles(List<DetalleOrdenCompraRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ReglaNegocioException("La orden debe contener al menos un producto");
        }

        Set<Integer> productosIncluidos = new HashSet<>();

        for (DetalleOrdenCompraRequest request : requests) {
            if (request == null || request.getIdProducto() == null) {
                throw new IllegalArgumentException("El producto no existe o está inactivo");
            }
            if (!productosIncluidos.add(request.getIdProducto())) {
                throw new ReglaNegocioException("El producto no puede repetirse dentro de la orden");
            }
            if (request.getCantidad() == null || request.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
            }
            if (request.getPrecioUnitario() == null || request.getPrecioUnitario().signum() < 0) {
                throw new IllegalArgumentException("El precio unitario no puede ser negativo");
            }
        }
    }

    private void validarFechaEstimada(LocalDate fechaEstimada, LocalDate fechaEmision) {
        if (fechaEstimada != null && fechaEstimada.isBefore(fechaEmision)) {
            throw new ReglaNegocioException(
                    "La fecha estimada de entrega no puede ser menor que la fecha de emisión"
            );
        }
    }

    private OrdenCompraResponse convertirAResponse(OrdenCompra orden) {
        List<DetalleOrdenCompraResponse> detalles = orden.getDetalles().stream()
                .map(this::convertirDetalleAResponse)
                .toList();

        BigDecimal total = BigDecimal.ZERO;
        for (DetalleOrdenCompraResponse detalle : detalles) {
            total = total.add(detalle.getSubtotal());
        }

        return OrdenCompraResponse.builder()
                .idOrden(orden.getIdOrden())
                .idProveedor(orden.getProveedor().getIdProveedor())
                .nomProveedor(orden.getProveedor().getNomProveedor())
                .idUsuario(orden.getUsuario().getIdUsuario())
                .nomUsuario(orden.getUsuario().getNomUsuario())
                .fechaEmision(orden.getFechaEmision())
                .fechaEstimadaEntrega(orden.getFechaEstimadaEntrega())
                .estado(orden.getEstado())
                .detalles(detalles)
                .totalOrden(total)
                .build();
    }

    private DetalleOrdenCompraResponse convertirDetalleAResponse(DetalleOrdenCompra detalle) {
        BigDecimal subtotal = detalle.getPrecioUnitario()
                .multiply(BigDecimal.valueOf(detalle.getCantidad()));

        return DetalleOrdenCompraResponse.builder()
                .idDetalle(detalle.getIdDetalle())
                .idProducto(detalle.getProducto().getIdProducto())
                .sku(detalle.getProducto().getSku())
                .nomProducto(detalle.getProducto().getNomProducto())
                .cantidad(detalle.getCantidad())
                .precioUnitario(detalle.getPrecioUnitario())
                .subtotal(subtotal)
                .build();
    }
}
