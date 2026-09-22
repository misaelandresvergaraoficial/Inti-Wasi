package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.inventario.EntradaRequest;
import com.intiwasi.backend.dto.inventario.EntradaResponse;
import com.intiwasi.backend.dto.inventario.MovimientoRequest;
import com.intiwasi.backend.dto.inventario.MovimientoResponse;
import com.intiwasi.backend.entity.DetalleOrdenCompra;
import com.intiwasi.backend.entity.Documento;
import com.intiwasi.backend.entity.Entrada;
import com.intiwasi.backend.entity.MovimientoInventario;
import com.intiwasi.backend.entity.OrdenCompra;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import com.intiwasi.backend.exception.ConflictoException;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.repository.DocumentoRepository;
import com.intiwasi.backend.repository.EntradaRepository;
import com.intiwasi.backend.repository.MovimientoInventarioRepository;
import com.intiwasi.backend.repository.OrdenCompraRepository;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntradaService {
    private final EntradaRepository entradaRepository;
    private final DocumentoRepository documentoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<EntradaResponse> listar() {
        return entradaRepository.findAllConRelaciones().stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public EntradaResponse obtener(Integer id) {
        return convertir(buscarEntrada(id));
    }

    @Transactional
    public EntradaResponse registrar(EntradaRequest request) {
        validarRequest(request);
        OrdenCompra orden = ordenCompraRepository.findByIdConDetallesForUpdate(request.getIdOrden())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Orden de compra no encontrada con ID: " + request.getIdOrden()));

        if (!"Pendiente".equals(orden.getEstado())) {
            throw new ReglaNegocioException("La orden de compra ya fue recibida");
        }
        if (entradaRepository.existsByOrdenCompra_IdOrden(orden.getIdOrden())) {
            throw new ConflictoException("La orden de compra ya tiene una entrada registrada");
        }

        Map<Integer, DetalleOrdenCompra> detallePorProducto = orden.getDetalles().stream()
                .collect(Collectors.toMap(d -> d.getProducto().getIdProducto(), Function.identity()));
        Map<Integer, Producto> productos = request.getMovimientos().stream()
                .map(movimiento -> movimiento.getIdProducto())
                .distinct()
                .map(id -> productoRepository.findById(id)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado con ID: " + id)))
                .collect(Collectors.toMap(producto -> producto.getIdProducto(), Function.identity()));

        for (MovimientoRequest movimiento : request.getMovimientos()) {
            Producto producto = productos.get(movimiento.getIdProducto());
            if (!Byte.valueOf((byte) 1).equals(producto.getEstado())) {
                throw new ReglaNegocioException("El producto está inactivo: " + producto.getIdProducto());
            }
            DetalleOrdenCompra detalle = detallePorProducto.get(producto.getIdProducto());
            if (detalle == null) {
                throw new ReglaNegocioException("El producto no pertenece a la orden de compra: " + producto.getIdProducto());
            }
            if (movimiento.getCantidad() > detalle.getCantidad()) {
                throw new ReglaNegocioException("La cantidad recibida supera la solicitada para el producto: " + producto.getIdProducto());
            }
        }

        Documento documento = nuevoDocumento(TipoDocumento.ENTRADA, usuarioActual());
        documento = documentoRepository.saveAndFlush(documento);

        Entrada entrada = new Entrada();
        entrada.setDocumento(documento);
        entrada.setOrdenCompra(orden);
        entrada.setDocumentoRef(request.getDocumentoRef().trim());
        entrada.setObservaciones(normalizar(request.getObservaciones()));
        entrada = entradaRepository.saveAndFlush(entrada);

        for (MovimientoRequest item : request.getMovimientos()) {
            MovimientoInventario movimiento = new MovimientoInventario();
            movimiento.setDocumento(documento);
            movimiento.setProducto(productos.get(item.getIdProducto()));
            movimiento.setCantidad(item.getCantidad());
            movimientoRepository.save(movimiento);
        }
        movimientoRepository.flush();

        Integer idEntrada = entrada.getIdEntrada();
        entityManager.clear();
        return convertir(buscarEntrada(idEntrada));
    }

    private Entrada buscarEntrada(Integer id) {
        return entradaRepository.findByIdConRelaciones(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrada no encontrada con ID: " + id));
    }

    private EntradaResponse convertir(Entrada entrada) {
        List<MovimientoResponse> movimientos = movimientoRepository
                .findByDocumento_IdDocumentoOrderByIdMovimientoAsc(entrada.getDocumento().getIdDocumento())
                .stream().map(this::convertirMovimiento).toList();
        return EntradaResponse.builder()
                .idEntrada(entrada.getIdEntrada())
                .idDocumento(entrada.getDocumento().getIdDocumento())
                .idOrden(entrada.getOrdenCompra().getIdOrden())
                .documentoRef(entrada.getDocumentoRef())
                .observaciones(entrada.getObservaciones())
                .fechaEmision(entrada.getDocumento().getFechaEmision())
                .idUsuario(entrada.getDocumento().getUsuario().getIdUsuario())
                .usuarioResponsable(entrada.getDocumento().getUsuario().getNomUsuario())
                .estadoOrden(entrada.getOrdenCompra().getEstado())
                .movimientos(movimientos)
                .build();
    }

    private MovimientoResponse convertirMovimiento(MovimientoInventario movimiento) {
        Producto p = movimiento.getProducto();
        return MovimientoResponse.builder().idMovimiento(movimiento.getIdMovimiento())
                .idProducto(p.getIdProducto()).sku(p.getSku()).nomProducto(p.getNomProducto())
                .cantidad(movimiento.getCantidad()).stockActual(p.getStockActual()).build();
    }

    private void validarRequest(EntradaRequest request) {
        if (request == null || request.getIdOrden() == null) {
            throw new IllegalArgumentException("La orden de compra es obligatoria");
        }
        if (request.getDocumentoRef() == null || request.getDocumentoRef().isBlank()
                || request.getDocumentoRef().length() > 50) {
            throw new IllegalArgumentException("El documento de referencia es obligatorio y no debe exceder 50 caracteres");
        }
        validarMovimientos(request.getMovimientos());
    }

    private void validarMovimientos(List<MovimientoRequest> movimientos) {
        if (movimientos == null || movimientos.isEmpty()) {
            throw new IllegalArgumentException("Debe incluir al menos un movimiento");
        }
        Set<Integer> ids = new HashSet<>();
        for (MovimientoRequest m : movimientos) {
            if (m == null || m.getIdProducto() == null) throw new IllegalArgumentException("El producto es obligatorio");
            if (m.getCantidad() == null || m.getCantidad() <= 0) throw new IllegalArgumentException("La cantidad debe ser entera y mayor que cero");
            if (!ids.add(m.getIdProducto())) throw new ReglaNegocioException("No se permiten productos repetidos");
        }
    }

    private Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new AuthenticationCredentialsNotFoundException("Usuario autenticado no encontrado");
        }
        return usuarioRepository.findByCorreoAndEstado(auth.getName(), 1)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Usuario autenticado no encontrado"));
    }

    private Documento nuevoDocumento(TipoDocumento tipo, Usuario usuario) {
        Documento documento = new Documento();
        documento.setTipoDocumento(tipo);
        documento.setUsuario(usuario);
        documento.setEstado((byte) 1);
        return documento;
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
