package com.intiwasi.backend.controller;

import com.intiwasi.backend.entity.Proveedor;
import com.intiwasi.backend.service.ProveedorService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

    @Autowired
    private ProveedorService proveedorService;

    @GetMapping
    public ResponseEntity<List<Proveedor>> listarActivos() {
        return ResponseEntity.ok(proveedorService.listarActivos());
    }

   @GetMapping("/{id}")
    public ResponseEntity<Proveedor> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(proveedorService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<Proveedor> registrar(@Valid @RequestBody Proveedor proveedor) {
        return new ResponseEntity<>(proveedorService.registrar(proveedor), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Proveedor> actualizar(@PathVariable Integer id, @Valid @RequestBody Proveedor proveedor) {
        return new ResponseEntity<>(proveedorService.actualizar(id, proveedor), HttpStatus.OK);
    }

   @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
        proveedorService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}