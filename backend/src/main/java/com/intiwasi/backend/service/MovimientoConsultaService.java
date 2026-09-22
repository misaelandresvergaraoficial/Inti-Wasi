package com.intiwasi.backend.service;

import com.intiwasi.backend.dto.inventario.MovimientoConsultaResponse;
import com.intiwasi.backend.entity.KardexMovimiento;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import com.intiwasi.backend.exception.RecursoNoEncontradoException;
import com.intiwasi.backend.repository.KardexRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MovimientoConsultaService {
    private final KardexRepository kardexRepository;

    @Transactional(readOnly = true)
    public Page<MovimientoConsultaResponse> consultar(
            LocalDate fechaInicial,
            LocalDate fechaFinal,
            Integer idProducto,
            TipoDocumento tipoDocumento,
            Integer idUsuario,
            Pageable pageable) {
        validarRango(fechaInicial, fechaFinal);
        LocalDateTime inicio = fechaInicial == null ? null : fechaInicial.atStartOfDay();
        LocalDateTime finExclusivo = fechaFinal == null ? null : fechaFinal.plusDays(1).atStartOfDay();
        return kardexRepository.buscar(inicio, finExclusivo, idProducto, tipoDocumento, idUsuario, pageable)
                .map(this::convertir);
    }

    @Transactional(readOnly = true)
    public MovimientoConsultaResponse obtener(Integer id) {
        return kardexRepository.findById(id).map(this::convertir)
                .orElseThrow(() -> new RecursoNoEncontradoException("Movimiento no encontrado con ID: " + id));
    }

    private void validarRango(LocalDate inicio, LocalDate fin) {
        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }
    }

    private MovimientoConsultaResponse convertir(KardexMovimiento k) {
        return MovimientoConsultaResponse.builder()
                .idMovimiento(k.getIdMovimiento()).idDocumento(k.getIdDocumento())
                .tipoDocumento(k.getTipoDocumento()).fechaEmision(k.getFechaEmision())
                .idUsuario(k.getIdUsuario()).usuarioResponsable(k.getUsuarioResponsable())
                .idProducto(k.getIdProducto()).sku(k.getSku()).nomProducto(k.getNomProducto())
                .nomCategoria(k.getNomCategoria()).cantidad(k.getCantidad())
                .cantidadConSigno(k.getCantidadConSigno()).motivo(k.getMotivo())
                .idOrden(k.getIdOrden()).documentoRef(k.getDocumentoRef()).build();
    }
}
