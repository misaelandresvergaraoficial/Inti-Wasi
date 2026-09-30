package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.inventario.EntradaRequest;
import com.intiwasi.backend.dto.inventario.EntradaResponse;
import com.intiwasi.backend.dto.inventario.CorreccionRequest;
import com.intiwasi.backend.service.EntradaService;
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

@RestController
@RequestMapping("/api/entradas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Administrador', 'Operador de Almacén')")
public class EntradaController {
    private final EntradaService entradaService;

    @GetMapping
    public List<EntradaResponse> listar() { return entradaService.listar(); }

    @GetMapping("/{id}")
    public EntradaResponse obtener(@PathVariable Integer id) { return entradaService.obtener(id); }

    @PostMapping
    public ResponseEntity<EntradaResponse> registrar(@Valid @RequestBody EntradaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(entradaService.registrar(request));
    }

    @PutMapping("/{id}")
    public EntradaResponse actualizar(@PathVariable Integer id, @Valid @RequestBody EntradaRequest request) {
        return entradaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anular(@PathVariable Integer id, @Valid @RequestBody CorreccionRequest request) {
        entradaService.anular(id, request.getMotivo());
        return ResponseEntity.noContent().build();
    }
}
