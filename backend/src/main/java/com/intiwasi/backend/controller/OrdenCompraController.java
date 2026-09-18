package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.OrdenCompra.OrdenCompraRequest;
import com.intiwasi.backend.dto.OrdenCompra.OrdenCompraResponse;
import com.intiwasi.backend.service.OrdenCompraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ordenes-compra")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;

    @GetMapping
    @PreAuthorize("hasAnyRole('Administrador', 'Operador de Almacén')")
    public ResponseEntity<List<OrdenCompraResponse>> listarTodas() {
        return ResponseEntity.ok(ordenCompraService.listarTodas());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'Operador de Almacén')")
    public ResponseEntity<OrdenCompraResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(ordenCompraService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<OrdenCompraResponse> crear(@Valid @RequestBody OrdenCompraRequest request) {
        return new ResponseEntity<>(ordenCompraService.crear(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<OrdenCompraResponse> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody OrdenCompraRequest request) {
        return ResponseEntity.ok(ordenCompraService.actualizar(id, request));
    }
}
