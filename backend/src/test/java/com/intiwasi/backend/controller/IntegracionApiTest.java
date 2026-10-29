package com.intiwasi.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IntegracionApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void loginConCredencialesInvalidasDevuelve401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"admin.misael@intiwasi.pe\",\"contrasena\":\"clave-incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void productosSinSesionDevuelve401() throws Exception {
        mvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "Administrador")
    void productosListaConAdministrador() throws Exception {
        mvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "Administrador")
    void movimientosPaginadosDevuelveMetadatos() throws Exception {
        mvc.perform(get("/api/movimientos")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "fechaEmision,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists());
    }

    @Test
    @WithMockUser(roles = "Operador de Almacén")
    void usuariosConOperadorDevuelve403() throws Exception {
        mvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    void documentacionOpenApiDisponible() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").exists())
                .andExpect(jsonPath("$.paths").exists());
    }
}
