package com.intiwasi.backend.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ApiConcurrencyFilterTest {
    @Test
    void admiteVeinteSolicitudesRechazaLaSiguienteYRecuperaCapacidad() throws Exception {
        var filter = new ApiConcurrencyFilter(Clock.systemUTC(), JsonMapper.builder().findAndAddModules().build(), 20);
        var entered = new CountDownLatch(20);
        var release = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = new ArrayList<Future<?>>();
            try {
                for (int i = 0; i < 20; i++) {
                    tasks.add(executor.submit(() -> {
                        filter.doFilter(request(), new MockHttpServletResponse(), (r, s) -> {
                            entered.countDown();
                            await(release);
                        });
                        return null;
                    }));
                }
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                var blocked = new MockHttpServletResponse();
                filter.doFilter(request(), blocked, (r, s) -> fail("No debe admitir la solicitud 21"));
                assertEquals(429, blocked.getStatus());
                assertEquals("1", blocked.getHeader("Retry-After"));
            } finally {
                release.countDown();
            }
            for (var task : tasks) task.get(5, TimeUnit.SECONDS);
        }
        var recovered = new MockHttpServletResponse();
        filter.doFilter(request(), recovered, (r, s) -> {});
        assertEquals(200, recovered.getStatus());
    }

    @Test
    void liberaLaPlazaAunqueFalleElProcesamiento() throws Exception {
        var filter = new ApiConcurrencyFilter(Clock.systemUTC(), JsonMapper.builder().findAndAddModules().build(), 1);
        assertThrows(ServletException.class, () -> filter.doFilter(request(), new MockHttpServletResponse(), (r, s) -> {
            throw new ServletException("Fallo controlado");
        }));
        var response = new MockHttpServletResponse();
        filter.doFilter(request(), response, (r, s) -> {});
        assertEquals(200, response.getStatus());
        assertThrows(IllegalArgumentException.class,
                () -> new ApiConcurrencyFilter(Clock.systemUTC(), JsonMapper.builder().build(), 0));
    }

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest("GET", "/api/productos");
    }

    private void await(CountDownLatch latch) throws ServletException {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new ServletException("No se liberó la prueba concurrente");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServletException(e);
        }
    }
}
