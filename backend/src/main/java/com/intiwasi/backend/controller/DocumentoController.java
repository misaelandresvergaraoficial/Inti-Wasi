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

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('Administrador')")
public class DocumentoController {
    private final DocumentoConsultaService documentoConsultaService;

    @GetMapping
    public List<DocumentoResponse> listar() {
        return documentoConsultaService.listar();
    }

    @GetMapping("/{id}")
    public DocumentoResponse obtener(@PathVariable Integer id) {
        return documentoConsultaService.obtener(id);
    }
}
