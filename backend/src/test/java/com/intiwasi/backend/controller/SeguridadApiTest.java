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
import jakarta.servlet.http.Cookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.intiwasi.backend.dto.Usuario.UsuarioResponse;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class, EntradaController.class, ProveedorController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class, SessionRegistry.class,
        LoginAttemptLimiter.class, SessionCookie.class, TimeConfig.class, JsonConfig.class,
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
        registry.add("app.auth.cookie-secure", () -> true);
    }

    private RequestPostProcessor csrfReal() {
        return request -> {
            try {
                Cookie csrf = mvc.perform(get("/api/auth/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN");
                java.util.List<Cookie> cookies = new java.util.ArrayList<>();
                if (request.getCookies() != null) cookies.addAll(java.util.List.of(request.getCookies()));
                cookies.add(csrf);
                request.setCookies(cookies.toArray(Cookie[]::new));
                request.addHeader("X-XSRF-TOKEN", csrf.getValue());
                return request;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
    }

    @Test
    void logoutInvalidaElJwtSoloDeEsaSesion() throws Exception {
        String email = "operador.prueba@intiwasi.pe";
        when(userDetails.loadUserByUsername(email)).thenReturn(User.withUsername(email)
                .password("hash-de-prueba").roles("Operador de Almacén").build());
        String first = jwt.generarToken(email);
        String second = jwt.generarToken(email);
        mvc.perform(get("/api/entradas").cookie(new Cookie(SessionCookie.NAME, first))).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").cookie(new Cookie(SessionCookie.NAME, first)).with(csrfReal())).andExpect(status().isNoContent());
        mvc.perform(get("/api/entradas").cookie(new Cookie(SessionCookie.NAME, first))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/entradas").cookie(new Cookie(SessionCookie.NAME, second))).andExpect(status().isOk());
    }

    @Test
    void cookiesYCsrfProtegenElFlujoDeSesion() throws Exception {
        String email = "sesion@intiwasi.test";
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(email, null, java.util.List.of()));
        when(userDetails.loadUserByUsername(email)).thenReturn(User.withUsername(email)
                .password("hash").roles("Operador de Almacén").build());
        UsuarioResponse usuario = new UsuarioResponse();
        usuario.setCorreo(email);
        when(usuarios.obtenerActual(email)).thenReturn(usuario);
        String body = "{\"correo\":\"sesion@intiwasi.test\",\"contrasena\":\"clave-prueba\"}";
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        var csrfResult = mvc.perform(get("/api/auth/csrf")).andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("XSRF-TOKEN="),
                        org.hamcrest.Matchers.containsString("Secure"))))
                .andReturn();
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);
        assertEquals("Strict", csrfCookie.getAttribute("SameSite"));
        assertFalse(csrfCookie.isHttpOnly());
        mvc.perform(post("/api/auth/login").cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", "incorrecto")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden()).andReturn();
        var result = mvc.perform(post("/api/auth/login").cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuario.correo").value(email))
                .andExpect(jsonPath("$.expiresAt").isNumber()).andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(header().stringValues("Set-Cookie", org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("intiwasi.session="),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("Secure"),
                        org.hamcrest.Matchers.containsString("SameSite=Strict"),
                        org.hamcrest.Matchers.containsString("Path=/")))))
                .andReturn();
        Cookie session = result.getResponse().getCookie(SessionCookie.NAME);
        assertNotNull(session);
        assertEquals(0, result.getResponse().getCookie("XSRF-TOKEN").getMaxAge());
        csrfCookie = mvc.perform(get("/api/auth/csrf").cookie(session)).andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);
        var me = mvc.perform(get("/api/auth/me").cookie(session, csrfCookie)).andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.correo").value(email)).andExpect(jsonPath("$.expiresAt").isNumber())
                .andReturn();
        Cookie csrfAfterMe = me.getResponse().getCookie("XSRF-TOKEN");
        assertTrue(csrfAfterMe == null || csrfAfterMe.getMaxAge() != 0, "La consulta de sesión no debe borrar CSRF");
        mvc.perform(post("/api/entradas").cookie(session, csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idOrden\":1,\"numeroGuiaRemision\":\"GUIA-AND-001\",\"movimientos\":[{\"idProducto\":1,\"cantidad\":1}]}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + session.getValue()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").cookie(session, csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isUnauthorized());
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
            mvc.perform(post("/api/auth/login").with(csrfReal()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").with(csrfReal()).contentType(MediaType.APPLICATION_JSON).content(body))
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
        mvc.perform(post("/api/entradas").with(csrfReal()).contentType(MediaType.APPLICATION_JSON).content(body))
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
