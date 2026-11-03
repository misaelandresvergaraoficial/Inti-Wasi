package com.intiwasi.backend.service;

import com.intiwasi.backend.repository.KardexRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoConsultaServiceTest {
    @Mock KardexRepository kardex;
    @InjectMocks MovimientoConsultaService service;

    @Test
    void conservaIdentidadesDistintasDeResponsablesHomonimosIncluidoInactivo() {
        when(kardex.listarResponsables()).thenReturn(List.of(
                new Object[]{7, "Alex Quispe", 1, "alex.uno@intiwasi.test"},
                new Object[]{8, "Alex Quispe", 0, "alex.dos@intiwasi.test"}));
        var opciones = service.responsables();
        assertEquals(2, opciones.size());
        assertEquals(7, opciones.get(0).idUsuario());
        assertEquals(8, opciones.get(1).idUsuario());
        assertEquals("alex.uno@intiwasi.test", opciones.get(0).correoUsuario());
        assertEquals("alex.dos@intiwasi.test", opciones.get(1).correoUsuario());
        assertEquals(0, opciones.get(1).estado());
    }
}
