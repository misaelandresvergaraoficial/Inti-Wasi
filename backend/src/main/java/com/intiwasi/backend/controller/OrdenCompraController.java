package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.OrdenCompra.OrdenCompraRequest;
import com.intiwasi.backend.dto.OrdenCompra.OrdenCompraResponse;
import com.intiwasi.backend.service.OrdenCompraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/ordenes-compra")
@RequiredArgsConstructor
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;

    @GetMapping
    @PreAuthorize("hasAnyRole('Administrador', 'Operador de Almacén')")
    public ResponseEntity<Page<OrdenCompraResponse>> listarTodas(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "false") boolean admisibles) {
        return ResponseEntity.ok(ordenCompraService.consultar(page, size, q, estado, desde, hasta, sort, admisibles));
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<Void> cancelar(@PathVariable Integer id) {
        ordenCompraService.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
