package com.intiwasi.backend.security;

import jakarta.servlet.Filter;
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
    void limiteDevuelve429YSeRecuperaAlTerminarLaVentana() throws Exception {
        MutableClock clock = new MutableClock();
        ApiRateLimitFilter limiter = new ApiRateLimitFilter(clock, JsonMapper.builder().findAndAddModules().build(), 2, 60, 2);
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
        ApiRateLimitFilter limiter = new ApiRateLimitFilter(new MutableClock(), JsonMapper.builder().findAndAddModules().build(), 20, 60, 2);
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
