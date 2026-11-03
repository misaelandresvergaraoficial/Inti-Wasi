package com.intiwasi.backend.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HistorialInventarioFiltroTest {
    @Test
    void aceptaDoscientosYRechazaLimitesInvalidosSinTruncarlos() {
        var page = HistorialInventarioFiltro.pagina(3, 200, "asc", true, "idDocumento");
        assertEquals(200, page.getPageSize());
        assertEquals(600, page.getOffset());
        assertTrue(page.getSort().getOrderFor("fechaEmision").isAscending());
        assertThrows(IllegalArgumentException.class, () -> HistorialInventarioFiltro.pagina(0, 201, "asc", true, "idDocumento"));
        assertThrows(IllegalArgumentException.class, () -> HistorialInventarioFiltro.pagina(-1, 20, "asc", true, "idDocumento"));
        assertThrows(IllegalArgumentException.class, () -> HistorialInventarioFiltro.pagina(0, 20, "arbitrario", true, "idDocumento"));
    }
}
