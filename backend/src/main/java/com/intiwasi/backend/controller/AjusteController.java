package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.inventario.AjusteRequest;
import com.intiwasi.backend.dto.inventario.AjusteResponse;
import com.intiwasi.backend.service.AjusteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ajustes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('Administrador')")
public class AjusteController {
    private final AjusteService ajusteService;

    @GetMapping
    public List<AjusteResponse> listar() { return ajusteService.listar(); }

    @GetMapping("/{id}")
    public AjusteResponse obtener(@PathVariable Integer id) { return ajusteService.obtener(id); }

    @PostMapping
    public ResponseEntity<AjusteResponse> registrar(@Valid @RequestBody AjusteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ajusteService.registrar(request));
    }
}
