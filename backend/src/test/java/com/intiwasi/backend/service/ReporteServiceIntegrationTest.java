package com.intiwasi.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ReporteServiceIntegrationTest {
    @Autowired ReporteService reportes;
    @Autowired DashboardService dashboard;

    @Test
    void consultasYResumenFuncionanConLaBaseConfigurada() {
        var resumen = dashboard.resumen();
        assertNotNull(resumen);
        assertEquals(30, resumen.getMovimientosUltimos30Dias().size());
        assertEquals(java.time.LocalDate.now(), resumen.getMovimientosUltimos30Dias().getLast().fecha());
        assertNotNull(reportes.inventario(null, PageRequest.of(0, 10)));
        assertNotNull(reportes.movimientos(null, null, null, null, null, PageRequest.of(0, 10)));
        assertNotNull(reportes.reposicion(null, PageRequest.of(0, 10)));
    }
}
