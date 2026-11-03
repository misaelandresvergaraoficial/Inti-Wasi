package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.inventario.DocumentoResponse;
import com.intiwasi.backend.entity.Documento;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
import com.intiwasi.backend.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import jakarta.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
public class DocumentoConsultaService {
    private final DocumentoRepository documentoRepository;

    @Transactional(readOnly = true)
    public Page<DocumentoResponse> listar(String q, Byte estado, LocalDate desde, LocalDate hasta,
                                      String tipo, int page, int size, String sort) {
        var pageable = HistorialInventarioFiltro.pagina(page, size, sort, true, "idDocumento");
        HistorialInventarioFiltro.validar(q, estado, desde, hasta);
        var tipoDocumento = tipo == null || tipo.isBlank() ? null : TipoDocumento.desdeValor(tipo);
        return documentoRepository.findAll((root, query, cb) -> {
            var documento = root;
            var filtros = HistorialInventarioFiltro.condiciones(documento, cb, estado, desde, hasta, tipoDocumento);
            Predicate busqueda = HistorialInventarioFiltro.texto(cb, q, root.get("idDocumento"),
                    documento.get("usuario").get("nomUsuario"), root.get("motivoCorreccion"));
            if (q != null && q.trim().matches("\\d{1,10}")) {
                long numero = Long.parseLong(q.trim());
                if (numero <= Integer.MAX_VALUE) {
                    busqueda = cb.or(busqueda, cb.equal(root.join("documentoOrigen",
                            jakarta.persistence.criteria.JoinType.LEFT).get("idDocumento"), (int) numero));
                }
            }
            filtros.add(busqueda);
            return cb.and(filtros.toArray(Predicate[]::new));
        }, pageable).map(this::convertir);
    }

    @Transactional(readOnly = true)
    public DocumentoResponse obtener(Integer id) {
        return convertir(documentoRepository.findByIdConRelaciones(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado con ID: " + id)));
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponse> relacionados(Integer id) {
        obtener(id);
        return documentoRepository.findAllByDocumentoOrigen_IdDocumentoOrderByIdDocumentoAsc(id)
                .stream().map(this::convertir).toList();
    }

    private DocumentoResponse convertir(Documento d) {
        return DocumentoResponse.builder()
                .idDocumento(d.getIdDocumento()).tipoDocumento(d.getTipoDocumento())
                .fechaEmision(d.getFechaEmision()).estado(d.getEstado())
                .idDocumentoOrigen(d.getDocumentoOrigen() == null ? null : d.getDocumentoOrigen().getIdDocumento())
                .motivoCorreccion(d.getMotivoCorreccion())
                .idUsuario(d.getUsuario().getIdUsuario()).usuarioResponsable(d.getUsuario().getNomUsuario())
                .build();
    }
}
