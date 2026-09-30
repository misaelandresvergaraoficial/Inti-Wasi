package com.intiwasi.backend.service;

import com.intiwasi.backend.entity.Documento;
import com.intiwasi.backend.entity.MovimientoInventario;
import com.intiwasi.backend.entity.Producto;
import com.intiwasi.backend.entity.Usuario;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import com.intiwasi.backend.entity.enums.TipoAjuste;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
import com.intiwasi.backend.exception.ReglaNegocioException;
import com.intiwasi.backend.repository.DocumentoRepository;
import com.intiwasi.backend.repository.AjusteRepository;
import com.intiwasi.backend.repository.MovimientoInventarioRepository;
import com.intiwasi.backend.repository.ProductoRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CorreccionInventarioService {
    private final DocumentoRepository documentoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;
    private final AjusteRepository ajusteRepository;
    private final EntityManager entityManager;

    @Transactional
    public Documento revertir(Integer idDocumento, TipoDocumento tipoEsperado, Usuario actor,
                              String motivo, byte estadoFinal) {
        if (motivo == null || motivo.isBlank() || motivo.length() > 255) {
            throw new IllegalArgumentException("El motivo de corrección es obligatorio y no debe exceder 255 caracteres");
        }
        Documento original = documentoRepository.findByIdForUpdate(idDocumento)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado con ID: " + idDocumento));
        if (original.getTipoDocumento() != tipoEsperado || original.getEstado() != 1) {
            throw new ReglaNegocioException("Solo se puede rectificar o anular un documento vigente del tipo solicitado");
        }

        List<MovimientoInventario> movimientos = movimientoRepository
                .findByDocumento_IdDocumentoOrderByIdMovimientoAsc(idDocumento);
        if (movimientos.isEmpty()) {
            throw new ReglaNegocioException("El documento no tiene movimientos que revertir");
        }
        List<Integer> ids = movimientos.stream().map(m -> m.getProducto().getIdProducto()).distinct().sorted().toList();
        List<Producto> bloqueados = productoRepository.findAllByIdForUpdate(ids);
        if (bloqueados.size() != ids.size()) {
            throw new RecursoNoEncontradoException("Uno o más productos del documento no existen");
        }
        Map<Integer, Producto> productos = bloqueados.stream()
                .collect(Collectors.toMap(producto -> producto.getIdProducto(), Function.identity()));
        boolean reversoDescuenta = tipoEsperado == TipoDocumento.ENTRADA
                || (tipoEsperado == TipoDocumento.AJUSTE && ajusteRepository
                .findByDocumento_IdDocumento(idDocumento)
                .map(ajuste -> ajuste.getTipoAjuste()).orElse(null) == TipoAjuste.INCREMENTO);
        for (MovimientoInventario anterior : movimientos) {
            Producto producto = productos.get(anterior.getProducto().getIdProducto());
            entityManager.refresh(producto);
            if (reversoDescuenta && producto.getStockActual() < anterior.getCantidad()) {
                throw new ReglaNegocioException("La corrección dejaría stock negativo para el producto: "
                        + producto.getIdProducto());
            }
        }

        Documento correccion = new Documento();
        correccion.setTipoDocumento(TipoDocumento.CORRECCION);
        correccion.setUsuario(actor);
        correccion.setEstado((byte) 1);
        correccion.setDocumentoOrigen(original);
        correccion.setMotivoCorreccion(motivo.trim());
        correccion = documentoRepository.saveAndFlush(correccion);

        for (MovimientoInventario anterior : movimientos) {
            MovimientoInventario inverso = new MovimientoInventario();
            inverso.setDocumento(correccion);
            inverso.setProducto(anterior.getProducto());
            inverso.setCantidad(anterior.getCantidad());
            movimientoRepository.save(inverso);
        }
        movimientoRepository.flush();
        original.setEstado(estadoFinal);
        documentoRepository.saveAndFlush(original);
        entityManager.clear();
        return documentoRepository.findById(idDocumento).orElseThrow();
    }
}
