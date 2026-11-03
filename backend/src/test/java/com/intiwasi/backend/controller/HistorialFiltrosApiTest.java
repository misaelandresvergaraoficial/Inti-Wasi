package com.intiwasi.backend.controller;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "Administrador")
class HistorialFiltrosApiTest {
    @Autowired
    private MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"documentos", "salidas", "ajustes"})
    void rechazaRangoInvertidoCon400AntesDeConsultarElRepositorio(String recurso) throws Exception {
        mvc.perform(get("/api/" + recurso)
                        .param("desde", "2026-12-31")
                        .param("hasta", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("fecha inicial")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"documentos", "salidas", "ajustes"})
    void rechazaEstadoFueraDeContratoCon400(String recurso) throws Exception {
        mvc.perform(get("/api/" + recurso).param("estado", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("estado")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"documentos", "salidas", "ajustes"})
    void rechazaBusquedaDe256CaracteresCon400(String recurso) throws Exception {
        mvc.perform(get("/api/" + recurso).param("q", "a".repeat(256)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("255")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"documentos", "salidas", "ajustes"})
    void aceptaFronterasValidasSinCambiarLosFiltros(String recurso) throws Exception {
        mvc.perform(get("/api/" + recurso)
                        .param("q", "a".repeat(255))
                        .param("estado", "2")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists());
    }
}
