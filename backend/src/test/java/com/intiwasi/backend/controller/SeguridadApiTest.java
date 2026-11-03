package com.intiwasi.backend.controller;

import com.intiwasi.backend.config.JsonConfig;
import com.intiwasi.backend.config.SecurityConfig;
import com.intiwasi.backend.config.TimeConfig;
import com.intiwasi.backend.dto.Proveedor.ProveedorResponse;
import com.intiwasi.backend.repository.UsuarioRepository;
import com.intiwasi.backend.security.*;
import com.intiwasi.backend.service.EntradaService;
import com.intiwasi.backend.service.ProveedorService;
import com.intiwasi.backend.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class, EntradaController.class, ProveedorController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class, SessionRegistry.class,
        LoginAttemptLimiter.class, TimeConfig.class, JsonConfig.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class SeguridadApiTest {
    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwt;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserDetailsService userDetails;
    @MockitoBean PasswordEncoder passwordEncoder;
    @MockitoBean UsuarioService usuarios;
    @MockitoBean UsuarioRepository usuarioRepository;
    @MockitoBean EntradaService entradas;
    @MockitoBean ProveedorService proveedores;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        byte[] bytes = new byte[64]; new SecureRandom().nextBytes(bytes);
        String secret = Base64.getEncoder().encodeToString(bytes);
        registry.add("jwt.secreto", () -> secret);
    }

    @Test
    void logoutInvalidaElJwtSoloDeEsaSesion() throws Exception {
        String email = "operador.prueba@intiwasi.pe";
        when(userDetails.loadUserByUsername(email)).thenReturn(User.withUsername(email)
                .password("hash-de-prueba").roles("Operador de Almacén").build());
        String first = jwt.generarToken(email);
        String second = jwt.generarToken(email);
        mvc.perform(get("/api/entradas").header("Authorization", "Bearer " + first)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + first)).andExpect(status().isNoContent());
        mvc.perform(get("/api/entradas").header("Authorization", "Bearer " + first)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/entradas").header("Authorization", "Bearer " + second)).andExpect(status().isOk());
    }

    @Test
    void documentacionNoEsPublica() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
    }

    @Test
    void excesoDeIntentosDevuelve429SinRevelarLaCuenta() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(
                new org.springframework.security.authentication.BadCredentialsException("Rechazado"));
        String body = "{\"correo\":\"operador.inexistente@intiwasi.pe\",\"contrasena\":\"incorrecta\"}";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
    }

    @Test
    @WithMockUser(roles = "Operador de Almacén")
    void operadorNoAccedeADocumentacionNiAdministracion() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "Operador de Almacén")
    void proveedorInactivoNoSeExponePorIdentificador() throws Exception {
        ProveedorResponse response = new ProveedorResponse();
        response.setEstado((byte) 0);
        when(proveedores.obtenerPorId(1)).thenReturn(response);
        mvc.perform(get("/api/proveedores/1")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"idOrden\":1,\"numeroGuiaRemision\":\"GUIA-AND-001\",\"movimientos\":[{\"idProducto\":1,\"cantidad\":1.5}]}",
            "{\"idOrden\":1,\"numeroGuiaRemision\":\"GUIA-AND-001\",\"movimientos\":[{\"idProducto\":1,\"cantidad\":1}],\"stockActual\":999}"})
    @WithMockUser(roles = "Operador de Almacén")
    void datosFueraDeContratoNoLleganAlServicio(String body) throws Exception {
        mvc.perform(post("/api/entradas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(entradas);
    }

    @Test
    void corsSoloAceptaElOrigenConfigurado() throws Exception {
        mvc.perform(options("/api/entradas").header("Origin", "https://externo.test")
                .header("Access-Control-Request-Method", "GET")).andExpect(status().isForbidden());
        mvc.perform(options("/api/entradas").header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")).andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }
}
