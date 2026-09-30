package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.inventario.DocumentoResponse;
import com.intiwasi.backend.entity.Documento;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
import com.intiwasi.backend.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentoConsultaService {
    private final DocumentoRepository documentoRepository;

    @Transactional(readOnly = true)
    public List<DocumentoResponse> listar() {
        return documentoRepository.findAllConRelaciones().stream().map(this::convertir).toList();
    }

    @Transactional(readOnly = true)
    public DocumentoResponse obtener(Integer id) {
        return convertir(documentoRepository.findByIdConRelaciones(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado con ID: " + id)));
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
