package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.inventario.MovimientoRequest;
import com.intiwasi.backend.dto.inventario.MovimientoResponse;
import com.intiwasi.backend.dto.inventario.SalidaRequest;
import com.intiwasi.backend.dto.inventario.SalidaResponse;
import com.intiwasi.backend.entity.Documento;
import com.intiwasi.backend.entity.MovimientoInventario;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.entity.Salida;
import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.repository.DocumentoRepository;
import com.intiwasi.backend.repository.MovimientoInventarioRepository;
import com.intiwasi.backend.repository.ProductoRepository;
import com.intiwasi.backend.repository.SalidaRepository;
import com.intiwasi.backend.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalidaService {
    private final SalidaRepository salidaRepository;
    private final DocumentoRepository documentoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<SalidaResponse> listar() {
        return salidaRepository.findAllConRelaciones().stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public SalidaResponse obtener(Integer id) {
        return convertir(buscar(id));
    }

    @Transactional
    public SalidaResponse registrar(SalidaRequest request) {
        validarRequest(request);
        List<Integer> ids = request.getMovimientos().stream()
                .map(movimiento -> movimiento.getIdProducto())
                .sorted().toList();
        List<Producto> bloqueados = productoRepository.findAllByIdForUpdate(ids);
        if (bloqueados.size() != ids.size()) {
            throw new RecursoNoEncontradoException("Uno o más productos no existen");
        }
        Map<Integer, Producto> productos = bloqueados.stream()
                .collect(Collectors.toMap(producto -> producto.getIdProducto(), Function.identity()));
        for (MovimientoRequest item : request.getMovimientos()) {
            Producto producto = productos.get(item.getIdProducto());
            if (!Byte.valueOf((byte) 1).equals(producto.getEstado())) {
                throw new ReglaNegocioException("El producto está inactivo: " + producto.getIdProducto());
            }
            if (producto.getStockActual() < item.getCantidad()) {
                throw new ReglaNegocioException("Stock insuficiente para el producto: " + producto.getIdProducto());
            }
        }

        Documento documento = new Documento();
        documento.setTipoDocumento(TipoDocumento.SALIDA);
        documento.setUsuario(usuarioActual());
        documento.setEstado((byte) 1);
        documento = documentoRepository.saveAndFlush(documento);

        Salida salida = new Salida();
        salida.setDocumento(documento);
        salida.setMotivo(request.getMotivo());
        salida.setObservaciones(normalizar(request.getObservaciones()));
        salida = salidaRepository.saveAndFlush(salida);

        for (MovimientoRequest item : request.getMovimientos().stream()
                .sorted(Comparator.comparing(movimiento -> movimiento.getIdProducto())).toList()) {
            MovimientoInventario movimiento = new MovimientoInventario();
            movimiento.setDocumento(documento);
            movimiento.setProducto(productos.get(item.getIdProducto()));
            movimiento.setCantidad(item.getCantidad());
            movimientoRepository.save(movimiento);
        }
        movimientoRepository.flush();
        Integer idSalida = salida.getIdSalida();
        entityManager.clear();
        return convertir(buscar(idSalida));
    }

    private Salida buscar(Integer id) {
        return salidaRepository.findByIdConRelaciones(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salida no encontrada con ID: " + id));
    }

    private SalidaResponse convertir(Salida salida) {
        List<MovimientoResponse> movimientos = movimientoRepository
                .findByDocumento_IdDocumentoOrderByIdMovimientoAsc(salida.getDocumento().getIdDocumento())
                .stream().map(this::convertirMovimiento).toList();
        return SalidaResponse.builder().idSalida(salida.getIdSalida())
                .idDocumento(salida.getDocumento().getIdDocumento()).motivo(salida.getMotivo())
                .observaciones(salida.getObservaciones()).fechaEmision(salida.getDocumento().getFechaEmision())
                .idUsuario(salida.getDocumento().getUsuario().getIdUsuario())
                .usuarioResponsable(salida.getDocumento().getUsuario().getNomUsuario())
                .movimientos(movimientos).build();
    }

    private MovimientoResponse convertirMovimiento(MovimientoInventario movimiento) {
        Producto p = movimiento.getProducto();
        return MovimientoResponse.builder().idMovimiento(movimiento.getIdMovimiento())
                .idProducto(p.getIdProducto()).sku(p.getSku()).nomProducto(p.getNomProducto())
                .cantidad(movimiento.getCantidad()).stockActual(p.getStockActual()).build();
    }

    private void validarRequest(SalidaRequest request) {
        if (request == null || request.getMotivo() == null) throw new IllegalArgumentException("El motivo de salida es obligatorio");
        if (request.getObservaciones() != null && request.getObservaciones().length() > 255) throw new IllegalArgumentException("Las observaciones no deben exceder 255 caracteres");
        validarMovimientos(request.getMovimientos());
    }

    private void validarMovimientos(List<MovimientoRequest> movimientos) {
        if (movimientos == null || movimientos.isEmpty()) throw new IllegalArgumentException("Debe incluir al menos un movimiento");
        Set<Integer> ids = new HashSet<>();
        for (MovimientoRequest m : movimientos) {
            if (m == null || m.getIdProducto() == null) throw new IllegalArgumentException("El producto es obligatorio");
            if (m.getCantidad() == null || m.getCantidad() <= 0) throw new IllegalArgumentException("La cantidad debe ser entera y mayor que cero");
            if (!ids.add(m.getIdProducto())) throw new ReglaNegocioException("No se permiten productos repetidos");
        }
    }

    private Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) throw new AuthenticationCredentialsNotFoundException("Usuario autenticado no encontrado");
        return usuarioRepository.findByCorreoAndEstado(auth.getName(), 1)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Usuario autenticado no encontrado"));
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
