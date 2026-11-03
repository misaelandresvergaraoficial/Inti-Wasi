package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.inventario.AjusteRequest;
import com.intiwasi.backend.dto.inventario.AjusteResponse;
import com.intiwasi.backend.dto.inventario.CorreccionRequest;
import com.intiwasi.backend.service.AjusteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/ajustes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('Administrador')")
public class AjusteController {
    private final AjusteService ajusteService;

    @GetMapping
    public Page<AjusteResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Byte estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "desc") String sort) {
        return ajusteService.listar(q, estado, desde, hasta, tipo, page, size, sort);
    }

    @GetMapping("/{id}")
    public AjusteResponse obtener(@PathVariable Integer id) { return ajusteService.obtener(id); }

    @GetMapping("/documento/{idDocumento}")
    public AjusteResponse porDocumento(@PathVariable Integer idDocumento) {
        return ajusteService.porDocumento(idDocumento);
    }

    @PostMapping
    public ResponseEntity<AjusteResponse> registrar(@Valid @RequestBody AjusteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ajusteService.registrar(request));
    }

    @PutMapping("/{id}")
    public AjusteResponse actualizar(@PathVariable Integer id, @Valid @RequestBody AjusteRequest request) {
        return ajusteService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anular(@PathVariable Integer id, @Valid @RequestBody CorreccionRequest request) {
        ajusteService.anular(id, request.getMotivo());
        return ResponseEntity.noContent().build();
    }
}
