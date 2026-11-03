package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.inventario.DocumentoResponse;
import com.intiwasi.backend.service.DocumentoConsultaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Administrador', 'Operador de Almacén')")
public class DocumentoController {
    private final DocumentoConsultaService documentoConsultaService;

    @GetMapping
    public Page<DocumentoResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Byte estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "desc") String sort) {
        return documentoConsultaService.listar(q, estado, desde, hasta, tipo, page, size, sort);
    }

    @GetMapping("/{id}/relacionados")
    public List<DocumentoResponse> relacionados(@PathVariable Integer id) {
        return documentoConsultaService.relacionados(id);
    }

    @GetMapping("/{id}")
    public DocumentoResponse obtener(@PathVariable Integer id) {
        return documentoConsultaService.obtener(id);
    }
}
