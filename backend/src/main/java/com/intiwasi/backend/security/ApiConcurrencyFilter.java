package com.intiwasi.backend.security;

import com.intiwasi.backend.dto.Comun.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.Semaphore;

public class ApiConcurrencyFilter extends OncePerRequestFilter {
    private final Semaphore requests;
    private final Clock clock;
    private final ObjectMapper mapper;

    public ApiConcurrencyFilter(Clock clock, ObjectMapper mapper, int concurrentRequests) {
        if (concurrentRequests <= 0) throw new IllegalArgumentException("El límite simultáneo debe ser positivo");
        this.requests = new Semaphore(concurrentRequests);
        this.clock = clock;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }
        if (!requests.tryAcquire()) {
            response.setStatus(429);
            response.setHeader("Retry-After", "1");
            response.setHeader("Cache-Control", "no-store");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), new ErrorResponse(LocalDateTime.now(clock), 429,
                    "Too Many Requests", "El servicio está ocupado. Intente nuevamente en unos segundos."));
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            requests.release();
        }
    }
}
