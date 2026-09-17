package com.intiwasi.backend.controller;

import com.intiwasi.backend.entity.Proveedor;
import com.intiwasi.backend.service.ProveedorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor 
@RestController
@RequestMapping("/api/proveedores")
@CrossOrigin(origins = "*")
public class ProveedorController {

    private ProveedorService proveedorService;

    @GetMapping
    public ResponseEntity<List<Proveedor>> listarActivos() {
        return new ResponseEntity<>(proveedorService.listarActivos(),HttpStatus.OK);
    }

   @GetMapping("/{id}")
    public ResponseEntity<Proveedor> obtenerPorId(@PathVariable Integer id) {
        return new ResponseEntity<>(proveedorService.obtenerPorId(id), HttpStatus.OK);
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