package com.intiwasi.backend.controller;

import com.intiwasi.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerHttpTest {

    private AuthenticationManager authenticationManager;
    private JwtUtil jwtUtil;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        jwtUtil = mock(JwtUtil.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authenticationManager, jwtUtil)).build();
    }

    @Test
    void credencialesIncorrectasResponden401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("credenciales inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo": "nadie@intiwasi.pe",
                                  "contrasena": "clave-incorrecta"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginCorrectoMantiene200() throws Exception {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "admin@intiwasi.pe",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrador"))
        );
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtil.generarToken("admin@intiwasi.pe")).thenReturn("token-de-prueba");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo": "admin@intiwasi.pe",
                                  "contrasena": "clave-valida"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-de-prueba"));
    }
}
