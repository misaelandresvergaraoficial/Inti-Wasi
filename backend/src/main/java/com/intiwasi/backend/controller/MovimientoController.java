package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.inventario.MovimientoConsultaResponse;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import com.intiwasi.backend.service.MovimientoConsultaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('Administrador')")
public class MovimientoController {
    private final MovimientoConsultaService movimientoService;

    @GetMapping
    public Page<MovimientoConsultaResponse> consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFinal,
            @RequestParam(required = false) Integer idProducto,
            @RequestParam(required = false) String tipoDocumento,
            @RequestParam(required = false) Integer idUsuario,
            Pageable pageable) {
        TipoDocumento tipo = tipoDocumento == null ? null : TipoDocumento.desdeValor(tipoDocumento);
        return movimientoService.consultar(fechaInicial, fechaFinal, idProducto, tipo, idUsuario, pageable);
    }

    @GetMapping("/{id}")
    public MovimientoConsultaResponse obtener(@PathVariable Integer id) {
        return movimientoService.obtener(id);
    }
}
