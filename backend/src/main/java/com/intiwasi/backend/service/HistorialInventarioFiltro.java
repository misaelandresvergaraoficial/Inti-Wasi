package com.intiwasi.backend.service;

import com.intiwasi.backend.entity.Documento;
import com.intiwasi.backend.entity.enums.TipoDocumento;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class HistorialInventarioFiltro {
    private HistorialInventarioFiltro() {}

    static Pageable pagina(int page, int size, String sort, boolean documento, String id) {
        if (page < 0 || size < 1 || size > 200) {
            throw new IllegalArgumentException("La página debe ser no negativa y el tamaño debe estar entre 1 y 200");
        }
        if (!List.of("asc", "desc").contains(sort)) {
            throw new IllegalArgumentException("El orden debe ser asc o desc");
        }
        Sort.Direction direction = Sort.Direction.fromString(sort);
        return PageRequest.of(page, size, Sort.by(direction,
                documento ? "fechaEmision" : "documento.fechaEmision", id));
    }

    static void validar(String q, Byte estado, LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }
        if (estado != null && (estado < 0 || estado > 2)) {
            throw new IllegalArgumentException("El estado debe ser 0, 1 o 2");
        }
        if (q != null && q.length() > 255) {
            throw new IllegalArgumentException("La búsqueda admite hasta 255 caracteres");
        }
    }

    static List<Predicate> condiciones(Path<Documento> documento, CriteriaBuilder cb,
                                       Byte estado, LocalDate desde, LocalDate hasta, TipoDocumento tipo) {
        List<Predicate> filtros = new ArrayList<>();
        if (estado != null) filtros.add(cb.equal(documento.get("estado"), estado));
        if (desde != null) filtros.add(cb.greaterThanOrEqualTo(documento.get("fechaEmision"), desde.atStartOfDay()));
        if (hasta != null) filtros.add(cb.lessThan(documento.get("fechaEmision"), hasta.plusDays(1).atStartOfDay()));
        if (tipo != null) filtros.add(cb.equal(documento.get("tipoDocumento"), tipo));
        return filtros;
    }

    static Predicate texto(CriteriaBuilder cb, String q, Path<?> id, Path<?> responsable, Path<?>... otros) {
        if (q == null || q.isBlank()) return cb.conjunction();
        String valor = q.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
        List<Predicate> opciones = new ArrayList<>();
        if (q.trim().matches("\\d{1,9}")) opciones.add(cb.equal(id, Integer.valueOf(q.trim())));
        opciones.add(cb.like(cb.lower(responsable.as(String.class)), "%" + valor + "%", '!'));
        for (Path<?> campo : otros) opciones.add(cb.like(cb.lower(campo.as(String.class)), "%" + valor + "%", '!'));
        return cb.or(opciones.toArray(Predicate[]::new));
    }
}
