package com.intiwasi.backend.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void dtoInvalidoResponde400() throws Exception {
        mockMvc.perform(post("/pruebas/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("nombre")));
    }

    @Test
    void jsonMalFormadoResponde400() throws Exception {
        mockMvc.perform(post("/pruebas/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El cuerpo de la solicitud contiene un JSON inválido"));
    }

    @Test
    void parametroDeRutaInvalidoResponde400() throws Exception {
        mockMvc.perform(get("/pruebas/tipo/no-es-numero"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void recursoInexistenteResponde404() throws Exception {
        mockMvc.perform(get("/pruebas/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void conflictoFuncionalEIntegridadResponden409SinExponerSql() throws Exception {
        mockMvc.perform(get("/pruebas/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(get("/pruebas/integridad"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La operación entra en conflicto con datos existentes"))
                .andExpect(content().string(not(containsString("INSERT INTO"))));
    }

    @Test
    void reglaDeNegocioResponde422() throws Exception {
        mockMvc.perform(get("/pruebas/regla"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void errorInesperadoResponde500ConMensajeSeguro() throws Exception {
        mockMvc.perform(get("/pruebas/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Se produjo un error interno en el servidor"))
                .andExpect(content().string(not(containsString("password-secreta"))))
                .andExpect(content().string(not(containsString("stackTrace"))));
    }

    @RestController
    @RequestMapping("/pruebas")
    static class ControladorDePrueba {

        @PostMapping("/validacion")
        void validar(@Valid @RequestBody PeticionDePrueba request) {
        }

        @GetMapping("/tipo/{id}")
        void validarTipo(@PathVariable Integer id) {
        }

        @GetMapping("/no-encontrado")
        void noEncontrado() {
            throw new RecursoNoEncontradoException("Recurso no encontrado");
        }

        @GetMapping("/conflicto")
        void conflicto() {
            throw new ConflictoException("Registro duplicado");
        }

        @GetMapping("/integridad")
        void integridad() {
            throw new DataIntegrityViolationException("INSERT INTO tabla_interna");
        }

        @GetMapping("/regla")
        void regla() {
            throw new ReglaNegocioException("La operación no está permitida");
        }

        @GetMapping("/inesperado")
        void inesperado() {
            throw new RuntimeException("password-secreta");
        }
    }

    static class PeticionDePrueba {

        @NotBlank(message = "El nombre es obligatorio")
        private String nombre;

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }
    }
}
