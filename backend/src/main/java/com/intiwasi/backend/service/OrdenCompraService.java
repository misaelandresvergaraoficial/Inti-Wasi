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
import com.intiwasi.backend.repository.EntradaRepository;
import com.intiwasi.backend.repository.OrdenCompraRepository;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.ProveedorRepository;
import com.intiwasi.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.time.LocalDate;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private static final String ESTADO_CANCELADA = "Cancelada";

    private final OrdenCompraRepository ordenCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntradaRepository entradaRepository;


    @Transactional(readOnly = true)
    public Page<OrdenCompraResponse> consultar(int page, int size, String q, String estado,
            LocalDate desde, LocalDate hasta, String sort, boolean admisibles) {
        if (page < 0 || size < 1 || size > 200) throw new IllegalArgumentException("La página debe ser positiva y admitir entre 1 y 200 registros");
        if (desde != null && hasta != null && desde.isAfter(hasta)) throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        if (!Set.of("asc", "desc").contains(sort)) throw new IllegalArgumentException("Orden de emisión no válido");
        String texto = q == null ? "" : q.trim().toLowerCase(java.util.Locale.ROOT);
        if (texto.length() > 255) throw new IllegalArgumentException("La búsqueda admite hasta 255 caracteres");
        String patron = "%" + texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        Integer idBuscado;
        try { idBuscado = Integer.valueOf(texto); } catch (NumberFormatException ex) { idBuscado = -1; }
        final Integer identidad = idBuscado;
        Specification<OrdenCompra> filtro = (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (admisibles) filtros.add(root.get("estado").in("Pendiente", "Parcial"));
            if (!texto.isEmpty()) filtros.add(cb.or(cb.like(cb.lower(root.get("proveedor").get("nomProveedor")), patron, '\\'), cb.equal(root.get("idOrden"), identidad)));
            if (estado != null && !estado.isBlank()) {
                if (!Set.of("Pendiente", "Parcial", "Recibida", "Cancelada").contains(estado)) throw new IllegalArgumentException("Estado de orden no válido");
                filtros.add(cb.equal(root.get("estado"), estado));
            }
            if (desde != null) filtros.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("fechaEmision"), desde));
            if (hasta != null) filtros.add(cb.lessThanOrEqualTo(root.<LocalDate>get("fechaEmision"), hasta));
            return cb.and(filtros.toArray(Predicate[]::new));
        };
        var orden = Sort.by(OrdenCompra::getFechaEmision, OrdenCompra::getIdOrden);
        return ordenCompraRepository.findAll(filtro, PageRequest.of(page, size,
                "asc".equals(sort) ? orden.ascending() : orden.descending())).map(this::convertirAResponse);
    }

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
        LocalDate fechaEmision = LocalDate.now(java.time.ZoneId.of("America/Lima"));
        validarFechaEstimada(request.getFechaEstimadaEntrega(), fechaEmision);
        validarDetalles(request.getDetalles());

        Proveedor proveedor = buscarProveedorActivo(request.getIdProveedor());
        Usuario usuario = buscarUsuarioAutenticado();

        List<DetalleOrdenCompra> detalles = construirDetalles(
                request.getDetalles(), proveedor
        );

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
        OrdenCompra orden = ordenCompraRepository.findByIdConDetallesForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Orden de compra no encontrada con ID: " + id
                ));

        if (!ESTADO_PENDIENTE.equals(orden.getEstado())
                || entradaRepository.existsByOrdenCompra_IdOrden(id)) {
            throw new ReglaNegocioException(
                    "Solo se puede editar una orden pendiente sin entradas registradas"
            );
        }

        validarFechaEstimada(
                request.getFechaEstimadaEntrega(), orden.getFechaEmision()
        );
        validarDetalles(request.getDetalles());

        Proveedor proveedor = buscarProveedorActivo(request.getIdProveedor());
        List<DetalleOrdenCompra> detallesNuevos = construirDetalles(
                request.getDetalles(), proveedor
        );

        orden.setProveedor(proveedor);
        orden.setFechaEstimadaEntrega(request.getFechaEstimadaEntrega());
        reemplazarDetalles(orden, detallesNuevos);

        return convertirAResponse(ordenCompraRepository.save(orden));
    }

    @Transactional
    public void cancelar(Integer id) {
        OrdenCompra orden = ordenCompraRepository.findByIdConDetallesForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Orden de compra no encontrada con ID: " + id
                ));

        if (ESTADO_RECIBIDA.equals(orden.getEstado())
                || ESTADO_CANCELADA.equals(orden.getEstado())) {
            throw new ReglaNegocioException(
                    "Solo se puede cancelar una orden pendiente o parcial"
            );
        }

        orden.setEstado(ESTADO_CANCELADA);
        ordenCompraRepository.saveAndFlush(orden);
    }

    private OrdenCompra buscarOrden(Integer id) {
        return ordenCompraRepository.findByIdConDetalles(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Orden de compra no encontrada con ID: " + id
                ));
    }

    private Proveedor buscarProveedorActivo(Integer idProveedor) {
        if (idProveedor == null) {
            throw new IllegalArgumentException(
                    "El proveedor no existe o está inactivo"
            );
        }

        Proveedor proveedor = proveedorRepository.findById(idProveedor)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El proveedor no existe o está inactivo"
                ));

        if (!Byte.valueOf((byte) 1).equals(proveedor.getEstado())) {
            throw new ReglaNegocioException(
                    "El proveedor no existe o está inactivo"
            );
        }

        return proveedor;
    }

    private Usuario buscarUsuarioAutenticado() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Usuario autenticado no encontrado"
            );
        }

        return usuarioRepository.findByCorreoAndEstado(
                        authentication.getName(), 1
                )
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "Usuario autenticado no encontrado"
                ));
    }

    private List<DetalleOrdenCompra> construirDetalles(
            List<DetalleOrdenCompraRequest> requests,
            Proveedor proveedor
    ) {
        List<DetalleOrdenCompra> detalles = new ArrayList<>();

        for (DetalleOrdenCompraRequest request : requests) {
            Producto producto = productoRepository.findById(request.getIdProducto())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "El producto no existe o está inactivo"
                    ));

            if (!Byte.valueOf((byte) 1).equals(producto.getEstado())) {
                throw new ReglaNegocioException(
                        "El producto no existe o está inactivo"
                );
            }

            if (producto.getProveedor() == null
                    || !proveedor.getIdProveedor().equals(
                            producto.getProveedor().getIdProveedor()
                    )) {
                throw new ReglaNegocioException(
                        "El producto " + producto.getSku()
                                + " no está asignado al proveedor de la orden"
                );
            }

            DetalleOrdenCompra detalle = new DetalleOrdenCompra();
            detalle.setProducto(producto);
            detalle.setCantidad(request.getCantidad());
            detalle.setPrecioUnitario(request.getPrecioUnitario());
            detalles.add(detalle);
        }

        return detalles;
    }

    private void reemplazarDetalles(
            OrdenCompra orden,
            List<DetalleOrdenCompra> detallesNuevos
    ) {
        Map<Integer, DetalleOrdenCompra> existentesPorProducto =
                orden.getDetalles().stream()
                        .collect(Collectors.toMap(
                                detalle -> detalle.getProducto().getIdProducto(),
                                Function.identity()
                        ));

        Set<Integer> productosSolicitados = detallesNuevos.stream()
                .map(detalle -> detalle.getProducto().getIdProducto())
                .collect(Collectors.toSet());

        orden.getDetalles().removeIf(
                detalle -> !productosSolicitados.contains(
                        detalle.getProducto().getIdProducto()
                )
        );

        for (DetalleOrdenCompra detalleNuevo : detallesNuevos) {
            Integer idProducto = detalleNuevo.getProducto().getIdProducto();
            DetalleOrdenCompra detalleExistente =
                    existentesPorProducto.get(idProducto);

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
        if (requests == null || requests.isEmpty() || requests.size() > 100) {
            throw new ReglaNegocioException(
                    "La orden debe contener entre 1 y 100 productos"
            );
        }

        Set<Integer> productosIncluidos = new HashSet<>();

        for (DetalleOrdenCompraRequest request : requests) {
            if (request == null || request.getIdProducto() == null) {
                throw new IllegalArgumentException(
                        "El producto no existe o está inactivo"
                );
            }

            if (!productosIncluidos.add(request.getIdProducto())) {
                throw new ReglaNegocioException(
                        "El producto no puede repetirse dentro de la orden"
                );
            }

            if (request.getCantidad() == null || request.getCantidad() <= 0) {
                throw new IllegalArgumentException(
                        "La cantidad debe ser mayor que cero"
                );
            }

            if (request.getPrecioUnitario() == null
                    || request.getPrecioUnitario().signum() < 0) {
                throw new IllegalArgumentException(
                        "El precio unitario no puede ser negativo"
                );
            }
        }
    }

    private void validarFechaEstimada(
            LocalDate fechaEstimada,
            LocalDate fechaEmision
    ) {
        if (fechaEstimada != null && fechaEstimada.isBefore(fechaEmision)) {
            throw new ReglaNegocioException(
                    "La fecha estimada de entrega no puede ser menor "
                            + "que la fecha de emisión"
            );
        }
    }

    private OrdenCompraResponse convertirAResponse(OrdenCompra orden) {
        List<DetalleOrdenCompraResponse> detalles = orden.getDetalles().stream()
                .map(detalle -> convertirDetalleAResponse(orden, detalle))
                .toList();

        BigDecimal total = BigDecimal.ZERO;

        for (DetalleOrdenCompraResponse detalle : detalles) {
            total = total.add(detalle.getSubtotal());
        }

        return OrdenCompraResponse.builder()
                .idOrden(orden.getIdOrden())
                .idProveedor(orden.getProveedor().getIdProveedor())
                .nomProveedor(orden.getProveedor().getNomProveedor())
                .rucProveedor(orden.getProveedor().getRuc())
                .prefijoGuia(orden.getProveedor().getPrefijoGuia())
                .idUsuario(orden.getUsuario().getIdUsuario())
                .nomUsuario(orden.getUsuario().getNomUsuario())
                .correoUsuario(orden.getUsuario().getCorreo())
                .fechaEmision(orden.getFechaEmision())
                .fechaEstimadaEntrega(orden.getFechaEstimadaEntrega())
                .estado(orden.getEstado())
                .editable(ESTADO_PENDIENTE.equals(orden.getEstado())
                        && !entradaRepository.existsByOrdenCompra_IdOrden(orden.getIdOrden()))
                .detalles(detalles)
                .totalOrden(total)
                .build();
    }

    private DetalleOrdenCompraResponse convertirDetalleAResponse(
            OrdenCompra orden,
            DetalleOrdenCompra detalle
    ) {
        BigDecimal subtotal = detalle.getPrecioUnitario()
                .multiply(BigDecimal.valueOf(detalle.getCantidad()));

        Integer recibida = entradaRepository.cantidadRecibida(
                orden.getIdOrden(),
                detalle.getProducto().getIdProducto()
        );

        if (recibida == null) {
            recibida = 0;
        }

        return DetalleOrdenCompraResponse.builder()
                .idDetalle(detalle.getIdDetalle())
                .idProducto(detalle.getProducto().getIdProducto())
                .sku(detalle.getProducto().getSku())
                .nomProducto(detalle.getProducto().getNomProducto())
                .cantidad(detalle.getCantidad())
                .cantidadRecibida(recibida)
                .cantidadPorRecibir(
                        ESTADO_CANCELADA.equals(orden.getEstado())
                                ? 0
                                : detalle.getCantidad() - recibida
                )
                .precioUnitario(detalle.getPrecioUnitario())
                .subtotal(subtotal)
                .build();
    }
}
