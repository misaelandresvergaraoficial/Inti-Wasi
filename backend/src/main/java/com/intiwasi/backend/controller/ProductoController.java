package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.Producto.ProductoRequest;
import com.intiwasi.backend.dto.Producto.ProductoResponse;
import com.intiwasi.backend.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")

public class ProductoController {

    private ProductoService productoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('Administrador', 'Operador de Almacén')")
    public ResponseEntity<List<ProductoResponse>> listarTodos() {
        return ResponseEntity.ok(productoService.listarTodosActivos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador', 'Operador de Almacén')")
    public ResponseEntity<ProductoResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(productoService.obtenerPorId(id));
    }

    @GetMapping("/stock-bajo")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<List<ProductoResponse>> listarStockBajo() {
        return ResponseEntity.ok(productoService.listarProductosStockBajo());
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<ProductoResponse> crearProducto(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse nuevoProducto = productoService.crearProducto(request);
        return new ResponseEntity<>(nuevoProducto, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<ProductoResponse> actualizarProducto(
            @PathVariable Integer id,
            @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizarProducto(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<Void> desactivarProducto(@PathVariable Integer id) {
        productoService.desactivarProducto(id);
        return ResponseEntity.noContent().build();
    }
}
