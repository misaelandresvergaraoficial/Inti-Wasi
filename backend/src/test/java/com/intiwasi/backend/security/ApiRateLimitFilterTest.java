package com.intiwasi.backend.security;

import jakarta.servlet.Filter;
import com.intiwasi.backend.config.ApiLimitProperties;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiRateLimitFilterTest {
    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void limitaAnonimosPorIpSinConfiarEnForwarded() throws Exception {
        ApiRateLimitFilter limiter = new ApiRateLimitFilter(new MutableClock(), JsonMapper.builder().findAndAddModules().build(), limits(300));
        for (int i = 0; i < 60; i++) {
            var request = new MockHttpServletRequest("GET", "/api/auth/csrf");
            request.setRemoteAddr("192.0.2.1");
            limiter.doFilter(request, new MockHttpServletResponse(), (r, s) -> {});
        }
        var request = new MockHttpServletRequest("GET", "/api/auth/csrf");
        request.setRemoteAddr("192.0.2.1");
        request.addHeader("X-Forwarded-For", "198.51.100.9");
        var response = new MockHttpServletResponse();
        limiter.doFilter(request, response, (r, s) -> {});
        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
    }

    @Test
    void limiteDevuelve429YSeRecuperaAlTerminarLaVentana() throws Exception {
        MutableClock clock = new MutableClock();
        ApiRateLimitFilter limiter = new ApiRateLimitFilter(clock, JsonMapper.builder().findAndAddModules().build(), limits(2));
        Filter auth = (request, response, chain) -> {
            SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated("usuario@test", null, java.util.List.of()));
            try { chain.doFilter(request, response); } finally { SecurityContextHolder.clearContext(); }
        };
        var mvc = MockMvcBuilders.standaloneSetup(new Endpoint()).addFilters(auth, limiter).build();
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
        mvc.perform(get("/api/productos")).andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60")).andExpect(jsonPath("$.status").value(429));
        clock.now.set(clock.instant().plusSeconds(60));
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
    }

    @Test
    void exportacionesLimitadasLiberanPermisosAunqueFalleLaCadena() throws Exception {
        ApiRateLimitFilter limiter = new ApiRateLimitFilter(new MutableClock(), JsonMapper.builder().findAndAddModules().build(), limits(20));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("usuario@test", null, java.util.List.of()));
        var first = request();
        var second = request();
        var third = request();
        var blocked = new MockHttpServletResponse();
        assertThrows(ServletException.class, () -> limiter.doFilter(first, new MockHttpServletResponse(), (r1, s1) ->
                limiter.doFilter(second, new MockHttpServletResponse(), (r2, s2) -> {
                    limiter.doFilter(third, blocked, (r3, s3) -> fail("No debe ejecutar la exportación"));
                    throw new ServletException("fallo controlado");
                })));
        assertEquals(429, blocked.getStatus());
        assertEquals("1", blocked.getHeader("Retry-After"));
        var recovered = new MockHttpServletResponse();
        limiter.doFilter(request(), recovered, (r, s) -> {});
        assertEquals(200, recovered.getStatus());
    }

    private MockHttpServletRequest request() { return new MockHttpServletRequest("GET", "/api/reportes/inventario/exportar"); }

    @Test
    void reparteElLimiteGlobalEntreCuentasDistintasYReiniciaLaVentana() throws Exception {
        MutableClock clock = new MutableClock();
        var limits = new ApiLimitProperties(10, 60, 20, 4, 60, 120, 2, 2);
        var filter = new ApiRateLimitFilter(clock, JsonMapper.builder().findAndAddModules().build(), limits);
        for (String user : java.util.List.of("uno", "dos")) {
            authenticate(user);
            var response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest("GET", "/api/productos"), response, (r, s) -> {});
            assertEquals(200, response.getStatus());
        }
        authenticate("tres");
        var blocked = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/productos"), blocked, (r, s) -> fail("Límite global agotado"));
        assertEquals(429, blocked.getStatus());
        clock.now.set(clock.instant().plusSeconds(60));
        var recovered = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/productos"), recovered, (r, s) -> {});
        assertEquals(200, recovered.getStatus());
    }

    @Test
    void limitaAnonimosAunqueCambienDeIpSinConsumirTodaLaCuotaAutenticada() throws Exception {
        var limits = new ApiLimitProperties(10, 60, 20, 4, 60, 2, 10, 2);
        var filter = new ApiRateLimitFilter(new MutableClock(), JsonMapper.builder().findAndAddModules().build(), limits);
        for (int i = 0; i < 2; i++) {
            var request = new MockHttpServletRequest("GET", "/api/auth/csrf");
            request.setRemoteAddr("192.0.2." + i);
            filter.doFilter(request, new MockHttpServletResponse(), (r, s) -> {});
        }
        var request = new MockHttpServletRequest("GET", "/api/auth/csrf");
        request.setRemoteAddr("198.51.100.1");
        var blocked = new MockHttpServletResponse();
        filter.doFilter(request, blocked, (r, s) -> fail("La cuota anónima debe ser compartida entre IP"));
        assertEquals(429, blocked.getStatus());
        authenticate("trabajador");
        var worker = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/productos"), worker, (r, s) -> {});
        assertEquals(200, worker.getStatus());
    }

    @Test
    void reservaCapacidadParaAutenticadosYLiberaLaCuotaAnonimaTrasUnError() throws Exception {
        var limits = new ApiLimitProperties(10, 60, 20, 1, 60, 120, 1200, 2);
        var filter = new ApiRateLimitFilter(new MutableClock(), JsonMapper.builder().findAndAddModules().build(), limits);
        assertThrows(ServletException.class, () -> filter.doFilter(new MockHttpServletRequest("GET", "/api/auth/login"),
                new MockHttpServletResponse(), (r, s) -> {
                    var blocked = new MockHttpServletResponse();
                    filter.doFilter(new MockHttpServletRequest("GET", "/api/auth/csrf"), blocked,
                            (r2, s2) -> fail("No debe ocupar otra plaza anónima"));
                    assertEquals(429, blocked.getStatus());
                    authenticate("trabajador");
                    var worker = new MockHttpServletResponse();
                    try {
                        filter.doFilter(new MockHttpServletRequest("GET", "/api/productos"), worker, (r2, s2) -> {});
                        assertEquals(200, worker.getStatus());
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                    throw new ServletException("Fallo controlado");
                }));
        var recovered = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/auth/csrf"), recovered, (r, s) -> {});
        assertEquals(200, recovered.getStatus());
    }

    private void authenticate(String user) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, java.util.List.of()));
    }

    private ApiLimitProperties limits(int requests) { return new ApiLimitProperties(requests, 60, 20, 4, 60, 120, 1200, 2); }

    @RestController
    static class Endpoint {
        @GetMapping("/api/productos")
        String productos() { return "ok"; }
    }

    static class MutableClock extends Clock {
        final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    }
}
