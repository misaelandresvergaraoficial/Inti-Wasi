package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.Proveedor.ProveedorRequest;
import com.intiwasi.backend.dto.Proveedor.ProveedorResponse;
import com.intiwasi.backend.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('Administrador', 'Operador de Almacén')")
    public ResponseEntity<List<ProveedorResponse>> listarActivos() {
        return ResponseEntity.ok(proveedorService.listarActivos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'Operador de Almacén')")
    public ResponseEntity<ProveedorResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(proveedorService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<ProveedorResponse> registrar(@Valid @RequestBody ProveedorRequest request) {
        ProveedorResponse nuevoProveedor = proveedorService.registrar(request);
        return new ResponseEntity<>(nuevoProveedor, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<ProveedorResponse> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ProveedorRequest request) {
        return ResponseEntity.ok(proveedorService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
        proveedorService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
