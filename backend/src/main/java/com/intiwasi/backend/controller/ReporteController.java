package com.intiwasi.backend.controller;

import com.intiwasi.backend.dto.inventario.MovimientoConsultaResponse;
import com.intiwasi.backend.dto.inventario.StockBajoResponse;
import com.intiwasi.backend.dto.reporte.InventarioActualResponse;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import com.intiwasi.backend.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('Administrador')")
public class ReporteController {
    private final ReporteService reporteService;

    @GetMapping("/inventario")
    public Page<InventarioActualResponse> inventario(@RequestParam(required = false) Integer idProducto, Pageable pageable) {
        return reporteService.inventario(idProducto, pageable);
    }

    @GetMapping("/movimientos")
    public Page<MovimientoConsultaResponse> movimientos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFinal,
            @RequestParam(required = false) Integer idProducto,
            @RequestParam(required = false) String tipoMovimiento,
            @RequestParam(required = false) Integer idUsuario,
            Pageable pageable) {
        return reporteService.movimientos(fechaInicial, fechaFinal, idProducto, tipo(tipoMovimiento), idUsuario, pageable);
    }

    @GetMapping("/reposicion")
    public Page<StockBajoResponse> reposicion(@RequestParam(required = false) Integer idProducto, Pageable pageable) {
        return reporteService.reposicion(idProducto, pageable);
    }

    @GetMapping("/{reporte}/exportar")
    public ResponseEntity<byte[]> exportar(
            @PathVariable String reporte,
            @RequestParam String formato,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFinal,
            @RequestParam(required = false) Integer idProducto,
            @RequestParam(required = false) String tipoMovimiento,
            @RequestParam(required = false) Integer idUsuario) {
        byte[] contenido = reporteService.exportar(reporte, formato, fechaInicial, fechaFinal, idProducto,
                tipo(tipoMovimiento), idUsuario);
        boolean pdf = "pdf".equalsIgnoreCase(formato);
        String extension = pdf ? "pdf" : "xls";
        MediaType mediaType = pdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType("application/vnd.ms-excel");
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(reporte + "." + extension, StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString()).body(contenido);
    }

    private TipoDocumento tipo(String valor) {
        return valor == null || valor.isBlank() ? null : TipoDocumento.desdeValor(valor);
    }
}
