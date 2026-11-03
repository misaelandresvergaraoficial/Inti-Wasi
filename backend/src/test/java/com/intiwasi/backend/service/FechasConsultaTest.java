package com.intiwasi.backend.service;

import com.intiwasi.backend.repository.KardexRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FechasConsultaTest {
    @Test
    void consultaDiaCompletoConLimiteFinalExclusivoSinConvertirFechaCivilAUTC() {
        var repository = mock(KardexRepository.class);
        var service = new MovimientoConsultaService(repository);
        var page = PageRequest.of(0, 20);
        when(repository.buscar(any(), any(), isNull(), isNull(), isNull(), eq(page)))
                .thenReturn(Page.empty(page));
        var lima = ZoneId.of("America/Lima");
        var midnight = LocalDateTime.of(2026, 11, 2, 0, 0).atZone(lima);
        assertEquals("2026-11-02T05:00:00Z", midnight.toInstant().toString());
        var date = midnight.toLocalDate();
        service.consultar(date, date, null, null, null, page);
        verify(repository).buscar(date.atStartOfDay(), date.plusDays(1).atStartOfDay(),
                null, null, null, page);
        var old = LocalDate.of(2020, 1, 31);
        service.consultar(old, old, null, null, null, page);
        verify(repository).buscar(old.atStartOfDay(), LocalDate.of(2020, 2, 1).atStartOfDay(),
                null, null, null, page);
    }
}
