package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.inventario.SalidaRequest;
import com.intiwasi.backend.dto.inventario.SalidaResponse;
import com.intiwasi.backend.dto.inventario.CorreccionRequest;
import com.intiwasi.backend.service.SalidaService;
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

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/salidas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Administrador', 'Operador de Almacén')")
public class SalidaController {
    private final SalidaService salidaService;

    @GetMapping
    public Page<SalidaResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Byte estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "desc") String sort) {
        return salidaService.listar(q, estado, desde, hasta, tipo, page, size, sort);
    }

    @GetMapping("/{id}")
    public SalidaResponse obtener(@PathVariable Integer id) { return salidaService.obtener(id); }

    @GetMapping("/documento/{idDocumento}")
    public SalidaResponse porDocumento(@PathVariable Integer idDocumento) {
        return salidaService.porDocumento(idDocumento);
    }

    @PostMapping
    public ResponseEntity<SalidaResponse> registrar(@Valid @RequestBody SalidaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salidaService.registrar(request));
    }

    @PutMapping("/{id}")
    public SalidaResponse actualizar(@PathVariable Integer id, @Valid @RequestBody SalidaRequest request) {
        return salidaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anular(@PathVariable Integer id, @Valid @RequestBody CorreccionRequest request) {
        salidaService.anular(id, request.getMotivo());
        return ResponseEntity.noContent().build();
    }
}
