package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.inventario.AjusteRequest;
import com.intiwasi.backend.dto.inventario.AjusteResponse;
import com.intiwasi.backend.dto.inventario.MovimientoRequest;
import com.intiwasi.backend.dto.inventario.MovimientoResponse;
import com.intiwasi.backend.entity.Ajuste;
import com.intiwasi.backend.entity.Documento;
import com.intiwasi.backend.entity.MovimientoInventario;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.entity.enums.TipoAjuste;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.repository.AjusteRepository;
import com.intiwasi.backend.repository.DocumentoRepository;
import com.intiwasi.backend.repository.MovimientoInventarioRepository;
import com.intiwasi.backend.repository.ProductoRepository;
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
public class AjusteService {
    private final AjusteRepository ajusteRepository;
    private final DocumentoRepository documentoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<AjusteResponse> listar() {
        return ajusteRepository.findAllConRelaciones().stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public AjusteResponse obtener(Integer id) {
        return convertir(buscar(id));
    }

    @Transactional
    public AjusteResponse registrar(AjusteRequest request) {
        validarRequest(request);
        List<Integer> ids = request.getMovimientos().stream()
                .map(movimiento -> movimiento.getIdProducto())
                .sorted()
                .toList();
        List<Producto> bloqueados = productoRepository.findAllByIdForUpdate(ids);
        if (bloqueados.size() != ids.size()) throw new RecursoNoEncontradoException("Uno o más productos no existen");
        Map<Integer, Producto> productos = bloqueados.stream()
                .collect(Collectors.toMap(producto -> producto.getIdProducto(), Function.identity()));
        for (MovimientoRequest item : request.getMovimientos()) {
            Producto producto = productos.get(item.getIdProducto());
            if (!Byte.valueOf((byte) 1).equals(producto.getEstado())) throw new ReglaNegocioException("El producto está inactivo: " + producto.getIdProducto());
            if (request.getTipoAjuste() == TipoAjuste.DECREMENTO && producto.getStockActual() < item.getCantidad()) {
                throw new ReglaNegocioException("El decremento supera el stock del producto: " + producto.getIdProducto());
            }
        }

        Documento documento = new Documento();
        documento.setTipoDocumento(TipoDocumento.AJUSTE);
        documento.setUsuario(usuarioActual());
        documento.setEstado((byte) 1);
        documento = documentoRepository.saveAndFlush(documento);

        Ajuste ajuste = new Ajuste();
        ajuste.setDocumento(documento);
        ajuste.setTipoAjuste(request.getTipoAjuste());
        ajuste.setMotivo(request.getMotivo().trim());
        ajuste.setObservaciones(normalizar(request.getObservaciones()));
        ajuste = ajusteRepository.saveAndFlush(ajuste);

        for (MovimientoRequest item : request.getMovimientos().stream()
                .sorted(Comparator.comparing(movimiento -> movimiento.getIdProducto()))
                .toList()) {
            MovimientoInventario movimiento = new MovimientoInventario();
            movimiento.setDocumento(documento);
            movimiento.setProducto(productos.get(item.getIdProducto()));
            movimiento.setCantidad(item.getCantidad());
            movimientoRepository.save(movimiento);
        }
        movimientoRepository.flush();
        Integer idAjuste = ajuste.getIdAjuste();
        entityManager.clear();
        return convertir(buscar(idAjuste));
    }

    private Ajuste buscar(Integer id) {
        return ajusteRepository.findByIdConRelaciones(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ajuste no encontrado con ID: " + id));
    }

    private AjusteResponse convertir(Ajuste ajuste) {
        List<MovimientoResponse> movimientos = movimientoRepository
                .findByDocumento_IdDocumentoOrderByIdMovimientoAsc(ajuste.getDocumento().getIdDocumento())
                .stream().map(this::convertirMovimiento).toList();
        return AjusteResponse.builder().idAjuste(ajuste.getIdAjuste())
                .idDocumento(ajuste.getDocumento().getIdDocumento()).tipoAjuste(ajuste.getTipoAjuste())
                .motivo(ajuste.getMotivo()).observaciones(ajuste.getObservaciones())
                .fechaEmision(ajuste.getDocumento().getFechaEmision())
                .idUsuario(ajuste.getDocumento().getUsuario().getIdUsuario())
                .usuarioResponsable(ajuste.getDocumento().getUsuario().getNomUsuario())
                .movimientos(movimientos).build();
    }

    private MovimientoResponse convertirMovimiento(MovimientoInventario movimiento) {
        Producto p = movimiento.getProducto();
        return MovimientoResponse.builder().idMovimiento(movimiento.getIdMovimiento())
                .idProducto(p.getIdProducto()).sku(p.getSku()).nomProducto(p.getNomProducto())
                .cantidad(movimiento.getCantidad()).stockActual(p.getStockActual()).build();
    }

    private void validarRequest(AjusteRequest request) {
        if (request == null || request.getTipoAjuste() == null) throw new IllegalArgumentException("El tipo de ajuste es obligatorio");
        if (request.getMotivo() == null || request.getMotivo().isBlank()) throw new IllegalArgumentException("El motivo del ajuste es obligatorio");
        if (request.getMotivo().length() > 255) throw new IllegalArgumentException("El motivo no debe exceder 255 caracteres");
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

    private String normalizar(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }
}
