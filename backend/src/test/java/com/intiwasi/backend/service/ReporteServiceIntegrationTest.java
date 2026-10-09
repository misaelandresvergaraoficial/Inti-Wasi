package com.intiwasi.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ReporteServiceIntegrationTest {
    @Autowired ReporteService reportes;
    @Autowired DashboardService dashboard;

    @Test
    void consultasYResumenFuncionanConLaBaseConfigurada() {
        assertNotNull(dashboard.resumen());
        assertNotNull(reportes.inventario(null, PageRequest.of(0, 10)));
        assertNotNull(reportes.movimientos(null, null, null, null, null, PageRequest.of(0, 10)));
        assertNotNull(reportes.reposicion(null, PageRequest.of(0, 10)));
    }
}
